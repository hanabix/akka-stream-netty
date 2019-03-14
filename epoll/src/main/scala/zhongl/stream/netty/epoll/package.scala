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
import io.netty.channel.epoll._

package object epoll {

  private def mayBe[C <: AbstractEpollStreamChannel](t: Transport[C]): Option[Transport[C]] = if (Epoll.isAvailable) Some(t) else None

  implicit def epollSocketTransport(implicit system: ActorSystem): Option[Transport[EpollSocketChannel]] =
    mayBe(new Transport[EpollSocketChannel] {
      override private[netty] def channelClass       = classOf[EpollSocketChannel]
      override private[netty] def serverChannelClass = classOf[EpollServerSocketChannel]
      override protected def group                   = new EpollEventLoopGroup()
    })

  implicit def epollDomainTransport(implicit system: ActorSystem): Option[Transport[EpollDomainSocketChannel]] =
    mayBe(new Transport[EpollDomainSocketChannel] {
      override private[netty] def channelClass       = classOf[EpollDomainSocketChannel]
      override private[netty] def serverChannelClass = classOf[EpollServerDomainSocketChannel]
      override protected def group                   = new EpollEventLoopGroup(1) // one thread enough for the domain socket scenario.
    })

}
