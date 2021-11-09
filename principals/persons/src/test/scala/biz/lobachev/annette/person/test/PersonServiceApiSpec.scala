package biz.lobachev.annette.person.test

import akka.Done
import akka.persistence.query.Sequence
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import biz.lobachev.annette.persons.api.PersonServiceApi
import biz.lobachev.annette.persons.api.person.PersonNotFound
import biz.lobachev.annette.persons.impl.PersonServiceApplication
import biz.lobachev.annette.persons.impl.person.PersonEntity
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
import scala.concurrent.{ExecutionContext, Future}

class PersonServiceApiSpec
    extends AnyWordSpec
    with BeforeAndAfterAll
    with Matchers
    with ScalaFutures
    with OptionValues
    with PersonTestData {

  final val N = 10

  private val server = ServiceTest.startServer(
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
               |akka.jvm-exit-on-fatal-error = false""".stripMargin
          )
          .resolve()

      override lazy val readSide: ReadSideTestDriver = new ReadSideTestDriver()(materializer, executionContext)
    }
  }

  override def afterAll(): Unit = server.stop()

  implicit val pc                                 = PatienceConfig(timeout = scaled(Span(2000, Millis)), interval = scaled(Span(200, Millis)))
  private implicit val exeCxt: ExecutionContext   = server.actorSystem.dispatcher
  private implicit val materializer: Materializer = server.materializer
  private val client                              = server.serviceClient.implement[PersonServiceApi]
  private val testDriver                          = server.application.readSide
//  private val dbDao                               = server.application.personDbDao
  private val offset                              = new AtomicInteger()

  "Person service" should {

    "getPerson for existing entity" in {
      val createPayload = generateCreatePersonPayload()

      withClue("Person is not expected to exist") {
        client
          .getPersonById(createPayload.id, false)
          .invoke()
          .recover { case th: Throwable => th }
          .futureValue shouldBe PersonNotFound(createPayload.id)
      }
      val (created, found) = (for {
        created <- client.createPerson.invoke(createPayload)
        found   <- client.getPersonById(createPayload.id, false).invoke()
      } yield (created, found)).futureValue

      created shouldBe Done
      found shouldBe convertToPerson(createPayload, found.updatedAt)

    }

    "getPersonsById for existing entities" in {
      val createPayloads   = (1 to N).map(_ => generateCreatePersonPayload())
      val createPayloadMap = createPayloads.map(p => p.id -> p).toMap
      withClue("Persons are not expected to exist") {
        client
          .getPersonsById(false)
          .invoke(createPayloads.map(_.id).toSet)
          .futureValue
          .size shouldBe 0
      }
      val found            = (for {
        _     <- Source(createPayloads)
                   .mapAsync(1)(entity => client.createPerson.invoke(entity))
                   .runWith(Sink.ignore)
        found <- client.getPersonsById(false).invoke(createPayloads.map(_.id).toSet)
      } yield found).futureValue

      found shouldBe found.map { case v => convertToPerson(createPayloadMap(v.id), v.updatedAt) }
    }
  }

  "PersonDbDao " should {
    "getPerson for existing entity (read side)" in {
      val createPayload = generateCreatePersonPayload()
      withClue("Person is not expected to exist") {
        client
          .getPersonById(createPayload.id, true)
          .invoke()
          .recover { case th: Throwable => th }
          .futureValue shouldBe PersonNotFound(createPayload.id)
      }
      val found         = (for {
        _     <- feedEvent(createPayload.id, convertToPersonCreated(createPayload))
        found <- client.getPersonById(createPayload.id, true).invoke()
      } yield found).futureValue

      found shouldBe convertToPerson(createPayload, found.updatedAt)

    }

    "getPersonsById for existing entities" in {
      val createPayloads   = (1 to N).map(_ => generateCreatePersonPayload())
      val createPayloadMap = createPayloads.map(p => p.id -> p).toMap
      withClue("Persons are not expected to exist") {
        client
          .getPersonsById(true)
          .invoke(createPayloads.map(_.id).toSet)
          .futureValue
          .size shouldBe 0
      }
      val found            = (for {
        _     <- Source(createPayloads)
                   .mapAsync(1)(entity => feedEvent(entity.id, convertToPersonCreated(entity)))
                   .runWith(Sink.ignore)
        found <- client.getPersonsById(true).invoke(createPayloads.map(_.id).toSet)
      } yield found).futureValue

      found shouldBe found.map { case v => convertToPerson(createPayloadMap(v.id), v.updatedAt) }
    }
  }

  private def feedEvent(id: String, event: PersonEntity.Event): Future[Done] =
    testDriver.feed(id, event, Sequence(offset.getAndIncrement.toLong))

}
