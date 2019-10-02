package part3testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

class BasicSpec extends TestKit(ActorSystem("BasicSpec"))
  with ImplicitSender with WordSpecLike with  BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import BasicSpec._

  "A simple actor" should {
    "Send back the same message" in {
      val echoActor = system.actorOf(Props[SimpleActor])
      val message = "hello, test"
      echoActor ! message

      expectMsg(message) // akka.test.single-expect-default
      testActor // default actor
    }
  }

  "A black hole actor" should {
    "send back some message" in {
      val blackHoleActor = system.actorOf(Props[BlackHole])
      val message = "hello, test"
      blackHoleActor ! message

      expectNoMessage(1 second)
    }
  }

  "A lab test actor" should {
    val labTestActor = system.actorOf(Props[LabTestActor])

    "turn a string into uppercase" in {
      labTestActor ! "I love akka"
      val reply = expectMsgType[String]

      assert(reply == "I LOVE AKKA")
    }

    "reply to a greeting" in {
      labTestActor ! "greeting"
      expectMsgAnyOf("hi", "hello")
    }

    "reply with favorite tech" in {
      labTestActor ! "favorite tech"
      expectMsgAllOf("Scala", "Akka")
    }

    "reply with cool tech in another way" in {
      labTestActor ! "favorite tech"
      val messages = receiveN(2) // Seq[Any]

    }

    "reply with cool tech in a fancy way" in {
      labTestActor ! "favorite tech"
      expectMsgPF() {
        case "Scala" => // only care that the PF is defined
        case "Akka" =>
      }
    }
  }

}

object BasicSpec {

  class SimpleActor extends Actor {
    override def receive: Receive = {
      case message => sender() ! message
    }
  }

  class BlackHole extends Actor {
    override def receive: Receive = Actor.emptyBehavior
  }

  class LabTestActor extends Actor {
    val random = new Random()
    override def receive: Receive = {
      case "greeting" =>
        if (random.nextBoolean()) sender() ! "hi" else sender() ! "hello"
      case "favorite tech" =>
        sender() ! "Scala"
        sender() ! "Akka"
      case message: String => sender ! message.toUpperCase
    }
  }
}
