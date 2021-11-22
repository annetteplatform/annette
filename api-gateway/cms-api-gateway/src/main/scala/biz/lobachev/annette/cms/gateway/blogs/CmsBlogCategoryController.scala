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

package biz.lobachev.annette.cms.gateway.blogs

import biz.lobachev.annette.api_gateway_core.authentication.AuthenticatedAction
import biz.lobachev.annette.api_gateway_core.authorization.Authorizer
import biz.lobachev.annette.cms.api.CmsService
import biz.lobachev.annette.cms.gateway.Permissions.{MAINTAIN_ALL_BLOG_CATEGORIES, VIEW_ALL_BLOG_CATEGORIES}
import biz.lobachev.annette.cms.gateway.blogs.category._
import biz.lobachev.annette.core.model.category._
import io.scalaland.chimney.dsl._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class CmsBlogCategoryController @Inject() (
  authenticated: AuthenticatedAction,
  authorizer: Authorizer,
  cc: ControllerComponents,
  cmsService: CmsService,
  implicit val ec: ExecutionContext
) extends AbstractController(cc) {

  // ****************************** Categories ******************************

  def createBlogCategory =
    authenticated.async(parse.json[CategoryDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_BLOG_CATEGORIES) {
        val payload = request.body
          .into[CreateCategoryPayload]
          .withFieldConst(_.createdBy, request.subject.principals.head)
          .transform
        for {
          _    <- cmsService.createBlogCategory(payload)
          role <- cmsService.getBlogCategoryById(payload.id, false)
        } yield Ok(Json.toJson(role))
      }
    }

  def updateBlogCategory =
    authenticated.async(parse.json[CategoryDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_BLOG_CATEGORIES) {
        val payload = request.body
          .into[UpdateCategoryPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _    <- cmsService.updateBlogCategory(payload)
          role <- cmsService.getBlogCategoryById(payload.id, false)
        } yield Ok(Json.toJson(role))
      }
    }

  def deleteBlogCategory =
    authenticated.async(parse.json[DeleteCategoryDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_BLOG_CATEGORIES) {
        val payload = request.body
          .into[DeleteCategoryPayload]
          .withFieldConst(_.deletedBy, request.subject.principals.head)
          .transform
        for {
          _ <- cmsService.deleteBlogCategory(payload)
        } yield Ok("")
      }
    }

  def getBlogCategoryById(id: CategoryId, fromReadSide: Boolean) =
    authenticated.async { implicit request =>
      val rules =
        if (fromReadSide) Seq(VIEW_ALL_BLOG_CATEGORIES, MAINTAIN_ALL_BLOG_CATEGORIES)
        else Seq(MAINTAIN_ALL_BLOG_CATEGORIES)
      authorizer.performCheckAny(rules: _*) {
        for {
          role <- cmsService.getBlogCategoryById(id, fromReadSide)
        } yield Ok(Json.toJson(role))
      }
    }

  def getBlogCategoriesById(fromReadSide: Boolean) =
    authenticated.async(parse.json[Set[CategoryId]]) { implicit request =>
      val rules =
        if (fromReadSide) Seq(VIEW_ALL_BLOG_CATEGORIES, MAINTAIN_ALL_BLOG_CATEGORIES)
        else Seq(MAINTAIN_ALL_BLOG_CATEGORIES)
      authorizer.performCheckAny(rules: _*) {
        for {
          result <- cmsService.getBlogCategoriesById(request.request.body, fromReadSide)
        } yield Ok(Json.toJson(result))
      }
    }

  def findBlogCategories: Action[CategoryFindQuery] =
    authenticated.async(parse.json[CategoryFindQuery]) { implicit request =>
      authorizer.performCheckAny(VIEW_ALL_BLOG_CATEGORIES, MAINTAIN_ALL_BLOG_CATEGORIES) {
        for {
          result <- cmsService.findBlogCategories(request.request.body)
        } yield Ok(Json.toJson(result))
      }
    }

}
