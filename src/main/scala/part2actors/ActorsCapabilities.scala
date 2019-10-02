package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import part2actors.ActorsCapabilities.Person.LiveTheLife

object ActorsCapabilities extends App {

  class SimpleActor extends Actor {
    override def receive: Receive = {
      case "Hi!" => context.sender() ! "Hello There"
      case message: String => println(s"[${context.self.path}] I received a $message from [${sender.path.name}]")
      case number: Int => println(s"[${self.path}] I received a number: $number")
      case specialMessage: SpecialMessage => println(s"[simple actor] I received a SpecialMessage: ${specialMessage.contents}")
      case SendMessageToYourself(content) => self ! content
      case SayHiTo(reference) => reference ! "Hi!"
      case WirelessPhoneMessage(message, ref) => ref forward (message + "s") // keep original sender
    }
  }

  val system = ActorSystem("actorCapabilitiesDemo")
  val simpleActor = system.actorOf(Props[SimpleActor], "simpleActor")

  simpleActor ! "Hello, actor"

  // 1 - messages can be of any type
  // a) Messages must be IMMUTABLE
  // b) Messages must be SERIALIZABLE
  // in practice use case classes and case objects
  simpleActor ! 42

  case class SpecialMessage(contents: String)

  simpleActor ! SpecialMessage("some content")

  // 2 - actors have information about their context and about themselves

  case class SendMessageToYourself(content: String)
  simpleActor ! SendMessageToYourself("I am an actor and I am proud of it")

  // 3 - actors can REPLY to messages
  val alice =  system.actorOf(Props[SimpleActor], "alice")
  val bob = system.actorOf(Props[SimpleActor], "bob")

  case class SayHiTo(ref: ActorRef)
  alice ! SayHiTo(bob)

  // 4 - dead letters
  alice ! "Hi!"

  // 5 - forwarding messages
  case class WirelessPhoneMessage(content: String, ref: ActorRef)
  alice ! WirelessPhoneMessage("Hi", bob)

  class CountActor extends Actor {
    import Counter._

    var value = 0

    override def receive: Receive = {
      case Increment => value += 1
      case Decrement => value -= 1
      case Print => println(s"Current value is: $value")
    }
  }
  val countActor = system.actorOf(Props[CountActor], "countActor")

  object Counter {
    case object Increment
    case object Decrement
    case object Print
  }

  import Counter._
  (1 to 5).foreach(_ => countActor ! Increment)
  countActor ! Increment
  countActor ! Increment
  countActor ! Print
  countActor ! Decrement
  countActor ! Print

  object BankAccount {
    case class Deposit(amount: Int)
    case class Withdraw(amount: Int)
    case object Statement

    case class TransactionSuccess(message: String)
    case class TransactionFailure(reason: String)
  }

  class BankAccount extends Actor {
    import BankAccount._

    var statement = 0

    override def receive: Receive = {
      case Deposit(amount) => {
        if (amount < 0) sender ! TransactionFailure("Invalid deposit amount")
        else {
          statement += amount
          sender ! TransactionSuccess("Deposit transaction was success!")
        }
      }
      case Withdraw(amount) => {
        if (amount < 0) sender ! TransactionFailure("Invalid withdraw amount")
        else if (amount > statement) sender ! TransactionFailure("You don't have enough money")
        else {
          statement -= amount
          sender ! TransactionSuccess("Withdraw transaction was success!")
        }
      }
      case Statement => sender ! s"Current statement is: $statement"
    }
  }

  import BankAccount._

  object Person {
    case class LiveTheLife(account: ActorRef)
  }
  class Person extends Actor {
    override def receive: Receive = {
      case LiveTheLife(account) =>
        account ! Deposit(10000)
        account ! Deposit(-10000)
        account ! Withdraw(-10000)
        account ! Withdraw(20000)
        account ! Withdraw(5000)
        account ! Statement
      case message => println(message.toString)
    }
  }

  val bankAccount = system.actorOf(Props[BankAccount], "bankAccount")
  val person = system.actorOf(Props[Person], "person")

  person ! LiveTheLife(bankAccount)

  bankAccount ! Statement
  bankAccount ! Deposit(100)
  bankAccount ! Statement
  bankAccount ! Withdraw(200)
  bankAccount ! Statement
  bankAccount ! Deposit(100)
  bankAccount ! Statement
  bankAccount ! Withdraw(200)
  bankAccount ! Statement
}
