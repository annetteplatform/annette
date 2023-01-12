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

package biz.lobachev.annette.service_catalog.impl.scope

import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import biz.lobachev.annette.core.model.DataSource
import biz.lobachev.annette.core.model.indexing.FindResult
import biz.lobachev.annette.service_catalog.api.scope._
import biz.lobachev.annette.service_catalog.impl.scope.ScopeEntity._
import biz.lobachev.annette.service_catalog.impl.scope.dao.{ScopeDbDao, ScopeIndexDao}
import com.typesafe.config.Config

import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class ScopeEntityService(
  clusterSharding: ClusterSharding,
  dbDao: ScopeDbDao,
  indexDao: ScopeIndexDao,
  config: Config
)(implicit
  ec: ExecutionContext,
  val materializer: Materializer
) {

  implicit val timeout =
    Try(config.getDuration("annette.timeout"))
      .map(d => Timeout(FiniteDuration(d.toNanos, TimeUnit.NANOSECONDS)))
      .getOrElse(Timeout(60.seconds))

  private def refFor(id: ScopeId): EntityRef[Command] =
    clusterSharding.entityRefFor(ScopeEntity.typeKey, id)

  private def convertSuccess(id: ScopeId, confirmation: Confirmation): Done =
    confirmation match {
      case Success      => Done
      case NotFound     => throw ScopeNotFound(id)
      case AlreadyExist => throw ScopeAlreadyExist(id)
      case _            => throw new RuntimeException("Match fail")
    }

  private def convertSuccessScope(id: ScopeId, confirmation: Confirmation): Scope =
    confirmation match {
      case SuccessScope(entity) => entity
      case NotFound             => throw ScopeNotFound(id)
      case AlreadyExist         => throw ScopeAlreadyExist(id)
      case _                    => throw new RuntimeException("Match fail")
    }

  def createScope(payload: CreateScopePayload): Future[Done] =
    for {
      result <- refFor(payload.id)
                  .ask[Confirmation](CreateScope(payload, _))
                  .map(res => convertSuccess(payload.id, res))
    } yield result

  def updateScope(payload: UpdateScopePayload): Future[Done] =
    for {
      result <- refFor(payload.id)
                  .ask[Confirmation](UpdateScope(payload, _))
                  .map(res => convertSuccess(payload.id, res))
    } yield result

  def activateScope(payload: ActivateScopePayload): Future[Done] =
    for {
      result <- refFor(payload.id)
                  .ask[Confirmation](ActivateScope(payload, _))
                  .map(res => convertSuccess(payload.id, res))
    } yield result

  def deactivateScope(payload: DeactivateScopePayload): Future[Done] =
    for {
      result <- refFor(payload.id)
                  .ask[Confirmation](DeactivateScope(payload, _))
                  .map(res => convertSuccess(payload.id, res))
    } yield result

  def deleteScope(payload: DeleteScopePayload): Future[Done] =
    refFor(payload.id)
      .ask[Confirmation](DeleteScope(payload, _))
      .map(res => convertSuccess(payload.id, res))

  def getScope(id: ScopeId, source: Option[String]): Future[Scope] =
    if (DataSource.fromOrigin(source)) {
      refFor(id)
        .ask[Confirmation](GetScope(id, _))
        .map(res => convertSuccessScope(id, res))
    } else {
      dbDao
        .getScope(id)
        .map(_.getOrElse(throw ScopeNotFound(id)))
    }

  def getScopes(
    ids: Set[ScopeId],
    source: Option[String]
  ): Future[Seq[Scope]] =
    if (DataSource.fromOrigin(source)) {
      Source(ids)
        .mapAsync(1) { id =>
          refFor(id)
            .ask[Confirmation](GetScope(id, _))
            .map {
              case ScopeEntity.SuccessScope(scope) => Some(scope)
              case _ => None
            }
        }
        .runWith(Sink.seq)
        .map(_.flatten)
    } else {
      dbDao.getScopes(ids)
    }

  def findScopes(query: FindScopeQuery): Future[FindResult] =
    indexDao.findScope(query)

}
