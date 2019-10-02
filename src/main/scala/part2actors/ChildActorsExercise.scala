package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ChildActorsExercise extends App {

  //Distributed Word count

  object WordCountMaster {
    case class Initialize(nChildren: Int)
    case class WordCountTask(id: Int, text: String)
    case class WordCountReply(id: Int, count: Int)
  }
  class WordCountMaster extends Actor {
    import WordCountMaster._

    override def receive: Receive = {
      case Initialize(count) => {
        println("[master] initializing")
        val childs = for (i <- 1 to count) yield context.actorOf(Props[WordCountWorker], s"wordCounterWorker_$i")
        context.become(receiveWithChilds(childs, 0, 0, Map.empty))
      }
    }

    def receiveWithChilds(childs: Seq[ActorRef], currentChild: Int, currentTaskId: Int, requestMap: Map[Int, ActorRef]): Receive = {
      case text: String =>
        println(s"[master] I have received $text - I will send it to child: $currentChild")
        val newRequestMap = requestMap + (currentTaskId -> sender())
        childs(currentChild) ! WordCountTask(currentTaskId, text)
        context.become(receiveWithChilds(childs, (currentChild + 1) % childs.length, currentTaskId + 1, newRequestMap))
      case WordCountReply(id, count) =>
        println(s"[master] Task id: $id Count of words is: $count")
        requestMap(id) ! count
        context.become(receiveWithChilds(childs, currentChild, currentTaskId, requestMap - id))
    }
  }

  class WordCountWorker extends Actor {
    import WordCountMaster._

    override def receive: Receive = {
      case WordCountTask(id, text) =>
        println(s"[${self.path}] I have received task $id with text $text")
        sender() ! WordCountReply(id, text.split(" ").length)
    }
  }

  class TestActor extends Actor {
    import WordCountMaster._

    override def receive: Receive = {
      case "go" =>
        val master = context.actorOf(Props[WordCountMaster], "master")
        master ! Initialize(3)
        val texts = List("I love akka", "Something else", "Scala is super dope", "yes", "me too")
        texts.foreach(text => master ! text)
      case count: Int =>
        println(s"[test actor] I have received a reply: $count")
    }
  }

  val system = ActorSystem("roundRobinWordCountExercise")
  val testActor = system.actorOf(Props[TestActor], "testActor")
  testActor ! "go"
}
