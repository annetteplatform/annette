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

package biz.lobachev.annette.attributes.impl.attribute_def

import java.time.OffsetDateTime

import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl.{EntityContext, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import biz.lobachev.annette.attributes.api.attribute_def._
import biz.lobachev.annette.core.model.AnnettePrincipal
import com.lightbend.lagom.scaladsl.persistence._
import io.scalaland.chimney.dsl._
import org.slf4j.LoggerFactory
import play.api.libs.json._

object AttributeDefEntity {

  trait CommandSerializable
  sealed trait Command                                                                  extends CommandSerializable
  final case class CreateAttributeDef(payload: CreateAttributeDefPayload, replyTo: ActorRef[Confirmation])
      extends Command
  final case class UpdateAttributeDef(payload: UpdateAttributeDefPayload, replyTo: ActorRef[Confirmation])
      extends Command
  final case class DeleteAttributeDef(payload: DeleteAttributeDefPayload, replyTo: ActorRef[Confirmation])
      extends Command
  final case class GetAttributeDef(id: AttributeDefId, replyTo: ActorRef[Confirmation]) extends Command

  sealed trait Confirmation
  final case object Success                                        extends Confirmation
  final case class SuccessAttributeDef(attributeDef: AttributeDef) extends Confirmation
  final case object AttributeDefAlreadyExist                       extends Confirmation
  final case object AttributeDefNotFound                           extends Confirmation
  final case object AttributeDefHasUsages                          extends Confirmation
  final case class NotApplicable(field: String)                    extends Confirmation

  implicit val confirmationSuccessFormat: Format[Success.type]                                   = Json.format
  implicit val confirmationSuccessAttributeDefFormat: Format[SuccessAttributeDef]                = Json.format
  implicit val confirmationAttributeDefAlreadyExistFormat: Format[AttributeDefAlreadyExist.type] = Json.format
  implicit val confirmationAttributeDefNotFoundFormat: Format[AttributeDefNotFound.type]         = Json.format
  implicit val confirmationAttributeDefHasUsagesFormat: Format[AttributeDefHasUsages.type]       = Json.format
  implicit val confirmationNotApplicableFormat: Format[NotApplicable]                            = Json.format
  implicit val confirmationFormat: Format[Confirmation]                                          = Json.format

  sealed trait Event extends AggregateEvent[Event] {
    override def aggregateTag: AggregateEventTagger[Event] = Event.Tag
  }

  object Event {
    val Tag: AggregateEventShards[Event] = AggregateEventTag.sharded[Event](numShards = 10)
  }

  final case class AttributeDefCreated(
    id: AttributeDefId,
    name: String,
    caption: String,
    attributeType: AttributeValueType.AttributeValueType,
    attributeId: AttributeId,
    subType: Option[String] = None,
    allowedValues: Map[String, String] = Map.empty,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class AttributeDefUpdated(
    id: AttributeDefId,
    name: String,
    caption: String,
    attributeId: AttributeId,
    subType: Option[String] = None,
    allowedValues: Map[String, String] = Map.empty,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  final case class AttributeDefDeleted(
    id: AttributeDefId,
    deletedBy: AnnettePrincipal,
    deletedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  implicit val eventAttributeDefCreatedFormat: Format[AttributeDefCreated] = Json.format
  implicit val eventAttributeDefUpdatedFormat: Format[AttributeDefUpdated] = Json.format
  implicit val eventAttributeDefDeletedFormat: Format[AttributeDefDeleted] = Json.format

  val empty = AttributeDefEntity()

  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Attributes_AttributeDef")

  def apply(persistenceId: PersistenceId): EventSourcedBehavior[Command, Event, AttributeDefEntity] =
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, AttributeDefEntity](
        persistenceId = persistenceId,
        emptyState = AttributeDefEntity.empty,
        commandHandler = (entity, cmd) => entity.applyCommand(cmd),
        eventHandler = (entity, evt) => entity.applyEvent(evt)
      )

  def apply(entityContext: EntityContext[Command]): Behavior[Command] =
    apply(PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId))
      .withTagger(AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2))

  implicit val entityFormat: Format[AttributeDefEntity] = Json.format

}

final case class AttributeDefEntity(maybeState: Option[AttributeDefState] = None) {
  import AttributeDefEntity._

  val log = LoggerFactory.getLogger(this.getClass)

  def applyCommand(cmd: Command): ReplyEffect[Event, AttributeDefEntity] =
    cmd match {
      case cmd: CreateAttributeDef => createAttributeDef(cmd)
      case cmd: UpdateAttributeDef => updateAttributeDef(cmd)
      case cmd: DeleteAttributeDef => deleteAttributeDef(cmd)
      case cmd: GetAttributeDef    => getAttributeDef(cmd)
    }

  def createAttributeDef(cmd: CreateAttributeDef): ReplyEffect[Event, AttributeDefEntity] = {
    val payload = cmd.payload
    maybeState match {
      case Some(_)                                                                                      => Effect.reply(cmd.replyTo)(AttributeDefAlreadyExist)
      case None if payload.attributeType != AttributeValueType.String && payload.subType.isDefined      =>
        Effect.reply(cmd.replyTo)(NotApplicable("subType"))
      case None if payload.attributeType != AttributeValueType.String && payload.allowedValues.nonEmpty =>
        Effect.reply(cmd.replyTo)(NotApplicable("allowedValues"))
      case _                                                                                            =>
        val event = cmd.payload.transformInto[AttributeDefCreated]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }
  }

  def updateAttributeDef(cmd: UpdateAttributeDef): ReplyEffect[Event, AttributeDefEntity] = {
    val payload = cmd.payload
    maybeState match {
      case None                                                                                              => Effect.reply(cmd.replyTo)(AttributeDefNotFound)
      case Some(state) if state.attributeType != AttributeValueType.String && payload.subType.isDefined      =>
        Effect.reply(cmd.replyTo)(NotApplicable("subType"))
      case Some(state) if state.attributeType != AttributeValueType.String && payload.allowedValues.nonEmpty =>
        Effect.reply(cmd.replyTo)(NotApplicable("allowedValues"))
      case Some(_)                                                                                           =>
        val event = payload.transformInto[AttributeDefUpdated]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }
  }

  def deleteAttributeDef(cmd: DeleteAttributeDef): ReplyEffect[Event, AttributeDefEntity] = {
    val payload = cmd.payload
    maybeState match {
      case None    => Effect.reply(cmd.replyTo)(AttributeDefNotFound)
      case Some(_) =>
        val event = payload.transformInto[AttributeDefDeleted]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }
  }

  def getAttributeDef(cmd: GetAttributeDef): ReplyEffect[Event, AttributeDefEntity] =
    maybeState match {
      case None        => Effect.reply(cmd.replyTo)(AttributeDefNotFound)
      case Some(state) =>
        val attributeDef = state.transformInto[AttributeDef]
        Effect.reply(cmd.replyTo)(SuccessAttributeDef(attributeDef))
    }

  def applyEvent(event: Event): AttributeDefEntity =
    event match {
      case event: AttributeDefCreated => onAttributeDefCreated(event)
      case event: AttributeDefUpdated => onAttributeDefUpdated(event)
      case _: AttributeDefDeleted     => onAttributeDefDeleted()
    }

  def onAttributeDefCreated(event: AttributeDefCreated): AttributeDefEntity = {
    val state = event.transformInto[AttributeDefState]
    AttributeDefEntity(
      Some(state)
    )
  }

  def onAttributeDefUpdated(event: AttributeDefUpdated): AttributeDefEntity =
    AttributeDefEntity(
      maybeState.map { state =>
        event
          .into[AttributeDefState]
          .withFieldConst(_.attributeType, state.attributeType)
          .transform
      }
    )

  def onAttributeDefDeleted(): AttributeDefEntity =
    AttributeDefEntity(None)

}
