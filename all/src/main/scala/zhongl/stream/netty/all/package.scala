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
import io.netty.channel.kqueue._
import io.netty.channel.socket._
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.channel.unix.DomainSocketChannel

package object all {

  import epoll._
  import jvm._
  import kqueue._

  implicit def socketTransport(implicit system: ActorSystem): Transport[SocketChannel] = {
    implicitly[Option[Transport[EpollSocketChannel]]]
      .orElse(implicitly[Option[Transport[KQueueSocketChannel]]])
      .getOrElse(implicitly[Transport[NioSocketChannel]])
  }

  implicit def domainTransport(implicit system: ActorSystem): Transport[DomainSocketChannel] = {
    implicitly[Option[Transport[EpollDomainSocketChannel]]]
      .orElse(implicitly[Option[Transport[KQueueDomainSocketChannel]]])
      .getOrElse(throw new IllegalStateException("Your environment do not support Unix domain socket"))
  }

}
