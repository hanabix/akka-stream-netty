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

  private var sinkCompleted = false

  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    log.debug("[{}] active", ctx.channel())
    ctx.read()
    pullSourceToOutBound(ctx)
  }

  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    log.debug("[{}] inactive", ctx.channel())
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
    log.error(cause, "Close channel [{}] by", ctx.channel())
    ctx.close()
  }

  private def offerSinkFromInBound(msg: AnyRef)(implicit ctx: ChannelHandlerContext): Unit = {
    @inline def illegal = {
      new IllegalStateException("Element should not be dropped, please check overflow strategy.")
    }

    @inline def debug[A, B](result: A)(f: A => B): B = {
      log.debug(s"[{}] offer {}", ctx.channel(), result)
      f(result)
    }

    log.debug(s"[{}] read {}", ctx.channel(), msg)

    sourceQ
      .offer(msg.asInstanceOf[In])
      .map(debug(_) {
        case QueueOfferResult.QueueClosed               => ctx.close()
        case QueueOfferResult.Enqueued if sinkCompleted => ctx.close() // As a client, close channel if no more request
        case QueueOfferResult.Enqueued                  => ctx.read()  // As a client, keep on reading
        case QueueOfferResult.Dropped                   => exceptionCaught(ctx, illegal)
        case QueueOfferResult.Failure(e)                => exceptionCaught(ctx, e)
      })
      .failed
      .foreach { _ =>
        log.debug(s"[{}] source completed", ctx.channel())
        ctx.close()
      }

  }

  private def pullSourceToOutBound(implicit ctx: ChannelHandlerContext): Unit = {

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
          case Some(e) => log.debug("[{}] write {}", ctx.channel(), e); writeAndFlush(e)
          case None    => log.debug("[{}] sink completed", ctx.channel()); sinkCompleted = true
        }
        .failed
        .foreach(exceptionCaught(ctx, _))
    }

  }

}
