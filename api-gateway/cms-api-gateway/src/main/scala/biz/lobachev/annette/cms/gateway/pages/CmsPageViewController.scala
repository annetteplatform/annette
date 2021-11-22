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
import biz.lobachev.annette.cms.api.pages.space._
import biz.lobachev.annette.cms.api.pages.page._
import biz.lobachev.annette.cms.gateway.Permissions
import biz.lobachev.annette.cms.gateway.pages.page._
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.indexing.{FindResult, SortBy}
import io.scalaland.chimney.dsl._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, ControllerComponents}

import java.time.OffsetDateTime
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CmsPageViewController @Inject() (
  authenticated: AuthenticatedAction,
  authorizer: Authorizer,
  cc: ControllerComponents,
  cmsService: CmsService,
  implicit val ec: ExecutionContext
) extends AbstractController(cc) {

  def findPageViews: Action[PageViewFindQueryDto] =
    authenticated.async(parse.json[PageViewFindQueryDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.VIEW_SPACES) {
        val payload = request.request.body
        for {
          spaces <- getLimitedSpaces(payload.spaces.getOrElse(Set.empty), request.subject.principals.toSet)
          _       = println(spaces)
          result <- if (spaces.nonEmpty) {
                      val sortBy =
                        if (payload.filter.map(_.isEmpty).getOrElse(true) && payload.sortBy.isEmpty)
                          Some(
                            Seq(
                              SortBy(
                                field = "publicationTimestamp",
                                descending = Some(true)
                              )
                            )
                          )
                        else payload.sortBy
                      val query  = payload
                        .into[PageFindQuery]
                        .withFieldConst(_.spaces, Some(spaces))
                        .withFieldConst(_.targets, Some(request.subject.principals.toSet))
                        .withFieldConst(_.publicationStatus, Some(PublicationStatus.Published))
                        .withFieldConst(_.publicationTimestampTo, Some(OffsetDateTime.now))
                        .withFieldConst(_.sortBy, sortBy)
                        .transform
                      cmsService.findPages(query)
                    } else Future.successful(FindResult(0, Seq.empty))
        } yield Ok(Json.toJson(result))
      }
    }

  def getPageViewsById =
    authenticated.async(parse.json[Set[PageId]]) { implicit request =>
      authorizer.performCheckAny(Permissions.VIEW_SPACES) {
        val ids = request.request.body
        for {
          result <- cmsService.getPageViews(
                      GetPageViewsPayload(
                        ids = ids,
                        directPrincipal = request.subject.principals.head,
                        principals = request.subject.principals.toSet
                      )
                    )
        } yield Ok(Json.toJson(result))
      }
    }

  def getPageViewById(pageId: PageId) =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(Permissions.VIEW_SPACES) {
        for {
          result   <- cmsService.getPageViews(
                        GetPageViewsPayload(
                          ids = Set(pageId),
                          directPrincipal = request.subject.principals.head,
                          principals = request.subject.principals.toSet
                        )
                      )
          resultMap = result.map(a => a.id -> a).toMap
        } yield Ok(Json.toJson(resultMap.get(pageId).getOrElse(throw PageNotFound(pageId))))
      }
    }

  def viewPage(id: PageId) =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(Permissions.VIEW_SPACES) {
        for {
          canAccess <- cmsService.canAccessToPage(
                         CanAccessToPagePayload(
                           id,
                           request.subject.principals.toSet
                         )
                       )
          _         <- if (canAccess)
                         cmsService.viewPage(ViewPagePayload(id, request.subject.principals.head))
                       else Future.failed(PageNotFound(id))
          result    <- cmsService.getPageMetricById(GetPageMetricPayload(id, request.subject.principals.head))
        } yield Ok(Json.toJson(result))
      }
    }

  def likePage(id: PageId) =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(Permissions.VIEW_SPACES) {
        for {
          canAccess <- cmsService.canAccessToPage(
                         CanAccessToPagePayload(
                           id,
                           request.subject.principals.toSet
                         )
                       )
          _         <- if (canAccess)
                         cmsService.likePage(LikePagePayload(id, request.subject.principals.head))
                       else Future.failed(PageNotFound(id))
          result    <- cmsService.getPageMetricById(GetPageMetricPayload(id, request.subject.principals.head))
        } yield Ok(Json.toJson(result))
      }
    }

  def unlikePage(id: PageId) =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(Permissions.VIEW_SPACES) {
        for {
          canAccess <- cmsService.canAccessToPage(
                         CanAccessToPagePayload(
                           id,
                           request.subject.principals.toSet
                         )
                       )
          _         <- if (canAccess)
                         cmsService.unlikePage(UnlikePagePayload(id, request.subject.principals.head))
                       else Future.failed(PageNotFound(id))
          result    <- cmsService.getPageMetricById(GetPageMetricPayload(id, request.subject.principals.head))
        } yield Ok(Json.toJson(result))
      }
    }

  private def getLimitedSpaces(spaces: Set[SpaceId], targets: Set[AnnettePrincipal]): Future[Set[SpaceId]] =
    for {
      findResults <- cmsService.findSpaces(
                       SpaceFindQuery(
                         size = 100,
                         spaceIds = if (spaces.isEmpty) None else Some(spaces),
                         active = Some(true),
                         targets = Some(targets)
                       )
                     )
    } yield findResults.hits.map(_.id).toSet

}
