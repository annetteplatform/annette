package biz.lobachev.annette.org_structure.items.test

import java.time.OffsetDateTime
import biz.lobachev.annette.core.model.OrgRolePrincipal
import biz.lobachev.annette.core.model.auth.{AnnettePrincipal, OrgRolePrincipal}
import biz.lobachev.annette.core.test.generator.RandomGenerator
import biz.lobachev.annette.org_structure.api.role._
import biz.lobachev.annette.core.utils.ChimneyCommons._
import io.scalaland.chimney.dsl._

trait OrgRoleTestData extends RandomGenerator {

  def generateCreateOrgRolePayload(
    id: String = generateId,
    name: String = generateWord(),
    description: String = generateText(),
    createdBy: AnnettePrincipal = OrgRolePrincipal(generateWord())
  ) =
    CreateOrgRolePayload(
      id = id,
      name,
      description,
      createdBy
    )

  def generateUpdateOrgRolePayload(
    id: String = generateId,
    name: String = generateWord(),
    description: String = generateText(),
    updatedBy: AnnettePrincipal = OrgRolePrincipal(generateWord())
  ) =
    UpdateOrgRolePayload(
      id = id,
      name,
      description,
      updatedBy
    )

  def generateDeactivateOrgRolePayload(
    id: String = generateId,
    updatedBy: AnnettePrincipal = OrgRolePrincipal(generateWord())
  ) =
    DeleteOrgRolePayload(
      id = id,
      updatedBy
    )

  def convertToOrgRole(payload: CreateOrgRolePayload, createdAt: OffsetDateTime) =
    payload
      .into[OrgRole]
      .withFieldComputed(_.updatedBy, _.createdBy)
      .withFieldConst(_.updatedAt, createdAt)
      .transform

  def convertToOrgRole(payload: UpdateOrgRolePayload, updatedAt: OffsetDateTime) =
    payload
      .into[OrgRole]
      .withFieldConst(_.updatedAt, updatedAt)
      .transform

}
