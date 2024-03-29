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

package biz.lobachev.annette.subscription.impl.subscription_type

import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import biz.lobachev.annette.core.model.DataSource
import biz.lobachev.annette.core.model.indexing.FindResult
import biz.lobachev.annette.subscription.api.subscription_type._
import biz.lobachev.annette.subscription.impl.subscription_type.dao.{SubscriptionTypeDbDao, SubscriptionTypeIndexDao}
import com.typesafe.config.Config

import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class SubscriptionTypeEntityService(
  clusterSharding: ClusterSharding,
  dbDao: SubscriptionTypeDbDao,
  indexDao: SubscriptionTypeIndexDao,
  config: Config
)(implicit
  ec: ExecutionContext,
  val materializer: Materializer
) {

  implicit val timeout =
    Try(config.getDuration("annette.timeout"))
      .map(d => Timeout(FiniteDuration(d.toNanos, TimeUnit.NANOSECONDS)))
      .getOrElse(Timeout(60.seconds))

  private def refFor(id: SubscriptionTypeId): EntityRef[SubscriptionTypeEntity.Command] =
    clusterSharding.entityRefFor(SubscriptionTypeEntity.typeKey, id)

  private def convertSuccess(id: SubscriptionTypeId, confirmation: SubscriptionTypeEntity.Confirmation): Done =
    confirmation match {
      case SubscriptionTypeEntity.Success      => Done
      case SubscriptionTypeEntity.NotFound     => throw SubscriptionTypeNotFound(id)
      case SubscriptionTypeEntity.AlreadyExist => throw SubscriptionTypeAlreadyExist(id)
      case _                                   => throw new RuntimeException("Match fail")
    }

  def createSubscriptionType(payload: CreateSubscriptionTypePayload): Future[Done] =
    refFor(payload.id)
      .ask[SubscriptionTypeEntity.Confirmation](SubscriptionTypeEntity.CreateSubscriptionType(payload, _))
      .map(res => convertSuccess(payload.id, res))

  def updateSubscriptionType(payload: UpdateSubscriptionTypePayload): Future[Done] =
    refFor(payload.id)
      .ask[SubscriptionTypeEntity.Confirmation](SubscriptionTypeEntity.UpdateSubscriptionType(payload, _))
      .map(res => convertSuccess(payload.id, res))

  def deleteSubscriptionType(payload: DeleteSubscriptionTypePayload): Future[Done] =
    refFor(payload.id)
      .ask[SubscriptionTypeEntity.Confirmation](SubscriptionTypeEntity.DeleteSubscriptionType(payload, _))
      .map(res => convertSuccess(payload.id, res))

  def getSubscriptionType(id: SubscriptionTypeId, source: Option[String]): Future[SubscriptionType] =
    if (DataSource.fromOrigin(source))
      getSubscriptionTypeFromOrigin(id)
    else
      getSubscriptionTypeFromReadSide(id)

  private def getSubscriptionTypeFromOrigin(id: SubscriptionTypeId): Future[SubscriptionType] =
    refFor(id)
      .ask[SubscriptionTypeEntity.Confirmation](SubscriptionTypeEntity.GetSubscriptionType(id, _))
      .map {
        case SubscriptionTypeEntity.SuccessSubscriptionType(entity) => entity
        case _                                                      => throw SubscriptionTypeNotFound(id)
      }

  private def getSubscriptionTypeFromReadSide(id: SubscriptionTypeId): Future[SubscriptionType] =
    for {
      maybeSubscriptionType <- dbDao.getSubscriptionType(id)
    } yield maybeSubscriptionType match {
      case Some(subscriptionType) => subscriptionType
      case None                   => throw SubscriptionTypeNotFound(id)
    }

  def getSubscriptionTypes(
    ids: Set[SubscriptionTypeId],
    source: Option[String]
  ): Future[Seq[SubscriptionType]] =
    if (DataSource.fromOrigin(source))
      Source(ids)
        .mapAsync(1) { id =>
          refFor(id)
            .ask[SubscriptionTypeEntity.Confirmation](SubscriptionTypeEntity.GetSubscriptionType(id, _))
            .map {
              case SubscriptionTypeEntity.SuccessSubscriptionType(entity) => Some(entity)
              case _                                                      => None
            }
        }
        .runWith(Sink.seq)
        .map(seq => seq.flatten)
    else
      dbDao.getSubscriptionTypes(ids)

  def findSubscriptionTypes(query: SubscriptionTypeFindQuery): Future[FindResult] =
    indexDao.findSubscriptionTypes(query)
}
