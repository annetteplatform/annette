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

package biz.lobachev.annette.attributes.impl.assignment

import java.time.OffsetDateTime
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl.{EntityContext, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import biz.lobachev.annette.attributes.api.assignment._
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import com.lightbend.lagom.scaladsl.persistence._
import io.scalaland.chimney.dsl._
import org.slf4j.LoggerFactory
import play.api.libs.json._

object AssignmentEntity {

  trait CommandSerializable
  sealed trait Command                                                                       extends CommandSerializable
  final case class AssignAttribute(
    payload: AssignAttributePayload,
    indexFieldName: Option[String],
    replyTo: ActorRef[Confirmation]
  )                                                                                          extends Command
  final case class UnassignAttribute(
    payload: UnassignAttributePayload,
    indexFieldName: Option[String],
    replyTo: ActorRef[Confirmation]
  )                                                                                          extends Command
  final case class GetAssignment(id: AttributeAssignmentId, replyTo: ActorRef[Confirmation]) extends Command
  final case class ReindexAssignment(id: AttributeAssignmentId, indexFieldName: String, replyTo: ActorRef[Confirmation])
      extends Command

  sealed trait Confirmation
  final case object Success                                                             extends Confirmation
  final case class SuccessAttributeAssignment(attributeAssignment: AttributeAssignment) extends Confirmation
  final case object AssignmentNotFound                                                  extends Confirmation
  final case object InvalidAttributeType                                                extends Confirmation

  implicit val confirmationSuccessFormat: Format[Success.type]                           = Json.format
  implicit val confirmationSuccessAttributeFormat: Format[SuccessAttributeAssignment]    = Json.format
  implicit val confirmationAttributeNotFoundFormat: Format[AssignmentNotFound.type]      = Json.format
  implicit val confirmationInvalidAttributeTypeFormat: Format[InvalidAttributeType.type] = Json.format

  implicit val confirmationFormat: Format[Confirmation] = Json.format[Confirmation]

  sealed trait Event extends AggregateEvent[Event] {
    override def aggregateTag: AggregateEventTagger[Event] = Event.Tag
  }

  object Event {
    val Tag: AggregateEventShards[Event] = AggregateEventTag.sharded[Event](numShards = 10)
  }

  final case class AttributeAssigned(
    id: AttributeAssignmentId,
    attribute: AttributeValue,
    indexFieldName: Option[String],
    updatedAt: OffsetDateTime = OffsetDateTime.now(),
    updatedBy: AnnettePrincipal
  ) extends Event
  final case class AttributeUnassigned(
    id: AttributeAssignmentId,
    indexFieldName: Option[String],
    updatedAt: OffsetDateTime = OffsetDateTime.now(),
    updatedBy: AnnettePrincipal
  ) extends Event
  final case class AssignmentReindexed(
    id: AttributeAssignmentId,
    attribute: AttributeValue,
    indexFieldName: String,
    updatedAt: OffsetDateTime = OffsetDateTime.now(),
    updatedBy: AnnettePrincipal
  ) extends Event

  implicit val eventAttributeAssignedFormat: Format[AttributeAssigned]     = Json.format
  implicit val eventAttributeUnassignedFormat: Format[AttributeUnassigned] = Json.format
  implicit val eventAttributeReindexedFormat: Format[AssignmentReindexed]  = Json.format

  val empty = AssignmentEntity()

  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Attributes_Assignment")

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

}

final case class AssignmentEntity(maybeState: Option[AttributeAssignmentState] = None) {
  import AssignmentEntity._

  val log = LoggerFactory.getLogger(this.getClass)

  def applyCommand(cmd: Command): ReplyEffect[Event, AssignmentEntity] =
    cmd match {
      case cmd: AssignAttribute   => assignAttribute(cmd)
      case cmd: UnassignAttribute => unassignAttribute(cmd)
      case cmd: GetAssignment     => getAssignment(cmd)
      case cmd: ReindexAssignment => reindexAssignment(cmd)
    }

  def assignAttribute(cmd: AssignAttribute): ReplyEffect[Event, AssignmentEntity] = {
    val event = cmd.payload
      .into[AttributeAssigned]
      .withFieldConst(_.indexFieldName, cmd.indexFieldName)
      .transform
    Effect
      .persist(event)
      .thenReply(cmd.replyTo)(_ => Success)
  }

  def unassignAttribute(cmd: UnassignAttribute): ReplyEffect[Event, AssignmentEntity] = {
    val event = cmd.payload
      .into[AttributeUnassigned]
      .withFieldConst(_.indexFieldName, cmd.indexFieldName)
      .transform
    Effect
      .persist(event)
      .thenReply(cmd.replyTo)(_ => Success)
  }

  def getAssignment(cmd: GetAssignment): ReplyEffect[Event, AssignmentEntity] =
    maybeState match {
      case Some(state) =>
        val attributeAssignment = state.transformInto[AttributeAssignment]
        Effect.reply(cmd.replyTo)(SuccessAttributeAssignment(attributeAssignment))
      case None        => Effect.reply(cmd.replyTo)(AssignmentNotFound)
    }

  def reindexAssignment(cmd: ReindexAssignment): ReplyEffect[Event, AssignmentEntity] =
    maybeState match {
      case Some(state) =>
        val event = state
          .into[AssignmentReindexed]
          .withFieldConst(_.indexFieldName, cmd.indexFieldName)
          .transform
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
      case None        => Effect.reply(cmd.replyTo)(AssignmentNotFound)
    }

  def applyEvent(event: Event): AssignmentEntity =
    event match {
      case event: AttributeAssigned => onAttributeAssigned(event)
      case _: AttributeUnassigned   => onAttributeUnassigned()
      case _: AssignmentReindexed   => this
    }

  def onAttributeAssigned(event: AttributeAssigned): AssignmentEntity = {
    val newState = event.transformInto[AttributeAssignmentState]
    AssignmentEntity(Some(newState))
  }

  def onAttributeUnassigned(): AssignmentEntity =
    AssignmentEntity(None)

}
