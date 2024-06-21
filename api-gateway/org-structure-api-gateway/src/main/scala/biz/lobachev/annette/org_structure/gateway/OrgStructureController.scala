/*
 * Copyright 2013 Valery Lobachev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package biz.lobachev.annette.org_structure.gateway

import biz.lobachev.annette.api_gateway_core.authentication.{AuthenticatedAction, AuthenticatedRequest}
import biz.lobachev.annette.api_gateway_core.authorization.{AuthorizationFailedException, Authorizer}
import biz.lobachev.annette.core.attribute.{UpdateAttributesPayload, UpdateAttributesPayloadDto}
import biz.lobachev.annette.core.model.{DataSource, PersonId}
import biz.lobachev.annette.org_structure.api.OrgStructureService
import biz.lobachev.annette.org_structure.api.category._
import biz.lobachev.annette.org_structure.api.hierarchy._
import biz.lobachev.annette.org_structure.api.role._
import biz.lobachev.annette.org_structure.gateway.Permissions._
import biz.lobachev.annette.org_structure.gateway.dto._
import biz.lobachev.annette.core.utils.ChimneyCommons._
import io.scalaland.chimney.dsl._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OrgStructureController @Inject() (
  authenticated: AuthenticatedAction,
  authorizer: Authorizer,
  orgStructureService: OrgStructureService,
  cc: ControllerComponents,
  implicit val ec: ExecutionContext
) extends AbstractController(cc) {

  def createOrganization(attributes: Option[String] = None) =
    authenticated.async(parse.json[CreateOrganizationPayloadDto]) { implicit request =>
      val payload = request.body
        .into[CreateOrganizationPayload]
        .withFieldConst(_.createdBy, request.subject.principals.head)
        .transform
      authorizer.performCheck(canMaintainOrg(payload.orgId)) {
        for {
          _      <- orgStructureService.createOrganization(payload)
          result <- orgStructureService.getOrgItem(payload.orgId, DataSource.FROM_ORIGIN, attributes)
        } yield Ok(Json.toJson(result))
      }
    }

  def createUnit(attributes: Option[String] = None) =
    authenticated.async(parse.json[CreateUnitPayloadDto]) { implicit request =>
      val payload = request.body
        .into[CreateUnitPayload]
        .withFieldConst(_.createdBy, request.subject.principals.head)
        .transform
      authorizer.performCheck(canMaintainOrg(OrgItemKey.extractOrgId(payload.unitId))) {
        for {
          _      <- orgStructureService.createUnit(payload)
          result <-
            orgStructureService.getOrgItems(Set(payload.unitId, payload.parentId), DataSource.FROM_ORIGIN, attributes)
        } yield Ok(Json.toJson(result))
      }
    }

  def createPosition(attributes: Option[String] = None) =
    authenticated.async(parse.json[CreatePositionPayloadDto]) { implicit request =>
      val payload = request.body
        .into[CreatePositionPayload]
        .withFieldConst(_.createdBy, request.subject.principals.head)
        .transform
      authorizer.performCheck(canMaintainOrg(OrgItemKey.extractOrgId(payload.positionId))) {
        for {
          _      <- orgStructureService.createPosition(payload)
          result <- orgStructureService.getOrgItems(
                      Set(payload.positionId, payload.parentId),
                      DataSource.FROM_ORIGIN,
                      attributes
                    )
        } yield Ok(Json.toJson(result))
      }
    }

  def updateName(attributes: Option[String] = None) =
    authenticated.async(parse.json[UpdateNamePayloadDto]) { implicit request =>
      val payload = request.body
        .into[UpdateNamePayload]
        .withFieldConst(_.updatedBy, request.subject.principals.head)
        .transform
      authorizer.performCheck(canMaintainOrg(OrgItemKey.extractOrgId(payload.itemId))) {
        for {
          _      <- orgStructureService.updateName(payload)
          result <- orgStructureService.getOrgItem(payload.itemId, DataSource.FROM_ORIGIN, attributes)
        } yield Ok(Json.toJson(result))
      }
    }

  def assignCategory(attributes: Option[String] = None) =
    authenticated.async(parse.json[AssignCategoryPayloadDto]) { implicit request =>
      val payload = request.body
        .into[AssignCategoryPayload]
        .withFieldConst(_.updatedBy, request.subject.principals.head)
        .transform
      authorizer.performCheck(canMaintainOrg(OrgItemKey.extractOrgId(payload.itemId))) {
        for {
          _      <- orgStructureService.assignCategory(payload)
          result <- orgStructureService.getOrgItem(payload.itemId, DataSource.FROM_ORIGIN, attributes)
        } yield Ok(Json.toJson(result))
      }
    }

  def updateSource(attributes: Option[String] = None) =
    authenticated.async(parse.json[UpdateSourcePayloadDto]) { implicit request =>
      val payload = request.body
        .into[UpdateSourcePayload]
        .withFieldConst(_.updatedBy, request.subject.principals.head)
        .transform
      authorizer.performCheck(canMaintainOrg(OrgItemKey.extractOrgId(payload.itemId))) {
        for {
          _      <- orgStructureService.updateSource(payload)
          result <- orgStructureService.getOrgItem(payload.itemId, DataSource.FROM_ORIGIN, attributes)
        } yield Ok(Json.toJson(result))
      }
    }

  def updateExternalId(attributes: Option[String] = None) =
    authenticated.async(parse.json[UpdateExternalIdPayloadDto]) { implicit request =>
      val payload = request.body
        .into[UpdateExternalIdPayload]
        .withFieldConst(_.updatedBy, request.subject.principals.head)
        .transform
      authorizer.performCheck(canMaintainOrg(OrgItemKey.extractOrgId(payload.itemId))) {
        for {
          _      <- orgStructureService.updateExternalId(payload)
          result <- orgStructureService.getOrgItem(payload.itemId, DataSource.FROM_ORIGIN, attributes)
        } yield Ok(Json.toJson(result))
      }
    }

  def moveItem =
    authenticated.async(parse.json[MoveItemPayloadDto]) { implicit request =>
      val payload = request.body
        .into[MoveItemPayload]
        .withFieldConst(_.updatedBy, request.subject.principals.head)
        .transform
      authorizer.performCheck(canMaintainOrg(OrgItemKey.extractOrgId(payload.itemId))) {
        for {
          _ <- orgStructureService.moveItem(payload)
        } yield Ok("")
      }
    }

  def updateOrgItemAttributes =
    authenticated.async(parse.json[UpdateAttributesPayloadDto]) { implicit request =>
      val payload    = request.body
        .into[UpdateAttributesPayload]
        .withFieldConst(_.updatedBy, request.subject.principals.head)
        .transform
      val attributes = payload.attributes.keys.toSeq
      authorizer.performCheck(canMaintainOrg(OrgItemKey.extractOrgId(payload.id))) {
        for {
          _         <- orgStructureService.updateOrgItemAttributes(payload)
          attrs = if (attributes.nonEmpty) Some(attributes.mkString(","))
                       else None
          entity    <- orgStructureService.getOrgItemAttributes(payload.id, DataSource.FROM_ORIGIN, attrs)
        } yield Ok(Json.toJson(entity))
      }
    }

  def assignChief(attributes: Option[String] = None) =
    authenticated.async(parse.json[AssignChiefPayloadDto]) { implicit request =>
      val payload = request.body
        .into[AssignChiefPayload]
        .withFieldConst(_.updatedBy, request.subject.principals.head)
        .transform
      authorizer.performCheck(canMaintainOrg(OrgItemKey.extractOrgId(payload.unitId))) {
        for {
          _      <- orgStructureService.assignChief(payload)
          result <- orgStructureService.getOrgItem(payload.unitId, DataSource.FROM_ORIGIN, attributes)
        } yield Ok(Json.toJson(result))
      }
    }

  def unassignChief(attributes: Option[String] = None) =
    authenticated.async(parse.json[UnassignChiefPayloadDto]) { implicit request =>
      val payload = request.body
        .into[UnassignChiefPayload]
        .withFieldConst(_.updatedBy, request.subject.principals.head)
        .transform
      authorizer.performCheck(canMaintainOrg(OrgItemKey.extractOrgId(payload.unitId))) {
        for {
          _      <- orgStructureService.unassignChief(payload)
          result <- orgStructureService.getOrgItem(payload.unitId, DataSource.FROM_ORIGIN, attributes)
        } yield Ok(Json.toJson(result))
      }
    }

  def changePositionLimit(attributes: Option[String] = None) =
    authenticated.async(parse.json[ChangePositionLimitPayloadDto]) { implicit request =>
      val payload = request.body
        .into[ChangePositionLimitPayload]
        .withFieldConst(_.updatedBy, request.subject.principals.head)
        .transform
      authorizer.performCheck(canMaintainOrg(OrgItemKey.extractOrgId(payload.positionId))) {
        for {
          _      <- orgStructureService.changePositionLimit(payload)
          result <- orgStructureService.getOrgItem(payload.positionId, DataSource.FROM_ORIGIN, attributes)
        } yield Ok(Json.toJson(result))
      }
    }

  def assignPerson(attributes: Option[String] = None) =
    authenticated.async(parse.json[AssignPersonPayloadDto]) { implicit request =>
      val payload = request.body
        .into[AssignPersonPayload]
        .withFieldConst(_.updatedBy, request.subject.principals.head)
        .transform
      authorizer.performCheck(canMaintainOrg(OrgItemKey.extractOrgId(payload.positionId))) {
        for {
          _      <- orgStructureService.assignPerson(payload)
          result <- orgStructureService.getOrgItem(payload.positionId, DataSource.FROM_ORIGIN, attributes)
        } yield Ok(Json.toJson(result))
      }
    }

  def unassignPerson(attributes: Option[String] = None) =
    authenticated.async(parse.json[UnassignPersonPayloadDto]) { implicit request =>
      val payload = request.body
        .into[UnassignPersonPayload]
        .withFieldConst(_.updatedBy, request.subject.principals.head)
        .transform
      authorizer.performCheck(canMaintainOrg(OrgItemKey.extractOrgId(payload.positionId))) {
        for {
          _      <- orgStructureService.unassignPerson(payload)
          result <- orgStructureService.getOrgItem(payload.positionId, DataSource.FROM_ORIGIN, attributes)
        } yield Ok(Json.toJson(result))
      }
    }

  def assignOrgRole(attributes: Option[String] = None) =
    authenticated.async(parse.json[AssignOrgRolePayloadDto]) { implicit request =>
      val payload = request.body
        .into[AssignOrgRolePayload]
        .withFieldConst(_.updatedBy, request.subject.principals.head)
        .transform
      authorizer.performCheck(canMaintainOrg(OrgItemKey.extractOrgId(payload.positionId))) {
        for {
          _      <- orgStructureService.assignOrgRole(payload)
          result <- orgStructureService.getOrgItem(payload.positionId, DataSource.FROM_ORIGIN, attributes)
        } yield Ok(Json.toJson(result))
      }
    }

  def unassignOrgRole(attributes: Option[String] = None) =
    authenticated.async(parse.json[UnassignOrgRolePayloadDto]) { implicit request =>
      val payload = request.body
        .into[UnassignOrgRolePayload]
        .withFieldConst(_.updatedBy, request.subject.principals.head)
        .transform
      authorizer.performCheck(canMaintainOrg(OrgItemKey.extractOrgId(payload.positionId))) {
        for {
          _      <- orgStructureService.unassignOrgRole(payload)
          result <- orgStructureService.getOrgItem(payload.positionId, DataSource.FROM_ORIGIN, attributes)
        } yield Ok(Json.toJson(result))
      }
    }

  def deleteOrgItem =
    authenticated.async(parse.json[DeleteOrgItemPayloadDto]) { implicit request =>
      val payload = request.body
        .into[DeleteOrgItemPayload]
        .withFieldConst(_.deletedBy, request.subject.principals.head)
        .transform
      authorizer.performCheck(canMaintainOrg(OrgItemKey.extractOrgId(payload.itemId))) {
        for {
          _ <- orgStructureService.deleteOrgItem(payload)
        } yield Ok("")
      }
    }

  def getOrganization(orgId: CompositeOrgItemId) =
    authenticated.async { implicit request =>
      authorizer.performCheck(canViewOrMaintainOrg(orgId)) {
        for {
          result <- orgStructureService.getOrganization(orgId)
        } yield Ok(Json.toJson(result))
      }
    }

  def getOrganizationTree(itemId: CompositeOrgItemId) =
    authenticated.async { implicit request =>
      authorizer.performCheck(canViewOrMaintainOrg(OrgItemKey.extractOrgId(itemId))) {
        for {
          result <- orgStructureService.getOrganizationTree(itemId)
        } yield Ok(Json.toJson(result))
      }
    }

  def getOrgItem(id: CompositeOrgItemId, source: Option[String], attributes: Option[String] = None) =
    authenticated.async { implicit request =>
      authorizer.performCheck(canViewOrMaintainOrg(OrgItemKey.extractOrgId(id))) {
        for {
          result <- orgStructureService.getOrgItem(id, source, attributes)
        } yield Ok(Json.toJson(result))
      }
    }

  def getOrgItems(source: Option[String], attributes: Option[String] = None) =
    authenticated.async(parse.json[Set[CompositeOrgItemId]]) { implicit request =>
      val ids = request.body
      authorizer.performCheck(canViewOrMaintainOrg(OrgItemKey.extractOrgId(ids.head))) {
        for {
          result <- orgStructureService.getOrgItems(ids, source, attributes)
        } yield Ok(Json.toJson(result))
      }
    }

  def findOrgItems =
    authenticated.async(parse.json[OrgItemFindQuery]) { implicit request =>
      val query = request.body
      for {
        allowedOrgs <- getViewOrMaintainOrgs()
        items       <- allowedOrgs match {
                         case AllowedAll       => orgStructureService.findOrgItems(query)
                         // TODO: restrict query by org ids
                         case _: AllowedOrgSet => orgStructureService.findOrgItems(query)
                         case AllowedNone      => Future.failed(AuthorizationFailedException())

                       }
      } yield Ok(Json.toJson(items))
    }

  def getOrgItemMetadata =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(VIEW_ALL_HIERARCHIES, MAINTAIN_ALL_HIERARCHIES) {
        for {
          meta <- orgStructureService.getOrgItemMetadata

        } yield Ok(Json.toJson(meta))
      }
    }

  def getOrgItemAttributes(id: CompositeOrgItemId, source: Option[String], attributes: Option[String] = None) =
    authenticated.async { implicit request =>
      authorizer.performCheck(canViewOrMaintainOrg(OrgItemKey.extractOrgId(id))) {
        for {
          attributes <- orgStructureService.getOrgItemAttributes(id, source, attributes)
        } yield Ok(Json.toJson(attributes))
      }
    }

  def getOrgItemsAttributes(source: Option[String], attributes: Option[String] = None) =
    authenticated.async(parse.json[Set[CompositeOrgItemId]]) { implicit request =>
      val ids = request.body
      authorizer.performCheck(canViewOrMaintainOrg(OrgItemKey.extractOrgId(ids.head))) {
        for {
          attributes <- orgStructureService.getOrgItemsAttributes(ids, source, attributes)
        } yield Ok(Json.toJson(attributes))
      }
    }

  def getPersonPrincipals(personId: PersonId) =
    authenticated.async { implicit request =>
      for {
        allowedOrgs <- getViewOrMaintainOrgs()
        items       <- allowedOrgs match {
                         case AllowedAll       => orgStructureService.getPersonPrincipals(personId)
                         // TODO: Filter by allowed orgs
                         case _: AllowedOrgSet => orgStructureService.getPersonPrincipals(personId)
                         case AllowedNone      => Future.failed(AuthorizationFailedException())

                       }
      } yield Ok(Json.toJson(items))
    }

  def getPersonPositions(personId: PersonId) =
    authenticated.async { implicit request =>
      for {
        allowedOrgs <- getViewOrMaintainOrgs()
        items       <- allowedOrgs match {
                         case AllowedAll       => orgStructureService.getPersonPositions(personId)
                         // TODO: Filter by allowed orgs
                         case _: AllowedOrgSet => orgStructureService.getPersonPositions(personId)
                         case AllowedNone      => Future.failed(AuthorizationFailedException())

                       }
      } yield Ok(Json.toJson(items))
    }

  // org role methods

  def createOrgRole =
    authenticated.async(parse.json[OrgRoleDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_ORG_ROLES) {
        val payload = request.body
          .into[CreateOrgRolePayload]
          .withFieldConst(_.createdBy, request.subject.principals.head)
          .transform
        for {
          _    <- orgStructureService.createOrgRole(payload)
          role <- orgStructureService.getOrgRole(payload.id, DataSource.FROM_ORIGIN)
        } yield Ok(Json.toJson(role))
      }
    }

  def updateOrgRole =
    authenticated.async(parse.json[OrgRoleDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_ORG_ROLES) {
        val payload = request.body
          .into[UpdateOrgRolePayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _    <- orgStructureService.updateOrgRole(payload)
          role <- orgStructureService.getOrgRole(payload.id, DataSource.FROM_ORIGIN)
        } yield Ok(Json.toJson(role))
      }
    }

  def deleteOrgRole =
    authenticated.async(parse.json[DeleteOrgRoleDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_ORG_ROLES) {
        val payload = request.body
          .into[DeleteOrgRolePayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _ <- orgStructureService.deleteOrgRole(payload)
        } yield Ok("")
      }
    }

  def getOrgRole(id: OrgRoleId, source: Option[String]) =
    if (DataSource.fromOrigin(source))
      authenticated.async { implicit request =>
        authorizer.performCheckAny(MAINTAIN_ALL_ORG_ROLES) {
          for {
            role <- orgStructureService.getOrgRole(id, source)
          } yield Ok(Json.toJson(role))
        }
      }
    else
      authenticated.async { implicit request =>
        authorizer.performCheckAny(VIEW_ALL_ORG_ROLES, MAINTAIN_ALL_ORG_ROLES) {
          for {
            role <- orgStructureService.getOrgRole(id, source)
          } yield Ok(Json.toJson(role))
        }
      }

  def getOrgRoles(source: Option[String]) =
    if (DataSource.fromOrigin(source))
      authenticated.async(parse.json[Set[OrgRoleId]]) { implicit request =>
        val ids = request.body
        authorizer.performCheckAny(MAINTAIN_ALL_ORG_ROLES) {
          for {
            roles <- orgStructureService.getOrgRoles(ids, source)
          } yield Ok(Json.toJson(roles))
        }
      }
    else
      authenticated.async(parse.json[Set[OrgRoleId]]) { implicit request =>
        val ids = request.body
        authorizer.performCheckAny(VIEW_ALL_ORG_ROLES, MAINTAIN_ALL_ORG_ROLES) {
          for {
            roles <- orgStructureService.getOrgRoles(ids, source)
          } yield Ok(Json.toJson(roles))
        }
      }

  def findOrgRoles =
    authenticated.async(parse.json[OrgRoleFindQuery]) { implicit request =>
      val query = request.body
      authorizer.performCheckAny(VIEW_ALL_ORG_ROLES, MAINTAIN_ALL_ORG_ROLES) {
        for {
          result <- orgStructureService.findOrgRoles(query)
        } yield Ok(Json.toJson(result))
      }
    }

  // category methods

  def createCategory =
    authenticated.async(parse.json[OrgCategoryDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_ORG_CATEGORIES) {
        val payload = request.body
          .into[CreateCategoryPayload]
          .withFieldConst(_.createdBy, request.subject.principals.head)
          .transform
        for {
          _    <- orgStructureService.createCategory(payload)
          role <- orgStructureService.getCategory(payload.id, DataSource.FROM_ORIGIN)
        } yield Ok(Json.toJson(role))
      }
    }

  def updateCategory =
    authenticated.async(parse.json[OrgCategoryDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_ORG_CATEGORIES) {
        val payload = request.body
          .into[UpdateCategoryPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _    <- orgStructureService.updateCategory(payload)
          role <- orgStructureService.getCategory(payload.id, DataSource.FROM_ORIGIN)
        } yield Ok(Json.toJson(role))
      }
    }

  def deleteCategory =
    authenticated.async(parse.json[DeleteOrgCategoryDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_ORG_CATEGORIES) {
        val payload = request.body
          .into[DeleteCategoryPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _ <- orgStructureService.deleteCategory(payload)
        } yield Ok("")
      }
    }

  def getCategory(id: OrgCategoryId, source: Option[String]) =
    if (DataSource.fromOrigin(source))
      authenticated.async { implicit request =>
        authorizer.performCheckAny(MAINTAIN_ALL_ORG_CATEGORIES) {
          for {
            role <- orgStructureService.getCategory(id, source)
          } yield Ok(Json.toJson(role))
        }
      }
    else
      authenticated.async { implicit request =>
        authorizer.performCheckAny(VIEW_ALL_ORG_CATEGORIES, MAINTAIN_ALL_ORG_CATEGORIES) {
          for {
            role <- orgStructureService.getCategory(id, source)
          } yield Ok(Json.toJson(role))
        }
      }

  def getCategories(source: Option[String]) =
    if (DataSource.fromOrigin(source))
      authenticated.async(parse.json[Set[OrgCategoryId]]) { implicit request =>
        val ids = request.body
        authorizer.performCheckAny(MAINTAIN_ALL_ORG_CATEGORIES) {
          for {
            roles <- orgStructureService.getCategories(ids, source)
          } yield Ok(Json.toJson(roles))
        }
      }
    else
      authenticated.async(parse.json[Set[OrgCategoryId]]) { implicit request =>
        val ids = request.body
        authorizer.performCheckAny(VIEW_ALL_ORG_CATEGORIES, MAINTAIN_ALL_ORG_CATEGORIES) {
          for {
            roles <- orgStructureService.getCategories(ids, source)
          } yield Ok(Json.toJson(roles))
        }
      }

  def findCategories =
    authenticated.async(parse.json[OrgCategoryFindQuery]) { implicit request =>
      val query = request.body
      authorizer.performCheckAny(VIEW_ALL_ORG_CATEGORIES, MAINTAIN_ALL_ORG_CATEGORIES) {
        for {
          result <- orgStructureService.findCategories(query)
        } yield Ok(Json.toJson(result))
      }
    }

  private def canMaintainOrg[A](orgId: CompositeOrgItemId)(implicit request: AuthenticatedRequest[A]): Future[Boolean] =
    authorizer.checkAny(MAINTAIN_ALL_HIERARCHIES, MAINTAIN_ORG_HIERARCHY(orgId))

  private def canViewOrMaintainOrg[A](
    orgId: CompositeOrgItemId
  )(implicit request: AuthenticatedRequest[A]): Future[Boolean] =
    authorizer.checkAny(
      VIEW_ALL_HIERARCHIES,
      VIEW_ORG_HIERARCHY(orgId),
      MAINTAIN_ALL_HIERARCHIES,
      MAINTAIN_ORG_HIERARCHY(orgId)
    )

  private def getViewOrMaintainOrgs[A]()(implicit request: AuthenticatedRequest[A]): Future[AllowedOrgs] =
    for {
      allowAll <- authorizer.checkAny(VIEW_ALL_HIERARCHIES, MAINTAIN_ALL_HIERARCHIES)
      result   <- if (allowAll) Future.successful(AllowedAll)
                  else findViewOrMaintainPermissions()
    } yield result

  private def findViewOrMaintainPermissions[A]()(implicit request: AuthenticatedRequest[A]): Future[AllowedOrgs] =
    for {
      orgs <- authorizer
                .findPermissions(VIEW_ORG_HIERARCHY_PERMISSION_ID, MAINTAIN_ORG_HIERARCHY_PERMISSION_ID)
                .map(_.map(_.permission.arg1))
    } yield
      if (orgs.nonEmpty) AllowedOrgSet(orgs)
      else AllowedNone

}
