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

import akka.stream.Materializer
import biz.lobachev.annette.api_gateway_core.authentication.{
  AuthenticatedAction,
  AuthenticatedRequest,
  CookieAuthenticatedAction
}
import biz.lobachev.annette.api_gateway_core.authorization.Authorizer
import biz.lobachev.annette.cms.api.common.article.{
  PublishPayload,
  UnpublishPayload,
  UpdateAuthorPayload,
  UpdatePublicationTimestampPayload,
  UpdateTitlePayload
}
import biz.lobachev.annette.cms.api.common.{
  AssignPrincipalPayload,
  CanAccessToEntityPayload,
  DeletePayload,
  UnassignPrincipalPayload
}
import biz.lobachev.annette.cms.api.content.{
  ChangeWidgetOrderPayload,
  DeleteWidgetPayload,
  UpdateContentSettingsPayload,
  UpdateWidgetPayload
}
import biz.lobachev.annette.cms.api.pages.page._
import biz.lobachev.annette.cms.api.files.{FileTypes, RemoveFilePayload, StoreFilePayload}
import biz.lobachev.annette.cms.api.pages.space.SpaceId
import biz.lobachev.annette.cms.api.{CmsService, CmsStorage}
import biz.lobachev.annette.cms.gateway.Permissions
import biz.lobachev.annette.cms.gateway.pages.page._
import biz.lobachev.annette.core.model.indexing.SortBy
import io.scalaland.chimney.dsl._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, ControllerComponents}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CmsPageController @Inject() (
  authenticated: AuthenticatedAction,
  cookieAuthenticated: CookieAuthenticatedAction,
  authorizer: Authorizer,
  cc: ControllerComponents,
  cmsService: CmsService,
  cmsStorage: CmsStorage,
  implicit val ec: ExecutionContext,
  implicit val materializer: Materializer
) extends AbstractController(cc) {

  private def canEditPageBySpaceId[T](spaceId: SpaceId)(implicit request: AuthenticatedRequest[T]): Future[Boolean] =
    for {
      canEditSpace <- cmsService.canEditSpacePages(CanAccessToEntityPayload(spaceId, request.subject.principals.toSet))
      canEditPage  <- if (canEditSpace) Future.successful(true)
                      else authorizer.checkAll(Permissions.MAINTAIN_ALL_PAGES)
    } yield canEditSpace || canEditPage

  private def canEditPageByPageId[T](pageId: PageId)(implicit request: AuthenticatedRequest[T]): Future[Boolean] =
    for {
      canEditSpace <- cmsService.canEditPage(CanAccessToEntityPayload(pageId, request.subject.principals.toSet))
      canEditPage  <- if (canEditSpace) Future.successful(true)
                      else authorizer.checkAll(Permissions.MAINTAIN_ALL_PAGES)
    } yield canEditSpace || canEditPage

  // ****************************** Page ******************************

  def createPage =
    authenticated.async(parse.json[CreatePagePayloadDto]) { implicit request =>
      authorizer.performCheck(canEditPageBySpaceId(request.body.spaceId)) {
        val payload = request.body
          .into[CreatePagePayload]
          .withFieldConst(_.createdBy, request.subject.principals.head)
          .transform
        for {
          page <- cmsService.createPage(payload)
        } yield Ok(Json.toJson(page))
      }
    }

  def updatePageTitle =
    authenticated.async(parse.json[UpdatePageTitlePayloadDto]) { implicit request =>
      authorizer.performCheck(canEditPageByPageId(request.body.id)) {
        val payload = request.body
          .into[UpdateTitlePayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          updated <- cmsService.updatePageTitle(payload)
        } yield Ok(Json.toJson(updated))
      }
    }

  def updatePageAuthor =
    authenticated.async(parse.json[UpdatePageAuthorPayloadDto]) { implicit request =>
      authorizer.performCheck(canEditPageByPageId(request.body.id)) {
        val payload = request.body
          .into[UpdateAuthorPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          updated <- cmsService.updatePageAuthor(payload)
        } yield Ok(Json.toJson(updated))
      }
    }

  def updatePageContentSettings =
    authenticated.async(parse.json[UpdateContentSettingsPayloadDto]) { implicit request =>
      authorizer.performCheck(canEditPageByPageId(request.body.id)) {
        val payload = request.body
          .into[UpdateContentSettingsPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          updated <- cmsService.updatePageContentSettings(payload)
        } yield Ok(Json.toJson(updated))
      }
    }

  def updatePageWidget =
    authenticated.async(parse.json[UpdateWidgetPayloadDto]) { implicit request =>
      authorizer.performCheck(canEditPageByPageId(request.body.id)) {
        val payload = request.body
          .into[UpdateWidgetPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          updated <- cmsService.updatePageWidget(payload)
        } yield Ok(Json.toJson(updated))
      }
    }

  def changePageWidgetOrder =
    authenticated.async(parse.json[ChangeWidgetOrderPayloadDto]) { implicit request =>
      authorizer.performCheck(canEditPageByPageId(request.body.id)) {
        val payload = request.body
          .into[ChangeWidgetOrderPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          updated <- cmsService.changePageWidgetOrder(payload)
        } yield Ok(Json.toJson(updated))
      }
    }

  def deletePageWidget =
    authenticated.async(parse.json[DeleteWidgetPayloadDto]) { implicit request =>
      authorizer.performCheck(canEditPageByPageId(request.body.id)) {
        val payload = request.body
          .into[DeleteWidgetPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          updated <- cmsService.deletePageWidget(payload)
        } yield Ok(Json.toJson(updated))
      }
    }

  def updatePagePublicationTimestamp =
    authenticated.async(parse.json[UpdatePagePublicationTimestampPayloadDto]) { implicit request =>
      authorizer.performCheck(canEditPageByPageId(request.body.id)) {
        val payload = request.body
          .into[UpdatePublicationTimestampPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          updated <- cmsService.updatePagePublicationTimestamp(payload)
        } yield Ok(Json.toJson(updated))
      }
    }

  def publishPage(id: String) =
    authenticated.async { implicit request =>
      authorizer.performCheck(canEditPageByPageId(id)) {
        val payload = PublishPayload(id, request.subject.principals.head)
        for {
          updated <- cmsService.publishPage(payload)
        } yield Ok(Json.toJson(updated))
      }
    }

  def unpublishPage(id: String) =
    authenticated.async { implicit request =>
      authorizer.performCheck(canEditPageByPageId(id)) {
        val payload = UnpublishPayload(id, request.subject.principals.head)
        for {
          updated <- cmsService.unpublishPage(payload)
        } yield Ok(Json.toJson(updated))
      }
    }

  def assignPageTargetPrincipal =
    authenticated.async(parse.json[AssignPageTargetPrincipalPayloadDto]) { implicit request =>
      authorizer.performCheck(canEditPageByPageId(request.body.id)) {
        val payload = request.body
          .into[AssignPrincipalPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          updated <- cmsService.assignPageTargetPrincipal(payload)
        } yield Ok(Json.toJson(updated))
      }
    }

  def unassignPageTargetPrincipal =
    authenticated.async(parse.json[UnassignPageTargetPrincipalPayloadDto]) { implicit request =>
      authorizer.performCheck(canEditPageByPageId(request.body.id)) {
        val payload = request.body
          .into[UnassignPrincipalPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          updated <- cmsService.unassignPageTargetPrincipal(payload)
        } yield Ok(Json.toJson(updated))
      }
    }

  def deletePage =
    authenticated.async(parse.json[DeletePagePayloadDto]) { implicit request =>
      authorizer.performCheck(canEditPageByPageId(request.body.id)) {
        val payload = request.body
          .into[DeletePayload]
          .withFieldConst(_.deletedBy, request.subject.principals.head)
          .transform
        for {
          updated <- cmsService.deletePage(payload)
        } yield Ok(Json.toJson(updated))
      }
    }

  def findPages: Action[PageFindQueryDto] =
    authenticated.async(parse.json[PageFindQueryDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_ALL_PAGES, Permissions.FIND_ALL_PAGES) {
        val payload = request.request.body
        for {
          result <- {
            val sortBy =
              if (payload.filter.map(_.isEmpty).getOrElse(true) && payload.sortBy.isEmpty)
                Some(
                  Seq(
                    SortBy("publicationTimestamp", Some(false))
                  )
                )
              else payload.sortBy
            val query  = payload
              .into[PageFindQuery]
              .withFieldConst(_.sortBy, sortBy)
              .transform
            cmsService.findPages(query)
          }

        } yield Ok(Json.toJson(result))
      }
    }

  def getPages(
    source: Option[String],
    withContent: Option[Boolean] = None,
    withTargets: Option[Boolean] = None
  ) =
    authenticated.async(parse.json[Set[PageId]]) { implicit request =>
      val filteredPagesFuture = filterPages(request.request.body)
      authorizer.performCheck(filteredPagesFuture.map(_.nonEmpty)) {
        for {
          filteredPages <- filteredPagesFuture
          result        <- cmsService.getPages(filteredPages, source, withContent, withTargets)
        } yield Ok(Json.toJson(result))
      }
    }

  private def filterPages[T](ids: Set[PageId])(implicit request: AuthenticatedRequest[T]): Future[Set[PageId]] =
    ids
      .map(id => canEditPageByPageId(id).map(f => id -> f))
      .foldLeft(Future.successful(Set.empty[PageId])) { (acc, a) =>
        a.flatMap { case id -> f => if (f) acc.map(_ + id) else acc }
      }

  def getPage(
    id: PageId,
    source: Option[String],
    withContent: Option[Boolean] = None,
    withTargets: Option[Boolean] = None
  ) =
    authenticated.async { implicit request =>
      authorizer.performCheck(canEditPageByPageId(id)) {
        for {
          result <- cmsService.getPage(id, source, withContent, withTargets)
        } yield Ok(Json.toJson(result))
      }
    }

  def uploadPageFile(pageId: String, fileType: String) =
    cookieAuthenticated.async(parse.multipartFormData) { implicit request =>
      authorizer.performCheck(canEditPageByPageId(pageId)) {
        val file    = request.body.files.head
        val fileId  = UUID.randomUUID().toString
        val payload = StoreFilePayload(
          objectId = s"page-$pageId",
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

  def removePageFile(pageId: String, fileType: String, fileId: String) =
    authenticated.async { implicit request =>
      authorizer.performCheck(canEditPageByPageId(pageId)) {
        for {
          _ <- cmsService.removeFile(
                 RemoveFilePayload(
                   objectId = s"page-$pageId",
                   fileType = FileTypes.withName(fileType),
                   fileId = fileId,
                   updatedBy = request.subject.principals.head
                 )
               )
        } yield Ok("")
      }
    }

  def getPageFiles(pageId: String) =
    authenticated.async { implicit request =>
      authorizer.performCheck(canEditPageByPageId(pageId)) {
        for {
          result <- cmsService.getFiles(s"page-$pageId")
        } yield Ok(Json.toJson(result))
      }
    }
}
