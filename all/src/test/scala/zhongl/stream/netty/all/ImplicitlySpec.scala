package zhongl.stream.netty.all

import java.net._
import java.nio.file.Files

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.testkit.TestKit
import akka.util.ByteString
import io.netty.channel.ServerChannel
import io.netty.channel.epoll._
import io.netty.channel.kqueue._
import io.netty.channel.socket._
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.channel.unix._
import org.scalatest._
import zhongl.stream.netty._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpecLike

class ImplicitlySpec extends TestKit(ActorSystem("implicitly")) with AsyncWordSpecLike with Matchers with BeforeAndAfterAll {

  implicit val mat = ActorMaterializer()
  implicit val ec  = system.dispatcher

  "import all._" should {

    "get platform-specified socket transport" in {
      def cc = Transport[SocketChannel].channel

      if (KQueue.isAvailable) {
        cc shouldBe classOf[KQueueSocketChannel]
      } else if (Epoll.isAvailable) {
        cc shouldBe classOf[EpollSocketChannel]
      } else {
        cc shouldBe classOf[NioSocketChannel]
      }
    }

    "get platform-specified domain socket transport" in {
      def cc = Transport[DomainSocketChannel].channel

      if (KQueue.isAvailable) {
        cc shouldBe classOf[KQueueDomainSocketChannel]
      } else if (Epoll.isAvailable) {
        cc shouldBe classOf[EpollDomainSocketChannel]
      } else {
        assertThrows[IllegalStateException](cc)
      }
    }

    "adapt akka stream by socket channel" in {
      runEcho[SocketChannel, ServerSocketChannel](new InetSocketAddress("localhost", 8080))
    }

    "adapt akka stream by domain socket channel" in {
      val file = Files.createTempFile("netty", "sock").toFile
      file.delete()
      file.deleteOnExit()

      runEcho[DomainSocketChannel, ServerDomainSocketChannel](new DomainSocketAddress(file))
    }
  }

  private def runEcho[C <: DuplexChannel, S <: ServerChannel](address: SocketAddress)(implicit t: Transport[C], s: Transport[S]) = {
    Netty().bindAndHandle[S](Flow[ByteString].map(identity), address).flatMap { sb =>
      val msg = ByteString("a")
      Source
        .single(msg)
        .via(Netty().outgoingConnection[C](sb.localAddress))
        .runWith(Sink.head)
        .map(_ shouldBe msg)
        .flatMap(a => sb.unbind().map(_ => a))
    }
  }
  override protected def afterAll(): Unit = TestKit.shutdownActorSystem(system)
}
