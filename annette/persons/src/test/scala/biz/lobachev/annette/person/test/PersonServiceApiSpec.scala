package biz.lobachev.annette.person.test

import java.util.UUID
import akka.Done
import akka.stream.scaladsl.{Sink, Source}
import biz.lobachev.annette.persons.api.PersonServiceApi
import biz.lobachev.annette.persons.api.person.PersonNotFound

import scala.concurrent.Future

class PersonServiceApiSpec extends AbstractPersonServiceApiSpec with PersonTestData {
  final val N = 10

  lazy val client           = server.serviceClient.implement[PersonServiceApi]
  implicit val materializer = server.materializer

  "person service" should {

    "getPerson for existing entity" in {
      val createPayload = generateCreatePersonPayload()
      for {
        created <- client.createPerson.invoke(createPayload)
        found   <- client.getPersonById(createPayload.id, false).invoke()
      } yield {
        created shouldBe Done
        found shouldBe convertToPerson(createPayload, found.updatedAt)
      }
    }

    "getPerson for non-existing entity" in {
      val id = UUID.randomUUID().toString
      for {
        found <- client.getPersonById(id, false).invoke().recover { case th: Throwable => th }
      } yield found shouldBe PersonNotFound(id)
    }

    "getPersons" in {
      val createPayloads   = (1 to N).map(_ => generateCreatePersonPayload())
      val createPayloadMap = createPayloads.map(p => p.id -> p).toMap
      for {
        _     <- Source(createPayloads)
                   .mapAsync(1)(entity => client.createPerson.invoke(entity))
                   .runWith(Sink.ignore)
        found <- client.getPersonsById(false).invoke(createPayloads.map(_.id).toSet)
      } yield found shouldBe found.map { case (id, v) => id -> convertToPerson(createPayloadMap(id), v.updatedAt) }
    }

    "getPerson for existing entity (read side)" in {
      val createPayload = generateCreatePersonPayload()
      (for {
        created <- client.createPerson.invoke(createPayload)
      } yield awaitSuccess() {
        for {
          found <- client.getPersonById(createPayload.id).invoke()
        } yield {
          created shouldBe Done
          found shouldBe convertToPerson(createPayload, found.updatedAt)
        }
      }).flatMap(identity)
    }

    "getPersons (read side)" in {
      val createPayloads   = (1 to N).map(_ => generateCreatePersonPayload())
      val createPayloadMap = createPayloads.map(p => p.id -> p).toMap
      (for {
        _ <- Source(createPayloads)
               .mapAsync(1)(entity => client.createPerson.invoke(entity))
               .runWith(Sink.ignore)
      } yield awaitSuccess() {
        for {
          found <- client.getPersonsById().invoke(createPayloads.map(_.id).toSet)
        } yield found shouldBe found.map { case (id, v) => id -> convertToPerson(createPayloadMap(id), v.updatedAt) }
      }).flatMap(identity)
    }

  }

}
