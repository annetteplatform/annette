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

import biz.lobachev.annette.authorization.api.assignment.{AssignPermissionPayload, UnassignPermissionPayload}
import biz.lobachev.annette.authorization.impl.assignment.AssignmentEntityService
import com.datastax.driver.core.BoundStatement
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}

import scala.concurrent.{ExecutionContext, Future}

private[impl] class RoleEntityAssigmentEventProcessor(
  readSide: CassandraReadSide,
  assignmentEntityService: AssignmentEntityService
)(implicit
  ec: ExecutionContext
) extends ReadSideProcessor[RoleEntity.Event] {

  def buildHandler(): ReadSideProcessor.ReadSideHandler[RoleEntity.Event] =
    readSide
      .builder[RoleEntity.Event]("role-assignment")
      .setEventHandler[RoleEntity.AssignmentCreated](e => createAssignment(e.event))
      .setEventHandler[RoleEntity.AssignmentDeleted](e => deleteAssignment(e.event))
      .build()

  def aggregateTags: Set[AggregateEventTag[RoleEntity.Event]] = RoleEntity.Event.Tag.allTags

  def createAssignment(event: RoleEntity.AssignmentCreated): Future[Seq[BoundStatement]] =
    for {
      _ <- assignmentEntityService.assignPermission(
             AssignPermissionPayload(
               principal = event.principal,
               permission = event.permission,
               source = event.source,
               updatedBy = event.updatedBy,
               updatedAt = Some(event.updatedAt)
             )
           )
    } yield List()

  def deleteAssignment(event: RoleEntity.AssignmentDeleted): Future[Seq[BoundStatement]] =
    for {
      _ <- assignmentEntityService.unassignPermission(
             UnassignPermissionPayload(
               principal = event.principal,
               permission = event.permission,
               source = event.source,
               updatedBy = event.updatedBy,
               updatedAt = Some(event.updatedAt)
             )
           )
    } yield List()

}
