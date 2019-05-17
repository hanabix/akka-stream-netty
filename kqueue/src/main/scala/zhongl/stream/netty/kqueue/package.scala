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

//noinspection TypeAnnotation
package object kqueue {

  implicit val kssct = Transport[KQueueServerSocketChannel](new KQueueEventLoopGroup())

  implicit val ksct = Transport[KQueueSocketChannel](new KQueueEventLoopGroup())

  // one thread enough for the domain socket scenario.
  implicit val ksdsct = Transport[KQueueServerDomainSocketChannel](new KQueueEventLoopGroup(1))

  implicit val kdsct = Transport[KQueueDomainSocketChannel](new KQueueEventLoopGroup(1))
}
