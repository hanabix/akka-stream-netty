/*
 *  Copyright 2019 Zhong Lunfu
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package zhongl.stream.netty

import java.net.SocketAddress

import akka.actor.{ActorSystem, CoordinatedShutdown}
import akka.stream._
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.util.ByteString
import io.netty.bootstrap._
import io.netty.channel._
import io.netty.channel.socket.DuplexChannel
import Netty._
import akka.dispatch.Futures

import scala.concurrent.{Channel => _, _}
import scala.concurrent.duration.Duration

abstract class Transport[+C <: DuplexChannel](implicit system: ActorSystem) {

  implicit private val ex = system.dispatcher
  implicit private val asFuture: ChannelFuture => Future[Channel] = { cf =>
    val p = Futures.promise[Channel]()
    cf.addListener({ f: ChannelFuture =>
      if (f.isSuccess) p.trySuccess(f.channel()) else p.tryFailure(f.cause())
    })
    p.future
  }

  private lazy val lazyGroup = { // ensure event group is initialized only once and could be shutdown gracefully
    val g = group
    CoordinatedShutdown(system).addJvmShutdownHook(g.shutdownGracefully())
    g
  }

  final def outgoingConnection(
      remoteAddress: SocketAddress,
      localAddress: Option[SocketAddress],
      halfClose: Boolean,
      connectTimeout: Duration
  ): Flow[ByteString, ByteString, Future[OutgoingConnection]] = {
    @inline def integer(d: Duration): Integer = if (d.isFinite()) d.toMillis.asInstanceOf[Integer] else Integer.MAX_VALUE

    Flow
      .fromSinkAndSourceMat(
        Sink.queue[ByteString](),
        Source.queue[ByteString](1, OverflowStrategy.fail)
      )(Keep.both)
      .mapMaterializedValue {
        case (sinkQ, sourceQ) =>
          val bootstrap = new Bootstrap()

          localAddress.foreach(bootstrap.localAddress)

          bootstrap
            .group(lazyGroup)
            .channel(channelClass)
            // disable auto read to enable back-pressure of stream.
            .option[java.lang.Boolean](ChannelOption.AUTO_READ, false)
            .option[java.lang.Boolean](ChannelOption.ALLOW_HALF_CLOSURE, halfClose)
            .option[Integer](ChannelOption.CONNECT_TIMEOUT_MILLIS, integer(connectTimeout))
            .handler(new ChannelInitializer[C] {
              override def initChannel(ch: C): Unit = {
                ch.pipeline()
                  .addLast(new ByteToByteStringCodec)
                  .addLast(new AkkaStreamChannelHandler(sourceQ, sinkQ)(system.log))
              }
            })
            .connect(remoteAddress)
            .map(ch => OutgoingConnection(ch.localAddress(), ch.remoteAddress()))
      }
  }

  final def bind(
      localAddress: SocketAddress,
      backlog: Int,
      halfClose: Boolean
  ): Source[IncomingConnection, Future[ServerBinding]] = {

    implicit val mat = ActorMaterializer()

    val (incomingQ, incomingS) = Source.queue[IncomingConnection](1, OverflowStrategy.fail).preMaterialize()

    val f = new ServerBootstrap()
      .group(lazyGroup)
      .channel(serverChannelClass)
      .option[Integer](ChannelOption.SO_BACKLOG, backlog)
      // disable auto read to enable back-pressure of stream.
      .childOption[java.lang.Boolean](ChannelOption.AUTO_READ, false)
      .childOption[java.lang.Boolean](ChannelOption.ALLOW_HALF_CLOSURE, halfClose)
      .childHandler(new ChannelInitializer[C] {
        override def initChannel(ch: C): Unit = {
          val (sinkQ, sink)     = Sink.queue[ByteString]().preMaterialize()
          val (sourceQ, source) = Source.queue[ByteString](1, OverflowStrategy.fail).preMaterialize()

          incomingQ.offer(IncomingConnection(ch.localAddress(), ch.remoteAddress(), Flow.fromSinkAndSource(sink, source)))

          ch.pipeline()
            .addLast(new ByteToByteStringCodec)
            .addLast(new AkkaStreamChannelHandler(sourceQ, sinkQ)(system.log))
        }
      })
      .bind(localAddress)
      .map(ch => ServerBinding(ch.localAddress())(() => ch.close().map(_ => {})))

    incomingS.mapMaterializedValue(_ => f)
  }

  private[netty] def channelClass: Class[_ <: C]

  private[netty] def serverChannelClass: Class[_ <: ServerChannel]

  protected def group: EventLoopGroup

}
