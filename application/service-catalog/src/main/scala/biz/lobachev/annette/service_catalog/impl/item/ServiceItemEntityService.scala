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

package biz.lobachev.annette.service_catalog.impl.item

import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import biz.lobachev.annette.core.model.DataSource
import biz.lobachev.annette.core.model.indexing.FindResult
import biz.lobachev.annette.service_catalog.api.item._
import biz.lobachev.annette.service_catalog.impl.item.ServiceItemEntity._
import biz.lobachev.annette.service_catalog.impl.item.dao.{ServiceItemDbDao, ServiceItemIndexDao}
import com.typesafe.config.Config

import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class ServiceItemEntityService(
  clusterSharding: ClusterSharding,
  dbDao: ServiceItemDbDao,
  indexDao: ServiceItemIndexDao,
  config: Config
)(implicit
  ec: ExecutionContext,
  val materializer: Materializer
) {

  implicit val timeout =
    Try(config.getDuration("annette.timeout"))
      .map(d => Timeout(FiniteDuration(d.toNanos, TimeUnit.NANOSECONDS)))
      .getOrElse(Timeout(60.seconds))

  private def refFor(id: ServiceItemId): EntityRef[ServiceItemEntity.Command] =
    clusterSharding.entityRefFor(ServiceItemEntity.typeKey, id)

  private def convertSuccess(id: ServiceItemId, confirmation: Confirmation): Done =
    confirmation match {
      case Success      => Done
      case NotFound     => throw ServiceItemNotFound(id)
      case AlreadyExist => throw ServiceItemAlreadyExist(id)
      case IsNotService => throw ServiceItemIsNotService(id)
      case IsNotGroup   => throw ServiceItemIsNotGroup(id)
      case _            => throw new RuntimeException("Match fail")
    }

  private def convertSuccessService(id: ServiceItemId, confirmation: Confirmation): ServiceItem =
    confirmation match {
      case SuccessServiceItem(entity) => entity
      case NotFound                   => throw ServiceItemNotFound(id)
      case AlreadyExist               => throw ServiceItemAlreadyExist(id)
      case _                          => throw new RuntimeException("Match fail")
    }

  def createGroup(payload: CreateGroupPayload): Future[Done] =
    for {
      result <- refFor(payload.id)
                  .ask[Confirmation](CreateGroup(payload, _))
                  .map(res => convertSuccess(payload.id, res))
    } yield result

  def updateGroup(payload: UpdateGroupPayload): Future[Done] =
    for {
      result <- refFor(payload.id)
                  .ask[Confirmation](UpdateGroup(payload, _))
                  .map(res => convertSuccess(payload.id, res))
    } yield result

  def createService(payload: CreateServicePayload): Future[Done] =
    for {
      result <- refFor(payload.id)
                  .ask[Confirmation](CreateService(payload, _))
                  .map(res => convertSuccess(payload.id, res))
    } yield result

  def updateService(payload: UpdateServicePayload): Future[Done] =
    for {
      result <- refFor(payload.id)
                  .ask[Confirmation](UpdateService(payload, _))
                  .map(res => convertSuccess(payload.id, res))
    } yield result

  def activateServiceItem(payload: ActivateServiceItemPayload): Future[Done] =
    for {
      result <- refFor(payload.id)
                  .ask[Confirmation](ActivateServiceItem(payload, _))
                  .map(res => convertSuccess(payload.id, res))
    } yield result

  def deactivateServiceItem(payload: DeactivateServiceItemPayload): Future[Done] =
    for {
      result <- refFor(payload.id)
                  .ask[Confirmation](DeactivateServiceItem(payload, _))
                  .map(res => convertSuccess(payload.id, res))
    } yield result

  def deleteServiceItem(payload: DeleteServiceItemPayload): Future[Done] =
    refFor(payload.id)
      .ask[Confirmation](DeleteServiceItem(payload, _))
      .map(res => convertSuccess(payload.id, res))

  def getServiceItem(id: ServiceItemId, source: Option[String]): Future[ServiceItem] =
    if (DataSource.fromOrigin(source)) {
      refFor(id)
        .ask[Confirmation](GetServiceItem(id, _))
        .map(res => convertSuccessService(id, res))
    } else {
      dbDao
        .getServiceItem(id)
        .map(_.getOrElse(throw ServiceItemNotFound(id)))
    }

  def getServiceItems(
    ids: Set[ServiceItemId],
    source: Option[String]
  ): Future[Seq[ServiceItem]] =
    if (DataSource.fromOrigin(source)) {
      Source(ids)
        .mapAsync(1) { id =>
          refFor(id)
            .ask[Confirmation](GetServiceItem(id, _))
            .map {
              case ServiceItemEntity.SuccessServiceItem(service) => Some(service)
              case _ => None
            }
        }
        .runWith(Sink.seq)
        .map(_.flatten)
    } else {
      dbDao.getServiceItems(ids)
    }

  def findServiceItems(query: FindServiceItemsQuery): Future[FindResult] =
    indexDao.findService(query)

}
