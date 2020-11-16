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

package biz.lobachev.annette.gateway.api.org_structure

import biz.lobachev.annette.core.model.PersonId
import biz.lobachev.annette.gateway.api.org_structure.Permissions._
import biz.lobachev.annette.gateway.api.org_structure.dto._
import biz.lobachev.annette.gateway.core.authentication.{AuthenticatedAction, AuthenticatedRequest}
import biz.lobachev.annette.gateway.core.authorization.{AuthorizationFailedException, Authorizer}
import biz.lobachev.annette.org_structure.api.OrgStructureService
import biz.lobachev.annette.org_structure.api.category.{
  CategoryFindQuery,
  CategoryId,
  CreateCategoryPayload,
  DeleteCategoryPayload,
  UpdateCategoryPayload
}
import biz.lobachev.annette.org_structure.api.hierarchy._
import biz.lobachev.annette.org_structure.api.role._
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

import scala.concurrent.{ExecutionContext, Future}
import io.scalaland.chimney.dsl._

@Singleton
class OrgStructureController @Inject() (
  authenticated: AuthenticatedAction,
  authorizer: Authorizer,
  orgStructureService: OrgStructureService,
  cc: ControllerComponents,
  implicit val ec: ExecutionContext
) extends AbstractController(cc) {

  def createOrganization =
    authenticated.async(parse.json[CreateOrganizationPayloadDto]) { implicit request =>
      val payload = request.body
        .into[CreateOrganizationPayload]
        .withFieldConst(_.createdBy, request.subject.principals.head)
        .transform
      authorizer.performCheck(canMaintainOrg(payload.orgId)) {
        for {
          _      <- orgStructureService.createOrganization(payload)
          result <- orgStructureService.getOrgItemById(payload.orgId, payload.orgId)
        } yield Ok(Json.toJson(result))
      }
    }

  def deleteOrganization =
    authenticated.async(parse.json[DeleteOrganizationPayloadDto]) { implicit request =>
      val payload = request.body
        .into[DeleteOrganizationPayload]
        .withFieldConst(_.deletedBy, request.subject.principals.head)
        .transform
      authorizer.performCheck(canMaintainOrg(payload.orgId)) {
        for {
          _ <- orgStructureService.deleteOrganization(payload)
        } yield Ok("")
      }
    }

  def getOrganizationById(orgId: OrgItemId) =
    authenticated.async { implicit request =>
      authorizer.performCheck(canViewOrMaintainOrg(orgId)) {
        for {
          result <- orgStructureService.getOrganizationById(orgId)
        } yield Ok(Json.toJson(result))
      }
    }

  def getOrganizationTree(orgId: OrgItemId, itemId: OrgItemId) =
    authenticated.async { implicit request =>
      authorizer.performCheck(canViewOrMaintainOrg(orgId)) {
        for {
          result <- orgStructureService.getOrganizationTree(orgId, itemId)
        } yield Ok(Json.toJson(result))
      }
    }

  def createUnit =
    authenticated.async(parse.json[CreateUnitPayloadDto]) { implicit request =>
      val payload = request.body
        .into[CreateUnitPayload]
        .withFieldConst(_.createdBy, request.subject.principals.head)
        .transform
      authorizer.performCheck(canMaintainOrg(payload.orgId)) {
        for {
          _      <- orgStructureService.createUnit(payload)
          result <- orgStructureService.getOrgItemsById(payload.orgId, Set(payload.unitId, payload.parentId))
        } yield Ok(Json.toJson(result))
      }
    }

  def deleteUnit =
    authenticated.async(parse.json[DeleteUnitPayloadDto]) { implicit request =>
      val payload = request.body
        .into[DeleteUnitPayload]
        .withFieldConst(_.deletedBy, request.subject.principals.head)
        .transform
      authorizer.performCheck(canMaintainOrg(payload.orgId)) {
        for {
          unit   <- orgStructureService.getOrgItemById(payload.orgId, payload.unitId)
          _      <- orgStructureService.deleteUnit(payload)
          result <- orgStructureService.getOrgItemById(payload.orgId, unit.parentId)
        } yield Ok(Json.toJson(result))
      }
    }

  def assignCategory =
    authenticated.async(parse.json[AssignCategoryPayloadDto]) { implicit request =>
      val payload = request.body
        .into[AssignCategoryPayload]
        .withFieldConst(_.updatedBy, request.subject.principals.head)
        .transform
      authorizer.performCheck(canMaintainOrg(payload.orgId)) {
        for {
          _      <- orgStructureService.assignCategory(payload)
          result <- orgStructureService.getOrgItemById(payload.orgId, payload.itemId)
        } yield Ok(Json.toJson(result))
      }
    }

  def assignChief =
    authenticated.async(parse.json[AssignChiefPayloadDto]) { implicit request =>
      val payload = request.body
        .into[AssignChiefPayload]
        .withFieldConst(_.updatedBy, request.subject.principals.head)
        .transform
      authorizer.performCheck(canMaintainOrg(payload.orgId)) {
        for {
          _      <- orgStructureService.assignChief(payload)
          result <- orgStructureService.getOrgItemById(payload.orgId, payload.unitId)
        } yield Ok(Json.toJson(result))
      }
    }

  def unassignChief =
    authenticated.async(parse.json[UnassignChiefPayloadDto]) { implicit request =>
      val payload = request.body
        .into[UnassignChiefPayload]
        .withFieldConst(_.updatedBy, request.subject.principals.head)
        .transform
      authorizer.performCheck(canMaintainOrg(payload.orgId)) {
        for {
          _      <- orgStructureService.unassignChief(payload)
          result <- orgStructureService.getOrgItemById(payload.orgId, payload.unitId)
        } yield Ok(Json.toJson(result))
      }
    }

  def createPosition =
    authenticated.async(parse.json[CreatePositionPayloadDto]) { implicit request =>
      val payload = request.body
        .into[CreatePositionPayload]
        .withFieldConst(_.createdBy, request.subject.principals.head)
        .transform
      authorizer.performCheck(canMaintainOrg(payload.orgId)) {
        for {
          _      <- orgStructureService.createPosition(payload)
          result <- orgStructureService.getOrgItemsById(payload.orgId, Set(payload.positionId, payload.parentId))
        } yield Ok(Json.toJson(result))
      }
    }

  def deletePosition =
    authenticated.async(parse.json[DeletePositionPayloadDto]) { implicit request =>
      val payload = request.body
        .into[DeletePositionPayload]
        .withFieldConst(_.deletedBy, request.subject.principals.head)
        .transform
      authorizer.performCheck(canMaintainOrg(payload.orgId)) {
        for {
          position <- orgStructureService.getOrgItemById(payload.orgId, payload.positionId)
          _        <- orgStructureService.deletePosition(payload)
          result   <- orgStructureService.getOrgItemById(payload.orgId, position.parentId)
        } yield Ok(Json.toJson(result))
      }
    }

  def updateName =
    authenticated.async(parse.json[UpdateNamePayloadDto]) { implicit request =>
      val payload = request.body
        .into[UpdateNamePayload]
        .withFieldConst(_.updatedBy, request.subject.principals.head)
        .transform
      authorizer.performCheck(canMaintainOrg(payload.orgId)) {
        for {
          _      <- orgStructureService.updateName(payload)
          result <- orgStructureService.getOrgItemById(payload.orgId, payload.orgItemId)
        } yield Ok(Json.toJson(result))
      }
    }

  def updateShortName =
    authenticated.async(parse.json[UpdateShortNamePayloadDto]) { implicit request =>
      val payload = request.body
        .into[UpdateShortNamePayload]
        .withFieldConst(_.updatedBy, request.subject.principals.head)
        .transform
      authorizer.performCheck(canMaintainOrg(payload.orgId)) {
        for {
          _      <- orgStructureService.updateShortName(payload)
          result <- orgStructureService.getOrgItemById(payload.orgId, payload.orgItemId)
        } yield Ok(Json.toJson(result))
      }
    }

  def changePositionLimit =
    authenticated.async(parse.json[ChangePositionLimitPayloadDto]) { implicit request =>
      val payload = request.body
        .into[ChangePositionLimitPayload]
        .withFieldConst(_.updatedBy, request.subject.principals.head)
        .transform
      authorizer.performCheck(canMaintainOrg(payload.orgId)) {
        for {
          _      <- orgStructureService.changePositionLimit(payload)
          result <- orgStructureService.getOrgItemById(payload.orgId, payload.positionId)
        } yield Ok(Json.toJson(result))
      }
    }

  def assignPerson =
    authenticated.async(parse.json[AssignPersonPayloadDto]) { implicit request =>
      val payload = request.body
        .into[AssignPersonPayload]
        .withFieldConst(_.updatedBy, request.subject.principals.head)
        .transform
      authorizer.performCheck(canMaintainOrg(payload.orgId)) {
        for {
          _      <- orgStructureService.assignPerson(payload)
          result <- orgStructureService.getOrgItemById(payload.orgId, payload.positionId)
        } yield Ok(Json.toJson(result))
      }
    }

  def unassignPerson =
    authenticated.async(parse.json[UnassignPersonPayloadDto]) { implicit request =>
      val payload = request.body
        .into[UnassignPersonPayload]
        .withFieldConst(_.updatedBy, request.subject.principals.head)
        .transform
      authorizer.performCheck(canMaintainOrg(payload.orgId)) {
        for {
          _      <- orgStructureService.unassignPerson(payload)
          result <- orgStructureService.getOrgItemById(payload.orgId, payload.positionId)
        } yield Ok(Json.toJson(result))
      }
    }

  def assignOrgRole =
    authenticated.async(parse.json[AssignOrgRolePayloadDto]) { implicit request =>
      val payload = request.body
        .into[AssignOrgRolePayload]
        .withFieldConst(_.updatedBy, request.subject.principals.head)
        .transform
      authorizer.performCheck(canMaintainOrg(payload.orgId)) {
        for {
          _      <- orgStructureService.assignOrgRole(payload)
          result <- orgStructureService.getOrgItemById(payload.orgId, payload.positionId)
        } yield Ok(Json.toJson(result))
      }
    }

  def unassignOrgRole =
    authenticated.async(parse.json[UnassignOrgRolePayloadDto]) { implicit request =>
      val payload = request.body
        .into[UnassignOrgRolePayload]
        .withFieldConst(_.updatedBy, request.subject.principals.head)
        .transform
      authorizer.performCheck(canMaintainOrg(payload.orgId)) {
        for {
          _      <- orgStructureService.unassignOrgRole(payload)
          result <- orgStructureService.getOrgItemById(payload.orgId, payload.positionId)
        } yield Ok(Json.toJson(result))
      }
    }

  def getOrgItemById(orgId: OrgItemId, id: OrgItemId) =
    authenticated.async { implicit request =>
      authorizer.performCheck(canViewOrMaintainOrg(orgId)) {
        for {
          result <- orgStructureService.getOrgItemById(orgId, id)
        } yield Ok(Json.toJson(result))
      }
    }

  def getOrgItemsById(orgId: OrgItemId) =
    authenticated.async(parse.json[Set[OrgItemId]]) { implicit request =>
      val ids = request.body
      authorizer.performCheck(canViewOrMaintainOrg(orgId)) {
        for {
          result <- orgStructureService.getOrgItemsById(orgId, ids)
        } yield Ok(Json.toJson(result))
      }
    }

  def getOrgItemByIdFromReadSide(id: OrgItemId) =
    authenticated.async { implicit request =>
      val itemFuture = orgStructureService.getOrgItemByIdFromReadSide(id)
      authorizer.performCheck(itemFuture.flatMap(item => canViewOrMaintainOrg(item.orgId))) {
        for {
          result <- orgStructureService.getOrgItemByIdFromReadSide(id)
        } yield Ok(Json.toJson(result))
      }
    }

  def getOrgItemsByIdFromReadSide =
    authenticated.async(parse.json[Set[OrgItemId]]) { implicit request =>
      val ids = request.body
      for {
        allowedOrgs <- getViewOrMaintainOrgs()
        items       <- allowedOrgs match {
                         case AllowedAll          => orgStructureService.getOrgItemsByIdFromReadSide(ids)
                         case AllowedOrgSet(orgs) =>
                           orgStructureService
                             .getOrgItemsByIdFromReadSide(ids)
                             .map(
                               _.filter { case (_, item) => orgs.contains(item.orgId) }
                             )
                         case AllowedNone         => Future.failed(AuthorizationFailedException())

                       }
      } yield Ok(Json.toJson(items))
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

  def moveItem =
    authenticated.async(parse.json[MoveItemPayloadDto]) { implicit request =>
      val payload = request.body
        .into[MoveItemPayload]
        .withFieldConst(_.updatedBy, request.subject.principals.head)
        .transform
      authorizer.performCheck(canMaintainOrg(payload.orgId)) {
        for {
          _ <- orgStructureService.moveItem(payload)
        } yield Ok("")
      }
    }

  def changeItemOrder =
    authenticated.async(parse.json[ChangeItemOrderPayloadDto]) { implicit request =>
      val payload = request.body
        .into[ChangeItemOrderPayload]
        .withFieldConst(_.updatedBy, request.subject.principals.head)
        .transform
      authorizer.performCheck(canMaintainOrg(payload.orgId)) {
        for {
          item   <- orgStructureService.getOrgItemById(payload.orgId, payload.orgItemId)
          _      <- orgStructureService.changeItemOrder(payload)
          result <- orgStructureService.getOrgItemById(payload.orgId, item.parentId)
        } yield Ok(Json.toJson(result))
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
          role <- orgStructureService.getOrgRoleById(payload.id, false)
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
          role <- orgStructureService.getOrgRoleById(payload.id, false)
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

  def getOrgRoleById(id: OrgRoleId, fromReadSide: Boolean) =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(VIEW_ALL_ORG_ROLES, MAINTAIN_ALL_ORG_ROLES) {
        for {
          role <- orgStructureService.getOrgRoleById(id, fromReadSide)
        } yield Ok(Json.toJson(role))
      }
    }

  def getOrgRolesById(fromReadSide: Boolean) =
    authenticated.async(parse.json[Set[OrgRoleId]]) { implicit request =>
      val ids = request.body
      authorizer.performCheckAny(VIEW_ALL_ORG_ROLES, MAINTAIN_ALL_ORG_ROLES) {
        for {
          roles <- orgStructureService.getOrgRolesById(ids, fromReadSide)
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
    authenticated.async(parse.json[CategoryDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_CATEGORIES) {
        val payload = request.body
          .into[CreateCategoryPayload]
          .withFieldConst(_.createdBy, request.subject.principals.head)
          .transform
        for {
          _    <- orgStructureService.createCategory(payload)
          role <- orgStructureService.getCategoryById(payload.id, false)
        } yield Ok(Json.toJson(role))
      }
    }

  def updateCategory =
    authenticated.async(parse.json[CategoryDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_CATEGORIES) {
        val payload = request.body
          .into[UpdateCategoryPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _    <- orgStructureService.updateCategory(payload)
          role <- orgStructureService.getCategoryById(payload.id, false)
        } yield Ok(Json.toJson(role))
      }
    }

  def deleteCategory =
    authenticated.async(parse.json[DeleteCategoryDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_CATEGORIES) {
        val payload = request.body
          .into[DeleteCategoryPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _ <- orgStructureService.deleteCategory(payload)
        } yield Ok("")
      }
    }

  def getCategoryById(id: CategoryId, fromReadSide: Boolean) =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(VIEW_ALL_CATEGORIES, MAINTAIN_ALL_CATEGORIES) {
        for {
          role <- orgStructureService.getCategoryById(id, fromReadSide)
        } yield Ok(Json.toJson(role))
      }
    }

  def getCategoriesById(fromReadSide: Boolean) =
    authenticated.async(parse.json[Set[CategoryId]]) { implicit request =>
      val ids = request.body
      authorizer.performCheckAny(VIEW_ALL_CATEGORIES, MAINTAIN_ALL_CATEGORIES) {
        for {
          roles <- orgStructureService.getCategoriesById(ids, fromReadSide)
        } yield Ok(Json.toJson(roles))
      }
    }

  def findCategories =
    authenticated.async(parse.json[CategoryFindQuery]) { implicit request =>
      val query = request.body
      authorizer.performCheckAny(VIEW_ALL_ORG_ROLES, MAINTAIN_ALL_ORG_ROLES) {
        for {
          result <- orgStructureService.findCategories(query)
        } yield Ok(Json.toJson(result))
      }
    }

  private def canMaintainOrg[A](orgId: OrgItemId)(implicit request: AuthenticatedRequest[A]): Future[Boolean] =
    authorizer.checkAny(MAINTAIN_ALL_HIERARCHIES, MAINTAIN_ORG_HIERARCHY(orgId))

  private def canViewOrMaintainOrg[A](orgId: OrgItemId)(implicit request: AuthenticatedRequest[A]): Future[Boolean] =
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
