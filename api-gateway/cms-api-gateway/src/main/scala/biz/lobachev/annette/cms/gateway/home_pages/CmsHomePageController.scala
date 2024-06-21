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

package biz.lobachev.annette.cms.gateway.home_pages

import akka.stream.Materializer
import biz.lobachev.annette.api_gateway_core.authentication.MaybeAuthenticatedAction
import biz.lobachev.annette.api_gateway_core.authorization.Authorizer
import biz.lobachev.annette.cms.api.CmsService
import biz.lobachev.annette.cms.api.home_pages.{AssignHomePagePayload, HomePage, HomePageFindQuery, UnassignHomePagePayload}
import biz.lobachev.annette.cms.api.pages.page._
import biz.lobachev.annette.cms.gateway.Permissions
import biz.lobachev.annette.cms.gateway.home_pages.dto.{AssignHomePagePayloadDto, UnassignHomePagePayloadDto}
import biz.lobachev.annette.core.model.DataSource
import biz.lobachev.annette.core.utils.ChimneyCommons._
import io.scalaland.chimney.dsl._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class CmsHomePageController @Inject() (
  maybeAuthenticated: MaybeAuthenticatedAction,
  authorizer: Authorizer,
  cc: ControllerComponents,
  cmsService: CmsService,
  implicit val ec: ExecutionContext,
  implicit val materializer: Materializer
) extends AbstractController(cc) {

  def assignHomePage =
    maybeAuthenticated.async(parse.json[AssignHomePagePayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_HOME_PAGES) {
        val payload = request.body
          .into[AssignHomePagePayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _      <- cmsService.assignHomePage(payload)
          result <- cmsService.getHomePage(HomePage.toCompositeId(payload.applicationId, payload.principal), DataSource.FROM_ORIGIN)
        } yield Ok(Json.toJson(result))
      }
    }

  def unassignHomePage =
    maybeAuthenticated.async(parse.json[UnassignHomePagePayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_HOME_PAGES) {
        val payload = request.body
          .into[UnassignHomePagePayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _ <- cmsService.unassignHomePage(payload)
        } yield Ok("")
      }
    }

  def findHomePages =
    maybeAuthenticated.async(parse.json[HomePageFindQuery]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_HOME_PAGES) {
        val query = request.request.body
        for {
          result <- cmsService.findHomePages(query)
        } yield Ok(Json.toJson(result))
      }
    }

  def getHomePage(id: PageId, source: Option[String]) =
    maybeAuthenticated.async { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_HOME_PAGES) {
        for {
          result <- cmsService.getHomePage(id, source)
        } yield Ok(Json.toJson(result))
      }
    }

  def getHomePages(source: Option[String]) =
    maybeAuthenticated.async(parse.json[Set[String]]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_HOME_PAGES) {
        val ids = request.request.body
        for {
          result <- cmsService.getHomePages(ids, source)
        } yield Ok(Json.toJson(result))
      }
    }

  def getMyHomePage(applicationId: String) =
    maybeAuthenticated.async { implicit request =>
      for {
        pageId <- cmsService.getHomePageByPrincipalCodes(applicationId, request.subject.principals.map(_.code))
        result <- cmsService.getPageViews(
                    GetPageViewsPayload(
                      ids = Set(pageId),
                      directPrincipal = request.subject.principals.head,
                      principals = request.subject.principals.toSet
                    )
                  )
      } yield Ok(Json.toJson(result.headOption.getOrElse(throw PageNotFound(pageId))))

    }

}
