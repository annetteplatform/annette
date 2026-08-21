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

package biz.lobachev.annette.subscription.api

import java.time.OffsetDateTime
import biz.lobachev.annette.core.exception.AnnetteGrpcExceptionMapping
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.indexing.{FindResult, HitResult, SortBy}
import biz.lobachev.annette.subscription.api.subscription._
import biz.lobachev.annette.subscription.api.subscription_type._
import biz.lobachev.annette.subscription.api.{grpc => g}
import org.apache.pekko.Done

import scala.concurrent.{ExecutionContext, Future}

class SubscriptionServiceGrpcImpl(client: g.SubscriptionServiceClient)(implicit val ec: ExecutionContext)
    extends SubscriptionService {

  private def unwrapPrincipal(p: Option[g.AnnettePrincipal]): AnnettePrincipal =
    p.map(pr => AnnettePrincipal(pr.code)).getOrElse(throw new IllegalArgumentException("missing principal"))

  private def fromDomain(p: AnnettePrincipal): g.AnnettePrincipal = g.AnnettePrincipal(p.code)

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

  private def toDomain(s: g.Subscription): Subscription =
    Subscription(
      subscriptionType = s.subscriptionType,
      objectId = s.objectId,
      principal = unwrapPrincipal(s.principal),
      updatedAt = OffsetDateTime.parse(s.updatedAt),
      updatedBy = unwrapPrincipal(s.updatedBy)
    )

  private def sortByToProto(sortBy: Option[Seq[SortBy]]): Seq[g.SortBy] =
    sortBy.map(_.map(s => g.SortBy(field = s.field, descending = s.descending))).getOrElse(Seq.empty)

  private def findResultFromProto(f: g.FindResult): FindResult =
    FindResult(
      total = f.total,
      hits = f.hits.map(h => HitResult(id = h.id, score = h.score, updatedAt = OffsetDateTime.parse(h.updatedAt)))
    )

  private def toDomain(t: g.SubscriptionType): SubscriptionType =
    SubscriptionType(
      id = t.id,
      name = t.name,
      updatedAt = OffsetDateTime.parse(t.updatedAt),
      updatedBy = unwrapPrincipal(t.updatedBy)
    )

  private def call[T](f: Future[T]): Future[T] = AnnetteGrpcExceptionMapping.recoverAnnette(f)

  override def createSubscription(payload: CreateSubscriptionPayload): Future[Done] =
    call(
      client.createSubscription(
        g.CreateSubscriptionPayload(
          subscriptionType = payload.subscriptionType,
          objectId = payload.objectId,
          principal = Some(fromDomain(payload.principal)),
          createdBy = Some(fromDomain(payload.createdBy))
        )
      )
    ).map(_ => Done)

  override def deleteSubscription(payload: DeleteSubscriptionPayload): Future[Done] =
    call(
      client.deleteSubscription(
        g.DeleteSubscriptionPayload(
          subscriptionType = payload.subscriptionType,
          objectId = payload.objectId,
          principal = Some(fromDomain(payload.principal)),
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def getSubscription(key: SubscriptionKey, source: Option[String]): Future[Subscription] =
    call(
      client.getSubscription(g.GetSubscriptionRequest(key = Some(fromDomain(key)), source = source))
    ).map(toDomain)

  override def getSubscriptions(keys: Set[SubscriptionKey], source: Option[String]): Future[Set[Subscription]] =
    call(
      client.getSubscriptions(
        g.GetSubscriptionsRequest(keys = keys.map(fromDomain).toSeq, source = source)
      )
    ).map(_.subscriptions.map(toDomain).toSet)

  override def getSubscriptionsByPrincipals(
    principals: Set[AnnettePrincipal],
    subscriptionType: SubscriptionTypeId
  ): Future[Set[SubscriptionKey]] =
    call(
      client.getSubscriptionsByPrincipals(
        g.GetSubscriptionsByPrincipalsRequest(
          subscriptionType = subscriptionType,
          principals = principals.map(fromDomain).toSeq
        )
      )
    ).map(_.keys.map(toDomain).toSet)

  override def getSubscriptionsByObjects(
    objectIds: Set[ObjectId],
    subscriptionType: SubscriptionTypeId
  ): Future[Set[SubscriptionKey]] =
    call(
      client.getSubscriptionsByObjects(
        g.GetSubscriptionsByObjectsRequest(
          subscriptionType = subscriptionType,
          objectIds = objectIds.toSeq
        )
      )
    ).map(_.keys.map(toDomain).toSet)

  override def findSubscriptions(query: SubscriptionFindQuery): Future[SubscriptionFindResult] =
    call(
      client.findSubscriptions(
        g.SubscriptionFindQuery(
          offset = query.offset,
          size = query.size,
          subscriptionTypes = query.subscriptionType.map(_.toSeq).getOrElse(Seq.empty),
          objectIds = query.objects.map(_.toSeq).getOrElse(Seq.empty),
          principals = query.principals.map(_.map(fromDomain).toSeq).getOrElse(Seq.empty),
          sortBy = sortByToProto(query.sortBy)
        )
      )
    ).map { r =>
      SubscriptionFindResult(
        total = r.total,
        hits = r.hits.map(
          h =>
            SubscriptionHitResult(
              subscription = toDomain(
                h.subscription.getOrElse(throw new IllegalArgumentException("missing subscription"))
              ),
              score = h.score,
              updatedAt = OffsetDateTime.parse(h.updatedAt)
            )
        )
      )
    }

  // subscriptionType methods

  override def createSubscriptionType(payload: CreateSubscriptionTypePayload): Future[Done] =
    call(
      client.createSubscriptionType(
        g.CreateSubscriptionTypePayload(
          id = payload.id,
          name = payload.name,
          createdBy = Some(fromDomain(payload.createdBy))
        )
      )
    ).map(_ => Done)

  override def createOrUpdateSubscriptionType(payload: CreateSubscriptionTypePayload): Future[Done] =
    createSubscriptionType(payload).recoverWith {
      case SubscriptionTypeAlreadyExist(_) =>
        updateSubscriptionType(
          UpdateSubscriptionTypePayload(id = payload.id, name = payload.name, updatedBy = payload.createdBy)
        )
    }

  override def updateSubscriptionType(payload: UpdateSubscriptionTypePayload): Future[Done] =
    call(
      client.updateSubscriptionType(
        g.UpdateSubscriptionTypePayload(
          id = payload.id,
          name = payload.name,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def deleteSubscriptionType(payload: DeleteSubscriptionTypePayload): Future[Done] =
    call(
      client.deleteSubscriptionType(
        g.DeleteSubscriptionTypePayload(id = payload.id, updatedBy = Some(fromDomain(payload.updatedBy)))
      )
    ).map(_ => Done)

  override def getSubscriptionType(id: SubscriptionTypeId, source: Option[String]): Future[SubscriptionType] =
    call(client.getSubscriptionType(g.GetSubscriptionTypeRequest(id = id, source = source))).map(toDomain)

  override def getSubscriptionTypes(
    ids: Set[SubscriptionTypeId],
    source: Option[String]
  ): Future[Seq[SubscriptionType]] =
    call(client.getSubscriptionTypes(g.GetSubscriptionTypesRequest(ids = ids.toSeq, source = source)))
      .map(_.subscriptionTypes.map(toDomain))

  override def findSubscriptionTypes(query: SubscriptionTypeFindQuery): Future[FindResult] =
    call(
      client.findSubscriptionTypes(
        g.SubscriptionTypeFindQuery(
          offset = query.offset,
          size = query.size,
          filter = query.filter,
          name = query.name,
          sortBy = sortByToProto(query.sortBy)
        )
      )
    ).map(findResultFromProto)
}
