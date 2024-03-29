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

package biz.lobachev.annette.authorization.impl.role

import java.util.concurrent.TimeUnit
import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import biz.lobachev.annette.authorization.api.role._
import biz.lobachev.annette.authorization.impl.role.dao.{RoleDbDao, RoleIndexDao}
import biz.lobachev.annette.core.model.DataSource
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.indexing.FindResult
import com.typesafe.config.Config

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class RoleEntityService(
  clusterSharding: ClusterSharding,
  dbDao: RoleDbDao,
  indexDao: RoleIndexDao,
  config: Config
)(implicit
  ec: ExecutionContext,
  val mat: Materializer
) {

  implicit val timeout =
    Try(config.getDuration("annette.timeout"))
      .map(d => Timeout(FiniteDuration(d.toNanos, TimeUnit.NANOSECONDS)))
      .getOrElse(Timeout(60.seconds))

  private def refFor(id: AuthRoleId): EntityRef[RoleEntity.Command] =
    clusterSharding.entityRefFor(RoleEntity.typeKey, id)

  private def convertSuccessRole(confirmation: RoleEntity.Confirmation): AuthRole =
    confirmation match {
      case RoleEntity.SuccessRole(role) => role
      case RoleEntity.RoleAlreadyExist  => throw RoleAlreadyExist()
      case RoleEntity.RoleNotFound      => throw RoleNotFound()
      case _                            => throw new RuntimeException("Match fail")
    }

  private def convertSuccessPrincipals(confirmation: RoleEntity.Confirmation): Set[AnnettePrincipal] =
    confirmation match {
      case RoleEntity.SuccessPrincipals(principals) => principals
      case RoleEntity.RoleAlreadyExist              => throw RoleAlreadyExist()
      case RoleEntity.RoleNotFound                  => throw RoleNotFound()
      case _                                        => throw new RuntimeException("Match fail")
    }

  private def convertSuccess(confirmation: RoleEntity.Confirmation): Done =
    confirmation match {
      case RoleEntity.Success          => Done
      case RoleEntity.RoleAlreadyExist => throw RoleAlreadyExist()
      case RoleEntity.RoleNotFound     => throw RoleNotFound()
      case _                           => throw new RuntimeException("Match fail")
    }

  def createRole(payload: CreateRolePayload): Future[Done] =
    refFor(payload.id)
      .ask[RoleEntity.Confirmation](RoleEntity.CreateRole(payload, _))
      .map(convertSuccess)

  def updateRole(payload: UpdateRolePayload): Future[Done] =
    refFor(payload.id)
      .ask[RoleEntity.Confirmation](RoleEntity.UpdateRole(payload, _))
      .map(convertSuccess)

  def deleteRole(payload: DeleteRolePayload): Future[Done] =
    refFor(payload.id)
      .ask[RoleEntity.Confirmation](RoleEntity.DeleteRole(payload, _))
      .map(convertSuccess)

  def assignPrincipal(payload: AssignPrincipalPayload): Future[Done] =
    refFor(payload.roleId)
      .ask[RoleEntity.Confirmation](RoleEntity.AssignPrincipal(payload, _))
      .map(convertSuccess)

  def unassignPrincipal(payload: UnassignPrincipalPayload): Future[Done] =
    refFor(payload.roleId)
      .ask[RoleEntity.Confirmation](RoleEntity.UnassignPrincipal(payload, _))
      .map(convertSuccess)

  def getRole(id: AuthRoleId, source: Option[String]): Future[AuthRole] =
    if (DataSource.fromOrigin(source)) {
      refFor(id)
        .ask[RoleEntity.Confirmation](RoleEntity.GetRole(id, _))
        .map(convertSuccessRole)
    } else {
      dbDao
        .getRole(id)
        .map(_.getOrElse(throw RoleNotFound()))
    }

  def getRolePrincipals(id: AuthRoleId, source: Option[String]): Future[Set[AnnettePrincipal]] =
    if (DataSource.fromOrigin(source)) {
      refFor(id)
        .ask[RoleEntity.Confirmation](RoleEntity.GetRolePrincipals(id, _))
        .map(convertSuccessPrincipals)
    } else {
      dbDao
        .getRolePrincipals(id)
        .map(_.getOrElse(throw RoleNotFound()))
    }

  def getRolePrincipals(id: AuthRoleId): Future[Set[AnnettePrincipal]] =
    refFor(id)
      .ask[RoleEntity.Confirmation](RoleEntity.GetRolePrincipals(id, _))
      .map(convertSuccessPrincipals)

  def getRoles(ids: Set[AuthRoleId], source: Option[String]): Future[Seq[AuthRole]] =
    if (DataSource.fromOrigin(source)) {
      for {
        roles <- Source(ids)
          .mapAsync(1) { id =>
            refFor(id)
              .ask[RoleEntity.Confirmation](RoleEntity.GetRole(id, _))
              .map {
                case RoleEntity.SuccessRole(role) => Some(role)
                case _ => None
              }
          }
          .runWith(Sink.seq)
      } yield roles.flatten
    } else {
      dbDao
        .getRoles(ids)
    }

  def findRoles(payload: AuthRoleFindQuery): Future[FindResult] =
    indexDao.findRoles(payload)

}
