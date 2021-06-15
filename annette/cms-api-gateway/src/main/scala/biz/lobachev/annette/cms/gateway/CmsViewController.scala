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
import biz.lobachev.annette.cms.api.category.{
  CategoryFindQuery,
  CategoryId,
  CreateCategoryPayload,
  DeleteCategoryPayload,
  UpdateCategoryPayload
}
import biz.lobachev.annette.cms.api.post._
import biz.lobachev.annette.cms.api.space._
import biz.lobachev.annette.cms.gateway.Permissions.{MAINTAIN_ALL_SPACE_CATEGORIES, VIEW_ALL_SPACE_CATEGORIES}
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

  val spaceSubscriptionType = "space"

//  private val log = LoggerFactory.getLogger(this.getClass)

  // ****************************** PostView ******************************

  def findPostViews: Action[PostFindQueryDto] =
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
          result <- cmsService.getPostViews(
                      GetPostViewsPayload(
                        ids = Set(postId),
                        directPrincipal = request.subject.principals.head,
                        principals = request.subject.principals.toSet,
                        withContent = true
                      )
                    )
        } yield Ok(Json.toJson(result.get(postId).getOrElse(throw PostNotFound(postId))))
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

  // ****************************** Spaces ******************************

  def findSpaceViews: Action[SpaceFindQueryDto] =
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

  def getSpaceViewsById =
    authenticated.async(parse.json[Set[SpaceId]]) { implicit request =>
      authorizer.performCheckAny(Permissions.VIEW_BLOGS, Permissions.VIEW_WIKIES) {
        val ids                = request.request.body
        val spaceFuture        = cmsService.getSpaceViews(
          GetSpaceViewsPayload(
            ids = ids,
            principals = request.subject.principals.toSet
          )
        )
        val subscriptionFuture = subscriptionService.findSubscriptions(
          SubscriptionFindQuery(
            size = 1000,
            principals = Some(request.subject.principals.toSet),
            subscriptionType = Some(Set(spaceSubscriptionType)),
            objects = Some(ids)
          )
        )
        for {
          spaces         <- spaceFuture
          subscriptions  <- subscriptionFuture
          subscriptionMap = subscriptions.hits
                              .map(_.subscription)
                              .toSet
                              .groupMap[String, AnnettePrincipal](_.objectId)(_.principal)
          result          = spaces.view
                              .mapValues(sv =>
                                sv.into[SpaceViewDto]
                                  .withFieldConst(_.subscriptions, subscriptionMap.get(sv.id).getOrElse(Set.empty))
                                  .transform
                              )
        } yield Ok(Json.toJson(result))
      }
    }

  def subscribeToSpace(spaceId: SpaceId) =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(Permissions.VIEW_BLOGS, Permissions.VIEW_WIKIES) {
        for {
          canAccessToSpace <- cmsService
                                .canAccessToSpace(
                                  CanAccessToSpacePayload(
                                    id = spaceId,
                                    principals = request.subject.principals.toSet
                                  )
                                )

          _                 = if (!canAccessToSpace) throw SpaceNotFound(spaceId)
          _                <- subscriptionService.createSubscription(
                                CreateSubscriptionPayload(
                                  spaceSubscriptionType,
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
      authorizer.performCheckAny(Permissions.VIEW_BLOGS, Permissions.VIEW_WIKIES) {
        for {
          _ <- subscriptionService.deleteSubscription(
                 DeleteSubscriptionPayload(
                   spaceSubscriptionType,
                   spaceId,
                   request.subject.principals.head,
                   request.subject.principals.head
                 )
               )
        } yield Ok("")
      }
    }

  // ****************************** Categories ******************************

  def createCategory =
    authenticated.async(parse.json[CategoryDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_SPACE_CATEGORIES) {
        val payload = request.body
          .into[CreateCategoryPayload]
          .withFieldConst(_.createdBy, request.subject.principals.head)
          .transform
        for {
          _    <- cmsService.createCategory(payload)
          role <- cmsService.getCategoryById(payload.id, false)
        } yield Ok(Json.toJson(role))
      }
    }

  def updateCategory =
    authenticated.async(parse.json[CategoryDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_SPACE_CATEGORIES) {
        val payload = request.body
          .into[UpdateCategoryPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _    <- cmsService.updateCategory(payload)
          role <- cmsService.getCategoryById(payload.id, false)
        } yield Ok(Json.toJson(role))
      }
    }

  def deleteCategory =
    authenticated.async(parse.json[DeleteCategoryDto]) { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_SPACE_CATEGORIES) {
        val payload = request.body
          .into[DeleteCategoryPayload]
          .withFieldConst(_.deletedBy, request.subject.principals.head)
          .transform
        for {
          _ <- cmsService.deleteCategory(payload)
        } yield Ok("")
      }
    }

  def getCategoryById(id: CategoryId) =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(VIEW_ALL_SPACE_CATEGORIES, MAINTAIN_ALL_SPACE_CATEGORIES) {
        for {
          role <- cmsService.getCategoryById(id, true)
        } yield Ok(Json.toJson(role))
      }
    }

  def getCategoryByIdForEdit(id: CategoryId) =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(MAINTAIN_ALL_SPACE_CATEGORIES) {
        for {
          role <- cmsService.getCategoryById(id, false)
        } yield Ok(Json.toJson(role))
      }
    }

  def findCategories: Action[CategoryFindQuery] =
    authenticated.async(parse.json[CategoryFindQuery]) { implicit request =>
      authorizer.performCheckAny(VIEW_ALL_SPACE_CATEGORIES, MAINTAIN_ALL_SPACE_CATEGORIES) {
        for {
          result <- cmsService.findCategories(request.request.body)
        } yield Ok(Json.toJson(result))
      }
    }

  def getCategoriesById =
    authenticated.async(parse.json[Set[CategoryId]]) { implicit request =>
      authorizer.performCheckAny(VIEW_ALL_SPACE_CATEGORIES, MAINTAIN_ALL_SPACE_CATEGORIES) {
        for {
          result <- cmsService.getCategoriesById(request.request.body, true)
        } yield Ok(Json.toJson(result))
      }
    }

}
