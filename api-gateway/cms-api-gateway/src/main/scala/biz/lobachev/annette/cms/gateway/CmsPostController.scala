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

import akka.stream.Materializer
import biz.lobachev.annette.api_gateway_core.authentication.{AuthenticatedAction, CookieAuthenticatedAction}
import biz.lobachev.annette.api_gateway_core.authorization.Authorizer
import biz.lobachev.annette.cms.api.blogs.post._
import biz.lobachev.annette.cms.api.files.{FileTypes, RemoveFilePayload, StoreFilePayload}
import biz.lobachev.annette.cms.api.{CmsService, CmsStorage}
import biz.lobachev.annette.cms.gateway.blogs.post._
import biz.lobachev.annette.core.model.indexing.SortBy
import io.scalaland.chimney.dsl._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, ControllerComponents}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class CmsPostController @Inject() (
  authenticated: AuthenticatedAction,
  cookieAuthenticated: CookieAuthenticatedAction,
  authorizer: Authorizer,
  cc: ControllerComponents,
  cmsService: CmsService,
  cmsStorage: CmsStorage,
  implicit val ec: ExecutionContext,
  implicit val materializer: Materializer
) extends AbstractController(cc) {

  val blogSubscriptionType = "blog"

  // ****************************** Post ******************************

  def createPost =
    authenticated.async(parse.json[CreatePostPayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_POSTS) {
        val payload = request.body
          .into[CreatePostPayload]
          .withFieldConst(_.createdBy, request.subject.principals.head)
          .transform
        for {
          post <- cmsService.createPost(payload)
        } yield Ok(Json.toJson(post))
      }
    }

  def updatePostTitle =
    authenticated.async(parse.json[UpdatePostTitlePayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_POSTS) {
        val payload = request.body
          .into[UpdatePostTitlePayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          updated <- cmsService.updatePostTitle(payload)
        } yield Ok(Json.toJson(updated))
      }
    }

  def updatePostAuthor =
    authenticated.async(parse.json[UpdatePostAuthorPayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_POSTS) {
        val payload = request.body
          .into[UpdatePostAuthorPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          updated <- cmsService.updatePostAuthor(payload)
        } yield Ok(Json.toJson(updated))
      }
    }

  def updatePostWidgetContent =
    authenticated.async(parse.json[UpdatePostWidgetContentPayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_POSTS) {
        val payload = request.body
          .into[UpdatePostWidgetContentPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          updated <- cmsService.updatePostWidgetContent(payload)
        } yield Ok(Json.toJson(updated))
      }
    }

  def changePostWidgetContentOrder =
    authenticated.async(parse.json[ChangePostWidgetContentOrderPayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_POSTS) {
        val payload = request.body
          .into[ChangePostWidgetContentOrderPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          updated <- cmsService.changePostWidgetContentOrder(payload)
        } yield Ok(Json.toJson(updated))
      }
    }

  def deletePostWidgetContent =
    authenticated.async(parse.json[DeletePostWidgetContentPayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_POSTS) {
        val payload = request.body
          .into[DeletePostWidgetContentPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          updated <- cmsService.deletePostWidgetContent(payload)
        } yield Ok(Json.toJson(updated))
      }
    }

  def updatePostPublicationTimestamp =
    authenticated.async(parse.json[UpdatePostPublicationTimestampPayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_POSTS) {
        val payload = request.body
          .into[UpdatePostPublicationTimestampPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          updated <- cmsService.updatePostPublicationTimestamp(payload)
        } yield Ok(Json.toJson(updated))
      }
    }

  def publishPost(id: String) =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_POSTS) {
        val payload = PublishPostPayload(id, request.subject.principals.head)
        for {
          updated <- cmsService.publishPost(payload)
        } yield Ok(Json.toJson(updated))
      }
    }

  def unpublishPost(id: String) =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_POSTS) {
        val payload = UnpublishPostPayload(id, request.subject.principals.head)
        for {
          updated <- cmsService.unpublishPost(payload)
        } yield Ok(Json.toJson(updated))
      }
    }

  def updatePostFeatured =
    authenticated.async(parse.json[UpdatePostFeaturedPayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_POSTS) {
        val payload = request.body
          .into[UpdatePostFeaturedPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          updated <- cmsService.updatePostFeatured(payload)
        } yield Ok(Json.toJson(updated))
      }
    }

  def assignPostTargetPrincipal =
    authenticated.async(parse.json[AssignPostTargetPrincipalPayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_POSTS) {
        val payload = request.body
          .into[AssignPostTargetPrincipalPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          updated <- cmsService.assignPostTargetPrincipal(payload)
        } yield Ok(Json.toJson(updated))
      }
    }

  def unassignPostTargetPrincipal =
    authenticated.async(parse.json[UnassignPostTargetPrincipalPayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_POSTS) {
        val payload = request.body
          .into[UnassignPostTargetPrincipalPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          updated <- cmsService.unassignPostTargetPrincipal(payload)
        } yield Ok(Json.toJson(updated))
      }
    }

  def deletePost =
    authenticated.async(parse.json[DeletePostPayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_POSTS) {
        val payload = request.body
          .into[DeletePostPayload]
          .withFieldConst(_.deletedBy, request.subject.principals.head)
          .transform
        for {
          updated <- cmsService.deletePost(payload)
        } yield Ok(Json.toJson(updated))
      }
    }

  def findPosts: Action[PostFindQueryDto] =
    authenticated.async(parse.json[PostFindQueryDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_POSTS) {
        val payload = request.request.body
        for {
          result <- {
            val sortBy =
              if (payload.filter.map(_.isEmpty).getOrElse(true) && payload.sortBy.isEmpty)
                Some(
                  Seq(
                    SortBy("featured", Some(false)),
                    SortBy("publicationTimestamp", Some(false))
                  )
                )
              else payload.sortBy
            val query  = payload
              .into[PostFindQuery]
              .withFieldConst(_.sortBy, sortBy)
              .transform
            cmsService.findPosts(query)
          }

        } yield Ok(Json.toJson(result))
      }
    }

  def getPostsById(
    fromReadSide: Boolean,
    withIntro: Option[Boolean] = None,
    withContent: Option[Boolean] = None,
    withTargets: Option[Boolean] = None
  ) =
    authenticated.async(parse.json[Set[PostId]]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_POSTS) {
        val ids = request.request.body
        for {
          result <- cmsService.getPostsById(ids, fromReadSide, withIntro, withContent, withTargets)
        } yield Ok(Json.toJson(result))
      }
    }

  def getPostById(
    id: PostId,
    fromReadSide: Boolean,
    withIntro: Option[Boolean] = None,
    withContent: Option[Boolean] = None,
    withTargets: Option[Boolean] = None
  ) =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_POSTS) {
        for {
          result <- cmsService.getPostById(id, fromReadSide, withIntro, withContent, withTargets)
        } yield Ok(Json.toJson(result))
      }
    }

  def uploadPostFile(postId: String, fileType: String) =
    cookieAuthenticated.async(parse.multipartFormData) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_POSTS) {
        val file    = request.body.files.head
        val fileId  = UUID.randomUUID().toString
        val payload = StoreFilePayload(
          objectId = s"post-$postId",
          fileType = FileTypes.withName(fileType),
          fileId = fileId,
          filename = file.filename,
          contentType = fileMimeTypes.forFileName(file.filename).getOrElse(play.api.http.ContentTypes.BINARY),
          updatedBy = request.subject.principals.head
        )
        for {
          _ <- cmsService.storeFile(payload)
          _ <- cmsStorage.uploadFile(file.ref.path, payload)
        } yield Ok(Json.toJson(payload))
      }
    }

  def removePostFile(postId: String, fileType: String, fileId: String) =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_POSTS) {
        for {
          _ <- cmsService.removeFile(
                 RemoveFilePayload(
                   objectId = s"post-$postId",
                   fileType = FileTypes.withName(fileType),
                   fileId = fileId,
                   updatedBy = request.subject.principals.head
                 )
               )
        } yield Ok("")
      }
    }

  def getPostFiles(postId: String) =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_POSTS) {
        for {
          result <- cmsService.getFiles(s"post-$postId")
        } yield Ok(Json.toJson(result))
      }
    }
}
