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
import biz.lobachev.annette.cms.api.space._
import biz.lobachev.annette.cms.gateway.space._
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
class CmsSpaceViewController @Inject() (
  authenticated: AuthenticatedAction,
  authorizer: Authorizer,
  cc: ControllerComponents,
  subscriptionService: SubscriptionService,
  cmsService: CmsService,
  implicit val ec: ExecutionContext
) extends AbstractController(cc) {

  val spaceSubscriptionType = "space"

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
                              .map(sv =>
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
}
