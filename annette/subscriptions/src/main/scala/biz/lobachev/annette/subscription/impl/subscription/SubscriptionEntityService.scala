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

package biz.lobachev.annette.subscription.impl.subscription

import java.util.concurrent.TimeUnit
import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.subscription.api.subscription._
import biz.lobachev.annette.subscription.api.subscription_type.SubscriptionTypeId
import biz.lobachev.annette.subscription.impl.subscription.SubscriptionEntity._
import biz.lobachev.annette.subscription.impl.subscription.dao.{SubscriptionDbDao, SubscriptionIndexDao}
import com.typesafe.config.Config
import io.scalaland.chimney.dsl._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class SubscriptionEntityService(
  clusterSharding: ClusterSharding,
  dbDao: SubscriptionDbDao,
  indexDao: SubscriptionIndexDao,
  config: Config
)(implicit
  ec: ExecutionContext,
  val materializer: Materializer
) {

  implicit val timeout =
    Try(config.getDuration("annette.timeout"))
      .map(d => Timeout(FiniteDuration(d.toNanos, TimeUnit.NANOSECONDS)))
      .getOrElse(Timeout(60.seconds))

  private def refFor(
    subscriptionType: SubscriptionTypeId,
    objectId: ObjectId,
    principal: AnnettePrincipal
  ): EntityRef[Command] =
    clusterSharding.entityRefFor(
      SubscriptionEntity.typeKey,
      s"${subscriptionType}~${objectId}~${principal.code}"
    )

  private def convertSuccess(
    subscriptionType: SubscriptionTypeId,
    objectId: ObjectId,
    principal: AnnettePrincipal,
    confirmation: Confirmation
  ): Done =
    confirmation match {
      case Success      => Done
      case NotFound     => throw SubscriptionNotFound(subscriptionType, objectId, principal.code)
      case AlreadyExist => throw SubscriptionAlreadyExist(subscriptionType, objectId, principal.code)
      case _            => throw new RuntimeException("Match fail")
    }

  private def convertSuccessSubscription(
    subscriptionType: SubscriptionTypeId,
    objectId: ObjectId,
    principal: AnnettePrincipal,
    confirmation: Confirmation
  ): Subscription =
    confirmation match {
      case SuccessSubscription(entity) => entity
      case NotFound                    => throw SubscriptionNotFound(subscriptionType, objectId, principal.code)
      case AlreadyExist                => throw SubscriptionAlreadyExist(subscriptionType, objectId, principal.code)
      case _                           => throw new RuntimeException("Match fail")
    }

  def createSubscription(payload: CreateSubscriptionPayload): Future[Done] =
    refFor(payload.subscriptionType, payload.objectId, payload.principal)
      .ask[Confirmation](replyTo =>
        payload
          .into[CreateSubscription]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(res => convertSuccess(payload.subscriptionType, payload.objectId, payload.principal, res))

  def deleteSubscription(payload: DeleteSubscriptionPayload): Future[Done] =
    refFor(payload.subscriptionType, payload.objectId, payload.principal)
      .ask[Confirmation](replyTo =>
        payload
          .into[DeleteSubscription]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(res => convertSuccess(payload.subscriptionType, payload.objectId, payload.principal, res))

  def getSubscription(key: SubscriptionKey): Future[Subscription] =
    refFor(key.subscriptionType, key.objectId, key.principal)
      .ask[Confirmation](GetSubscription(key, _))
      .map(res => convertSuccessSubscription(key.subscriptionType, key.objectId, key.principal, res))

  def getSubscriptionById(key: SubscriptionKey, fromReadSide: Boolean): Future[Subscription] =
    if (fromReadSide)
      dbDao
        .getSubscriptionById(key)
        .map(_.getOrElse(throw SubscriptionNotFound(key.subscriptionType, key.objectId, key.principal.code)))
    else
      getSubscription(key)

  def getSubscriptionsById(
    keys: Set[SubscriptionKey],
    fromReadSide: Boolean
  ): Future[Set[Subscription]] =
    Source(keys)
      .mapAsync(1) { key =>
        if (fromReadSide)
          dbDao.getSubscriptionById(key)
        else
          refFor(key.subscriptionType, key.objectId, key.principal)
            .ask[Confirmation](GetSubscription(key, _))
            .map {
              case SubscriptionEntity.SuccessSubscription(subscription) => Some(subscription)
              case _                                                    => None
            }
      }
      .runWith(Sink.seq)
      .map(_.flatten.toSet)

  def getSubscriptionsByPrincipals(
    subscriptionType: SubscriptionTypeId,
    principals: Set[AnnettePrincipal]
  ): Future[Set[SubscriptionKey]] =
    dbDao.getSubscriptionsByPrincipals(subscriptionType, principals)

  def getSubscriptionsByObjects(
    subscriptionType: SubscriptionTypeId,
    objectIds: Set[ObjectId]
  ): Future[Set[SubscriptionKey]] =
    dbDao.getSubscriptionsByObjects(subscriptionType, objectIds)

  def findSubscriptions(query: SubscriptionFindQuery): Future[SubscriptionFindResult] =
    indexDao.findSubscription(query)

}
