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

import biz.lobachev.annette.api_gateway_core.authentication.{MaybeAuthenticatedAction}
import biz.lobachev.annette.api_gateway_core.authorization.Authorizer
import biz.lobachev.annette.cms.api.{common, CmsService}
import biz.lobachev.annette.cms.api.blogs.blog._
import biz.lobachev.annette.cms.api.blogs.post._
import biz.lobachev.annette.cms.api.common.article.{
  GetMetricPayload,
  LikePayload,
  PublicationStatus,
  UnlikePayload,
  ViewPayload
}
import biz.lobachev.annette.cms.api.common.CanAccessToEntityPayload
import biz.lobachev.annette.cms.gateway.Permissions
import biz.lobachev.annette.cms.gateway.blogs.post._
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.indexing.{FindResult, SortBy}
import biz.lobachev.annette.subscription.api.SubscriptionService
import biz.lobachev.annette.subscription.api.subscription.SubscriptionFindQuery
import io.scalaland.chimney.dsl._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, ControllerComponents}

import java.time.OffsetDateTime
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CmsPostViewController @Inject() (
  maybeAuthenticated: MaybeAuthenticatedAction,
  authorizer: Authorizer,
  cc: ControllerComponents,
  subscriptionService: SubscriptionService,
  cmsService: CmsService,
  implicit val ec: ExecutionContext
) extends AbstractController(cc) {

  val blogSubscriptionType = "blog"

  def findPostViews: Action[PostViewFindQueryDto] =
    maybeAuthenticated.async(parse.json[PostViewFindQueryDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.VIEW_BLOGS) {
        val payload = request.request.body
        for {
          blogs  <- getLimitedBlogs(payload.blogs.getOrElse(Set.empty), request.subject.principals.toSet)
          result <- if (blogs.nonEmpty) {
                      val sortBy =
                        if (payload.filter.map(_.isEmpty).getOrElse(true) && payload.sortBy.isEmpty)
                          Some(
                            Seq(
                              SortBy(
                                field = "featured",
                                descending = Some(true)
                              ),
                              SortBy(
                                field = "publicationTimestamp",
                                descending = Some(true)
                              )
                            )
                          )
                        else payload.sortBy
                      val query  = payload
                        .into[PostFindQuery]
                        .withFieldConst(_.blogs, Some(blogs))
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
    maybeAuthenticated.async(parse.json[Set[PostId]]) { implicit request =>
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
    maybeAuthenticated.async(parse.json[Set[PostId]]) { implicit request =>
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
    maybeAuthenticated.async { implicit request =>
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
    maybeAuthenticated.async { implicit request =>
      authorizer.performCheckAny(Permissions.VIEW_BLOGS) {
        for {
          canAccess <- cmsService.canAccessToPost(
                         CanAccessToEntityPayload(
                           id,
                           request.subject.principals.toSet
                         )
                       )
          _         <- if (canAccess)
                         cmsService.viewPost(ViewPayload(id, request.subject.principals.head))
                       else Future.failed(PostNotFound(id))
          result    <- cmsService.getPostMetricById(GetMetricPayload(id, request.subject.principals.head))
        } yield Ok(Json.toJson(result))
      }
    }

  def likePost(id: PostId) =
    maybeAuthenticated.async { implicit request =>
      authorizer.performCheckAny(Permissions.VIEW_BLOGS) {
        for {
          canAccess <- cmsService.canAccessToPost(
                         common.CanAccessToEntityPayload(
                           id,
                           request.subject.principals.toSet
                         )
                       )
          _         <- if (canAccess)
                         cmsService.likePost(LikePayload(id, request.subject.principals.head))
                       else Future.failed(PostNotFound(id))
          result    <- cmsService.getPostMetricById(GetMetricPayload(id, request.subject.principals.head))
        } yield Ok(Json.toJson(result))
      }
    }

  def unlikePost(id: PostId) =
    maybeAuthenticated.async { implicit request =>
      authorizer.performCheckAny(Permissions.VIEW_BLOGS) {
        for {
          canAccess <- cmsService.canAccessToPost(
                         common.CanAccessToEntityPayload(
                           id,
                           request.subject.principals.toSet
                         )
                       )
          _         <- if (canAccess)
                         cmsService.unlikePost(UnlikePayload(id, request.subject.principals.head))
                       else Future.failed(PostNotFound(id))
          result    <- cmsService.getPostMetricById(GetMetricPayload(id, request.subject.principals.head))
        } yield Ok(Json.toJson(result))
      }
    }

  private def getLimitedBlogs(blogs: Set[BlogId], targets: Set[AnnettePrincipal]): Future[Set[BlogId]] =
    if (blogs.nonEmpty)
      // restrict blogs that user has access
      for {
        findResults      <- cmsService.findBlogs(
                              BlogFindQuery(
                                size = blogs.size,
                                blogIds = Some(blogs),
                                active = Some(true),
                                targets = Some(targets)
                              )
                            )
      } yield findResults.hits.map(_.id).toSet
    else
      // restrict blogs that user subscribed and has access
      for {
        subscriptions    <- subscriptionService.findSubscriptions(
                              SubscriptionFindQuery(
                                size = 100,
                                subscriptionType = Some(Set(blogSubscriptionType)),
                                principals = Some(targets)
                              )
                            )
        subscribedBlogIds = subscriptions.hits.map(_.subscription.objectId)
        result           <- if (subscribedBlogIds.nonEmpty)
                              cmsService
                                .findBlogs(
                                  BlogFindQuery(
                                    size = subscribedBlogIds.size,
                                    blogIds = Some(subscribedBlogIds.toSet),
                                    active = Some(true),
                                    targets = Some(targets)
                                  )
                                )
                                .map(_.hits.map(_.id).toSet)
                            else Future.successful(Set.empty[BlogId])
      } yield result

}
