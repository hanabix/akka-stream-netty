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

import io.netty.channel.epoll._
import io.netty.channel.kqueue._
import io.netty.channel.socket.{ServerSocketChannel, SocketChannel}
import io.netty.channel.socket.nio.{NioServerSocketChannel, NioSocketChannel}
import io.netty.channel.unix.{DomainSocketChannel, ServerDomainSocketChannel}
import zhongl.stream.netty._
import jvm._
import epoll._
import kqueue._

private[all] trait Transports {
  def available: Boolean
}

private[all] trait SocketTransports extends Transports {
  def socketChannelT: Transport[SocketChannel]
  def serverSocketChannelT: Transport[ServerSocketChannel]
}

private[all] trait DomainSocketTransports extends Transports {
  def domainSocketChannelT: Transport[DomainSocketChannel]
  def serverDomainSocketChannelT: Transport[ServerDomainSocketChannel]
}

private[all] object NioTransports extends SocketTransports {
  override def available: Boolean                                   = true
  override def socketChannelT: Transport[SocketChannel]             = Transport[NioSocketChannel]
  override def serverSocketChannelT: Transport[ServerSocketChannel] = Transport[NioServerSocketChannel]
}

private[all] object EpollTransports extends SocketTransports with DomainSocketTransports {
  override def available: Boolean                                               = Epoll.isAvailable
  override def socketChannelT: Transport[SocketChannel]                         = Transport[EpollSocketChannel]
  override def serverSocketChannelT: Transport[ServerSocketChannel]             = Transport[EpollServerSocketChannel]
  override def domainSocketChannelT: Transport[DomainSocketChannel]             = Transport[EpollDomainSocketChannel]
  override def serverDomainSocketChannelT: Transport[ServerDomainSocketChannel] = Transport[EpollServerDomainSocketChannel]
}

private[all] object KQueueTransports extends SocketTransports with DomainSocketTransports {
  override def available: Boolean                                               = KQueue.isAvailable
  override def socketChannelT: Transport[SocketChannel]                         = Transport[KQueueSocketChannel]
  override def serverSocketChannelT: Transport[ServerSocketChannel]             = Transport[KQueueServerSocketChannel]
  override def domainSocketChannelT: Transport[DomainSocketChannel]             = Transport[KQueueDomainSocketChannel]
  override def serverDomainSocketChannelT: Transport[ServerDomainSocketChannel] = Transport[KQueueServerDomainSocketChannel]
}
