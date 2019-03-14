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

import akka.actor.ActorSystem
import io.netty.channel.kqueue._

package object kqueue {
  private def mayBe[C <: AbstractKQueueStreamChannel](t: Transport[C]): Option[Transport[C]] = if (KQueue.isAvailable) Some(t) else None

  implicit def mayBeSocketTransport(implicit system: ActorSystem): Option[Transport[KQueueSocketChannel]] =
    mayBe(new Transport[KQueueSocketChannel] {
      override private[netty] def channelClass       = classOf[KQueueSocketChannel]
      override private[netty] def serverChannelClass = classOf[KQueueServerSocketChannel]
      override protected def group                   = new KQueueEventLoopGroup()
    })

  implicit def mayBeDomainTransport(implicit system: ActorSystem): Option[Transport[KQueueDomainSocketChannel]] =
    mayBe(new Transport[KQueueDomainSocketChannel] {
      override private[netty] def channelClass       = classOf[KQueueDomainSocketChannel]
      override private[netty] def serverChannelClass = classOf[KQueueServerDomainSocketChannel]
      override protected def group                   = new KQueueEventLoopGroup(1) // one thread enough for the domain socket scenario.
    })

}
