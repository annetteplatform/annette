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

import biz.lobachev.annette.api_gateway_core.authentication.{AuthenticatedAction, AuthenticatedRequest}
import biz.lobachev.annette.api_gateway_core.authorization.Authorizer
import biz.lobachev.annette.cms.api.CmsService
import biz.lobachev.annette.cms.api.blogs.blog._
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
import biz.lobachev.annette.cms.gateway.Permissions
import biz.lobachev.annette.cms.gateway.blogs.blog._
import biz.lobachev.annette.core.model.DataSource
import io.scalaland.chimney.dsl._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CmsBlogController @Inject() (
  authenticated: AuthenticatedAction,
  authorizer: Authorizer,
  cc: ControllerComponents,
  cmsService: CmsService,
  implicit val ec: ExecutionContext
) extends AbstractController(cc) {

  private def canEditBlog[T](blogId: BlogId)(implicit request: AuthenticatedRequest[T]): Future[Boolean] =
    for {
      canEditBlog <- cmsService.canEditBlogPosts(CanAccessToEntityPayload(blogId, request.subject.principals.toSet))
      maintainAll <- if (canEditBlog) Future.successful(true)
                     else authorizer.checkAll(Permissions.MAINTAIN_ALL_POSTS)
    } yield canEditBlog || maintainAll

  // ****************************** Blogs ******************************

  def createBlog =
    authenticated.async(parse.json[CreateBlogPayloadDto]) { implicit request =>
      authorizer.performCheck(canEditBlog(request.body.id)) {
        val payload = request.body
          .into[CreateBlogPayload]
          .withFieldConst(_.createdBy, request.subject.principals.head)
          .transform
        for {
          _    <- cmsService.createBlog(payload)
          blog <- cmsService.getBlog(payload.id, DataSource.FROM_ORIGIN)
        } yield Ok(Json.toJson(blog))
      }
    }

  def updateBlogName =
    authenticated.async(parse.json[UpdateBlogNamePayloadDto]) { implicit request =>
      authorizer.performCheck(canEditBlog(request.body.id)) {
        val payload = request.body
          .into[UpdateNamePayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _    <- cmsService.updateBlogName(payload)
          blog <- cmsService.getBlog(payload.id, DataSource.FROM_ORIGIN)
        } yield Ok(Json.toJson(blog))
      }
    }

  def updateBlogDescription =
    authenticated.async(parse.json[UpdateBlogDescriptionPayloadDto]) { implicit request =>
      authorizer.performCheck(canEditBlog(request.body.id)) {
        val payload = request.body
          .into[UpdateDescriptionPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _    <- cmsService.updateBlogDescription(payload)
          blog <- cmsService.getBlog(payload.id, DataSource.FROM_ORIGIN)
        } yield Ok(Json.toJson(blog))
      }
    }

  def updateBlogCategoryId =
    authenticated.async(parse.json[UpdateBlogCategoryPayloadDto]) { implicit request =>
      authorizer.performCheck(canEditBlog(request.body.id)) {
        val payload = request.body
          .into[UpdateCategoryIdPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _    <- cmsService.updateBlogCategoryId(payload)
          blog <- cmsService.getBlog(payload.id, DataSource.FROM_ORIGIN)
        } yield Ok(Json.toJson(blog))
      }
    }

  def assignBlogAuthorPrincipal =
    authenticated.async(parse.json[AssignBlogPrincipalPayloadDto]) { implicit request =>
      authorizer.performCheck(canEditBlog(request.body.id)) {
        val payload = request.body
          .into[AssignPrincipalPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _    <- cmsService.assignBlogAuthorPrincipal(payload)
          blog <- cmsService.getBlog(payload.id, DataSource.FROM_ORIGIN)
        } yield Ok(Json.toJson(blog))
      }
    }

  def unassignBlogAuthorPrincipal =
    authenticated.async(parse.json[UnassignBlogPrincipalPayloadDto]) { implicit request =>
      authorizer.performCheck(canEditBlog(request.body.id)) {
        val payload = request.body
          .into[UnassignPrincipalPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _    <- cmsService.unassignBlogAuthorPrincipal(payload)
          blog <- cmsService.getBlog(payload.id, DataSource.FROM_ORIGIN)
        } yield Ok(Json.toJson(blog))
      }
    }

  def assignBlogTargetPrincipal =
    authenticated.async(parse.json[AssignBlogPrincipalPayloadDto]) { implicit request =>
      authorizer.performCheck(canEditBlog(request.body.id)) {
        val payload = request.body
          .into[AssignPrincipalPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _    <- cmsService.assignBlogTargetPrincipal(payload)
          blog <- cmsService.getBlog(payload.id, DataSource.FROM_ORIGIN)
        } yield Ok(Json.toJson(blog))
      }
    }

  def unassignBlogTargetPrincipal =
    authenticated.async(parse.json[UnassignBlogPrincipalPayloadDto]) { implicit request =>
      authorizer.performCheck(canEditBlog(request.body.id)) {
        val payload = request.body
          .into[UnassignPrincipalPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _    <- cmsService.unassignBlogTargetPrincipal(payload)
          blog <- cmsService.getBlog(payload.id, DataSource.FROM_ORIGIN)
        } yield Ok(Json.toJson(blog))
      }
    }

  def activateBlog =
    authenticated.async(parse.json[ActivateBlogPayloadDto]) { implicit request =>
      authorizer.performCheck(canEditBlog(request.body.id)) {
        val payload = request.body
          .into[ActivatePayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _    <- cmsService.activateBlog(payload)
          blog <- cmsService.getBlog(payload.id, DataSource.FROM_ORIGIN)
        } yield Ok(Json.toJson(blog))
      }
    }

  def deactivateBlog =
    authenticated.async(parse.json[DeactivateBlogPayloadDto]) { implicit request =>
      authorizer.performCheck(canEditBlog(request.body.id)) {
        val payload = request.body
          .into[DeactivatePayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _    <- cmsService.deactivateBlog(payload)
          blog <- cmsService.getBlog(payload.id, DataSource.FROM_ORIGIN)
        } yield Ok(Json.toJson(blog))
      }
    }

  def deleteBlog =
    authenticated.async(parse.json[DeleteBlogPayloadDto]) { implicit request =>
      authorizer.performCheck(canEditBlog(request.body.id)) {
        val payload = request.body
          .into[DeletePayload]
          .withFieldConst(_.deletedBy, request.subject.principals.head)
          .transform
        for {
          _ <- cmsService.deleteBlog(payload)
        } yield Ok(Json.toJson(""))
      }
    }

  def getBlog(id: BlogId, source: Option[String]) =
    authenticated.async { implicit request =>
      authorizer.performCheck(canEditBlog(id)) {
        for {
          blog <- cmsService.getBlog(id, source)
        } yield Ok(Json.toJson(blog))
      }
    }

  def getBlogs(source: Option[String]) =
    authenticated.async(parse.json[Set[BlogId]]) { implicit request =>
      val filteredBlogsFuture = filterBlogs(request.request.body)
      authorizer.performCheck(filteredBlogsFuture.map(_.nonEmpty)) {
        for {
          filteredBlogs <- filteredBlogsFuture
          blogs         <- cmsService.getBlogs(filteredBlogs, source)
        } yield Ok(Json.toJson(blogs))
      }
    }

  private def filterBlogs[T](ids: Set[BlogId])(implicit request: AuthenticatedRequest[T]): Future[Set[BlogId]] =
    ids
      .map(id => canEditBlog(id).map(f => id -> f))
      .foldLeft(Future.successful(Set.empty[BlogId])) { (acc, a) =>
        a.flatMap { case id -> f => if (f) acc.map(_ + id) else acc }
      }

  def findBlogs: Action[BlogFindQueryDto] =
    authenticated.async(parse.json[BlogFindQueryDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_BLOGS, Permissions.FIND_ALL_BLOGS) {
        val payload = request.request.body
        val query   = payload.transformInto[BlogFindQuery]
        for {
          result <- cmsService.findBlogs(query)
        } yield Ok(Json.toJson(result))
      }
    }

}
