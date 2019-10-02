package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import part2actors.ActorsCapabilities.system
import part2actors.ChangingActorBehaviour.FussyKid.{KidAccept, KidReject}
import part2actors.ChangingActorBehaviour.Mom.MomStart

object ChangingActorBehaviour extends App {

  object FussyKid {
    case object KidAccept
    case object KidReject
    val HAPPY = "happy"
    val SAD = "sad"
  }
  class FussyKid extends Actor {
    import FussyKid._
    import Mom._

    var state = HAPPY

    override def receive: Receive = {
      case Food(VEGETABLE) => state = SAD
      case Food(CHOCOLATE) => state = HAPPY
      case Ask(_) =>
        if (state == HAPPY) sender ! KidAccept
        else sender ! KidReject
    }
  }

  object Mom {
    case class MomStart(kid: ActorRef)
    case class Food(food: String)
    case class Ask(message: String)
    val VEGETABLE = "veggies"
    val CHOCOLATE = "chocolate"
  }

  class Mom extends Actor {

    import Mom._
    override def receive: Receive = {
      case MomStart(kid) => {
        kid ! Food(VEGETABLE)
        kid ! Food(VEGETABLE)
        kid ! Food(CHOCOLATE)
        kid ! Ask("Do you want to play?")
      }
      case KidAccept => println("Yay, my kid is happy")
      case KidReject => println("Sad(((")
    }
  }

  val system = ActorSystem("changingActorBehavior")
  val fussyKid = system.actorOf(Props[FussyKid], "kid")
  val mom = system.actorOf(Props[Mom], "mom")

  mom ! MomStart(fussyKid)

  class StatelessFussyKid extends Actor {
    import FussyKid._
    import Mom._

    override def receive: Receive = happyReceive

    def happyReceive: Receive = {
      case Food(VEGETABLE) => context.become(sadReceive, false)
      case Food(CHOCOLATE) =>
      case Ask(_) => sender ! KidAccept
    }

    def sadReceive: Receive = {
      case Food(VEGETABLE) => context.become(sadReceive, false)
      case Food(CHOCOLATE) => context.unbecome
      case Ask(_) => sender ! KidReject
    }
  }

  val statelessFussyKid = system.actorOf(Props[StatelessFussyKid], "statelessKid")
  mom ! MomStart(statelessFussyKid)




  //-----------------------------------------------------------------------------------------

  class CountActor extends Actor {
    import Counter._

    override def receive(): Receive = receiveNum(0)

    def receiveNum(num: Int): Receive = {
      case Increment => context.become(receiveNum(num + 1))
      case Decrement => context.become(receiveNum(num - 1))
      case Print => println(s"[counter] current amount is $num")
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


  case class Vote(president: String)
  case object VoteStatusRequest
  case class VoteStatusReply(candidate: Option[String])
  class Citizen extends Actor {
    override def receive: Receive = {
      case Vote(president) => context.become(vote(president))
      case VoteStatusRequest => sender ! VoteStatusReply(None)
    }

    def vote(vote: String): Receive = {
      case VoteStatusRequest => sender ! VoteStatusReply(Some(vote))
    }
  }

  case class AggregateVotes(citizens: Set[ActorRef])
  class VoteAggregator extends Actor {
    override def receive: Receive = awaitingCommand

    def awaitingStatus(stillWait: Set[ActorRef], votes: Map[String, Int]): Receive = {
      case VoteStatusReply(None) =>
        sender() ! VoteStatusRequest // may produce infinite loop
      case VoteStatusReply(Some(candidate)) =>
        val newList = stillWait - sender()
        val currentVote = votes.getOrElse(candidate, 0)
        val newVotes = votes + (candidate -> (currentVote + 1))
        if (newList.isEmpty) {
          println(s"Poll stats: $newVotes")
        } else {
          context.become(awaitingStatus(newList, newVotes))
        }
    }

    def awaitingCommand: Receive = {
      case AggregateVotes(citizens) =>
        citizens.foreach(_ ! VoteStatusRequest)
        context.become(awaitingStatus(citizens, Map()))
    }
  }

  val alice = system.actorOf(Props[Citizen])
  val bob = system.actorOf(Props[Citizen])
  val charlie = system.actorOf(Props[Citizen])
  val daniel = system.actorOf(Props[Citizen])

  alice ! Vote("Martin")
  bob ! Vote("Jonas")
  charlie ! Vote("Ronald")
  daniel ! Vote("Ronald")

  val voteAggregator = system.actorOf(Props[VoteAggregator])
  voteAggregator ! AggregateVotes(Set(alice, bob, charlie, daniel))

}
