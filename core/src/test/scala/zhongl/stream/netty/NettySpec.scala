package zhongl.stream.netty

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.testkit.TestKit
import akka.util.ByteString
import io.netty.channel.socket.nio.NioSocketChannel
import org.scalatest._

class NettySpec extends TestKit(ActorSystem("netty")) with AsyncWordSpecLike with Matchers with BeforeAndAfterAll {

  implicit val mat = ActorMaterializer()

  "Netty" should {
    "use nio transport" in {
      val address = new InetSocketAddress("localhost", 12306)
      Netty().bindAndHandle[NioSocketChannel](Flow[ByteString].map(identity), address, halfClose = true).flatMap { sb =>
        val msg = ByteString("a")
        Source
          .single(msg)
          .via(
            Netty().outgoingConnection[NioSocketChannel](address)
          )
          .runWith(Sink.head)
          .map(_ shouldBe msg)
          .flatMap { a =>
            sb.unbind().map(_ => a)
          }
      }
    }
  }

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
}
