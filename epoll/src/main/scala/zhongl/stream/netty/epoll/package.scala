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

import io.netty.channel.epoll._
import io.netty.channel._

package object epoll {

  trait EpollTransport[C <: Channel] extends Transport[C] {
    override def group: EventLoopGroup = new EpollEventLoopGroup()
  }

  trait EpollDomainTransport[C <: Channel] extends Transport[C] {
    // one thread enough for the domain socket scenario.
    override def group: EventLoopGroup = new EpollEventLoopGroup(1)
  }

  implicit val epollServerSocketChannelT: Transport[EpollServerSocketChannel] = new EpollTransport[EpollServerSocketChannel] {
    override def channel = classOf[EpollServerSocketChannel]
  }

  implicit val epollSocketChannelT: Transport[EpollSocketChannel] = new EpollTransport[EpollSocketChannel] {
    override def channel = classOf[EpollSocketChannel]
  }

  implicit val epollServerDomainSocketChannelT: Transport[EpollServerDomainSocketChannel] = new EpollDomainTransport[EpollServerDomainSocketChannel] {
    override def channel = classOf[EpollServerDomainSocketChannel]
  }

  implicit val epollDomainSocketChannelT: Transport[EpollDomainSocketChannel] = new EpollDomainTransport[EpollDomainSocketChannel] {
    override def channel = classOf[EpollDomainSocketChannel]
  }
}
