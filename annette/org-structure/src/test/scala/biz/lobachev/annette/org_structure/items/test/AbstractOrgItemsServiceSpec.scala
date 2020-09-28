package biz.lobachev.annette.org_structure.items.test

import akka.actor.ActorSystem
import biz.lobachev.annette.org_structure.impl.{OrgStructureSerializerRegistry, OrgStructureServiceApplication}
import com.lightbend.lagom.scaladsl.api.AdditionalConfiguration
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

import scala.concurrent.duration.{FiniteDuration, _}
import scala.concurrent.{Future, Promise}
import scala.util.Random

abstract class AbstractOrgItemsServiceSpec extends AsyncWordSpec with BeforeAndAfterAll with Matchers {

  val system      =
    ActorSystem("PersonEntitySpec", JsonSerializerRegistry.actorSystemSetupFor(OrgStructureSerializerRegistry))
  implicit val ec = system.dispatcher

  lazy val server = ServiceTest.startServer(
    ServiceTest.defaultSetup
      .withCassandra(true)
  ) { ctx =>
    new OrgStructureServiceApplication(ctx) with LocalServiceLocator {
      override def additionalConfiguration: AdditionalConfiguration =
        super.additionalConfiguration ++ ConfigFactory.parseString(
          s"""
             |cassandra-query-journal.events-by-tag.eventual-consistency-delay = 2s
             |lagom.circuit-breaker.default.enabled = off
             |elastic.url = "https://localhost:9200"
             |elastic.prefix = test-${Random.nextInt(99999)}
             |elastic.username = admin
             |elastic.password = admin
             |elastic.allowInsecure = true
             |akka.jvm-exit-on-fatal-error = false""".stripMargin
        )
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
    block: => Future[T]
  ): Future[T] = {
    val checkUntil = System.currentTimeMillis() + maxDuration.toMillis

    def doCheck(): Future[T] =
      block.recoverWith {
        case _ if checkUntil > System.currentTimeMillis() =>
          val timeout = Promise[T]()
          server.application.actorSystem.scheduler.scheduleOnce(checkEvery) {
            timeout.completeWith(doCheck())
          }(server.executionContext)
          timeout.future
      }

    doCheck()
  }

}
