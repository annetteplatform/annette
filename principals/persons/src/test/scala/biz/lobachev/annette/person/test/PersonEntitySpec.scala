package biz.lobachev.annette.person.test

import java.util.UUID

import akka.actor.testkit.typed.scaladsl.{LogCapturing, ScalaTestWithActorTestKit}
import akka.persistence.typed.PersistenceId
import biz.lobachev.annette.persons.impl.person.PersonEntity
import org.scalatest.wordspec.AnyWordSpecLike

class PersonEntitySpec
    extends ScalaTestWithActorTestKit(
      s"""
         |akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
         |akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
         |akka.persistence.snapshot-store.local.dir = "target/snapshot-${UUID.randomUUID().toString}"
         |""".stripMargin
    )
    with AnyWordSpecLike
    with LogCapturing
    with PersonTestData {

  "PersonEntity" must {

    "create person" in {

      val payload = generateCreatePersonPayload()
      val probe   = createTestProbe[PersonEntity.Confirmation]()
      val entity  = spawn(PersonEntity(PersistenceId("PersonEntity", payload.id)))

      entity ! PersonEntity.CreatePerson(payload, probe.ref)
      val result = probe.receiveMessage()
      result shouldBe PersonEntity.Success

      entity ! PersonEntity.GetPerson(payload.id, probe.ref)
      val result3       = probe.receiveMessage()
      val resultEntity3 = result3.asInstanceOf[PersonEntity.SuccessPerson].entity
      resultEntity3 shouldBe convertToPerson(payload, resultEntity3.updatedAt)
    }

    "create person with same id" in {

      val payload      = generateCreatePersonPayload()
      val probe        = createTestProbe[PersonEntity.Confirmation]()
      val personEntity = spawn(PersonEntity(PersistenceId("PersonEntity", payload.id)))
      personEntity ! PersonEntity.CreatePerson(payload, probe.ref)
      val result       = probe.receiveMessage()
      result shouldBe PersonEntity.Success

      personEntity ! PersonEntity.CreatePerson(payload, probe.ref)
      val result2 = probe.receiveMessage()
      result2 shouldBe a[PersonEntity.AlreadyExist.type]
    }

    "update person" in {

      val createPayload = generateCreatePersonPayload()
      val probe         = createTestProbe[PersonEntity.Confirmation]()
      val entity        = spawn(PersonEntity(PersistenceId("PersonEntity", createPayload.id)))
      entity ! PersonEntity.CreatePerson(createPayload, probe.ref)
      val result1       = probe.receiveMessage()
      result1 shouldBe PersonEntity.Success

      val updatePayload = generateUpdatePersonPayload(id = createPayload.id)

      entity ! PersonEntity.UpdatePerson(updatePayload, probe.ref)
      val result2 = probe.receiveMessage()
      result2 shouldBe PersonEntity.Success

      entity ! PersonEntity.GetPerson(updatePayload.id, probe.ref)
      val result3       = probe.receiveMessage()
      val resultEntity3 = result3.asInstanceOf[PersonEntity.SuccessPerson].entity
      resultEntity3 shouldBe convertToPerson(updatePayload, resultEntity3.updatedAt)
    }

    "update nonexisting person" in {

      val updatePayload = generateUpdatePersonPayload()
      val probe         = createTestProbe[PersonEntity.Confirmation]()
      val entity        = spawn(PersonEntity(PersistenceId("PersonEntity", updatePayload.id)))
      entity ! PersonEntity.UpdatePerson(updatePayload, probe.ref)
      val result1       = probe.receiveMessage()
      result1 shouldBe a[PersonEntity.NotFound.type]

    }

    "delete person" in {

      val createPayload = generateCreatePersonPayload()
      val probe         = createTestProbe[PersonEntity.Confirmation]()
      val entity        = spawn(PersonEntity(PersistenceId("PersonEntity", createPayload.id)))
      entity ! PersonEntity.CreatePerson(createPayload, probe.ref)
      val result1       = probe.receiveMessage()
      result1 shouldBe PersonEntity.Success

      entity ! PersonEntity.GetPerson(createPayload.id, probe.ref)
      val result2       = probe.receiveMessage()
      val resultEntity2 = result2.asInstanceOf[PersonEntity.SuccessPerson].entity
      resultEntity2 shouldBe convertToPerson(createPayload, resultEntity2.updatedAt)

      val deletePayload = generateDeletePersonPayload(createPayload.id)
      entity ! PersonEntity.DeletePerson(deletePayload, probe.ref)
      val result3       = probe.receiveMessage()
      result3 shouldBe PersonEntity.Success

      entity ! PersonEntity.GetPerson(createPayload.id, probe.ref)
      val result4 = probe.receiveMessage()
      result4 shouldBe a[PersonEntity.NotFound.type]

    }

    "deactivate nonexisting person" in {
      val id                  = generateId
      val probe               = createTestProbe[PersonEntity.Confirmation]()
      val entity              = spawn(PersonEntity(PersistenceId("PersonEntity", id)))
      val deletePersonPayload = generateDeletePersonPayload(id)
      entity ! PersonEntity.DeletePerson(deletePersonPayload, probe.ref)
      val result2             = probe.receiveMessage()
      result2 shouldBe a[PersonEntity.NotFound.type]
    }

  }

}
