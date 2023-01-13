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

import biz.lobachev.annette.api_gateway_core.authentication.AuthenticatedAction
import biz.lobachev.annette.api_gateway_core.authorization.Authorizer
import biz.lobachev.annette.cms.api.CmsService
import biz.lobachev.annette.cms.gateway.Permissions.{MAINTAIN_ALL_SPACE_CATEGORIES, VIEW_ALL_SPACE_CATEGORIES}
import biz.lobachev.annette.cms.gateway.pages.category._
import biz.lobachev.annette.core.model.DataSource
import biz.lobachev.annette.core.model.category._
import io.scalaland.chimney.dsl._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class CmsSpaceCategoryController @Inject() (
  authenticated: AuthenticatedAction,
  authorizer: Authorizer,
  cc: ControllerComponents,
  cmsService: CmsService,
  implicit val ec: ExecutionContext
) extends AbstractController(cc) {

  // ****************************** Categories ******************************

  def createSpaceCategory =
    authenticated.async(parse.json[CategoryDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_SPACE_CATEGORIES) {
        val payload = request.body
          .into[CreateCategoryPayload]
          .withFieldConst(_.createdBy, request.subject.principals.head)
          .transform
        for {
          _    <- cmsService.createSpaceCategory(payload)
          role <- cmsService.getSpaceCategory(payload.id, DataSource.FROM_ORIGIN)
        } yield Ok(Json.toJson(role))
      }
    }

  def updateSpaceCategory =
    authenticated.async(parse.json[CategoryDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_SPACE_CATEGORIES) {
        val payload = request.body
          .into[UpdateCategoryPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _    <- cmsService.updateSpaceCategory(payload)
          role <- cmsService.getSpaceCategory(payload.id, DataSource.FROM_ORIGIN)
        } yield Ok(Json.toJson(role))
      }
    }

  def deleteSpaceCategory =
    authenticated.async(parse.json[DeleteCategoryDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_SPACE_CATEGORIES) {
        val payload = request.body
          .into[DeleteCategoryPayload]
          .withFieldConst(_.deletedBy, request.subject.principals.head)
          .transform
        for {
          _ <- cmsService.deleteSpaceCategory(payload)
        } yield Ok("")
      }
    }

  def getSpaceCategory(id: CategoryId, source: Option[String]) =
    authenticated.async { implicit request =>
      val rules =
        if (DataSource.fromOrigin(source))
          Seq(MAINTAIN_ALL_SPACE_CATEGORIES)
        else
          Seq(VIEW_ALL_SPACE_CATEGORIES, MAINTAIN_ALL_SPACE_CATEGORIES)
      authorizer.performCheckAny(rules: _*) {
        for {
          role <- cmsService.getSpaceCategory(id, source)
        } yield Ok(Json.toJson(role))
      }
    }

  def getSpaceCategories(source: Option[String]) =
    authenticated.async(parse.json[Set[CategoryId]]) { implicit request =>
      val rules =
        if (DataSource.fromOrigin(source))
          Seq(MAINTAIN_ALL_SPACE_CATEGORIES)
        else
          Seq(VIEW_ALL_SPACE_CATEGORIES, MAINTAIN_ALL_SPACE_CATEGORIES)
      authorizer.performCheckAny(rules: _*) {
        for {
          result <- cmsService.getSpaceCategories(request.request.body, source)
        } yield Ok(Json.toJson(result))
      }
    }

  def findSpaceCategories: Action[CategoryFindQuery] =
    authenticated.async(parse.json[CategoryFindQuery]) { implicit request =>
      authorizer.performCheckAny(VIEW_ALL_SPACE_CATEGORIES, MAINTAIN_ALL_SPACE_CATEGORIES) {
        for {
          result <- cmsService.findSpaceCategories(request.request.body)
        } yield Ok(Json.toJson(result))
      }
    }

}
