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

import scala.util._

package object epoll {

  implicit def tryEpollSocketTransport(implicit system: ActorSystem): Try[Transport[EpollSocketChannel]] =
    (try Success(Epoll.ensureAvailability())
    catch {
      case e: Throwable => Failure(e)
    }).map { _ =>
      new Transport[EpollSocketChannel] {
        override private[netty] def channelClass       = classOf[EpollSocketChannel]
        override private[netty] def serverChannelClass = classOf[EpollServerSocketChannel]
        override protected def group                   = new EpollEventLoopGroup()
      }
    }

  implicit def forceEpollSocketTransport(implicit system: ActorSystem): Transport[EpollSocketChannel] = tryEpollSocketTransport.get

  implicit def tryEpollDomainTransport(implicit system: ActorSystem): Try[Transport[EpollDomainSocketChannel]] =
    (try Success(Epoll.ensureAvailability())
    catch {
      case e: Throwable => Failure(e)
    }).map { _ =>
      new Transport[EpollDomainSocketChannel] {
        override private[netty] def channelClass       = classOf[EpollDomainSocketChannel]
        override private[netty] def serverChannelClass = classOf[EpollServerDomainSocketChannel]
        override protected def group                   = new EpollEventLoopGroup(1) // one thread enough for the domain socket scenario.
      }
    }

  implicit def forceEpollDomainTransport(implicit system: ActorSystem): Transport[EpollDomainSocketChannel] = tryEpollDomainTransport.get

}
