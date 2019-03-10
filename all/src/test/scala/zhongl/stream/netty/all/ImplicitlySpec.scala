package zhongl.stream.netty.all

import java.net._
import java.nio.file.Files

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.testkit.TestKit
import akka.util.ByteString
import io.netty.channel.socket._
import io.netty.channel.unix._
import org.scalatest._
import zhongl.stream.netty._

import scala.concurrent.duration._

class ImplicitlySpec extends TestKit(ActorSystem("implicitly")) with AsyncWordSpecLike with Matchers with BeforeAndAfterAll {

  implicit val mat = ActorMaterializer()
  implicit val ec = system.dispatcher

  "import all._" should {

    "adapt akka stream by socket channel" in {
      runEcho[SocketChannel](new InetSocketAddress("localhost", 8080))
    }

    "adapt akka stream by domain socket channel" in {
      val file = Files.createTempFile("netty","sock").toFile
      file.delete()
      file.deleteOnExit()

      runEcho[DomainSocketChannel](new DomainSocketAddress(file))
    }
  }

  private def runEcho[C <: DuplexChannel](address: SocketAddress)(implicit t: Transport[C]) = {
    Netty().bindAndHandle[C](Flow[ByteString].map(identity), address, halfClose = true).flatMap { sb =>
      val msg = ByteString("a")
      Source.repeat(msg)
        .delay(1.seconds) // avoid head of empty stream
        .via(Netty().outgoingConnection[C](address))
        .runWith(Sink.head)
        .map(_ shouldBe msg)
        .flatMap { a =>
          sb.unbind().map(_ => a)
        }
    }
  }
  override protected def afterAll(): Unit = TestKit.shutdownActorSystem(system)
}
