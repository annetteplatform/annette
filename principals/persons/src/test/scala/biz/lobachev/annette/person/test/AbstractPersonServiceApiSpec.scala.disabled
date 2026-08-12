package biz.lobachev.annette.person.test

import akka.actor.ActorSystem
import biz.lobachev.annette.persons.impl.PersonServiceApplication
import biz.lobachev.annette.persons.impl.person.model.PersonSerializerRegistry
import com.lightbend.lagom.scaladsl.api.AdditionalConfiguration
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}
//import scala.util.Random

abstract class AbstractPersonServiceApiSpec extends AsyncWordSpec with BeforeAndAfterAll with Matchers {

  val system      = ActorSystem("PersonEntitySpec", JsonSerializerRegistry.actorSystemSetupFor(PersonSerializerRegistry))
  implicit val ec = system.dispatcher

  lazy val server = ServiceTest.startServer(
    ServiceTest.defaultSetup
      .withCassandra(true)
  ) { ctx =>
    new PersonServiceApplication(ctx) with LocalServiceLocator {
      override def additionalConfiguration: AdditionalConfiguration =
        super.additionalConfiguration ++ ConfigFactory
          .parseString(
            s"""
               |cassandra-query-journal.events-by-tag.eventual-consistency-delay = 0s
               |lagom.circuit-breaker.default.enabled = off
               |
               |akka.jvm-exit-on-fatal-error = false""".stripMargin
          )
          .resolve()
    }
  }

  override protected def beforeAll() = {
    server
    ()
  }

  override def afterAll(): Unit =
    //Await.ready(system.terminate, 10.seconds)
    server.stop()

  def awaitSuccess[T](maxDuration: FiniteDuration = 30.seconds, checkEvery: FiniteDuration = 2.second)(
    block: (Int) => Future[T]
  ): Future[T] = {
    val checkUntil = System.currentTimeMillis() + maxDuration.toMillis

    def doCheck(iteration: Int): Future[T] =
      block(iteration).recoverWith {
        case _ if checkUntil > System.currentTimeMillis() =>
          val timeout = Promise[T]()
          server.application.actorSystem.scheduler.scheduleOnce(checkEvery) {
            timeout.completeWith(doCheck(iteration + 1))
          }(server.executionContext)
          timeout.future
      }

    doCheck(1)
  }

}
