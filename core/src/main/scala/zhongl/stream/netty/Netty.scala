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
import akka.actor.{ActorSystem, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider}
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString
import io.netty.channel.socket.DuplexChannel

import scala.concurrent.Future
import scala.concurrent.duration.Duration

/**
  * As a extension of [[ActorSystem]], [[Netty]] provide some duplex network transport base on native lib.
  */
object Netty extends ExtensionId[Netty] with ExtensionIdProvider {
  def apply()(implicit system: ActorSystem): Netty = super.apply(system)

  override def createExtension(system: ExtendedActorSystem): Netty = new Netty(system)
  override def lookup(): ExtensionId[_ <: Extension]               = Netty

  final case class OutgoingConnection(localAddress: SocketAddress, remoteAddress: SocketAddress)
  final case class IncomingConnection(localAddress: SocketAddress, remoteAddress: SocketAddress, flow: Flow[ByteString, ByteString, NotUsed])
  final case class ServerBinding(localAddress: SocketAddress)(private val unbindAction: () ⇒ Future[Unit]) {
    def unbind(): Future[Unit] = unbindAction()
  }
}

class Netty(system: ExtendedActorSystem) extends Extension {
  import Netty._

  /** Bind to a local address to accept incoming connection from a client. */
  def bind[C <: DuplexChannel](
      localAddress: SocketAddress,
      backlog: Int = 100,
      halfClose: Boolean = false
  )(implicit t: Transport[C]): Source[IncomingConnection, Future[ServerBinding]] = {
    t.bind(localAddress, backlog, halfClose)
  }

  /** Bind to a local address to accept incoming connection handling with a [[Flow]]. */
  def bindAndHandle[C <: DuplexChannel](
      flow: Flow[ByteString, ByteString, _],
      localAddress: SocketAddress,
      backlog: Int = 100,
      halfClose: Boolean = false
  )(implicit t: Transport[C], mat: Materializer): Future[ServerBinding] = {
    bind(localAddress, backlog, halfClose).to(Sink.foreach(_.flow.join(flow).run())).run()
  }

  /** Connect to a remote address. */
  def outgoingConnection[C <: DuplexChannel](
      remoteAddress: SocketAddress,
      localAddress: Option[SocketAddress] = None,
      halfClose: Boolean = true,
      connectTimeout: Duration = Duration.Inf
  )(implicit t: Transport[C]): Flow[ByteString, ByteString, Future[OutgoingConnection]] = {
    t.outgoingConnection(remoteAddress, localAddress, halfClose, connectTimeout)
  }
}
