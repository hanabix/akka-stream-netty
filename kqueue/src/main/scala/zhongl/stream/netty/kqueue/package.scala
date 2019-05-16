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

import io.netty.channel.kqueue._
import io.netty.channel._

package object kqueue {
  trait KQueueTransport[C <: Channel] extends Transport[C] {
    override def group: EventLoopGroup = new KQueueEventLoopGroup()
  }

  trait KQueueDomainTransport[C <: Channel] extends Transport[C] {
    // one thread enough for the domain socket scenario.
    override def group: EventLoopGroup = new KQueueEventLoopGroup(1)
  }

  implicit val kqueueServerSocketChannelT: Transport[KQueueServerSocketChannel] = new KQueueTransport[KQueueServerSocketChannel] {
    override def channel = classOf[KQueueServerSocketChannel]
  }

  implicit val kqueueSocketChannelT: Transport[KQueueSocketChannel] = new KQueueTransport[KQueueSocketChannel] {
    override def channel = classOf[KQueueSocketChannel]
  }

  implicit val kqueueServerDomainSocketChannelT: Transport[KQueueServerDomainSocketChannel] =
    new KQueueDomainTransport[KQueueServerDomainSocketChannel] {
      override def channel = classOf[KQueueServerDomainSocketChannel]
    }

  implicit val kqueueDomainSocketChannelT: Transport[KQueueDomainSocketChannel] = new KQueueDomainTransport[KQueueDomainSocketChannel] {
    override def channel = classOf[KQueueDomainSocketChannel]
  }
}
