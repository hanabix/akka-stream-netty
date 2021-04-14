[![CI](https://github.com/hanabix/akka-stream-netty/actions/workflows/ci.yml/badge.svg)](https://github.com/hanabix/akka-stream-netty/actions/workflows/ci.yml) [![Publish](https://github.com/hanabix/akka-stream-netty/actions/workflows/sbt-release.yml/badge.svg)](https://github.com/hanabix/akka-stream-netty/actions/workflows/sbt-release.yml)[![Coveralls github](https://img.shields.io/coveralls/github/hanabix/akka-stream-netty.svg)](https://coveralls.io/github/hanabix/akka-stream-netty?branch=master) [![Codacy Badge](https://app.codacy.com/project/badge/Grade/9319321446b0455bb585530da133ff5d)](https://www.codacy.com/gh/hanabix/akka-stream-netty/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=hanabix/akka-stream-netty&amp;utm_campaign=Badge_Grade) [![Maven Central](https://img.shields.io/maven-central/v/com.github.zhongl/akka-stream-netty-all_2.13)](https://search.maven.org/artifact/com.github.zhongl/akka-stream-netty-all_2.13)

**akka-stream-netty** is a scala lib to adapt [netty](https://netty.io) transport to [akka-stream](https://doc.akka.io/docs/akka/current/stream/index.html), which let us can use native transport with:

  - epoll
  - kqueue
  - unix domain socket

> [alpakka-unix-domain-socket](https://github.com/akka/alpakka) would be a alternative if you only want to use unix domain socket.

# Dependencies

```scala
libraryDependencies += "com.github.zhongl" %% "akka-stream-netty-all" % <latest tag>
```

# Usage

```scala
import java.net._
import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.util.ByteString
import io.netty.channel.socket._
import zhongl.stream.netty._
import all._

implicit val system = ActorSystem("demo")
implicit val mat = ActorMaterializer()
implicit val ec = system.dispatcher

Netty().bindAndHandle[ServerSocketChannel](Flow[ByteString].map(identity), new InetSocketAddress("localhost", 8080)).flatMap { sb =>
  Source.repeat(ByteString("a"))
    .delay(1.seconds) 
    .via(Netty().outgoingConnection[SocketChannel](sb.localAddress))
    .runForeach(println)
    .flatMap(_ => sb.unbind())    
}
```

More usage information please see [test cases](./all/src/test/scala/zhongl/stream/netty/all).
