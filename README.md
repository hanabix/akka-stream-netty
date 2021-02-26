# akka-stream-netty [![CI](https://github.com/zhongl/akka-stream-netty/actions/workflows/ci.yml/badge.svg)](https://github.com/zhongl/akka-stream-netty/actions/workflows/ci.yml) [![Release](https://github.com/zhongl/akka-stream-netty/actions/workflows/release.yml/badge.svg)](https://github.com/zhongl/akka-stream-netty/actions/workflows/release.yml) [![Coveralls github](https://img.shields.io/coveralls/github/zhongl/akka-stream-netty.svg)](https://coveralls.io/github/zhongl/akka-stream-netty?branch=master) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/319be6e6e88c423e9e83d9c99e3e4fdc)](https://www.codacy.com/app/zhongl/akka-stream-netty?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=zhongl/akka-stream-netty&amp;utm_campaign=Badge_Grade)

A scala lib to adapt [netty](https://netty.io) transport to [akka-stream](https://doc.akka.io/docs/akka/current/stream/index.html), which let us can use native transport with:

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
