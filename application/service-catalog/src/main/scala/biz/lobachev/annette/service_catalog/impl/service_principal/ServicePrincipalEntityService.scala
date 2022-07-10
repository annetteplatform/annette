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

package biz.lobachev.annette.service_catalog.impl.service_principal

import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.stream.Materializer
import akka.util.Timeout
import biz.lobachev.annette.core.model.indexing.FindResult
import biz.lobachev.annette.service_catalog.api.service_principal.{
  AssignServicePrincipalPayload,
  ServicePrincipalFindQuery,
  UnassignServicePrincipalPayload
}
import biz.lobachev.annette.service_catalog.impl.service_principal.dao.ServicePrincipalIndexDao
import com.typesafe.config.Config

import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class ServicePrincipalEntityService(
  clusterSharding: ClusterSharding,
//  dbDao: ServicePrincipalDbDao,
  indexDao: ServicePrincipalIndexDao,
  config: Config
)(implicit
  ec: ExecutionContext,
  val mat: Materializer
) {

  implicit val timeout =
    Try(config.getDuration("annette.timeout"))
      .map(d => Timeout(FiniteDuration(d.toNanos, TimeUnit.NANOSECONDS)))
      .getOrElse(Timeout(60.seconds))

  private def refFor(id: String): EntityRef[ServicePrincipalEntity.Command] =
    clusterSharding.entityRefFor(ServicePrincipalEntity.typeKey, id)

  private def convertSuccess(confirmation: ServicePrincipalEntity.Confirmation): Done =
    confirmation match {
      case ServicePrincipalEntity.Success => Done
      case _                              => throw new RuntimeException("Match fail")
    }

  def assignServicePrincipal(payload: AssignServicePrincipalPayload): Future[Done] = {
    val id = ServicePrincipalEntity.servicePrincipalId(payload.serviceId, payload.principal)
    refFor(id)
      .ask[ServicePrincipalEntity.Confirmation](ServicePrincipalEntity.AssignPrincipal(payload, _))
      .map(convertSuccess)
  }

  def unassignServicePrincipal(payload: UnassignServicePrincipalPayload): Future[Done] = {
    val id = ServicePrincipalEntity.servicePrincipalId(payload.serviceId, payload.principal)
    refFor(id)
      .ask[ServicePrincipalEntity.Confirmation](ServicePrincipalEntity.UnassignPrincipal(payload, _))
      .map(convertSuccess)
  }

  def findServicePrincipals(query: ServicePrincipalFindQuery): Future[FindResult] =
    indexDao.findServicePrincipals(query)

}
