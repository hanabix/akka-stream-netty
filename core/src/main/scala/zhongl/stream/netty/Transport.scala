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

import io.netty.channel._

import scala.concurrent.{Channel => _}
import scala.reflect.ClassTag

trait Transport[+C <: Channel] {
  def channel: Class[_ <: C]
  def group: EventLoopGroup
}

object Transport {
  def apply[C <: Channel](implicit t: Transport[C]) = t

  def apply[C <: Channel: ClassTag](_group: EventLoopGroup): Transport[C] = new Transport[C] {
    override def channel = implicitly[ClassTag[C]].runtimeClass.asInstanceOf[Class[C]]
    override def group   = _group
  }

  def dummy[C <: Channel: ClassTag] = new Dummy(implicitly[ClassTag[C]])

  class Dummy[C <: Channel](c: ClassTag[C]) extends Transport[C] {
    override def channel: Class[_ <: C] = c.runtimeClass.asInstanceOf[Class[C]]
    override def group: EventLoopGroup  = throw new UnsupportedOperationException(s"${channel.getName} is unavailable in your environment")

  }
}
