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

import java.time.OffsetDateTime

import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl.{EntityContext, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import biz.lobachev.annette.authorization.api.assignment._
import biz.lobachev.annette.authorization.impl.assignment.model.AssignmentState
import biz.lobachev.annette.core.model.{AnnettePrincipal, Permission}
import com.lightbend.lagom.scaladsl.persistence._
import io.scalaland.chimney.dsl._
import play.api.libs.json._

object AssignmentEntity {

  trait CommandSerializable
  sealed trait Command                                                                                 extends CommandSerializable
  final case class AssignPermission(payload: AssignPermissionPayload, replyTo: ActorRef[Confirmation]) extends Command
  final case class UnassignPermission(payload: UnassignPermissionPayload, replyTo: ActorRef[Confirmation])
      extends Command

  sealed trait Confirmation
  final case object Success extends Confirmation

  implicit val confirmationSuccessFormat: Format[Success.type] = Json.format
  implicit val confirmationFormat: Format[Confirmation]        = Json.format[Confirmation]

  sealed trait Event extends AggregateEvent[Event] {
    override def aggregateTag: AggregateEventTagger[Event] = Event.Tag
  }

  object Event {
    val Tag: AggregateEventShards[Event] = AggregateEventTag.sharded[Event](numShards = 10)
  }

  final case class PermissionAssigned(
    principal: AnnettePrincipal,
    permission: Permission,
    source: AuthSource,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class PermissionUnassigned(
    principal: AnnettePrincipal,
    permission: Permission,
    source: AuthSource,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  implicit val eventPermissionAssignedFormat: Format[PermissionAssigned]     = Json.format
  implicit val eventPermissionUnassignedFormat: Format[PermissionUnassigned] = Json.format

  val empty = AssignmentEntity()

  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Authorization_Assignment")

  def apply(persistenceId: PersistenceId): EventSourcedBehavior[Command, Event, AssignmentEntity] =
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, AssignmentEntity](
        persistenceId = persistenceId,
        emptyState = AssignmentEntity.empty,
        commandHandler = (entity, cmd) => entity.applyCommand(cmd),
        eventHandler = (entity, evt) => entity.applyEvent(evt)
      )

  def apply(entityContext: EntityContext[Command]): Behavior[Command] =
    apply(PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId))
      .withTagger(AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2))

  implicit val entityFormat: Format[AssignmentEntity] = Json.format

  def assignmentId(
    principal: AnnettePrincipal,
    permission: Permission,
    source: AuthSource
  ): String =
    // TODO: review assignment id calculation (replace whitespace with another symbol)
    s"${principal.code} ${permission.code} ${source.code}"

}

final case class AssignmentEntity(maybeState: Option[AssignmentState] = None) {
  import AssignmentEntity._

  def applyCommand(cmd: Command): ReplyEffect[Event, AssignmentEntity] =
    cmd match {
      case cmd: AssignPermission   => assignPermission(cmd)
      case cmd: UnassignPermission => unassignPermission(cmd)
    }

  def assignPermission(cmd: AssignPermission): ReplyEffect[Event, AssignmentEntity] = {
    val event = cmd.payload
      .into[PermissionAssigned]
      .withFieldConst(_.updatedAt, cmd.payload.updatedAt.getOrElse(OffsetDateTime.now))
      .transform
    Effect
      .persist(event)
      .thenReply(cmd.replyTo)(_ => Success)
  }

  def unassignPermission(cmd: UnassignPermission): ReplyEffect[Event, AssignmentEntity] = {
    val event = cmd.payload
      .into[PermissionUnassigned]
      .withFieldConst(_.updatedAt, cmd.payload.updatedAt.getOrElse(OffsetDateTime.now))
      .transform
    Effect
      .persist(event)
      .thenReply(cmd.replyTo)(_ => Success)
  }

  def applyEvent(event: Event): AssignmentEntity =
    event match {
      case event: PermissionAssigned => onPermissionAssigned(event)
      case _: PermissionUnassigned   => onPermissionUnassigned()
    }

  def onPermissionAssigned(event: PermissionAssigned): AssignmentEntity =
    AssignmentEntity(
      Some(
        event.transformInto[AssignmentState]
      )
    )

  def onPermissionUnassigned(): AssignmentEntity =
    AssignmentEntity(None)

}
