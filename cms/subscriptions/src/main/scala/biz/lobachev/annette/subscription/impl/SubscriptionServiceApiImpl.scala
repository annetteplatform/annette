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

package biz.lobachev.annette.subscription.impl

import java.time.OffsetDateTime
import com.google.protobuf.empty.Empty
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.indexing.FindResult
import biz.lobachev.annette.subscription.api.{grpc => g}
import biz.lobachev.annette.subscription.api.grpc.SubscriptionService
import biz.lobachev.annette.subscription.api.subscription._
import biz.lobachev.annette.subscription.api.subscription_type._
import biz.lobachev.annette.subscription.impl.subscription.SubscriptionEntityService
import biz.lobachev.annette.subscription.impl.subscription_type.SubscriptionTypeEntityService

import scala.concurrent.{ExecutionContext, Future}

class SubscriptionServiceApiImpl(
  subscriptionEntityService: SubscriptionEntityService,
  subscriptionTypeEntityService: SubscriptionTypeEntityService
)(implicit ec: ExecutionContext) extends SubscriptionService {

  private def toDomain(p: g.AnnettePrincipal): AnnettePrincipal =
    AnnettePrincipal(p.code)

  private def fromDomain(p: AnnettePrincipal): g.AnnettePrincipal =
    g.AnnettePrincipal(p.code)

  private def unwrapPrincipal(p: Option[g.AnnettePrincipal]): AnnettePrincipal =
    p.map(toDomain).getOrElse(throw new IllegalArgumentException("missing principal"))

  private def fromDomain(k: SubscriptionKey): g.SubscriptionKey =
    g.SubscriptionKey(
      subscriptionType = k.subscriptionType,
      objectId = k.objectId,
      principal = Some(fromDomain(k.principal))
    )

  private def toDomain(k: g.SubscriptionKey): SubscriptionKey =
    SubscriptionKey(
      subscriptionType = k.subscriptionType,
      objectId = k.objectId,
      principal = unwrapPrincipal(k.principal)
    )

  private def fromDomain(s: Subscription): g.Subscription =
    g.Subscription(
      subscriptionType = s.subscriptionType,
      objectId = s.objectId,
      principal = Some(fromDomain(s.principal)),
      updatedAt = s.updatedAt.toString,
      updatedBy = Some(fromDomain(s.updatedBy))
    )

  private def fromDomain(t: SubscriptionType): g.SubscriptionType =
    g.SubscriptionType(
      id = t.id,
      name = t.name,
      updatedAt = t.updatedAt.toString,
      updatedBy = Some(fromDomain(t.updatedBy))
    )

  private def formatOdt(odt: OffsetDateTime): String = odt.toString

  // === Subscription CRUD ===

  override def createSubscription(in: g.CreateSubscriptionPayload): Future[Empty] =
    subscriptionEntityService
      .createSubscription(
        CreateSubscriptionPayload(
          subscriptionType = in.subscriptionType,
          objectId = in.objectId,
          principal = unwrapPrincipal(in.principal),
          createdBy = unwrapPrincipal(in.createdBy)
        )
      )
      .map(_ => Empty())

  override def deleteSubscription(in: g.DeleteSubscriptionPayload): Future[Empty] =
    subscriptionEntityService
      .deleteSubscription(
        DeleteSubscriptionPayload(
          subscriptionType = in.subscriptionType,
          objectId = in.objectId,
          principal = unwrapPrincipal(in.principal),
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def getSubscription(in: g.GetSubscriptionRequest): Future[g.Subscription] =
    subscriptionEntityService
      .getSubscription(toDomain(in.key.getOrElse(throw new IllegalArgumentException("missing key"))), in.source)
      .map(fromDomain)

  override def getSubscriptions(in: g.GetSubscriptionsRequest): Future[g.GetSubscriptionsResponse] =
    subscriptionEntityService
      .getSubscriptions(in.keys.map(toDomain).toSet, in.source)
      .map(subscriptions => g.GetSubscriptionsResponse(subscriptions = subscriptions.map(fromDomain).toSeq))

  override def getSubscriptionsByPrincipals(
    in: g.GetSubscriptionsByPrincipalsRequest
  ): Future[g.GetSubscriptionKeysResponse] =
    subscriptionEntityService
      .getSubscriptionsByPrincipals(in.subscriptionType, in.principals.map(toDomain).toSet)
      .map(keys => g.GetSubscriptionKeysResponse(keys = keys.map(fromDomain).toSeq))

  override def getSubscriptionsByObjects(
    in: g.GetSubscriptionsByObjectsRequest
  ): Future[g.GetSubscriptionKeysResponse] =
    subscriptionEntityService
      .getSubscriptionsByObjects(in.subscriptionType, in.objectIds.toSet)
      .map(keys => g.GetSubscriptionKeysResponse(keys = keys.map(fromDomain).toSeq))

  override def findSubscriptions(in: g.SubscriptionFindQuery): Future[g.SubscriptionFindResult] =
    subscriptionEntityService
      .findSubscriptions(
        SubscriptionFindQuery(
          offset = in.offset,
          size = in.size,
          subscriptionType = if (in.subscriptionTypes.isEmpty) None else Some(in.subscriptionTypes.toSet),
          objects = if (in.objectIds.isEmpty) None else Some(in.objectIds.toSet),
          principals = if (in.principals.isEmpty) None else Some(in.principals.map(toDomain).toSet),
          sortBy = Some(in.sortBy.map(s => biz.lobachev.annette.core.model.indexing.SortBy(field = s.field, descending = s.descending)).toSeq).filter(_.nonEmpty)
        )
      )
      .map(fromDomain)

  // === SubscriptionType CRUD ===

  override def createSubscriptionType(in: g.CreateSubscriptionTypePayload): Future[Empty] =
    subscriptionTypeEntityService
      .createSubscriptionType(
        CreateSubscriptionTypePayload(
          id = in.id,
          name = in.name,
          createdBy = unwrapPrincipal(in.createdBy)
        )
      )
      .map(_ => Empty())

  override def updateSubscriptionType(in: g.UpdateSubscriptionTypePayload): Future[Empty] =
    subscriptionTypeEntityService
      .updateSubscriptionType(
        UpdateSubscriptionTypePayload(
          id = in.id,
          name = in.name,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def deleteSubscriptionType(in: g.DeleteSubscriptionTypePayload): Future[Empty] =
    subscriptionTypeEntityService
      .deleteSubscriptionType(
        DeleteSubscriptionTypePayload(id = in.id, updatedBy = unwrapPrincipal(in.updatedBy))
      )
      .map(_ => Empty())

  override def getSubscriptionType(in: g.GetSubscriptionTypeRequest): Future[g.SubscriptionType] =
    subscriptionTypeEntityService
      .getSubscriptionType(in.id, in.source)
      .map(fromDomain)

  override def getSubscriptionTypes(in: g.GetSubscriptionTypesRequest): Future[g.GetSubscriptionTypesResponse] =
    subscriptionTypeEntityService
      .getSubscriptionTypes(in.ids.toSet, in.source)
      .map(types => g.GetSubscriptionTypesResponse(subscriptionTypes = types.map(fromDomain)))

  override def findSubscriptionTypes(in: g.SubscriptionTypeFindQuery): Future[g.FindResult] =
    subscriptionTypeEntityService
      .findSubscriptionTypes(
        SubscriptionTypeFindQuery(
          offset = in.offset,
          size = in.size,
          filter = in.filter,
          name = in.name,
          sortBy = Some(in.sortBy.map(s => biz.lobachev.annette.core.model.indexing.SortBy(field = s.field, descending = s.descending)).toSeq).filter(_.nonEmpty)
        )
      )
      .map(fromDomain)

  // === domain → proto (response converters) ===

  private def fromDomain(r: SubscriptionFindResult): g.SubscriptionFindResult =
    g.SubscriptionFindResult(
      total = r.total,
      hits = r.hits.map(h => g.SubscriptionHitResult(subscription = Some(fromDomain(h.subscription)), score = h.score, updatedAt = formatOdt(h.updatedAt)))
    )

  private def fromDomain(f: FindResult): g.FindResult =
    g.FindResult(
      total = f.total,
      hits = f.hits.map(h => g.HitResult(id = h.id, score = h.score, updatedAt = formatOdt(h.updatedAt)))
    )
}
