package part6patterns

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import scala.language.postfixOps

// step 1 - import ask pattern
import akka.pattern.ask
import akka.pattern.pipe

class AskSpec extends TestKit(ActorSystem("AskSpec")) with ImplicitSender with WordSpecLike with BeforeAndAfterAll{
  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import AskSpec._

  "An authenticator" should {
    authenticatorTestSuite(Props[AuthManager])
  }

  "A piped authenticator" should {
    authenticatorTestSuite(Props[PipedAuthManager])
  }

  def authenticatorTestSuite(props: Props) = {
    import AuthManager._

    "fail to authenticate a non-register user" in {
      val authManager = system.actorOf(props)
      authManager ! Authenticate("daniel","rtjvm")
      expectMsg(AuthFailure(AUTH_FAILURE_NOT_FOUND))
    }

    "fail to authenticate if invalid password" in {
      val authManager = system.actorOf(props)
      authManager ! RegisterUser("daniel","rtjvm")
      authManager ! Authenticate("daniel","iloveakka")
      expectMsg(AuthFailure(AUTH_FAILURE_PASSORD_INCORRECT))
    }

    "successfully authenticate a registered user" in {
      val authManager = system.actorOf(props)
      authManager ! RegisterUser("daniel","rtjvm")
      authManager ! Authenticate("daniel","rtjvm")
      expectMsg(AuthSuccess)
    }
  }

}

object AskSpec {

  case class Read(key: String)
  case class Write(key: String, value: String)

  class KVActor extends Actor with ActorLogging {
    override def receive: Receive = online(Map())

    def online(kv: Map[String, String]): Receive = {
      case Read(key) =>
        log.info(s"Trying to read the value at the key: $key")
        sender() ! kv.get(key) // Option[String]
      case Write(key, value) =>
        log.info(s"Writing the value $value for the key $key")
        context.become(online(kv + (key -> value)))
    }
  }

  // user authenticator actor
  case class RegisterUser(username: String, password: String)
  case class Authenticate(username: String, password: String)
  case class AuthFailure(message: String)
  case object AuthSuccess
  object AuthManager {
    val AUTH_FAILURE_NOT_FOUND = "Username not found"
    val AUTH_FAILURE_PASSORD_INCORRECT = "Password incorrect"
    val AUTH_FAILURE_SYSTEM_ERROR = "System error"
  }
  class AuthManager extends Actor with ActorLogging {

    import AuthManager._

    // step 2 - logistics
    implicit val timeout: Timeout = Timeout(1 second)
    implicit val executionContext: ExecutionContext = context.dispatcher

    protected val authDb = context.actorOf(Props[KVActor])

    override def receive: Receive = {
      case RegisterUser(username, password) =>
        authDb ! Write(username, password)
      case Authenticate(username, password) =>
        handleAuthentication(username, password)
    }

    def handleAuthentication(username: String, password: String) = {
      val originalSender = sender()
      // step 3 - ask the actor
      val future = authDb ? Read(username)
      // step 4 - handle the future
      future.onComplete {
        // step 5 (most important)
        // NEVER CALL METHODS ON THE ACTOR INSTANCE OR ACCESS MUTABLE STATE IN ONCOMPLETE
        case Success(None) => originalSender ! AuthFailure(AUTH_FAILURE_NOT_FOUND)
        case Success(Some(dbPassword)) =>
          if (dbPassword == password) originalSender ! AuthSuccess
          else originalSender ! AuthFailure(AUTH_FAILURE_PASSORD_INCORRECT)
        case Failure(_) =>
          originalSender ! AuthFailure(AUTH_FAILURE_SYSTEM_ERROR)
      }
    }
  }

  class PipedAuthManager extends AuthManager {
    import AuthManager._
    override def handleAuthentication(username: String, password: String): Unit = {
      val future = authDb ? Read(username) // Future[Any]
      val passwordFuture = future.mapTo[Option[String]] // Future[Option[String]]
      val responseFuture = passwordFuture.map {
        case None => AuthFailure(AUTH_FAILURE_NOT_FOUND)
        case Some(dbPassword) =>
          if (dbPassword == password) AuthSuccess
          else AuthFailure(AUTH_FAILURE_PASSORD_INCORRECT)
      } // Future[AuthSuccess / AuthFailure]

      /*
        When the future completes, send the response to the actor ref
       */
      responseFuture.pipeTo(sender)
    }
  }
}
