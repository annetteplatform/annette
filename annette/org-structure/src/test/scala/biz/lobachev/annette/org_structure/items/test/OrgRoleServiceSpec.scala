package biz.lobachev.annette.org_structure.items.test

import akka.Done
import biz.lobachev.annette.org_structure.api.OrgStructureServiceApi
import biz.lobachev.annette.org_structure.api.role._

import scala.concurrent.Future

class OrgRoleServiceSpec extends AbstractOrgItemsServiceSpec with OrgRoleTestData {
  final val N = 10

  lazy val client = server.serviceClient.implement[OrgStructureServiceApi]

  "orgRole service" should {

    "getOrgRole for existing entity" in {
      val createPayload = generateCreateOrgRolePayload()
      for {
        created <- client.createOrgRole.invoke(createPayload)
        found   <- client.getOrgRoleById(createPayload.id, false).invoke()
      } yield {
        created shouldBe Done
        found shouldBe convertToOrgRole(createPayload, found.updatedAt)
      }
    }

    "getOrgRole for non-existing entity" in {
      val createPayload = generateCreateOrgRolePayload()
      for {
        found <- client.getOrgRoleById(createPayload.id, false).invoke().recover { case th: Throwable => th }
      } yield found shouldBe OrgRoleNotFound(createPayload.id)
    }

    "getOrgRoles" in {
      val createPayloads   = (1 to N).map(_ => generateCreateOrgRolePayload())
      val createPayloadMap = createPayloads.map(p => p.id -> p).toMap
      for {
        _     <- Future.traverse(createPayloads)(entity => client.createOrgRole.invoke(entity))
        found <- client.getOrgRolesById(false).invoke(createPayloads.map(_.id).toSet)
      } yield found shouldBe found.map(f => convertToOrgRole(createPayloadMap(f.id), f.updatedAt))
    }

    "getOrgRole for existing entity (read side)" in {
      val createPayload = generateCreateOrgRolePayload()
      (for {
        _ <- client.createOrgRole.invoke(createPayload)
      } yield awaitSuccess() {
        for {
          found <- client.getOrgRoleById(createPayload.id, true).invoke()
        } yield found shouldBe convertToOrgRole(createPayload, found.updatedAt)
      }).flatMap(identity)
    }

    "getOrgRoles (read side)" in {
      val createPayloads   = (1 to N).map(_ => generateCreateOrgRolePayload())
      val createPayloadMap = createPayloads.map(p => p.id -> p).toMap
      (for {
        _ <- Future.traverse(createPayloads)(entity => client.createOrgRole.invoke(entity))
      } yield awaitSuccess() {
        for {
          found <- client.getOrgRolesById(true).invoke(createPayloads.map(_.id).toSet)
        } yield {

          val r = found shouldBe found.map(f => convertToOrgRole(createPayloadMap(f.id), f.updatedAt))
          println("success")
          r
        }
      }).flatMap(identity)
    }

  }

}
