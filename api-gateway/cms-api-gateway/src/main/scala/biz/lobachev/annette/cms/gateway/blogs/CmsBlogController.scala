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
import biz.lobachev.annette.cms.api.blogs.blog._
import biz.lobachev.annette.cms.gateway.Permissions
import biz.lobachev.annette.cms.gateway.Permissions.MAINTAIN_ALL_BLOGS
import biz.lobachev.annette.cms.gateway.blogs.blog._
import io.scalaland.chimney.dsl._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class CmsBlogController @Inject() (
  authenticated: AuthenticatedAction,
  authorizer: Authorizer,
  cc: ControllerComponents,
  cmsService: CmsService,
  implicit val ec: ExecutionContext
) extends AbstractController(cc) {

  val blogSubscriptionType = "blog"

  // ****************************** Blogs ******************************

  def createBlog =
    authenticated.async(parse.json[CreateBlogPayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_BLOGS) {
        val payload = request.body
          .into[CreateBlogPayload]
          .withFieldConst(_.createdBy, request.subject.principals.head)
          .transform
        for {
          _    <- cmsService.createBlog(payload)
          blog <- cmsService.getBlogById(payload.id, false)
        } yield Ok(Json.toJson(blog))
      }
    }

  def updateBlogName =
    authenticated.async(parse.json[UpdateBlogNamePayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_BLOGS) {
        val payload = request.body
          .into[UpdateBlogNamePayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _    <- cmsService.updateBlogName(payload)
          blog <- cmsService.getBlogById(payload.id, false)
        } yield Ok(Json.toJson(blog))
      }
    }

  def updateBlogDescription =
    authenticated.async(parse.json[UpdateBlogDescriptionPayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_BLOGS) {
        val payload = request.body
          .into[UpdateBlogDescriptionPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _    <- cmsService.updateBlogDescription(payload)
          blog <- cmsService.getBlogById(payload.id, false)
        } yield Ok(Json.toJson(blog))
      }
    }

  def updateBlogCategoryId =
    authenticated.async(parse.json[UpdateBlogCategoryPayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_BLOGS) {
        val payload = request.body
          .into[UpdateBlogCategoryPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _    <- cmsService.updateBlogCategoryId(payload)
          blog <- cmsService.getBlogById(payload.id, false)
        } yield Ok(Json.toJson(blog))
      }
    }

  def assignBlogTargetPrincipal =
    authenticated.async(parse.json[AssignBlogTargetPrincipalPayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_BLOGS) {
        val payload = request.body
          .into[AssignBlogTargetPrincipalPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _    <- cmsService.assignBlogTargetPrincipal(payload)
          blog <- cmsService.getBlogById(payload.id, false)
        } yield Ok(Json.toJson(blog))
      }
    }

  def unassignBlogTargetPrincipal =
    authenticated.async(parse.json[UnassignBlogTargetPrincipalPayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_BLOGS) {
        val payload = request.body
          .into[UnassignBlogTargetPrincipalPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _    <- cmsService.unassignBlogTargetPrincipal(payload)
          blog <- cmsService.getBlogById(payload.id, false)
        } yield Ok(Json.toJson(blog))
      }
    }

  def activateBlog =
    authenticated.async(parse.json[ActivateBlogPayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_BLOGS) {
        val payload = request.body
          .into[ActivateBlogPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _    <- cmsService.activateBlog(payload)
          blog <- cmsService.getBlogById(payload.id, false)
        } yield Ok(Json.toJson(blog))
      }
    }

  def deactivateBlog =
    authenticated.async(parse.json[DeactivateBlogPayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_BLOGS) {
        val payload = request.body
          .into[DeactivateBlogPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _    <- cmsService.deactivateBlog(payload)
          blog <- cmsService.getBlogById(payload.id, false)
        } yield Ok(Json.toJson(blog))
      }
    }

  def deleteBlog =
    authenticated.async(parse.json[DeleteBlogPayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_BLOGS) {
        val payload = request.body
          .into[DeleteBlogPayload]
          .withFieldConst(_.deletedBy, request.subject.principals.head)
          .transform
        for {
          _ <- cmsService.deleteBlog(payload)
        } yield Ok(Json.toJson(""))
      }
    }

  def getBlogById(id: BlogId, fromReadSide: Boolean) =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_BLOGS) {
        for {
          blog <- cmsService.getBlogById(id, fromReadSide)
        } yield Ok(Json.toJson(blog))
      }
    }

  def getBlogsById(fromReadSide: Boolean) =
    authenticated.async(parse.json[Set[BlogId]]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_BLOGS) {
        val ids = request.request.body
        for {
          blogs <- cmsService.getBlogsById(ids, fromReadSide)
        } yield Ok(Json.toJson(blogs))
      }
    }

  def findBlogs: Action[BlogFindQueryDto] =
    authenticated.async(parse.json[BlogFindQueryDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_BLOGS) {
        val payload = request.request.body
        val query   = payload.transformInto[BlogFindQuery]
        for {
          result <- cmsService.findBlogs(query)
        } yield Ok(Json.toJson(result))
      }
    }

}
