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
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio._

package object jvm {
  implicit def jvm(implicit sys: ActorSystem): Transport[NioSocketChannel] = new Transport[NioSocketChannel] {
    override def channelClass = classOf[NioSocketChannel]
    override def serverChannelClass = classOf[NioServerSocketChannel]
    override protected def group = new NioEventLoopGroup()
  }

}
