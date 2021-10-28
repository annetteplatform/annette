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

package biz.lobachev.annette.cms.gateway

import biz.lobachev.annette.api_gateway_core.authentication.AuthenticatedAction
import biz.lobachev.annette.api_gateway_core.authorization.Authorizer
import biz.lobachev.annette.cms.api.CmsService
import biz.lobachev.annette.cms.api.space._
import biz.lobachev.annette.cms.gateway.Permissions.{MAINTAIN_ALL_SPACES}
import biz.lobachev.annette.cms.gateway.space._
import io.scalaland.chimney.dsl._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext}

@Singleton
class CmsSpaceController @Inject() (
  authenticated: AuthenticatedAction,
  authorizer: Authorizer,
  cc: ControllerComponents,
  cmsService: CmsService,
  implicit val ec: ExecutionContext
) extends AbstractController(cc) {

  val spaceSubscriptionType = "space"

  // ****************************** Spaces ******************************

  def createSpace =
    authenticated.async(parse.json[CreateSpacePayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_SPACES) {
        val payload = request.body
          .into[CreateSpacePayload]
          .withFieldConst(_.createdBy, request.subject.principals.head)
          .transform
        for {
          _     <- cmsService.createSpace(payload)
          space <- cmsService.getSpaceById(payload.id, false)
        } yield Ok(Json.toJson(space))
      }
    }

  def updateSpaceName =
    authenticated.async(parse.json[UpdateSpaceNamePayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_SPACES) {
        val payload = request.body
          .into[UpdateSpaceNamePayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _     <- cmsService.updateSpaceName(payload)
          space <- cmsService.getSpaceById(payload.id, false)
        } yield Ok(Json.toJson(space))
      }
    }

  def updateSpaceDescription =
    authenticated.async(parse.json[UpdateSpaceDescriptionPayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_SPACES) {
        val payload = request.body
          .into[UpdateSpaceDescriptionPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _     <- cmsService.updateSpaceDescription(payload)
          space <- cmsService.getSpaceById(payload.id, false)
        } yield Ok(Json.toJson(space))
      }
    }

  def updateSpaceCategory =
    authenticated.async(parse.json[UpdateSpaceCategoryPayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_SPACES) {
        val payload = request.body
          .into[UpdateSpaceCategoryPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _     <- cmsService.updateSpaceCategory(payload)
          space <- cmsService.getSpaceById(payload.id, false)
        } yield Ok(Json.toJson(space))
      }
    }

  def assignSpaceTargetPrincipal =
    authenticated.async(parse.json[AssignSpaceTargetPrincipalPayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_SPACES) {
        val payload = request.body
          .into[AssignSpaceTargetPrincipalPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _     <- cmsService.assignSpaceTargetPrincipal(payload)
          space <- cmsService.getSpaceById(payload.id, false)
        } yield Ok(Json.toJson(space))
      }
    }

  def unassignSpaceTargetPrincipal =
    authenticated.async(parse.json[UnassignSpaceTargetPrincipalPayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_SPACES) {
        val payload = request.body
          .into[UnassignSpaceTargetPrincipalPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _     <- cmsService.unassignSpaceTargetPrincipal(payload)
          space <- cmsService.getSpaceById(payload.id, false)
        } yield Ok(Json.toJson(space))
      }
    }

  def activateSpace =
    authenticated.async(parse.json[ActivateSpacePayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_SPACES) {
        val payload = request.body
          .into[ActivateSpacePayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _     <- cmsService.activateSpace(payload)
          space <- cmsService.getSpaceById(payload.id, false)
        } yield Ok(Json.toJson(space))
      }
    }

  def deactivateSpace =
    authenticated.async(parse.json[DeactivateSpacePayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_SPACES) {
        val payload = request.body
          .into[DeactivateSpacePayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _     <- cmsService.deactivateSpace(payload)
          space <- cmsService.getSpaceById(payload.id, false)
        } yield Ok(Json.toJson(space))
      }
    }

  def deleteSpace =
    authenticated.async(parse.json[DeleteSpacePayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_SPACES) {
        val payload = request.body
          .into[DeleteSpacePayload]
          .withFieldConst(_.deletedBy, request.subject.principals.head)
          .transform
        for {
          _ <- cmsService.deleteSpace(payload)
        } yield Ok(Json.toJson(""))
      }
    }

  def getSpaceById(id: SpaceId, fromReadSide: Boolean) =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_SPACES) {
        for {
          space <- cmsService.getSpaceById(id, fromReadSide)
        } yield Ok(Json.toJson(space))
      }
    }

  def getSpacesById(fromReadSide: Boolean) =
    authenticated.async(parse.json[Set[SpaceId]]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_SPACES) {
        val ids = request.request.body
        for {
          spaces <- cmsService.getSpacesById(ids, fromReadSide)
        } yield Ok(Json.toJson(spaces))
      }
    }

  def findSpaces: Action[SpaceFindQueryDto] =
    authenticated.async(parse.json[SpaceFindQueryDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_SPACES) {
        val payload = request.request.body
        val query   = payload.transformInto[SpaceFindQuery]
        for {
          result <- cmsService.findSpaces(query)
        } yield Ok(Json.toJson(result))
      }
    }

}
