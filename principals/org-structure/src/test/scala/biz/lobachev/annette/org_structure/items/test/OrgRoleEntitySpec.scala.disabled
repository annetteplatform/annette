package biz.lobachev.annette.org_structure.items.test

import java.util.UUID

import akka.actor.testkit.typed.scaladsl.{LogCapturing, ScalaTestWithActorTestKit}
import akka.persistence.typed.PersistenceId
import biz.lobachev.annette.org_structure.impl.role.OrgRoleEntity
import org.scalatest.wordspec.AnyWordSpecLike

class OrgRoleEntitySpec
    extends ScalaTestWithActorTestKit(
      s"""
         |akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
         |akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
         |akka.persistence.snapshot-store.local.dir = "target/snapshot-${UUID.randomUUID().toString}"
         |""".stripMargin
    )
    with AnyWordSpecLike
    with LogCapturing
    with OrgRoleTestData {

  "OrgRoleEntity" must {

    "create orgRole" in {

      val payload = generateCreateOrgRolePayload()
      val probe   = createTestProbe[OrgRoleEntity.Confirmation]()
      val entity  = spawn(OrgRoleEntity(PersistenceId("OrgRoleEntity", payload.id)))
      entity ! OrgRoleEntity.CreateOrgRole(payload, probe.ref)

      val result = probe.receiveMessage()
      result shouldBe OrgRoleEntity.Success

      entity ! OrgRoleEntity.GetOrgRole(payload.id, probe.ref)
      val result2       = probe.receiveMessage()
      val resultEntity2 = result2.asInstanceOf[OrgRoleEntity.SuccessOrgRole].entity
      resultEntity2 shouldBe convertToOrgRole(payload, resultEntity2.updatedAt)
    }

    "create orgRole with same id" in {

      val payload = generateCreateOrgRolePayload()
      val probe   = createTestProbe[OrgRoleEntity.Confirmation]()
      val entity  = spawn(OrgRoleEntity(PersistenceId("OrgRoleEntity", payload.id)))
      entity ! OrgRoleEntity.CreateOrgRole(payload, probe.ref)
      val result  = probe.receiveMessage()
      result shouldBe OrgRoleEntity.Success

      entity ! OrgRoleEntity.CreateOrgRole(payload, probe.ref)
      val result2 = probe.receiveMessage()
      result2 shouldBe OrgRoleEntity.AlreadyExist
    }

    "update orgRole" in {

      val payload = generateCreateOrgRolePayload()
      val probe   = createTestProbe[OrgRoleEntity.Confirmation]()
      val entity  = spawn(OrgRoleEntity(PersistenceId("OrgRoleEntity", payload.id)))
      entity ! OrgRoleEntity.CreateOrgRole(payload, probe.ref)
      val result1 = probe.receiveMessage()
      result1 shouldBe OrgRoleEntity.Success

      val updatePayload = generateUpdateOrgRolePayload(id = payload.id)
      entity ! OrgRoleEntity.UpdateOrgRole(updatePayload, probe.ref)
      val result2       = probe.receiveMessage()
      result2 shouldBe OrgRoleEntity.Success

      entity ! OrgRoleEntity.GetOrgRole(payload.id, probe.ref)
      val result3       = probe.receiveMessage()
      val resultEntity3 = result3.asInstanceOf[OrgRoleEntity.SuccessOrgRole].entity
      resultEntity3 shouldBe convertToOrgRole(updatePayload, resultEntity3.updatedAt)

    }

    "update nonexisting orgRole" in {

      val updatePayload = generateUpdateOrgRolePayload()
      val probe         = createTestProbe[OrgRoleEntity.Confirmation]()
      val entity        = spawn(OrgRoleEntity(PersistenceId("OrgRoleEntity", updatePayload.id)))
      entity ! OrgRoleEntity.UpdateOrgRole(updatePayload, probe.ref)
      val result1       = probe.receiveMessage()
      result1 shouldBe OrgRoleEntity.NotFound

    }

    "delete orgRole" in {

      val payload = generateCreateOrgRolePayload()
      val probe   = createTestProbe[OrgRoleEntity.Confirmation]()
      val entity  = spawn(OrgRoleEntity(PersistenceId("OrgRoleEntity", payload.id)))
      entity ! OrgRoleEntity.CreateOrgRole(payload, probe.ref)
      probe.receiveMessage() shouldBe OrgRoleEntity.Success

      val deactivatePayload = generateDeactivateOrgRolePayload(payload.id)
      entity ! OrgRoleEntity.DeleteOrgRole(deactivatePayload, probe.ref)
      probe.receiveMessage() shouldBe OrgRoleEntity.Success

      entity ! OrgRoleEntity.GetOrgRole(payload.id, probe.ref)
      probe.receiveMessage() shouldBe OrgRoleEntity.NotFound
    }

    "delete nonexisting orgRole" in {
      val id                = generateId
      val probe             = createTestProbe[OrgRoleEntity.Confirmation]()
      val entity            = spawn(OrgRoleEntity(PersistenceId("OrgRoleEntity", id)))
      val deactivatePayload = generateDeactivateOrgRolePayload(id)
      entity ! OrgRoleEntity.DeleteOrgRole(deactivatePayload, probe.ref)
      val result2           = probe.receiveMessage()
      result2 shouldBe OrgRoleEntity.NotFound
    }

  }

}
