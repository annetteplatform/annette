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

package biz.lobachev.annette.service_catalog.service.service

import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import biz.lobachev.annette.core.model.indexing.FindResult
import biz.lobachev.annette.service_catalog.api.item._
import biz.lobachev.annette.service_catalog.service.service.ServiceEntity._
import biz.lobachev.annette.service_catalog.service.service.dao.{ServiceDbDao, ServiceIndexDao}
import com.typesafe.config.Config

import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class ServiceEntityService(
  clusterSharding: ClusterSharding,
  dbDao: ServiceDbDao,
  indexDao: ServiceIndexDao,
  config: Config
)(implicit
  ec: ExecutionContext,
  val materializer: Materializer
) {

  implicit val timeout =
    Try(config.getDuration("annette.timeout"))
      .map(d => Timeout(FiniteDuration(d.toNanos, TimeUnit.NANOSECONDS)))
      .getOrElse(Timeout(60.seconds))

  private def refFor(id: ScopeItemId): EntityRef[ServiceEntity.Command] =
    clusterSharding.entityRefFor(ServiceEntity.typeKey, id)

  private def convertSuccess(id: ScopeItemId, confirmation: Confirmation): Done =
    confirmation match {
      case Success      => Done
      case NotFound     => throw ServiceNotFound(id)
      case AlreadyExist => throw ServiceAlreadyExist(id)
      case _            => throw new RuntimeException("Match fail")
    }

  private def convertSuccessService(id: ScopeItemId, confirmation: Confirmation): ServiceItem =
    confirmation match {
      case SuccessService(entity) => entity
      case NotFound               => throw ServiceNotFound(id)
      case AlreadyExist           => throw ServiceAlreadyExist(id)
      case _                      => throw new RuntimeException("Match fail")
    }

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

  def activateService(payload: ActivateScopeItemPayload): Future[Done] =
    for {
      result <- refFor(payload.id)
                  .ask[Confirmation](ActivateService(payload, _))
                  .map(res => convertSuccess(payload.id, res))
    } yield result

  def deactivateService(payload: DeactivateScopeItemPayload): Future[Done] =
    for {
      result <- refFor(payload.id)
                  .ask[Confirmation](DeactivateService(payload, _))
                  .map(res => convertSuccess(payload.id, res))
    } yield result

  def deleteService(payload: DeleteScopeItemPayload): Future[Done] =
    refFor(payload.id)
      .ask[Confirmation](DeleteService(payload, _))
      .map(res => convertSuccess(payload.id, res))

  def getServiceById(id: ScopeItemId, fromReadSide: Boolean): Future[ServiceItem] =
    if (fromReadSide)
      dbDao
        .getServiceById(id)
        .map(_.getOrElse(throw ServiceNotFound(id)))
    else
      refFor(id)
        .ask[Confirmation](GetService(id, _))
        .map(res => convertSuccessService(id, res))

  def getServicesById(
    ids: Set[ScopeItemId],
    fromReadSide: Boolean
  ): Future[Seq[ServiceItem]] =
    if (fromReadSide)
      dbDao.getServicesById(ids)
    else
      Source(ids)
        .mapAsync(1) { id =>
          refFor(id)
            .ask[Confirmation](GetService(id, _))
            .map {
              case ServiceEntity.SuccessService(service) => Some(service)
              case _                                     => None
            }
        }
        .runWith(Sink.seq)
        .map(_.flatten)

  def findServices(query: FindScopeItemsQuery): Future[FindResult] =
    indexDao.findService(query)

}
