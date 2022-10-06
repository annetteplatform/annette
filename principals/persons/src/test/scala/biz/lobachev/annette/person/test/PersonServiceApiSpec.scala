package biz.lobachev.annette.person.test

import akka.Done
import akka.persistence.query.Sequence
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import biz.lobachev.annette.persons.api.PersonServiceApi
import biz.lobachev.annette.persons.api.person.{DeletePersonPayload, PersonNotFound}
import biz.lobachev.annette.persons.impl.PersonServiceCasApplication
import biz.lobachev.annette.persons.impl.person.PersonEntity
import biz.lobachev.annette.persons.impl.person.PersonEntity.PersonDeleted
import com.lightbend.lagom.scaladsl.api.AdditionalConfiguration
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.{ReadSideTestDriver, ServiceTest}
import com.typesafe.config.ConfigFactory
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Span}
import org.scalatest.wordspec.AnyWordSpec

import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.duration.SECONDS
import scala.concurrent.{ExecutionContext, Future}

class PersonServiceApiSpec
    extends AnyWordSpec
    with BeforeAndAfterAll
    with Matchers
    with ScalaFutures
    with OptionValues
    with PersonTestData {

  final val N = 100

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup
      .withCassandra(true)
  ) { ctx =>
    new PersonServiceCasApplication(ctx) with LocalServiceLocator {
      override def additionalConfiguration: AdditionalConfiguration =
        super.additionalConfiguration ++ ConfigFactory
          .parseString(
            s"""
               |cassandra-query-journal.events-by-tag.eventual-consistency-delay = 0s
               |lagom.circuit-breaker.default.enabled = off
               |akka.jvm-exit-on-fatal-error = false""".stripMargin
          )
          .resolve()

      override lazy val readSide: ReadSideTestDriver = new ReadSideTestDriver()(materializer, executionContext)
    }
  }

  override def afterAll(): Unit = server.stop()

  implicit val pc                                 = PatienceConfig(timeout = scaled(Span(10000, Millis)), interval = scaled(Span(200, Millis)))
  private implicit val exeCxt: ExecutionContext   = server.actorSystem.dispatcher
  private implicit val materializer: Materializer = server.materializer
  private val client                              = server.serviceClient.implement[PersonServiceApi]
  private val testDriver                          = server.application.readSide
//  private val dbDao                               = server.application.personDbDao
  private val offset                              = new AtomicInteger()

  "Person service (write-side)" should {

    "create and get person" in {
      val createPayload = generateCreatePersonPayload()

      withClue("Person is not expected to exist") {
        client
          .getPersonById(createPayload.id, false)
          .invoke()
          .recover { case th: Throwable => th }
          .futureValue shouldBe PersonNotFound(createPayload.id)
      }
      val future = for {
        created <- client.createPerson.invoke(createPayload)
        found   <- client.getPersonById(createPayload.id, false).invoke()
      } yield (created, found)

      withClue("Person is created on first event") {
        whenReady(future) {
          case (created, found) =>
            created shouldBe Done
            found shouldBe convertToPerson(createPayload, found.updatedAt)
        }
      }
    }

    "create, update and delete persons" in {
      val createPayloads = (1 to N).map(_ => generateCreatePersonPayload())
      val createdMap     = createPayloads.map(p => p.id -> p).toMap
      withClue("Persons are not expected to exist") {
        client
          .getPersonsById(false)
          .invoke(createPayloads.map(_.id).toSet)
          .futureValue
          .size shouldBe 0
      }
      val createFuture   = for {
        _     <- Source(createPayloads)
                   .mapAsync(1)(entity => client.createPerson.invoke(entity))
                   .runWith(Sink.ignore)
        found <- client.getPersonsById(false).invoke(createPayloads.map(_.id).toSet)
      } yield found

      withClue("Persons are created on first event") {
        whenReady(createFuture) { found =>
          found.size shouldBe createPayloads.size
          found.map(v => v shouldBe convertToPerson(createdMap(v.id), v.updatedAt))
        }
      }

      // To ensure that events have a different timestamp
      SECONDS.sleep(2)

      val updatePayloads = createPayloads.map(p => generateUpdatePersonPayload(p.id))
      val updatedMap     = updatePayloads.map(p => p.id -> p).toMap

      val updateFuture = for {
        _     <- Source(updatePayloads)
                   .mapAsync(1)(entity => client.updatePerson.invoke(entity))
                   .runWith(Sink.ignore)
        found <- client.getPersonsById(false).invoke(createPayloads.map(_.id).toSet)
      } yield found

      withClue("Persons are updated on second event") {
        whenReady(updateFuture) { found =>
          found.size shouldBe updatePayloads.size
          found.map(v => v shouldBe convertToPerson(updatedMap(v.id), v.updatedAt))
        }
      }

      // To ensure that events have a different timestamp
      SECONDS.sleep(2);

      val deleteFuture = for {
        _     <- Source(updatePayloads)
                   .mapAsync(1)(entity => client.deletePerson.invoke(DeletePersonPayload(entity.id, entity.updatedBy)))
                   .runWith(Sink.ignore)
        found <- client.getPersonsById(false).invoke(createPayloads.map(_.id).toSet)
      } yield found

      withClue("Persons are deleted on third event") {
        whenReady(deleteFuture) { found =>
          found.size shouldBe 0
        }
      }

    }
  }

  "Person service (read-side) " should {
    "create and get person" in {
      val createPayload = generateCreatePersonPayload()
      withClue("Person is not expected to exist") {
        client
          .getPersonById(createPayload.id, true)
          .invoke()
          .recover { case th: Throwable => th }
          .futureValue shouldBe PersonNotFound(createPayload.id)
      }
      val future        = for {
        _     <- feedEvent(createPayload.id, convertToPersonCreated(createPayload))
        found <- client.getPersonById(createPayload.id, true).invoke()
      } yield found

      withClue("Person is created on first event") {
        whenReady(future) { found =>
          found shouldBe convertToPerson(createPayload, found.updatedAt)
        }
      }
    }

    "create, update and delete persons" in {
      val createPayloads = (1 to N).map(_ => generateCreatePersonPayload())
      val createdMap     = createPayloads.map(p => p.id -> p).toMap
      withClue("Persons are not expected to exist") {
        client
          .getPersonsById(true)
          .invoke(createPayloads.map(_.id).toSet)
          .futureValue
          .size shouldBe 0
      }
      val createFuture   = for {
        _     <- Source(createPayloads)
                   .mapAsync(1)(entity => feedEvent(entity.id, convertToPersonCreated(entity)))
                   .runWith(Sink.ignore)
        found <- client.getPersonsById(true).invoke(createPayloads.map(_.id).toSet)
      } yield found

      withClue("Persons are created on first event") {
        whenReady(createFuture) { found =>
          found.size shouldBe createPayloads.size
          found.map(v => v shouldBe convertToPerson(createdMap(v.id), v.updatedAt))
        }
      }

      // To ensure that events have a different timestamp
      SECONDS.sleep(2);

      val updatePayloads = createPayloads.map(p => generateUpdatePersonPayload(p.id))
      val updatedMap     = updatePayloads.map(p => p.id -> p).toMap

      val updateFuture = for {
        _     <- Source(updatePayloads)
                   .mapAsync(1)(entity => feedEvent(entity.id, convertToPersonUpdated(entity)))
                   .runWith(Sink.ignore)
        found <- client.getPersonsById(true).invoke(createPayloads.map(_.id).toSet)
      } yield found

      withClue("Persons are updated on second event") {
        whenReady(updateFuture) { found =>
          found.size shouldBe updatePayloads.size
          found.map(v => v shouldBe convertToPerson(updatedMap(v.id), v.updatedAt))
        }
      }

      // To ensure that events have a different timestamp
      SECONDS.sleep(2);

      val deleteFuture = for {
        _     <- Source(updatePayloads)
                   .mapAsync(1)(entity => feedEvent(entity.id, PersonDeleted(entity.id, entity.updatedBy)))
                   .runWith(Sink.ignore)
        found <- client.getPersonsById(true).invoke(updatePayloads.map(_.id).toSet)
      } yield found

      withClue("Persons are deleted on third event") {
        whenReady(deleteFuture) { found =>
          found.size shouldBe 0
        }
      }
    }
  }

  private def feedEvent(id: String, event: PersonEntity.Event): Future[Done] =
    testDriver.feed(id, event, Sequence(offset.getAndIncrement.toLong))

}
