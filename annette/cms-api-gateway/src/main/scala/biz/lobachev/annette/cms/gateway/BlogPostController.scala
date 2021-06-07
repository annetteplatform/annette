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

import biz.lobachev.annette.api_gateway_core.authentication.AuthenticatedAction
import biz.lobachev.annette.api_gateway_core.authorization.Authorizer
import biz.lobachev.annette.cms.api.CmsService
import biz.lobachev.annette.cms.api.post.{GetPostViewsPayload, PostFindQuery, PostId, PublicationStatus}
import biz.lobachev.annette.cms.api.space.{SpaceFindQuery, SpaceId, SpaceType}
import biz.lobachev.annette.cms.gateway.dto._
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.subscription.api.SubscriptionService
import biz.lobachev.annette.subscription.api.subscription.SubscriptionFindQuery
import io.scalaland.chimney.dsl._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, ControllerComponents}

import java.time.OffsetDateTime
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BlogPostController @Inject() (
  authenticated: AuthenticatedAction,
  authorizer: Authorizer,
  cc: ControllerComponents,
  subscriptionService: SubscriptionService,
  cmsService: CmsService,
  implicit val ec: ExecutionContext
) extends AbstractController(cc) {

//  private val log = LoggerFactory.getLogger(this.getClass)

  def findBlogPosts: Action[PostFindQueryDto] =
    authenticated.async(parse.json[PostFindQueryDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.VIEW_BLOGS) {
        val payload = request.request.body
        for {
          spaces <- getLimitedSpaces(payload.spaces.getOrElse(Set.empty), request.subject.principals.toSet)
          query   = payload
                      .into[PostFindQuery]
                      .withFieldConst(_.spaces, Some(spaces))
                      .withFieldConst(_.targets, Some(request.subject.principals.toSet))
                      .withFieldConst(_.publicationStatus, Some(PublicationStatus.Published))
                      .withFieldConst(_.publicationTimestampFrom, Some(OffsetDateTime.now))
                      .transform
          result <- cmsService.findPosts(query)
        } yield Ok(Json.toJson(result))
      }
    }

  def getPostViews =
    authenticated.async(parse.json[Set[PostId]]) { implicit request =>
      authorizer.performCheckAny(Permissions.VIEW_BLOGS) {
        val ids = request.request.body
        for {
          result <- cmsService.getPostViews(
                      GetPostViewsPayload(
                        ids = ids,
                        principals = request.subject.principals.toSet
                      )
                    )
        } yield Ok(Json.toJson(result))
      }
    }

  private def getLimitedSpaces(spaces: Set[SpaceId], targets: Set[AnnettePrincipal]): Future[Set[SpaceId]] =
    if (spaces.nonEmpty)
      // restrict spaces that user has access
      for {
        findResults       <- cmsService.findSpaces(
                               SpaceFindQuery(
                                 size = spaces.size,
                                 spaceType = Some(SpaceType.Blog),
                                 spaceIds = Some(spaces),
                                 active = Some(true),
                                 targets = Some(targets)
                               )
                             )
      } yield findResults.hits.map(_.id).toSet
    else
      // restrict spaces that user subscribed and has access
      for {
        subscriptions     <- subscriptionService.findSubscriptions(
                               SubscriptionFindQuery(
                                 size = 100,
                                 subscriptionType = Some(Set("blog")),
                                 principals = Some(targets)
                               )
                             )
        subscribedSpaceIds = subscriptions.hits.map(_.subscription.objectId)
        result            <- if (subscribedSpaceIds.nonEmpty)
                               cmsService
                                 .findSpaces(
                                   SpaceFindQuery(
                                     size = subscribedSpaceIds.size,
                                     spaceType = Some(SpaceType.Blog),
                                     spaceIds = Some(subscribedSpaceIds.toSet),
                                     active = Some(true),
                                     targets = Some(targets)
                                   )
                                 )
                                 .map(_.hits.map(_.id).toSet)
                             else Future.successful(Set.empty[SpaceId])
      } yield result

}
