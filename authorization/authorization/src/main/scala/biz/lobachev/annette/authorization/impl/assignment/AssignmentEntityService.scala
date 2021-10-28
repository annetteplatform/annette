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

package biz.lobachev.annette.authorization.impl.assignment

import java.util.concurrent.TimeUnit
import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.stream.Materializer
import akka.util.Timeout
import biz.lobachev.annette.authorization.api.assignment._
import biz.lobachev.annette.authorization.impl.assignment.dao.{AssignmentDbDao, AssignmentIndexDao}
import com.typesafe.config.Config

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class AssignmentEntityService(
  clusterSharding: ClusterSharding,
  dbDao: AssignmentDbDao,
  indexDao: AssignmentIndexDao,
  config: Config
)(implicit
  ec: ExecutionContext,
  val mat: Materializer
) {

  implicit val timeout =
    Try(config.getDuration("annette.timeout"))
      .map(d => Timeout(FiniteDuration(d.toNanos, TimeUnit.NANOSECONDS)))
      .getOrElse(Timeout(60.seconds))

  private def refFor(id: String): EntityRef[AssignmentEntity.Command] =
    clusterSharding.entityRefFor(AssignmentEntity.typeKey, id)

  private def convertSuccess(confirmation: AssignmentEntity.Confirmation): Done =
    confirmation match {
      case AssignmentEntity.Success => Done
      case _                        => throw new RuntimeException("Match fail")
    }

  def assignPermission(payload: AssignPermissionPayload): Future[Done] = {
    val id = AssignmentEntity.assignmentId(payload.principal, payload.permission, payload.source)
    refFor(id)
      .ask[AssignmentEntity.Confirmation](AssignmentEntity.AssignPermission(payload, _))
      .map(convertSuccess)
  }

  def unassignPermission(payload: UnassignPermissionPayload): Future[Done] = {
    val id = AssignmentEntity.assignmentId(payload.principal, payload.permission, payload.source)
    refFor(id)
      .ask[AssignmentEntity.Confirmation](AssignmentEntity.UnassignPermission(payload, _))
      .map(convertSuccess)
  }

  def findPermissions(payload: FindPermissions): Future[Set[PermissionAssignment]] =
    dbDao.findPermissions(payload)

  def checkAnyPermission(payload: CheckPermissions): Future[Boolean] =
    dbDao.checkAnyPermission(payload)

  def checkAllPermission(payload: CheckPermissions): Future[Boolean] =
    dbDao.checkAllPermission(payload)

  def findAssignments(payload: FindAssignmentsQuery): Future[AssignmentFindResult] =
    indexDao.findAssignments(payload)

}
