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

package biz.lobachev.annette.org_structure.impl.role

import java.util.concurrent.TimeUnit
import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import biz.lobachev.annette.core.model.elastic.FindResult
import biz.lobachev.annette.org_structure.api.role._
import biz.lobachev.annette.org_structure.impl.role.dao.{OrgRoleDbDao, OrgRoleIndexDao}
import com.typesafe.config.Config

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class OrgRoleEntityService(
  clusterSharding: ClusterSharding,
  dbDao: OrgRoleDbDao,
  indexDao: OrgRoleIndexDao,
  config: Config
)(implicit
  ec: ExecutionContext,
  val mat: Materializer
) {

  implicit val timeout =
    Try(config.getDuration("annette.timeout"))
      .map(d => Timeout(FiniteDuration(d.toNanos, TimeUnit.NANOSECONDS)))
      .getOrElse(Timeout(60.seconds))

  private def refFor(id: OrgRoleId): EntityRef[OrgRoleEntity.Command] =
    clusterSharding.entityRefFor(OrgRoleEntity.typeKey, id)

  private def convertSuccess(id: OrgRoleId, confirmation: OrgRoleEntity.Confirmation): Done =
    confirmation match {
      case OrgRoleEntity.Success      => Done
      case OrgRoleEntity.NotFound     => throw OrgRoleNotFound(id)
      case OrgRoleEntity.AlreadyExist => throw OrgRoleAlreadyExist(id)
      case _                          => throw new RuntimeException("Match fail")
    }

  def createOrgRole(payload: CreateOrgRolePayload): Future[Done] =
    refFor(payload.id)
      .ask[OrgRoleEntity.Confirmation](OrgRoleEntity.CreateOrgRole(payload, _))
      .map(res => convertSuccess(payload.id, res))

  def updateOrgRole(payload: UpdateOrgRolePayload): Future[Done] =
    refFor(payload.id)
      .ask[OrgRoleEntity.Confirmation](OrgRoleEntity.UpdateOrgRole(payload, _))
      .map(res => convertSuccess(payload.id, res))

  def deleteOrgRole(payload: DeleteOrgRolePayload): Future[Done] =
    refFor(payload.id)
      .ask[OrgRoleEntity.Confirmation](OrgRoleEntity.DeleteOrgRole(payload, _))
      .map(res => convertSuccess(payload.id, res))

  def getOrgRoleById(id: OrgRoleId, fromReadSide: Boolean): Future[OrgRole] =
    if (fromReadSide) getOrgRoleByIdFromReadSide(id)
    else getOrgRoleById(id)

  def getOrgRoleById(id: OrgRoleId): Future[OrgRole] =
    refFor(id)
      .ask[OrgRoleEntity.Confirmation](OrgRoleEntity.GetOrgRole(id, _))
      .map {
        case OrgRoleEntity.SuccessOrgRole(entity) => entity
        case _                                    => throw OrgRoleNotFound(id)
      }

  def getOrgRoleByIdFromReadSide(id: OrgRoleId): Future[OrgRole] =
    for {
      maybeOrgRole <- dbDao.getOrgRoleById(id)
    } yield maybeOrgRole match {
      case Some(orgRole) => orgRole
      case None          => throw OrgRoleNotFound(id)
    }

  def getOrgRolesById(ids: Set[OrgRoleId], fromReadSide: Boolean): Future[Map[OrgRoleId, OrgRole]] =
    if (fromReadSide) dbDao.getOrgRolesById(ids)
    else
      Source(ids)
        .mapAsync(1) { id =>
          refFor(id)
            .ask[OrgRoleEntity.Confirmation](OrgRoleEntity.GetOrgRole(id, _))
            .map {
              case OrgRoleEntity.SuccessOrgRole(entity) => Some(entity)
              case _                                    => None
            }
        }
        .runWith(Sink.seq)
        .map(seq => seq.flatten.map(role => role.id -> role).toMap)

  def findOrgRoles(query: OrgRoleFindQuery): Future[FindResult]                                    =
    indexDao.findOrgRole(query)

}
