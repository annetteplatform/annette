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
import biz.lobachev.annette.cms.api.blogs.blog._
import biz.lobachev.annette.cms.gateway.blogs.blog._
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.subscription.api.SubscriptionService
import biz.lobachev.annette.subscription.api.subscription.{
  CreateSubscriptionPayload,
  DeleteSubscriptionPayload,
  SubscriptionFindQuery
}
import io.scalaland.chimney.dsl._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext}

@Singleton
class CmsBlogViewController @Inject() (
  authenticated: AuthenticatedAction,
  authorizer: Authorizer,
  cc: ControllerComponents,
  subscriptionService: SubscriptionService,
  cmsService: CmsService,
  implicit val ec: ExecutionContext
) extends AbstractController(cc) {

  val blogSubscriptionType = "blog"

  def findBlogViews: Action[BlogFindQueryDto] =
    authenticated.async(parse.json[BlogFindQueryDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.VIEW_BLOGS) {
        val payload = request.request.body
        val query   = payload
          .into[BlogFindQuery]
          .withFieldConst(_.targets, Some(request.subject.principals.toSet))
          .withFieldConst(_.active, Some(true))
          .transform

        for {
          result <- cmsService.findBlogs(query)
        } yield Ok(Json.toJson(result))
      }
    }

  def getBlogViewsById =
    authenticated.async(parse.json[Set[BlogId]]) { implicit request =>
      authorizer.performCheckAny(Permissions.VIEW_BLOGS) {
        val ids                = request.request.body
        val blogFuture         = cmsService.getBlogViews(
          GetBlogViewsPayload(
            ids = ids,
            principals = request.subject.principals.toSet
          )
        )
        val subscriptionFuture = subscriptionService.findSubscriptions(
          SubscriptionFindQuery(
            size = 1000,
            principals = Some(request.subject.principals.toSet),
            subscriptionType = Some(Set(blogSubscriptionType)),
            objects = Some(ids)
          )
        )
        for {
          blogs          <- blogFuture
          subscriptions  <- subscriptionFuture
          subscriptionMap = subscriptions.hits
                              .map(_.subscription)
                              .toSet
                              .groupMap[String, AnnettePrincipal](_.objectId)(_.principal)
          result          = blogs.view
                              .map(sv =>
                                sv.into[BlogViewDto]
                                  .withFieldConst(_.subscriptions, subscriptionMap.get(sv.id).getOrElse(Set.empty))
                                  .transform
                              )
        } yield Ok(Json.toJson(result))
      }
    }

  def subscribeToBlog(blogId: BlogId) =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(Permissions.VIEW_BLOGS) {
        for {
          canAccessToBlog <- cmsService
                               .canAccessToBlog(
                                 CanAccessToBlogPayload(
                                   id = blogId,
                                   principals = request.subject.principals.toSet
                                 )
                               )

          _                = if (!canAccessToBlog) throw BlogNotFound(blogId)
          _               <- subscriptionService.createSubscription(
                               CreateSubscriptionPayload(
                                 blogSubscriptionType,
                                 blogId,
                                 request.subject.principals.head,
                                 request.subject.principals.head
                               )
                             )
          subscriptions   <- subscriptionService.findSubscriptions(
                               SubscriptionFindQuery(
                                 size = 1000,
                                 principals = Some(request.subject.principals.toSet),
                                 subscriptionType = Some(Set(blogSubscriptionType)),
                                 objects = Some(Set(blogId))
                               )
                             )
          result           = subscriptions.hits.map(_.subscription.principal).toSet + request.subject.principals.head
        } yield Ok(Json.toJson(result))
      }
    }

  def unsubscribeFromBlog(blogId: BlogId) =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(Permissions.VIEW_BLOGS) {
        for {
          _             <- subscriptionService.deleteSubscription(
                             DeleteSubscriptionPayload(
                               blogSubscriptionType,
                               blogId,
                               request.subject.principals.head,
                               request.subject.principals.head
                             )
                           )
          subscriptions <- subscriptionService.findSubscriptions(
                             SubscriptionFindQuery(
                               size = 1000,
                               principals = Some(request.subject.principals.toSet),
                               subscriptionType = Some(Set(blogSubscriptionType)),
                               objects = Some(Set(blogId))
                             )
                           )
          result         = subscriptions.hits.map(_.subscription.principal).toSet - request.subject.principals.head
        } yield Ok(Json.toJson(result))
      }
    }
}
