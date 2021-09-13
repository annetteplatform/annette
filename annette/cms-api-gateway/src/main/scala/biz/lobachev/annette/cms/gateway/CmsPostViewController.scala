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
import biz.lobachev.annette.cms.api.post._
import biz.lobachev.annette.cms.api.space._
import biz.lobachev.annette.cms.gateway.post._
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.elastic.{FindResult, SortBy}
import biz.lobachev.annette.subscription.api.SubscriptionService
import biz.lobachev.annette.subscription.api.subscription.{SubscriptionFindQuery}
import io.scalaland.chimney.dsl._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, ControllerComponents}

import java.time.OffsetDateTime
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CmsPostViewController @Inject() (
  authenticated: AuthenticatedAction,
  authorizer: Authorizer,
  cc: ControllerComponents,
  subscriptionService: SubscriptionService,
  cmsService: CmsService,
  implicit val ec: ExecutionContext
) extends AbstractController(cc) {

  val spaceSubscriptionType = "space"

  def findPostViews: Action[PostViewFindQueryDto] =
    authenticated.async(parse.json[PostViewFindQueryDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.VIEW_BLOGS) {
        val payload = request.request.body
        for {
          spaces <- getLimitedSpaces(payload.spaces.getOrElse(Set.empty), request.subject.principals.toSet)
          result <- if (spaces.nonEmpty) {
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
                        .withFieldConst(_.spaces, Some(spaces))
                        .withFieldConst(_.targets, Some(request.subject.principals.toSet))
                        .withFieldConst(_.publicationStatus, Some(PublicationStatus.Published))
                        .withFieldConst(_.publicationTimestampTo, Some(OffsetDateTime.now))
                        .withFieldConst(_.sortBy, sortBy)
                        .transform
                      cmsService.findPosts(query)
                    } else Future.successful(FindResult(0, Seq.empty))
        } yield Ok(Json.toJson(result))
      }
    }

  def getPostViewAnnotationsById =
    authenticated.async(parse.json[Set[PostId]]) { implicit request =>
      authorizer.performCheckAny(Permissions.VIEW_BLOGS) {
        val ids = request.request.body
        for {
          postViewAnnotations <- cmsService.getPostViews(
                                   GetPostViewsPayload(
                                     ids = ids,
                                     directPrincipal = request.subject.principals.head,
                                     principals = request.subject.principals.toSet,
                                     false
                                   )
                                 )
        } yield Ok(Json.toJson(postViewAnnotations))
      }
    }

  def getPostViewsById =
    authenticated.async(parse.json[Set[PostId]]) { implicit request =>
      authorizer.performCheckAny(Permissions.VIEW_BLOGS) {
        val ids = request.request.body
        for {
          result <- cmsService.getPostViews(
                      GetPostViewsPayload(
                        ids = ids,
                        directPrincipal = request.subject.principals.head,
                        principals = request.subject.principals.toSet,
                        withContent = true
                      )
                    )
        } yield Ok(Json.toJson(result))
      }
    }

  def getPostViewById(postId: PostId) =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(Permissions.VIEW_BLOGS) {
        for {
          result   <- cmsService.getPostViews(
                        GetPostViewsPayload(
                          ids = Set(postId),
                          directPrincipal = request.subject.principals.head,
                          principals = request.subject.principals.toSet,
                          withContent = true
                        )
                      )
          resultMap = result.map(a => a.id -> a).toMap
        } yield Ok(Json.toJson(resultMap.get(postId).getOrElse(throw PostNotFound(postId))))
      }
    }

  def viewPost(id: PostId) =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(Permissions.VIEW_BLOGS, Permissions.VIEW_WIKIES) {
        for {
          canAccess <- cmsService.canAccessToPost(
                         CanAccessToPostPayload(
                           id,
                           request.subject.principals.toSet
                         )
                       )
          _         <- if (canAccess)
                         cmsService.viewPost(ViewPostPayload(id, request.subject.principals.head))
                       else Future.failed(PostNotFound(id))
          result    <- cmsService.getPostMetricById(GetPostMetricPayload(id, request.subject.principals.head))
        } yield Ok(Json.toJson(result))
      }
    }

  def likePost(id: PostId) =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(Permissions.VIEW_BLOGS, Permissions.VIEW_WIKIES) {
        for {
          canAccess <- cmsService.canAccessToPost(
                         CanAccessToPostPayload(
                           id,
                           request.subject.principals.toSet
                         )
                       )
          _         <- if (canAccess)
                         cmsService.likePost(LikePostPayload(id, request.subject.principals.head))
                       else Future.failed(PostNotFound(id))
          result    <- cmsService.getPostMetricById(GetPostMetricPayload(id, request.subject.principals.head))
        } yield Ok(Json.toJson(result))
      }
    }

  def unlikePost(id: PostId) =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(Permissions.VIEW_BLOGS, Permissions.VIEW_WIKIES) {
        for {
          canAccess <- cmsService.canAccessToPost(
                         CanAccessToPostPayload(
                           id,
                           request.subject.principals.toSet
                         )
                       )
          _         <- if (canAccess)
                         cmsService.unlikePost(UnlikePostPayload(id, request.subject.principals.head))
                       else Future.failed(PostNotFound(id))
          result    <- cmsService.getPostMetricById(GetPostMetricPayload(id, request.subject.principals.head))
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
        _                  = println(s"getLimitedSpaces: findResult ${findResults.toString}")
      } yield findResults.hits.map(_.id).toSet
    else
      // restrict spaces that user subscribed and has access
      for {
        subscriptions     <- subscriptionService.findSubscriptions(
                               SubscriptionFindQuery(
                                 size = 100,
                                 subscriptionType = Some(Set(spaceSubscriptionType)),
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
