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
import biz.lobachev.annette.cms.api.category.{CategoryFindQuery, CategoryId}
import biz.lobachev.annette.cms.api.post._
import biz.lobachev.annette.cms.api.space._
import biz.lobachev.annette.cms.gateway.dto._
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.elastic.{FindResult, SortBy}
import biz.lobachev.annette.subscription.api.SubscriptionService
import biz.lobachev.annette.subscription.api.subscription.{
  CreateSubscriptionPayload,
  DeleteSubscriptionPayload,
  SubscriptionFindQuery
}
import io.scalaland.chimney.dsl._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, ControllerComponents}

import java.time.OffsetDateTime
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CmsViewController @Inject() (
  authenticated: AuthenticatedAction,
  authorizer: Authorizer,
  cc: ControllerComponents,
  subscriptionService: SubscriptionService,
  cmsService: CmsService,
  implicit val ec: ExecutionContext
) extends AbstractController(cc) {

  val blogSubscriptionType = "blog"

//  private val log = LoggerFactory.getLogger(this.getClass)

  def findMyBlogSubscriptions =
    authenticated.async(parse.json[SubscriptionFindQueryDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.VIEW_BLOGS) {
        val payload = request.request.body
        val query   = payload
          .into[SubscriptionFindQuery]
          .withFieldConst(_.principals, Some(request.subject.principals.toSet))
          .withFieldConst(_.subscriptionType, Some(Set(blogSubscriptionType)))
          .withFieldComputed(_.objects, _.spaceIds)
          .transform

        for {
          result <- subscriptionService.findSubscriptions(query)
        } yield Ok(
          Json.toJson(
            result.hits.map(hit => BlogSubscriptionDto(hit.subscription.objectId, hit.subscription.principal))
          )
        )
      }
    }

  def subscribeToSpace(spaceId: SpaceId) =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(Permissions.VIEW_BLOGS) {
        for {
          spaceExist <- cmsService
                          .getSpaceViews(
                            GetSpaceViewsPayload(
                              ids = Set(spaceId),
                              principals = request.subject.principals.toSet
                            )
                          )
                          .map(_.contains(spaceId))
          _           = if (!spaceExist) throw SpaceNotFound(spaceId)
          _          <- subscriptionService.createSubscription(
                          CreateSubscriptionPayload(
                            blogSubscriptionType,
                            spaceId,
                            request.subject.principals.head,
                            request.subject.principals.head
                          )
                        )
        } yield Ok("")
      }
    }

  def unsubscribeFromSpace(spaceId: SpaceId) =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(Permissions.VIEW_BLOGS) {
        for {
          _ <- subscriptionService.deleteSubscription(
                 DeleteSubscriptionPayload(
                   blogSubscriptionType,
                   spaceId,
                   request.subject.principals.head,
                   request.subject.principals.head
                 )
               )
        } yield Ok("")
      }
    }

  def findSpaces: Action[SpaceFindQueryDto] =
    authenticated.async(parse.json[SpaceFindQueryDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.VIEW_BLOGS, Permissions.VIEW_WIKIES) {
        val payload = request.request.body
        val query   = payload
          .into[SpaceFindQuery]
          .withFieldConst(_.targets, Some(request.subject.principals.toSet))
          .withFieldConst(_.active, Some(true))
          .transform

        for {
          result <- cmsService.findSpaces(query)
        } yield Ok(Json.toJson(result))
      }
    }

  def getSpaces =
    authenticated.async(parse.json[Set[SpaceId]]) { implicit request =>
      authorizer.performCheckAny(Permissions.VIEW_BLOGS, Permissions.VIEW_WIKIES) {
        val ids = request.request.body
        for {
          result <- cmsService.getSpaceViews(
                      GetSpaceViewsPayload(
                        ids = ids,
                        principals = request.subject.principals.toSet
                      )
                    )
        } yield Ok(Json.toJson(result))
      }
    }

  def findBlogPosts: Action[PostFindQueryDto] =
    authenticated.async(parse.json[PostFindQueryDto]) { implicit request =>
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

  def findCategories: Action[CategoryFindQuery] =
    authenticated.async(parse.json[CategoryFindQuery]) { implicit request =>
      authorizer.performCheckAny(Permissions.VIEW_BLOGS, Permissions.VIEW_WIKIES) {
        for {
          result <- cmsService.findCategories(request.request.body)
        } yield Ok(Json.toJson(result))
      }
    }

  def getCategories =
    authenticated.async(parse.json[Set[CategoryId]]) { implicit request =>
      authorizer.performCheckAny(Permissions.VIEW_BLOGS, Permissions.VIEW_WIKIES) {
        for {
          result <- cmsService.getCategoriesById(request.request.body, true)

        } yield Ok(Json.toJson(result))
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

  def getPostMetricById(id: PostId) =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(Permissions.VIEW_BLOGS, Permissions.VIEW_WIKIES) {
        for {
          canAccess <- cmsService.canAccessToPost(
                         CanAccessToPostPayload(
                           id,
                           request.subject.principals.toSet
                         )
                       )
          result    <- if (canAccess)
                         cmsService.getPostMetricById(GetPostMetricPayload(id, request.subject.principals.head))
                       else Future.failed(PostNotFound(id))
        } yield Ok(Json.toJson(result))
      }
    }

  def getPostMetricsById =
    authenticated.async(parse.json[Set[PostId]]) { implicit request =>
      authorizer.performCheckAny(Permissions.VIEW_BLOGS, Permissions.VIEW_WIKIES) {
        for {
          result <- cmsService.getPostMetricsById(
                      GetPostMetricsPayload(request.request.body, request.subject.principals.head)
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
        _                  = println(s"getLimitedSpaces: findResult ${findResults.toString}")
      } yield findResults.hits.map(_.id).toSet
    else
      // restrict spaces that user subscribed and has access
      for {
        subscriptions     <- subscriptionService.findSubscriptions(
                               SubscriptionFindQuery(
                                 size = 100,
                                 subscriptionType = Some(Set(blogSubscriptionType)),
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
