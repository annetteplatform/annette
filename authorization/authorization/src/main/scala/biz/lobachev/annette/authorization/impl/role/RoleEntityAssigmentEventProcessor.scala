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
import biz.lobachev.annette.microservice_core.pekko.event_processing.Tagger
import biz.lobachev.annette.microservice_core.pekko.projection.ProjectionBase
import org.apache.pekko.Done
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.projection.eventsourced.EventEnvelope

import scala.concurrent.{ExecutionContext, Future}

// Filename typo "Assigment" preserved from the Lagom variant for git-blame continuity.
// Replaces the Lagom-variant RoleEntityAssigmentEventProcessor (extends ReadSideProcessor[Event]).
// Subscribes to RoleEntity tags; reacts to AssignmentCreated / AssignmentDeleted events emitted
// by RoleEntity (role changes propagate to AssignmentEntity via this projection).
private[impl] class RoleEntityAssigmentEventProcessor(
  assignmentEntityService: AssignmentEntityService
)(implicit
  val system: ActorSystem[_],
  override val ec: ExecutionContext
) extends ProjectionBase[RoleEntity.Event] {

  override val projectionName: String = "role-assignment"
  override val tags: Seq[String] = Tagger.fromEventName[RoleEntity.Event](10).allTags

  override def process(envelope: EventEnvelope[RoleEntity.Event]): Future[Done] =
    envelope.event match {
      case evt: RoleEntity.AssignmentCreated =>
        assignmentEntityService
          .assignPermission(
            AssignPermissionPayload(
              principal = evt.principal,
              permission = evt.permission,
              source = evt.source,
              updatedBy = evt.updatedBy,
              updatedAt = Some(evt.updatedAt)
            )
          )
          .map(_ => Done)
      case evt: RoleEntity.AssignmentDeleted =>
        assignmentEntityService
          .unassignPermission(
            UnassignPermissionPayload(
              principal = evt.principal,
              permission = evt.permission,
              source = evt.source,
              updatedBy = evt.updatedBy,
              updatedAt = Some(evt.updatedAt)
            )
          )
          .map(_ => Done)
      case _ => Future.successful(Done)
    }
}
