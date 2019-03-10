# akka-stream-netty [![Codacy Badge](https://api.codacy.com/project/badge/Grade/41d8541afaaf4267bb1120d04e265971)](https://www.codacy.com/app/zhonglunfu/akka-stream-netty?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=zhongl/akka-stream-netty&amp;utm_campaign=Badge_Grade) [![Build Status](https://travis-ci.org/zhongl/akka-stream-netty.svg?branch=master)](https://travis-ci.org/zhongl/akka-stream-netty) [![Version Badge](https://jitpack.io/v/zhongl/akka-stream-netty.svg)](https://jitpack.io/#zhongl/akka-stream-netty) [![Coveralls github](https://img.shields.io/coveralls/github/zhongl/akka-stream-netty.svg)](https://coveralls.io/github/zhongl/akka-stream-netty?branch=master)

A scala lib to adapt [netty](https://netty.io) transport to [akka-stream](https://doc.akka.io/docs/akka/current/stream/index.html), which let us can use native transport with epoll or kqueue.

# Resolvers

```scala
resolvers += "jitpack" at "https://jitpack.io"
```

# Dependencies

```scala
libraryDependencies += "com.github.zhongl.akka-stream-netty" %% "all" % <latest tag>
```

# Usage

```scala
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.util.ByteString
import io.netty.channel.socket.SocketChannel
import zhongl.stream.netty._
import all._

implicit val system = ActorSystem("demo")
implicit val mat = ActorMaterializer()
implicit val ec = system.dispatcher

Netty().bindAndHandle[SocketChannel](Flow[ByteString].map(identity), address, halfClose = true).flatMap { sb =>
  Source.repeat(ByteString("a"))
    .delay(1.seconds) 
    .via(Netty().outgoingConnection[SocketChannel](address))
    .runForeach(println)
    .flatMap(_ => sb.unbind())    
}
```