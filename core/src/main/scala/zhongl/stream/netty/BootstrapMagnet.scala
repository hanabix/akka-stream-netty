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

import io.netty.bootstrap.{Bootstrap, ServerBootstrap}
import io.netty.channel.{Channel, ChannelFuture, ChannelHandler, ChannelOption, ServerChannel}

import scala.concurrent.duration.Duration

trait BootstrapMagnet {
  def apply(): ChannelFuture
}

object BootstrapMagnet {
  implicit def bind[C <: ServerChannel](tuple: (Transport[C], ChannelHandler, SocketAddress, Int, Boolean)): BootstrapMagnet = () => {
    val (transport, handler, local, backlog, halfClose) = tuple
    new ServerBootstrap()
      .group(transport.group)
      .channel(transport.channel)
      .option[Integer](ChannelOption.SO_BACKLOG, backlog)
      // disable auto read to enable back-pressure of stream.
      .childOption[java.lang.Boolean](ChannelOption.AUTO_READ, false)
      .childOption[java.lang.Boolean](ChannelOption.ALLOW_HALF_CLOSURE, halfClose)
      .childHandler(handler)
      .bind(local)
  }

  implicit def connect[C <: Channel](
      tuple: (Transport[C], ChannelHandler, SocketAddress, Option[SocketAddress], Boolean, Duration)
  ): BootstrapMagnet = () => {
    val (transport, handler, remote, local, halfClose, connectTimeout) = tuple
    local
      .map(a => new Bootstrap().localAddress(a))
      .getOrElse(new Bootstrap())
      .group(transport.group)
      .channel(transport.channel)
      // disable auto read to enable back-pressure of stream.
      .option[java.lang.Boolean](ChannelOption.AUTO_READ, false)
      .option[java.lang.Boolean](ChannelOption.ALLOW_HALF_CLOSURE, halfClose)
      .option[Integer](ChannelOption.CONNECT_TIMEOUT_MILLIS, integer(connectTimeout))
      .handler(handler)
      .connect(remote)
  }

  @inline private def integer(d: Duration): Integer                                                                          =
    if (d.isFinite) d.toMillis.asInstanceOf[Integer] else Integer.MAX_VALUE
}
