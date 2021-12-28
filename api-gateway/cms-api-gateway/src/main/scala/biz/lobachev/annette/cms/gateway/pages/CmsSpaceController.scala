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

package biz.lobachev.annette.cms.gateway.pages

import biz.lobachev.annette.api_gateway_core.authentication.{AuthenticatedAction, AuthenticatedRequest}
import biz.lobachev.annette.api_gateway_core.authorization.Authorizer
import biz.lobachev.annette.cms.api.CmsService
import biz.lobachev.annette.cms.api.common.{
  ActivatePayload,
  AssignPrincipalPayload,
  CanAccessToEntityPayload,
  DeactivatePayload,
  DeletePayload,
  UnassignPrincipalPayload,
  UpdateCategoryIdPayload,
  UpdateDescriptionPayload,
  UpdateNamePayload
}
import biz.lobachev.annette.cms.api.pages.space._
import biz.lobachev.annette.cms.gateway.Permissions
import biz.lobachev.annette.cms.gateway.pages.space._
import io.scalaland.chimney.dsl._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CmsSpaceController @Inject() (
  authenticated: AuthenticatedAction,
  authorizer: Authorizer,
  cc: ControllerComponents,
  cmsService: CmsService,
  implicit val ec: ExecutionContext
) extends AbstractController(cc) {

  private def canEditSpace[T](spaceId: SpaceId)(implicit request: AuthenticatedRequest[T]): Future[Boolean] =
    for {
      canEditSpace <- cmsService.canEditSpacePages(CanAccessToEntityPayload(spaceId, request.subject.principals.toSet))
      maintainAll  <- if (canEditSpace) Future.successful(true)
                      else authorizer.checkAll(Permissions.MAINTAIN_ALL_POSTS)
    } yield canEditSpace || maintainAll

  // ****************************** Spaces ******************************

  def createSpace =
    authenticated.async(parse.json[CreateSpacePayloadDto]) { implicit request =>
      authorizer.performCheck(canEditSpace(request.body.id)) {
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
      authorizer.performCheck(canEditSpace(request.body.id)) {
        val payload = request.body
          .into[UpdateNamePayload]
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
      authorizer.performCheck(canEditSpace(request.body.id)) {
        val payload = request.body
          .into[UpdateDescriptionPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _     <- cmsService.updateSpaceDescription(payload)
          space <- cmsService.getSpaceById(payload.id, false)
        } yield Ok(Json.toJson(space))
      }
    }

  def updateSpaceCategoryId =
    authenticated.async(parse.json[UpdateSpaceCategoryPayloadDto]) { implicit request =>
      authorizer.performCheck(canEditSpace(request.body.id)) {
        val payload = request.body
          .into[UpdateCategoryIdPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _     <- cmsService.updateSpaceCategoryId(payload)
          space <- cmsService.getSpaceById(payload.id, false)
        } yield Ok(Json.toJson(space))
      }
    }

  def assignSpaceAuthorPrincipal =
    authenticated.async(parse.json[AssignSpacePrincipalPayloadDto]) { implicit request =>
      authorizer.performCheck(canEditSpace(request.body.id)) {
        val payload = request.body
          .into[AssignPrincipalPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _     <- cmsService.assignSpaceAuthorPrincipal(payload)
          space <- cmsService.getSpaceById(payload.id, false)
        } yield Ok(Json.toJson(space))
      }
    }

  def unassignSpaceAuthorPrincipal =
    authenticated.async(parse.json[UnassignSpacePrincipalPayloadDto]) { implicit request =>
      authorizer.performCheck(canEditSpace(request.body.id)) {
        val payload = request.body
          .into[UnassignPrincipalPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _     <- cmsService.unassignSpaceAuthorPrincipal(payload)
          space <- cmsService.getSpaceById(payload.id, false)
        } yield Ok(Json.toJson(space))
      }
    }

  def assignSpaceTargetPrincipal =
    authenticated.async(parse.json[AssignSpacePrincipalPayloadDto]) { implicit request =>
      authorizer.performCheck(canEditSpace(request.body.id)) {
        val payload = request.body
          .into[AssignPrincipalPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _     <- cmsService.assignSpaceTargetPrincipal(payload)
          space <- cmsService.getSpaceById(payload.id, false)
        } yield Ok(Json.toJson(space))
      }
    }

  def unassignSpaceTargetPrincipal =
    authenticated.async(parse.json[UnassignSpacePrincipalPayloadDto]) { implicit request =>
      authorizer.performCheck(canEditSpace(request.body.id)) {
        val payload = request.body
          .into[UnassignPrincipalPayload]
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
      authorizer.performCheck(canEditSpace(request.body.id)) {
        val payload = request.body
          .into[ActivatePayload]
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
      authorizer.performCheck(canEditSpace(request.body.id)) {
        val payload = request.body
          .into[DeactivatePayload]
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
      authorizer.performCheck(canEditSpace(request.body.id)) {
        val payload = request.body
          .into[DeletePayload]
          .withFieldConst(_.deletedBy, request.subject.principals.head)
          .transform
        for {
          _ <- cmsService.deleteSpace(payload)
        } yield Ok(Json.toJson(""))
      }
    }

  def getSpaceById(id: SpaceId, fromReadSide: Boolean) =
    authenticated.async { implicit request =>
      authorizer.performCheck(canEditSpace(id)) {
        for {
          space <- cmsService.getSpaceById(id, fromReadSide)
        } yield Ok(Json.toJson(space))
      }
    }

  def getSpacesById(fromReadSide: Boolean) =
    authenticated.async(parse.json[Set[SpaceId]]) { implicit request =>
      val filteredSpacesFuture = filterSpaces(request.request.body)
      authorizer.performCheck(filteredSpacesFuture.map(_.nonEmpty)) {
        for {
          filteredSpaces <- filteredSpacesFuture
          spaces         <- cmsService.getSpacesById(filteredSpaces, fromReadSide)
        } yield Ok(Json.toJson(spaces))
      }
    }

  private def filterSpaces[T](ids: Set[SpaceId])(implicit request: AuthenticatedRequest[T]): Future[Set[SpaceId]] =
    ids
      .map(id => canEditSpace(id).map(f => id -> f))
      .foldLeft(Future.successful(Set.empty[SpaceId])) { (acc, a) =>
        a.flatMap { case id -> f => if (f) acc.map(_ + id) else acc }
      }

  def findSpaces: Action[SpaceFindQueryDto] =
    authenticated.async(parse.json[SpaceFindQueryDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_SPACES, Permissions.FIND_ALL_SPACES) {
        val payload = request.request.body
        val query   = payload.transformInto[SpaceFindQuery]
        for {
          result <- cmsService.findSpaces(query)
        } yield Ok(Json.toJson(result))
      }
    }

}
