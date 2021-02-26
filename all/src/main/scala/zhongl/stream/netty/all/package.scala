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

import io.netty.channel.Channel
import io.netty.channel.socket._
import io.netty.channel.unix.{DomainSocketChannel, ServerDomainSocketChannel}

import scala.reflect.ClassTag

package object all {

  implicit val sct: Transport[SocketChannel]               = findAvailable[SocketTransports, SocketChannel]
  implicit val ssct: Transport[ServerSocketChannel]        = findAvailable[SocketTransports, ServerSocketChannel]
  implicit val dsct: Transport[DomainSocketChannel]        = findAvailable[DomainSocketTransports, DomainSocketChannel]
  implicit val sdsct: Transport[ServerDomainSocketChannel] = findAvailable[DomainSocketTransports, ServerDomainSocketChannel]

  implicit private def stss: Seq[SocketTransports]        = Seq(EpollTransports, KQueueTransports, NioTransports)
  implicit private def dstss: Seq[DomainSocketTransports] = Seq(EpollTransports, KQueueTransports)

  private def findAvailable[T <: Transports, C <: Channel](implicit
      s: Seq[T],
      c: ClassTag[C],
      g: GetTransport[T, C]
  ): Transport[C] = {
    s.find(_.available).map(g(_)).getOrElse {
      throw new IllegalStateException(s"${c.runtimeClass.getName} is unavailable in your environment")
    }
  }

}
