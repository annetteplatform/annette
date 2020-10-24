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

package biz.lobachev.annette.attributes.impl.index

import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl.{EntityContext, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import biz.lobachev.annette.attributes.api.assignment.{AttributeValue, ObjectId}
import biz.lobachev.annette.attributes.api.attribute_def.AttributeValueType
import biz.lobachev.annette.attributes.api.schema.{AttributeIndex, SchemaAttributeId}
import com.lightbend.lagom.scaladsl.persistence._
import io.scalaland.chimney.dsl._
import org.slf4j.LoggerFactory
import play.api.libs.json._

object IndexEntity {

  trait CommandSerializable
  sealed trait Command extends CommandSerializable
  final case class CreateIndexAttribute(
    id: SchemaAttributeId,
    attributeType: AttributeValueType.AttributeValueType,
    index: AttributeIndex,
    fieldName: String,
    replyTo: ActorRef[Confirmation]
  )                    extends Command
  final case class RemoveIndexAttribute(id: SchemaAttributeId, fieldName: String, replyTo: ActorRef[Confirmation])
      extends Command
  final case class AssignIndexAttribute(
    id: SchemaAttributeId,
    objectId: ObjectId,
    attribute: AttributeValue,
    fieldName: String,
    replyTo: ActorRef[Confirmation]
  )                    extends Command
  final case class UnassignIndexAttribute(
    id: SchemaAttributeId,
    objectId: ObjectId,
    fieldName: String,
    replyTo: ActorRef[Confirmation]
  )                    extends Command

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

  final case class IndexAttributeCreated(
    id: SchemaAttributeId,
    attributeType: AttributeValueType.AttributeValueType,
    index: AttributeIndex,
    fieldName: String
  )                                                                                                       extends Event
  final case class IndexAttributeRemoved(id: SchemaAttributeId, fieldName: String)                        extends Event
  final case class IndexAttributeAssigned(
    id: SchemaAttributeId,
    objectId: ObjectId,
    attribute: AttributeValue,
    fieldName: String
  )                                                                                                       extends Event
  final case class IndexAttributeUnassigned(id: SchemaAttributeId, objectId: ObjectId, fieldName: String) extends Event

  implicit val eventIndexAttributeCreatedFormat: Format[IndexAttributeCreated]       = Json.format
  implicit val eventIndexAttributeRemovedFormat: Format[IndexAttributeRemoved]       = Json.format
  implicit val eventIndexAttributeAssignedFormat: Format[IndexAttributeAssigned]     = Json.format
  implicit val eventIndexAttributeUnassignedFormat: Format[IndexAttributeUnassigned] = Json.format

  val empty = IndexEntity()

  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Attributes_Index")

  def apply(persistenceId: PersistenceId): EventSourcedBehavior[Command, Event, IndexEntity] =
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, IndexEntity](
        persistenceId = persistenceId,
        emptyState = IndexEntity.empty,
        commandHandler = (entity, cmd) => entity.applyCommand(cmd),
        eventHandler = (entity, _) => entity.applyEvent()
      )

  def apply(entityContext: EntityContext[Command]): Behavior[Command] =
    apply(PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId))
      .withTagger(AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 10, keepNSnapshots = 20))

  implicit val entityFormat: Format[IndexEntity] = Json.format

}

final case class IndexEntity(maybeState: Option[Int] = None) {
  import IndexEntity._

  val log = LoggerFactory.getLogger(this.getClass)

  def applyCommand(cmd: Command): ReplyEffect[Event, IndexEntity] =
    cmd match {
      case cmd: CreateIndexAttribute   => createIndexAttribute(cmd)
      case cmd: RemoveIndexAttribute   => removeIndexAttribute(cmd)
      case cmd: AssignIndexAttribute   => assignIndexAttribute(cmd)
      case cmd: UnassignIndexAttribute => unassignIndexAttribute(cmd)
    }

  def createIndexAttribute(cmd: CreateIndexAttribute): ReplyEffect[Event, IndexEntity] = {
    val event = cmd.transformInto[IndexAttributeCreated]
    Effect.persist(event).thenReply(cmd.replyTo)(_ => Success)
  }

  def removeIndexAttribute(cmd: RemoveIndexAttribute): ReplyEffect[Event, IndexEntity] = {
    val event = cmd.transformInto[IndexAttributeRemoved]
    Effect.persist(event).thenReply(cmd.replyTo)(_ => Success)
  }

  def assignIndexAttribute(cmd: AssignIndexAttribute): ReplyEffect[Event, IndexEntity] = {
    val event = cmd.transformInto[IndexAttributeAssigned]
    Effect.persist(event).thenReply(cmd.replyTo)(_ => Success)
  }

  def unassignIndexAttribute(cmd: UnassignIndexAttribute): ReplyEffect[Event, IndexEntity] = {
    val event = cmd.transformInto[IndexAttributeUnassigned]
    Effect.persist(event).thenReply(cmd.replyTo)(_ => Success)
  }

  def applyEvent(): IndexEntity = this

}
