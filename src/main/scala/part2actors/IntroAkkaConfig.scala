package part2actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object IntroAkkaConfig extends App {

  class SimpleLoggingActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  /**
    * 1 - Inline configuration
    */
  val configString =
    """
      | akka {
      |   loglevel = "ERROR"
      | }
    """.stripMargin

  val config = ConfigFactory.parseString(configString)
  val system = ActorSystem("ConfigurationDemo", ConfigFactory.load(config))

  val actor = system.actorOf(Props[SimpleLoggingActor])
  actor ! "Message to remember"

  val defaultConfigFileSystem = ActorSystem("DefaultConfigFileDemo")
  val defaultConfigFileActor = defaultConfigFileSystem.actorOf(Props[SimpleLoggingActor])
  defaultConfigFileActor ! "Remember me"

  /**
    * 3 - Separate config in the same file
    */
  val specialConfig = ConfigFactory.load().getConfig("mySpecialConfig")
  val specialConfigSystem = ActorSystem("specialConfigDemo", specialConfig)
  val specialConfigActor = specialConfigSystem.actorOf(Props[SimpleLoggingActor])

  specialConfigActor ! "I'm special"

  /**
    * 4 - Separate config in another file
    */
  val separateConfig = ConfigFactory.load("secretFolder/secretConfiguration.conf")
  println(s"Secret configuration log level: ${separateConfig.getString("akka.loglevel")}")

  /**
    * 5 - Different file formats
    */
  val jsonConfig = ConfigFactory.load("json/jsonConfig.json")
  println(s"JSON configuration property: ${jsonConfig.getString("aJsonProperty")}")
  println(s"JSON configuration log level: ${jsonConfig.getString("akka.loglevel")}")
}
