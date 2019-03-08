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

import akka.dispatch.ExecutionContexts
import akka.event.LoggingAdapter
import akka.stream.QueueOfferResult
import akka.stream.scaladsl.{SinkQueueWithCancel, SourceQueueWithComplete}
import io.netty.channel._
import io.netty.channel.socket.DuplexChannel

import scala.concurrent.ExecutionContext

/**
  * A channel handler to integrate akka stream with two queues.
  *
  * Caution: the [[ChannelConfig.isAutoRead]] supposed to be `false` for back-pressure.
  */
class AkkaStreamChannelHandler[In, Out](sourceQ: SourceQueueWithComplete[In], sinkQ: SinkQueueWithCancel[Out])(implicit log: LoggingAdapter)
    extends ChannelInboundHandlerAdapter {

  implicit private def ec(implicit ctx: ChannelHandlerContext): ExecutionContext = {
    ExecutionContexts.fromExecutorService(ctx.executor())
  }

  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    ctx.read()
    pullSourceToOutBound(ctx)
  }

  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    sourceQ.complete()
    sinkQ.cancel()
  }

  override def channelRead(ctx: ChannelHandlerContext, msg: AnyRef): Unit = {
    offerSinkFromInBound(msg)(ctx)
  }

  override def channelWritabilityChanged(ctx: ChannelHandlerContext): Unit = {
    pullSourceToOutBound(ctx)
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    log.error(cause, "Close channel by error")
    ctx.close()
  }

  private def offerSinkFromInBound(msg: AnyRef)(implicit ctx: ChannelHandlerContext): Unit = {
    @inline def illegal = {
      new IllegalStateException("Element should not be dropped, please check overflow strategy.")
    }

    @inline def shutdownInputOrClose: Channel => Unit = {
      case ch: DuplexChannel if !ch.isOutputShutdown => ch.shutdownInput().addListener(catchFailure(ctx))
      case ch if ch.isOpen                           => ch.close()
      case _                                         => // ignore
    }

    sourceQ
      .offer(msg.asInstanceOf[In])
      .map {
        case QueueOfferResult.Enqueued    => ctx.read()
        case QueueOfferResult.QueueClosed => shutdownInputOrClose(ctx.channel())
        case QueueOfferResult.Dropped     => exceptionCaught(ctx, illegal)
        case QueueOfferResult.Failure(e)  => exceptionCaught(ctx, e)
      }
      .failed
      .foreach(_ => shutdownInputOrClose(ctx.channel())) // source was completed

  }

  private def pullSourceToOutBound(implicit ctx: ChannelHandlerContext): Unit = {
    @inline def shutdownOutputOrClose: Channel => Unit = {
      case ch: DuplexChannel if !ch.isInputShutdown => ch.shutdownOutput().addListener(catchFailure(ctx))
      case ch if ch.isOpen                          => ch.close()
      case _                                        => //ignore
    }

    @inline def writeAndFlush(e: Out): Unit = {
      ctx
        .writeAndFlush(e)
        .addListener({ f: ChannelFuture =>
          if (f.isSuccess) pullSourceToOutBound(ctx) else exceptionCaught(ctx, f.cause())
        })
    }

    if (ctx.channel().isWritable) {
      sinkQ
        .pull()
        .map {
          case Some(e) => writeAndFlush(e)
          case None    => shutdownOutputOrClose(ctx.channel()) // sink was completed
        }
        .failed
        .foreach(exceptionCaught(ctx, _))
    }

  }

  @inline private def catchFailure(ctx: ChannelHandlerContext): ChannelFutureListener = { f: ChannelFuture =>
    if (!f.isSuccess) exceptionCaught(ctx, f.cause())
  }
}
