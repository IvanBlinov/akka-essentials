package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import part2actors.ChildActors.CreditCard.{AttachedToAccount, CheckStatus}
import part2actors.ChildActors.Parent.{CreateChild, TellChild}

object ChildActors extends App {

  //Actors can create other actors

  object Parent {
    case class CreateChild(name: String)
    case class TellChild(message: String)
  }
  class Parent extends Actor {
    import Parent._

    var child: ActorRef = null
    override def receive: Receive = {
      case CreateChild(name) =>
        println(s"${self.path} creating child with name: $name")
        val childRef = context.actorOf(Props[Child], name)
        context.become(withChild(childRef))
    }

    def withChild(child: ActorRef): Receive = {
      case TellChild(message) => child forward message
    }
  }

  class Child extends Actor {
    override def receive: Receive = {
      case message => println(s"${self.path} I got: $message")
    }
  }

  val system = ActorSystem("ParentChildDemo")
  val parent = system.actorOf(Props[Parent], "parent")

  parent ! CreateChild("child")
  parent ! TellChild("I am your father")


  /**
    * Actor selection
    */

  val childSelection = system.actorSelection("/user/parent/child")
  childSelection ! "I found you"

  /**
    * Danger
    *
    * NEVER PASS MUTABLE ACTOR STATE, OR THE 'THIS' REFERENCE TO CHILD ACTORS
    */

  object NaiveBankAccount {
    case class Deposit(amount: Int)
    case class Withdraw(amount: Int)
    case object InitializeAccount
  }
  class NaiveBankAccount extends Actor {
    import NaiveBankAccount._
    import CreditCard._

    var amount = 0
    override def receive: Receive = {
      case InitializeAccount =>
        val creditCardRef = system.actorOf(Props[CreditCard], "card")
        creditCardRef ! AttachedToAccount(this)
      case Deposit(funds) => deposit(funds)
      case Withdraw(funds)  => withdraw(funds)
    }

    def deposit(funds: Int) = amount += funds
    def withdraw(funds: Int) = amount -= funds
  }

  object CreditCard {
    case class AttachedToAccount(bankAccount: NaiveBankAccount) // !!
    case object CheckStatus
  }
  class CreditCard extends Actor {
    override def receive: Receive = {
      case AttachedToAccount(account) => context.become(attachedTo(account))
    }

    def attachedTo(account: NaiveBankAccount): Receive = {
      case CheckStatus =>
        println(s"${self.path} your message has been proceeded")
        account.withdraw(1)
    }
  }

  import NaiveBankAccount._
  import CreditCard._

  val bankAccountRef = system.actorOf(Props[NaiveBankAccount], "account")
  bankAccountRef ! InitializeAccount
  bankAccountRef ! Deposit(100)


  Thread.sleep(500)
  val creditCard = system.actorSelection("/user/account/card")
  creditCard ! CheckStatus

  //WRONG!!!!
}
