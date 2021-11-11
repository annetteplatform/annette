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

import akka.Done
import akka.http.scaladsl.model.ContentType
import akka.stream.Materializer
import akka.stream.alpakka.s3.MetaHeaders
import akka.stream.alpakka.s3.scaladsl.S3
import akka.stream.scaladsl.FileIO
import biz.lobachev.annette.api_gateway_core.authentication.AuthenticatedAction
import biz.lobachev.annette.api_gateway_core.authorization.Authorizer
import biz.lobachev.annette.cms.api.CmsService
import biz.lobachev.annette.cms.api.blogs.post._
import biz.lobachev.annette.cms.gateway.blogs.post._
import biz.lobachev.annette.cms.gateway.s3.CmsS3Helper
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.indexing.SortBy
import io.scalaland.chimney.dsl._
import play.api.libs.Files
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, ControllerComponents, MultipartFormData}

import java.net.URLEncoder
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CmsPostController @Inject() (
  authenticated: AuthenticatedAction,
  authorizer: Authorizer,
  cc: ControllerComponents,
  cmsService: CmsService,
  cmsS3Initializer: CmsS3Helper,
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
          _    <- cmsService.createPost(payload)
          post <- cmsService.getPostById(payload.id, false)
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
          _    <- cmsService.updatePostTitle(payload)
          post <- cmsService.getPostById(payload.id, false)
        } yield Ok(Json.toJson(post))
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
          _    <- cmsService.updatePostAuthor(payload)
          post <- cmsService.getPostById(payload.id, false)
        } yield Ok(Json.toJson(post))
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
          _    <- cmsService.updatePostWidgetContent(payload)
          post <- cmsService.getPostById(payload.id, false)
        } yield Ok(Json.toJson(post))
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
          _    <- cmsService.changePostWidgetContentOrder(payload)
          post <- cmsService.getPostById(payload.id, false)
        } yield Ok(Json.toJson(post))
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
          _    <- cmsService.deletePostWidgetContent(payload)
          post <- cmsService.getPostById(payload.id, false)
        } yield Ok(Json.toJson(post))
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
          _    <- cmsService.updatePostPublicationTimestamp(payload)
          post <- cmsService.getPostById(payload.id, false)
        } yield Ok(Json.toJson(post))
      }
    }

  def publishPost(id: String) =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_POSTS) {
        val payload = PublishPostPayload(id, request.subject.principals.head)
        for {
          _    <- cmsService.publishPost(payload)
          post <- cmsService.getPostById(id, false)
        } yield Ok(Json.toJson(post))
      }
    }

  def unpublishPost(id: String) =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_POSTS) {
        val payload = UnpublishPostPayload(id, request.subject.principals.head)
        for {
          _    <- cmsService.unpublishPost(payload)
          post <- cmsService.getPostById(id, false)
        } yield Ok(Json.toJson(post))
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
          _    <- cmsService.updatePostFeatured(payload)
          post <- cmsService.getPostById(payload.id, false)
        } yield Ok(Json.toJson(post))
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
          _    <- cmsService.assignPostTargetPrincipal(payload)
          post <- cmsService.getPostById(payload.id, false)
        } yield Ok(Json.toJson(post))
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
          _    <- cmsService.unassignPostTargetPrincipal(payload)
          post <- cmsService.getPostById(payload.id, false)
        } yield Ok(Json.toJson(post))
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
          _ <- cmsService.deletePost(payload)
        } yield Ok(Json.toJson(""))
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

  def getPostAnnotationsById(fromReadSide: Boolean) =
    authenticated.async(parse.json[Set[PostId]]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_POSTS) {
        val ids = request.request.body
        for {
          postAnnotations <- cmsService.getPostAnnotationsById(ids, fromReadSide)
        } yield Ok(Json.toJson(postAnnotations))
      }
    }

  def getPostsById(fromReadSide: Boolean) =
    authenticated.async(parse.json[Set[PostId]]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_POSTS) {
        val ids = request.request.body
        for {
          result <- cmsService.getPostsById(ids, fromReadSide)
        } yield Ok(Json.toJson(result))
      }
    }

  def getPostById(id: PostId, fromReadSide: Boolean) =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_POSTS) {
        for {
          result <- cmsService.getPostById(id, fromReadSide)
        } yield Ok(Json.toJson(result))
      }
    }

  def uploadPostMedia(postId: String, mediaId: String) =
    authenticated.async(parse.multipartFormData) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_POSTS) {
        val maybeFile = request.body.file("file")
        for {
          _ <- uploadFile(postId, "media", mediaId, maybeFile, request.subject.principals.head)
        } yield Ok("")
      }
    }

  def removePostMedia(postId: String, mediaId: String) =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_POSTS) {
        for {
          _ <- cmsRemoveFile(postId, "media", mediaId, request.subject.principals.head)
        } yield Ok("")
      }
    }

  def uploadPostDoc(postId: String, docId: String) =
    authenticated.async(parse.multipartFormData) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_POSTS) {
        val maybeFile = request.body.file("file")
        for {
          _ <- uploadFile(postId, "doc", docId, maybeFile, request.subject.principals.head)
        } yield Ok("")
      }
    }

  def removePostDoc(postId: String, docId: String) =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_POSTS) {
        for {
          _ <- cmsRemoveFile(postId, "doc", docId, request.subject.principals.head)
        } yield Ok("")
      }
    }

  private def uploadFile(
    postId: PostId,
    fileType: String,
    fileId: String,
    maybeFile: Option[MultipartFormData.FilePart[Files.TemporaryFile]],
    principal: AnnettePrincipal
  ): Future[Done] = {
    if (maybeFile.isEmpty) throw FileNotFoundInRequest(postId, fileId)

    val filename = maybeFile.get.filename
    for {
      _ <- cmsStoreFile(postId, fileType, fileId, principal, filename)

      metaHeaders = Map(
                      "filename" -> URLEncoder.encode(maybeFile.get.filename, "UTF-8"),
                      "postId"   -> postId,
                      "fileType" -> fileType
                    )
      uploadSink  = S3.multipartUpload(
                      cmsS3Initializer.postFileBucket,
                      cmsS3Initializer.makePostS3FileKey(postId, fileType, fileId),
                      metaHeaders = MetaHeaders(metaHeaders),
                      contentType = ContentType
                        .parse(
                          fileMimeTypes.forFileName(filename).getOrElse(play.api.http.ContentTypes.BINARY)
                        )
                        .toOption
                        .get
                    )

      _          <- FileIO
                      .fromPath(maybeFile.get.ref.path)
                      .runWith(uploadSink)
                      .recoverWith {
                        case th =>
                          // remove file if upload failed
                          cmsRemoveFile(postId, fileType, fileId, principal)
                          Future.failed(th)
                      }
    } yield Done
  }

  private def cmsRemoveFile(
    postId: PostId,
    fileType: String,
    fileId: String,
    principal: AnnettePrincipal
  ): Future[Done] = ???
//    if (fileType == "media")
//      cmsService.removePostMedia(
//        RemovePostMediaPayload(postId, fileId, principal)
//      )
//    else
//      cmsService.removePostDoc(
//        RemovePostDocPayload(postId, fileId, principal)
//      )

  private def cmsStoreFile(
    postId: PostId,
    fileType: String,
    fileId: String,
    principal: AnnettePrincipal,
    filename: String
  ): Future[Done] = ???
//    if (fileType == "media")
//      cmsService.storePostMedia(
//        StorePostMediaPayload(postId, fileId, filename, principal)
//      )
//    else
//      cmsService.storePostDoc(
//        StorePostDocPayload(postId, fileId, filename, filename, principal)
//      )

}
