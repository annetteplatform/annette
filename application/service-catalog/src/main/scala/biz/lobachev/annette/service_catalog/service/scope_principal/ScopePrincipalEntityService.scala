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

package biz.lobachev.annette.service_catalog.service.scope_principal

import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.stream.Materializer
import akka.util.Timeout
import biz.lobachev.annette.core.model.indexing.FindResult
import biz.lobachev.annette.service_catalog.api.scope_principal.{
  AssignScopePrincipalPayload,
  FindScopePrincipalQuery,
  UnassignScopePrincipalPayload
}
import biz.lobachev.annette.service_catalog.service.scope_principal.dao.ScopePrincipalIndexDao
import com.typesafe.config.Config

import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class ScopePrincipalEntityService(
  clusterSharding: ClusterSharding,
//  dbDao: ScopePrincipalDbDao,
  indexDao: ScopePrincipalIndexDao,
  config: Config
)(implicit
  ec: ExecutionContext,
  val mat: Materializer
) {

  implicit val timeout =
    Try(config.getDuration("annette.timeout"))
      .map(d => Timeout(FiniteDuration(d.toNanos, TimeUnit.NANOSECONDS)))
      .getOrElse(Timeout(60.seconds))

  private def refFor(id: String): EntityRef[ScopePrincipalEntity.Command] =
    clusterSharding.entityRefFor(ScopePrincipalEntity.typeKey, id)

  private def convertSuccess(confirmation: ScopePrincipalEntity.Confirmation): Done =
    confirmation match {
      case ScopePrincipalEntity.Success => Done
      case _                            => throw new RuntimeException("Match fail")
    }

  def assignScopePrincipal(payload: AssignScopePrincipalPayload): Future[Done] = {
    val id = ScopePrincipalEntity.scopePrincipalId(payload.scopeId, payload.principal)
    refFor(id)
      .ask[ScopePrincipalEntity.Confirmation](ScopePrincipalEntity.AssignPrincipal(payload, _))
      .map(convertSuccess)
  }

  def unassignScopePrincipal(payload: UnassignScopePrincipalPayload): Future[Done] = {
    val id = ScopePrincipalEntity.scopePrincipalId(payload.scopeId, payload.principal)
    refFor(id)
      .ask[ScopePrincipalEntity.Confirmation](ScopePrincipalEntity.UnassignPrincipal(payload, _))
      .map(convertSuccess)
  }

  def findScopePrincipals(query: FindScopePrincipalQuery): Future[FindResult] =
    indexDao.findScopePrincipals(query)

}
