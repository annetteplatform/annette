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

package biz.lobachev.annette.principal_group.gateway

import biz.lobachev.annette.api_gateway_core.authentication.{AuthenticatedAction}
import biz.lobachev.annette.api_gateway_core.authorization.Authorizer
import biz.lobachev.annette.core.model.category._
import biz.lobachev.annette.principal_group.gateway.Permissions._
import biz.lobachev.annette.principal_group.api.PrincipalGroupService
import biz.lobachev.annette.principal_group.api.group.{
  AssignPrincipalPayload,
  CreatePrincipalGroupPayload,
  DeletePrincipalGroupPayload,
  PrincipalGroupFindQuery,
  PrincipalGroupId,
  UnassignPrincipalPayload,
  UpdatePrincipalGroupCategoryPayload,
  UpdatePrincipalGroupDescriptionPayload,
  UpdatePrincipalGroupNamePayload
}
import biz.lobachev.annette.principal_group.gateway.dto._
import io.scalaland.chimney.dsl._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext}

@Singleton
class PrincipalGroupController @Inject() (
  authenticated: AuthenticatedAction,
  authorizer: Authorizer,
  principalGroupService: PrincipalGroupService,
  cc: ControllerComponents,
  implicit val ec: ExecutionContext
) extends AbstractController(cc) {

  // private val log = LoggerFactory.getLogger(this.getClass)

  def createPrincipalGroup =
    authenticated.async(parse.json[CreatePrincipalGroupPayloadDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_PRINCIPAL_GROUPS) {
        val payload = request.body
          .into[CreatePrincipalGroupPayload]
          .withFieldConst(_.createdBy, request.subject.principals.head)
          .transform
        for {
          _              <- principalGroupService.createPrincipalGroup(payload)
          principalGroup <- principalGroupService.getPrincipalGroupById(payload.id, false)
        } yield Ok(Json.toJson(principalGroup))
      }
    }

  def updatePrincipalGroupName =
    authenticated.async(parse.json[UpdatePrincipalGroupNamePayloadDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_PRINCIPAL_GROUPS) {
        val payload = request.body
          .into[UpdatePrincipalGroupNamePayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _              <- principalGroupService.updatePrincipalGroupName(payload)
          principalGroup <- principalGroupService.getPrincipalGroupById(payload.id, false)
        } yield Ok(Json.toJson(principalGroup))
      }
    }

  def updatePrincipalGroupDescription =
    authenticated.async(parse.json[UpdatePrincipalGroupDescriptionPayloadDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_PRINCIPAL_GROUPS) {
        val payload = request.body
          .into[UpdatePrincipalGroupDescriptionPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _              <- principalGroupService.updatePrincipalGroupDescription(payload)
          principalGroup <- principalGroupService.getPrincipalGroupById(payload.id, false)
        } yield Ok(Json.toJson(principalGroup))
      }
    }

  def updatePrincipalGroupCategory =
    authenticated.async(parse.json[UpdatePrincipalGroupCategoryPayloadDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_PRINCIPAL_GROUPS) {
        val payload = request.body
          .into[UpdatePrincipalGroupCategoryPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _              <- principalGroupService.updatePrincipalGroupCategory(payload)
          principalGroup <- principalGroupService.getPrincipalGroupById(payload.id, false)
        } yield Ok(Json.toJson(principalGroup))
      }
    }

  def assignPrincipal =
    authenticated.async(parse.json[AssignPrincipalPayloadDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_PRINCIPAL_GROUPS) {
        val payload = request.body
          .into[AssignPrincipalPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _ <- principalGroupService.assignPrincipal(payload)
        } yield Ok(Json.toJson(""))
      }
    }

  def unassignPrincipal =
    authenticated.async(parse.json[UnassignPrincipalPayloadDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_PRINCIPAL_GROUPS) {
        val payload = request.body
          .into[UnassignPrincipalPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _ <- principalGroupService.unassignPrincipal(payload)
        } yield Ok(Json.toJson(""))
      }
    }

  def deletePrincipalGroup =
    authenticated.async(parse.json[DeletePrincipalGroupPayloadDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_PRINCIPAL_GROUPS) {
        val payload = request.body
          .into[DeletePrincipalGroupPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _ <- principalGroupService.deletePrincipalGroup(payload)
        } yield Ok("")
      }
    }

  def getPrincipalGroupById(id: PrincipalGroupId, fromReadSide: Boolean) =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(VIEW_ALL_PRINCIPAL_GROUPS, MAINTAIN_ALL_PRINCIPAL_GROUPS) {
        for {
          principalGroup <- principalGroupService.getPrincipalGroupById(id, fromReadSide)
        } yield Ok(Json.toJson(principalGroup))
      }
    }

  def getPrincipalGroupsById(fromReadSide: Boolean) =
    authenticated.async(parse.json[Set[PrincipalGroupId]]) { implicit request =>
      authorizer.performCheckAny(VIEW_ALL_PRINCIPAL_GROUPS, MAINTAIN_ALL_PRINCIPAL_GROUPS) {
        for {
          principalGroups <- principalGroupService.getPrincipalGroupsById(request.body, fromReadSide)
        } yield Ok(Json.toJson(principalGroups))
      }
    }

  def getAssignments(id: PrincipalGroupId) =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(VIEW_ALL_PRINCIPAL_GROUPS, MAINTAIN_ALL_PRINCIPAL_GROUPS) {
        for {
          principalGroup <- principalGroupService.getAssignments(id)
        } yield Ok(Json.toJson(principalGroup))
      }
    }

  def findPrincipalGroups =
    authenticated.async(parse.json[PrincipalGroupFindQuery]) { implicit request =>
      authorizer.performCheckAny(VIEW_ALL_PRINCIPAL_GROUPS, MAINTAIN_ALL_PRINCIPAL_GROUPS) {
        for {
          result <- principalGroupService.findPrincipalGroups(request.body)
        } yield Ok(Json.toJson(result))
      }

    }

  // category methods

  def createCategory =
    authenticated.async(parse.json[PrincipalGroupCategoryDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_PRINCIPAL_GROUP_CATEGORIES) {
        val payload = request.body
          .into[CreateCategoryPayload]
          .withFieldConst(_.createdBy, request.subject.principals.head)
          .transform
        for {
          _    <- principalGroupService.createCategory(payload)
          role <- principalGroupService.getCategoryById(payload.id, false)
        } yield Ok(Json.toJson(role))
      }
    }

  def updateCategory =
    authenticated.async(parse.json[PrincipalGroupCategoryDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_PRINCIPAL_GROUP_CATEGORIES) {
        val payload = request.body
          .into[UpdateCategoryPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _    <- principalGroupService.updateCategory(payload)
          role <- principalGroupService.getCategoryById(payload.id, false)
        } yield Ok(Json.toJson(role))
      }
    }

  def deleteCategory =
    authenticated.async(parse.json[DeletePrincipalGroupCategoryDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_PRINCIPAL_GROUP_CATEGORIES) {
        val payload = request.body
          .into[DeleteCategoryPayload]
          .withFieldConst(_.deletedBy, request.subject.principals.head)
          .transform
        for {
          _ <- principalGroupService.deleteCategory(payload)
        } yield Ok("")
      }
    }

  def getCategoryById(id: CategoryId, fromReadSide: Boolean) =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(VIEW_ALL_PRINCIPAL_GROUP_CATEGORIES, MAINTAIN_ALL_PRINCIPAL_GROUP_CATEGORIES) {
        for {
          role <- principalGroupService.getCategoryById(id, fromReadSide)
        } yield Ok(Json.toJson(role))
      }
    }

  def getCategoriesById(fromReadSide: Boolean) =
    authenticated.async(parse.json[Set[CategoryId]]) { implicit request =>
      val ids = request.body
      authorizer.performCheckAny(VIEW_ALL_PRINCIPAL_GROUP_CATEGORIES, MAINTAIN_ALL_PRINCIPAL_GROUP_CATEGORIES) {
        for {
          roles <- principalGroupService.getCategoriesById(ids, fromReadSide)
        } yield Ok(Json.toJson(roles))
      }
    }

  def findCategories =
    authenticated.async(parse.json[CategoryFindQuery]) { implicit request =>
      val query = request.body
      authorizer.performCheckAny(VIEW_ALL_PRINCIPAL_GROUP_CATEGORIES, MAINTAIN_ALL_PRINCIPAL_GROUP_CATEGORIES) {
        for {
          result <- principalGroupService.findCategories(query)
        } yield Ok(Json.toJson(result))
      }
    }

}
