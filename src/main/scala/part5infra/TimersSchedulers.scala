package part5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Cancellable, Props, Timers}

import scala.concurrent.duration._
import scala.language.postfixOps

object TimersSchedulers extends App {

  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  val system = ActorSystem("SchedulersTimersDemo")
  val simpleActor = system.actorOf(Props[SimpleActor])

  system.log.info("Scheduling reminder for simpleActor")


  /*system.scheduler.scheduleOnce(1 second) {
    simpleActor ! "reminder"
  } (system.dispatcher)

  implicit val executionContext = system.dispatcher
  system.scheduler.scheduleOnce(1 second) {
    simpleActor ! "reminder implicit"
  }
*/
  import system.dispatcher
  /*system.scheduler.scheduleOnce(1 second) {
    simpleActor ! "reminder import"
  }*/

  /*val routine: Cancellable = system.scheduler.schedule(1 second, 2 seconds) {
    simpleActor ! "heartbeat"
  }

  system.scheduler.scheduleOnce(5 seconds) {
    routine.cancel()
  }*/

  class SelfClosingActor extends Actor with ActorLogging {

    var schedule = createTimeoutWindow()

    def createTimeoutWindow(): Cancellable = {
      context.system.scheduler.scheduleOnce(1 second) {
        self ! "timeout"
      }
    }

    override def receive: Receive = {
      case "timeout" =>
        log.info("stopping myself")
        context.stop(self)
      case message =>
        log.info(s"receive $message, staying alive")
        schedule.cancel()
        schedule = createTimeoutWindow()
    }
  }

  /*val selfClosingActor = system.actorOf(Props[SelfClosingActor], "selfClosingActor")
  system.scheduler.scheduleOnce(250 millis) {
    selfClosingActor ! "ping"
  }

  system.scheduler.scheduleOnce(2 seconds) {
    system.log.info("Sending pong to selfClosingActor")
    selfClosingActor ! "pong"
  }*/

  /**
    * Timer
    */

  case object TimerKey
  case object Start
  case object Reminder
  case object Stop
  class TimerBasedHeartbeatActor extends Actor with ActorLogging with Timers {

    timers.startSingleTimer(TimerKey, Start, 500 millis)

    override def receive: Receive = {
      case Start =>
        log.info("bootstrapping")
        timers.startPeriodicTimer(TimerKey, Reminder, 1 second)
      case Reminder =>
        log.info("I'm alive")
      case Stop =>
        log.warning("Stopping")
        timers.cancel(TimerKey)
        context.stop(self)
    }
  }

  val timerHBActor = system.actorOf(Props[TimerBasedHeartbeatActor], "timerActor")
  system.scheduler.scheduleOnce(5 seconds) {
    timerHBActor ! Stop
  }
}
