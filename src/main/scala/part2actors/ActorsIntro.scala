package part2actors

import akka.actor.{Actor, ActorSystem, Props}

object ActorsIntro extends App {

  //part 1 - actor systems
  val actorSystem = ActorSystem("firstActorSystem")
  println(actorSystem.name)

  //part 2 - create actors
  //word count actor

  class WordCountActor extends Actor {

    var totalWords = 0

    //behaviour
    override def receive: Receive = {
      case message: String =>
        println(s"[word counter] I have received: $message")
        totalWords += message.split(" ").length
      case msg => println(s"[message actor] I can't understand ${msg.toString}")
    }
  }

  //part 3 - instantiate the actor
  val wordCounter = actorSystem.actorOf(Props[WordCountActor], "wordCounter")
  val anotherWordCounter = actorSystem.actorOf(Props[WordCountActor], "anotherWordCounter")

  //part 4 - communicate
  wordCounter ! "I am learning Akka and it's pretty damn cool"
  anotherWordCounter ! "Another different message"
  //asynchronous

  object Person {
    def props(name: String): Props = {
      Props(new Person(name))
    }
  }

  class Person(name: String) extends Actor {
    override def receive: Receive = {
      case "hi" => println(s"Hi, my name is $name")
      case _ =>
    }
  }

  val person = actorSystem.actorOf(Props(new Person("Bob")))
  person ! "hi"

  val anotherPerson = actorSystem.actorOf(Person.props("Alice"))
}
