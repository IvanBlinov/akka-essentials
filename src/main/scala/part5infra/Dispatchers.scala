package part5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

object Dispatchers extends App {

  class Counter extends Actor with ActorLogging {

    var count = 0

    override def receive: Receive = {
      case message =>
        count+=1
        log.info(s"[$count] $message")
    }
  }

  val system = ActorSystem("DispatchersDemo") //, ConfigFactory.load().getConfig("dispatchersDemo")

  // #1 - Programmatic (in code)
  /*val actors = for (i <- 1 to 10) yield {
    system.actorOf(Props[Counter].withDispatcher("my-dispatcher"), s"counter_$i")
  }
  val r = new Random()
  for (i <- 1 to 1000) {
    actors(r.nextInt(10)) ! i
  }*/

  // #2 - From config
  val rtjvmActor = system.actorOf(Props[Counter], "rtjvm")

  /**
    * Dispatchers implement the ExecutionContext trait
    */

  class DBActor extends Actor with ActorLogging {
    // solution #1
    implicit val executionContext: ExecutionContext = context.system.dispatchers.lookup("my-dispatcher")
    //implicit val executionContext: ExecutionContext = context.dispatcher

    //solution #2 - Use Routers

    override def receive: Receive = {
      case message => Future {
        // wait on resource
        Thread.sleep(5000)
        log.info(s"Success: $message")
      }
    }
  }

  val dbActor = system.actorOf(Props[DBActor])
  //dbActor ! "The meaning of life is 42"

  val nonBlockingActor = system.actorOf(Props[Counter])
  for (i <- 1 to 1000) {
    val message = s"Important message $i"
    dbActor ! message
    nonBlockingActor ! message
  }
}
