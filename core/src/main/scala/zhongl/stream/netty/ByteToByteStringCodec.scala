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

import java.util

import akka.util.ByteString
import io.netty.buffer.ByteBuf
import io.netty.channel._
import io.netty.handler.codec.ByteToMessageCodec

class ByteToByteStringCodec extends ByteToMessageCodec[ByteString] {

  override def encode(ctx: ChannelHandlerContext, msg: ByteString, out: ByteBuf): Unit = {
    out.writeBytes(msg.asByteBuffer)
  }

  override def decode(ctx: ChannelHandlerContext, in: ByteBuf, out: util.List[AnyRef]): Unit = {
    val bytes = Array.ofDim[Byte](in.readableBytes())
    in.readBytes(bytes)
    out.add(ByteString(bytes))
  }
}
