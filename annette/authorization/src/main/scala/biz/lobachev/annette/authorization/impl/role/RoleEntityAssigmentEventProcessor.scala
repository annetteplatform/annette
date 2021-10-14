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

import akka.Done
import biz.lobachev.annette.authorization.api.assignment.{AssignPermissionPayload, UnassignPermissionPayload}
import biz.lobachev.annette.authorization.impl.assignment.AssignmentEntityService
import biz.lobachev.annette.microservice_core.event_processing.SimpleEventHandling
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}

import scala.concurrent.ExecutionContext

private[impl] class RoleEntityAssigmentEventProcessor(
  readSide: CassandraReadSide,
  assignmentEntityService: AssignmentEntityService
)(implicit
  ec: ExecutionContext
) extends ReadSideProcessor[RoleEntity.Event]
    with SimpleEventHandling {

  def buildHandler(): ReadSideProcessor.ReadSideHandler[RoleEntity.Event] =
    readSide
      .builder[RoleEntity.Event]("role-assignment")
      .setEventHandler[RoleEntity.AssignmentCreated](handle(createAssignment))
      .setEventHandler[RoleEntity.AssignmentDeleted](handle(deleteAssignment))
      .build()

  def aggregateTags: Set[AggregateEventTag[RoleEntity.Event]] = RoleEntity.Event.Tag.allTags

  def createAssignment(event: RoleEntity.AssignmentCreated) =
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
    } yield Done

  def deleteAssignment(event: RoleEntity.AssignmentDeleted) =
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
    } yield Done

}
