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

package zhongl.stream.netty.all

import io.netty.channel.Channel
import io.netty.channel.socket.{ServerSocketChannel, SocketChannel}
import io.netty.channel.unix.{DomainSocketChannel, ServerDomainSocketChannel}
import zhongl.stream.netty.Transport

private[all] trait GetTransport[T <: Transports, C <: Channel] {
  def apply(t: T): Transport[C]
}

private[all] object GetTransport                               {
  implicit val getSocketChannelT: GetTransport[SocketTransports, SocketChannel]                               = _.socketChannelT
  implicit val getServerSocketChannelT: GetTransport[SocketTransports, ServerSocketChannel]                   = _.serverSocketChannelT
  implicit val getDomainSocketChannelT: GetTransport[DomainSocketTransports, DomainSocketChannel]             = _.domainSocketChannelT
  implicit val getServerDomainSocketChannelT: GetTransport[DomainSocketTransports, ServerDomainSocketChannel] = _.serverDomainSocketChannelT
}
