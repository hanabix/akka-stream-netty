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

import akka.NotUsed
import akka.actor.{ActorSystem, CoordinatedShutdown, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider}
import akka.dispatch.Futures
import akka.stream._
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.util.ByteString
import io.netty.channel._
import io.netty.channel.socket.DuplexChannel

import scala.concurrent.Future
import scala.concurrent.duration.Duration

/** As a extension of [[ActorSystem]], [[Netty]] provide some duplex network transport base on native lib.
  */
object Netty extends ExtensionId[Netty] with ExtensionIdProvider {
  def apply()(implicit system: ActorSystem): Netty = super.apply(system)

  override def createExtension(system: ExtendedActorSystem): Netty = new Netty(system)
  override def lookup(): ExtensionId[_ <: Extension]               = Netty

  final case class OutgoingConnection(localAddress: SocketAddress, remoteAddress: SocketAddress)
  final case class IncomingConnection(localAddress: SocketAddress, remoteAddress: SocketAddress, flow: Flow[ByteString, ByteString, NotUsed])
  final case class ServerBinding(localAddress: SocketAddress)(private val unbindAction: () => Future[Unit]) {
    def unbind(): Future[Unit] = unbindAction()
  }
}

class Netty(system: ExtendedActorSystem) extends Extension {
  import Netty._

  implicit private val ex = system.dispatcher

  implicit private val asFuture: ChannelFuture => Future[Channel] = { cf =>
    val p = Futures.promise[Channel]()
    cf.addListener({ f: ChannelFuture =>
      if (f.isSuccess) p.trySuccess(f.channel()) else p.tryFailure(f.cause())
    })
    p.future
  }

  /** Bind to a local address to accept incoming connection from a client. */
  def bind[C <: ServerChannel: Transport](
      localAddress: SocketAddress,
      backlog: Int = 100,
      halfClose: Boolean = false
  ): Source[IncomingConnection, Future[ServerBinding]] = {

    implicit val mat = ActorMaterializer()(system)

    CoordinatedShutdown(system).addJvmShutdownHook(Transport[C].group.shutdownGracefully())

    val (incomingQ, incomingS) = Source.queue[IncomingConnection](1, OverflowStrategy.fail).preMaterialize()

    val handler = new ChannelInitializer[Channel] {
      override def initChannel(ch: Channel): Unit = {
        val (sinkQ, sink)     = Sink.queue[ByteString]().preMaterialize()
        val (sourceQ, source) = Source.queue[ByteString](1, OverflowStrategy.fail).preMaterialize()

        incomingQ.offer(IncomingConnection(ch.localAddress(), ch.remoteAddress(), Flow.fromSinkAndSource(sink, source)))

        ch.pipeline()
          .addLast(new ByteToByteStringCodec)
          .addLast(new AkkaStreamChannelHandler(sourceQ, sinkQ)(system.log))
      }
    }

    val f = bootstrap(Transport[C], handler, localAddress, backlog, halfClose)

    incomingS.mapMaterializedValue(_ => f.map(ch => ServerBinding(ch.localAddress())(() => ch.close().map(_ => {}))))
  }

  /** Bind to a local address to accept incoming connection handling with a [[Flow]]. */
  def bindAndHandle[C <: ServerChannel: Transport](
      flow: Flow[ByteString, ByteString, _],
      localAddress: SocketAddress,
      backlog: Int = 100,
      halfClose: Boolean = false
  )(implicit mat: Materializer): Future[ServerBinding] = {
    bind(localAddress, backlog, halfClose).to(Sink.foreach(_.flow.join(flow).run())).run()
  }

  /** Connect to a remote address. */
  def outgoingConnection[C <: DuplexChannel: Transport](
      remoteAddress: SocketAddress,
      localAddress: Option[SocketAddress] = None,
      halfClose: Boolean = true,
      connectTimeout: Duration = Duration.Inf
  ): Flow[ByteString, ByteString, Future[OutgoingConnection]] = {

    CoordinatedShutdown(system).addJvmShutdownHook(Transport[C].group.shutdownGracefully())

    Flow
      .fromSinkAndSourceMat(
        Sink.queue[ByteString](),
        Source.queue[ByteString](1, OverflowStrategy.fail)
      )(Keep.both)
      .mapMaterializedValue { case (sinkQ, sourceQ) =>
        val handler = new ChannelInitializer[C] {
          override def initChannel(ch: C): Unit = {
            ch.pipeline()
              .addLast(new ByteToByteStringCodec)
              .addLast(new AkkaStreamChannelHandler(sourceQ, sinkQ)(system.log))
          }
        }

        bootstrap(Transport[C], handler, remoteAddress, localAddress, halfClose, connectTimeout)
          .map(ch => OutgoingConnection(ch.localAddress(), ch.remoteAddress()))
      }

  }

  private def bootstrap(magnet: BootstrapMagnet): ChannelFuture = magnet()
}
