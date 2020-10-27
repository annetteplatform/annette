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

package biz.lobachev.annette.attributes.impl.schema

import java.time.OffsetDateTime

import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl.{EntityContext, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import biz.lobachev.annette.attributes.api.attribute.{Attribute, AttributeId, AttributeType}
import biz.lobachev.annette.attributes.api.schema._
import biz.lobachev.annette.attributes.impl.schema.model.{AttributeIndexState, AttributeState, SchemaState}
import biz.lobachev.annette.core.model.{AnnettePrincipal, Caption}
import com.lightbend.lagom.scaladsl.persistence._
import io.scalaland.chimney.dsl._
import org.slf4j.LoggerFactory
import play.api.libs.json._

object SchemaEntity {

  trait CommandSerializable
  sealed trait Command                                                                         extends CommandSerializable
  final case class CreateSchema(payload: CreateSchemaPayload, replyTo: ActorRef[Confirmation]) extends Command
  final case class UpdateSchema(payload: UpdateSchemaPayload, replyTo: ActorRef[Confirmation]) extends Command
  final case class ActivateSchema(
    payload: ActivateSchemaPayload,
    attributesWithAssignment: Set[AttributeId],
    replyTo: ActorRef[Confirmation]
  )                                                                                            extends Command
  final case class DeleteSchema(payload: DeleteSchemaPayload, replyTo: ActorRef[Confirmation]) extends Command
  final case class GetSchema(replyTo: ActorRef[Confirmation])                                  extends Command
  final case class GetSchemaAttributes(replyTo: ActorRef[Confirmation])                        extends Command
  final case class GetSchemaAttribute(
    attributeId: AttributeId,
    replyTo: ActorRef[Confirmation]
  )                                                                                            extends Command

  sealed trait Confirmation
  final case object Success                                                   extends Confirmation
  final case class SuccessSchema(schema: Schema)                              extends Confirmation
  final case class SuccessSchemaAttribute(schemaAttribute: Option[Attribute]) extends Confirmation
  final case class SuccessSchemaAttributes(schemaAttributes: Seq[Attribute])  extends Confirmation
  final case object SchemaAlreadyExist                                        extends Confirmation
  final case object SchemaNotFound                                            extends Confirmation
  final case object EmptySchema                                               extends Confirmation
  final case object TypeChangeNotAllowed                                      extends Confirmation
  final case object AttributeNotFound                                         extends Confirmation
  final case class AttributesHasAssignments(attributes: String)               extends Confirmation

  implicit val confirmationSuccessFormat: Format[Success.type]                            = Json.format
  implicit val confirmationSuccessSchemaFormat: Format[SuccessSchema]                     = Json.format
  implicit val confirmationSuccessSchemaAttributeFormat: Format[SuccessSchemaAttribute]   = Json.format
  implicit val confirmationSuccessSchemaAttributesFormat: Format[SuccessSchemaAttributes] = Json.format
  implicit val confirmationSchemaAlreadyExistFormat: Format[SchemaAlreadyExist.type]      = Json.format
  implicit val confirmationSchemaNotFoundFormat: Format[SchemaNotFound.type]              = Json.format
  implicit val confirmationEmptySchemaFormat: Format[EmptySchema.type]                    = Json.format
  implicit val confirmationTypeChangeNotAllowedFormat: Format[TypeChangeNotAllowed.type]  = Json.format
  implicit val confirmationAttributeNotFound: Format[AttributeNotFound.type]              = Json.format
  implicit val confirmationAttributesHasAssignments: Format[AttributesHasAssignments]     = Json.format

  implicit val confirmationFormat: Format[Confirmation] =
//    implicit val config = JsonConfiguration(
//      discriminator = "confirmationType",
//      typeNaming = JsonNaming { fullName =>
//        fullName.split("\\.").toSeq.last
//      }
//    )
    Json.format[Confirmation]

  sealed trait Event extends AggregateEvent[Event] {
    override def aggregateTag: AggregateEventTagger[Event] = Event.Tag
  }

  object Event {
    val Tag: AggregateEventShards[Event] = AggregateEventTag.sharded[Event](numShards = 10)
  }

  final case class SchemaCreated(
    id: SchemaId,
    name: String,
    preparedAttributes: Set[PreparedAttribute],
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class SchemaNameUpdated(
    id: SchemaId,
    name: String,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  final case class ActiveAttributeCreated(
    id: SchemaId,
    attributeId: AttributeId,
    name: String,
    caption: Caption,
    attributeType: AttributeType,
    index: Option[AttributeIndexState] = None,
    activatedBy: AnnettePrincipal,
    activatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class ActiveAttributeUpdated(
    id: SchemaId,
    attributeId: AttributeId,
    name: String,
    caption: Caption,
    attributeType: AttributeType,
    index: Option[AttributeIndexState] = None,
    activatedBy: AnnettePrincipal,
    activatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class ActiveAttributeRemoved(
    id: SchemaId,
    attributeId: AttributeId,
    activatedBy: AnnettePrincipal,
    activatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class IndexAttributeCreated(
    id: SchemaId,
    attributeId: AttributeId,
    index: AttributeIndexState,
    alias: String,
    reindexAssignments: Boolean,
    activatedBy: AnnettePrincipal,
    activatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  final case class IndexAttributeRemoved(
    id: SchemaId,
    attributeId: AttributeId,
    alias: String,
    removeAssignments: Boolean,
    activatedBy: AnnettePrincipal,
    activatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class PreparedAttributeCreated(
    id: SchemaId,
    attributeId: AttributeId,
    name: String,
    caption: Caption,
    attributeType: AttributeType,
    index: Option[PreparedAttributeIndex] = None,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class PreparedAttributeUpdated(
    id: SchemaId,
    attributeId: AttributeId,
    name: String,
    caption: Caption,
    attributeType: AttributeType,
    index: Option[PreparedAttributeIndex] = None,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class PreparedAttributeRemoved(
    id: SchemaId,
    attributeId: AttributeId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class SchemaDeleted(
    id: SchemaId,
    deletedBy: AnnettePrincipal,
    deletedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  implicit val eventSchemaCreatedFormat: Format[SchemaCreated]                       = Json.format
  implicit val eventSchemaNameUpdatedFormat: Format[SchemaNameUpdated]               = Json.format
  implicit val eventActiveAttributeCreatedFormat: Format[ActiveAttributeCreated]     = Json.format
  implicit val eventActiveAttributeUpdatedFormat: Format[ActiveAttributeUpdated]     = Json.format
  implicit val eventActiveAttributeRemovedFormat: Format[ActiveAttributeRemoved]     = Json.format
  implicit val eventIndexAttributeCreatedFormat: Format[IndexAttributeCreated]       = Json.format
  implicit val eventIndexAttributeRemovedFormat: Format[IndexAttributeRemoved]       = Json.format
  implicit val eventPreparedAttributeCreatedFormat: Format[PreparedAttributeCreated] = Json.format
  implicit val eventPreparedAttributeUpdatedFormat: Format[PreparedAttributeUpdated] = Json.format
  implicit val eventPreparedAttributeRemovedFormat: Format[PreparedAttributeRemoved] = Json.format
  implicit val eventSchemaDeletedFormat: Format[SchemaDeleted]                       = Json.format

  val empty = SchemaEntity()

  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Attributes_Schema")

  def apply(persistenceId: PersistenceId): EventSourcedBehavior[Command, Event, SchemaEntity] =
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, SchemaEntity](
        persistenceId = persistenceId,
        emptyState = SchemaEntity.empty,
        commandHandler = (entity, cmd) => entity.applyCommand(cmd),
        eventHandler = (entity, evt) => entity.applyEvent(evt)
      )

  def apply(entityContext: EntityContext[Command]): Behavior[Command] =
    apply(PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId))
      .withTagger(AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2))

  implicit val entityFormat: Format[SchemaEntity] = Json.format

  def alias(id: SchemaId, attributeId: AttributeId, aliasNo: Int) = {
    val schemaId = id.sub.map(sub => s"${id.id}_${sub}").getOrElse(id.id)
    s"attr_${schemaId}_${attributeId}_${aliasNo}"
  }

}

final case class SchemaEntity(maybeState: Option[SchemaState] = None) {
  import SchemaEntity._

  val log = LoggerFactory.getLogger(this.getClass)

  def applyCommand(cmd: Command): ReplyEffect[Event, SchemaEntity] =
    cmd match {
      case cmd: CreateSchema        => createSchema(cmd)
      case cmd: UpdateSchema        => updateSchema(cmd)
      case cmd: ActivateSchema      => activateSchema(cmd)
      case cmd: DeleteSchema        => deleteSchema(cmd)
      case cmd: GetSchema           => getSchema(cmd)
      case cmd: GetSchemaAttribute  => getSchemaAttribute(cmd)
      case cmd: GetSchemaAttributes => getSchemaAttributes(cmd)
    }

  def createSchema(cmd: CreateSchema): ReplyEffect[Event, SchemaEntity] =
    maybeState match {
      case Some(_) => Effect.reply(cmd.replyTo)(SchemaAlreadyExist)
      case None    =>
        val event = cmd.payload
          .transformInto[SchemaCreated]
        Effect.persist(event).thenReply(cmd.replyTo)(_ => Success)
    }

  def updateSchema(cmd: UpdateSchema): ReplyEffect[Event, SchemaEntity] =
    maybeState match {
      case None        => Effect.reply(cmd.replyTo)(SchemaNotFound)
      case Some(state)
          if cmd.payload.preparedAttributes
            .exists(a => state.activeAttributes.get(a.attributeId).exists(_.attributeType != a.attributeType)) =>
        Effect.reply(cmd.replyTo)(TypeChangeNotAllowed)
      case Some(state) =>
        val now             = OffsetDateTime.now
        val updateNameEvent =
          if (state.name != cmd.payload.name)
            Seq(
              cmd.payload
                .into[SchemaNameUpdated]
                .withFieldConst(_.updatedAt, now)
                .transform
            )
          else
            Seq.empty

        val createdIds                    = cmd.payload.preparedAttributes.map(_.attributeId) -- state.preparedAttributes.keys.toSet
        val createPreparedAttributeEvents = cmd.payload.preparedAttributes
          .filter(a => createdIds.contains(a.attributeId))
          .map(a =>
            a.into[PreparedAttributeUpdated]
              .withFieldConst(_.id, cmd.payload.id)
              .withFieldConst(_.attributeId, a.attributeId)
              .withFieldConst(_.updatedBy, cmd.payload.updatedBy)
              .withFieldConst(_.updatedAt, now)
              .transform
          )

        val removedIds                     = state.preparedAttributes.keys.toSet -- cmd.payload.preparedAttributes.map(_.attributeId)
        val removePreparedAttributeEvents  = removedIds.map(attributeId =>
          cmd.payload
            .into[PreparedAttributeRemoved]
            .withFieldConst(_.attributeId, attributeId)
            .withFieldConst(_.updatedAt, now)
            .transform
        )
        val updatePreparedAttributesEvents = cmd.payload.preparedAttributes
          .flatMap(cmdAttr =>
            state.preparedAttributes
              .get(cmdAttr.attributeId)
              .flatMap(stateAttr =>
                if (cmdAttr != stateAttr)
                  Some(
                    cmdAttr
                      .into[PreparedAttributeUpdated]
                      .withFieldConst(_.id, cmd.payload.id)
                      .withFieldConst(_.updatedBy, cmd.payload.updatedBy)
                      .withFieldConst(_.updatedAt, now)
                      .transform
                  )
                else None
              )
          )
          .toSeq
        Effect
          .persist(
            updateNameEvent ++ createPreparedAttributeEvents ++
              updatePreparedAttributesEvents ++ removePreparedAttributeEvents
          )
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def activateSchema(cmd: ActivateSchema): ReplyEffect[Event, SchemaEntity] = {
    maybeState match {
      case None        => Effect.reply(cmd.replyTo)(SchemaNotFound)
      case Some(state) =>
        val removeIds                 = state.activeAttributes.keys.toSet -- state.preparedAttributes.keys.toSet
        val removeIdsHavingAssignment = removeIds.intersect(cmd.attributesWithAssignment)
        if (removeIdsHavingAssignment.nonEmpty)
          Effect.reply(cmd.replyTo)(AttributesHasAssignments(removeIdsHavingAssignment.mkString(", ")))
        else {

          val now               = OffsetDateTime.now
          val createIds         = state.preparedAttributes.keys.toSet -- state.activeAttributes.keys.toSet
          val createAttrEvents  = createIds.map { attributeId =>
            val aliasNo = state.usedAliases.getOrElse(attributeId, -1) + 1
            val index   = state
              .preparedAttributes(attributeId)
              .index
              .map(a => AttributeIndexState.from(a, aliasNo))
            state
              .preparedAttributes(attributeId)
              .into[ActiveAttributeCreated]
              .withFieldConst(_.id, cmd.payload.id)
              .withFieldConst(_.index, index)
              .withFieldConst(_.activatedBy, cmd.payload.activatedBy)
              .withFieldConst(_.activatedAt, now)
              .transform
          }.toSeq
          val createIndexEvents = createIds.flatMap { attributeId =>
            val aliasNo = state.usedAliases.getOrElse(attributeId, -1) + 1
            state
              .preparedAttributes(attributeId)
              .index
              .map(a =>
                a.into[IndexAttributeCreated]
                  .withFieldConst(_.id, state.id)
                  .withFieldConst(_.attributeId, attributeId)
                  .withFieldConst(_.index, AttributeIndexState.from(a, aliasNo))
                  .withFieldConst(_.alias, alias(state.id, attributeId, aliasNo))
                  .withFieldConst(_.reindexAssignments, cmd.attributesWithAssignment.contains(attributeId))
                  .withFieldConst(_.activatedBy, cmd.payload.activatedBy)
                  .withFieldConst(_.activatedAt, now)
                  .transform
              )
          }.toSeq

          val removeAttrEvents  = removeIds
            .map(attributeId =>
              cmd.payload
                .into[ActiveAttributeRemoved]
                .withFieldConst(_.attributeId, attributeId)
                .withFieldConst(_.activatedAt, now)
                .transform
            )
            .toSeq
          val removeIndexEvents = removeIds
            .flatMap(attributeId =>
              state
                .activeAttributes(attributeId)
                .index
                .map(attr =>
                  attr
                    .into[IndexAttributeRemoved]
                    .withFieldConst(_.id, state.id)
                    .withFieldConst(_.attributeId, attributeId)
                    .withFieldConst(_.alias, alias(state.id, attributeId, attr.aliasNo))
                    .withFieldConst(_.removeAssignments, cmd.attributesWithAssignment.contains(attributeId))
                    .withFieldConst(_.activatedBy, cmd.payload.activatedBy)
                    .withFieldConst(_.activatedAt, now)
                    .transform
                )
            )
            .toSeq
          val updateAttrEvents  = (for {
            prepAttr   <- state.preparedAttributes.values
            activeAttr <- state.activeAttributes.get(prepAttr.attributeId)
          } yield
            if (isAttributeChanged(prepAttr, activeAttr)) {
              val index =
                if (isIndexChanged(prepAttr.index, activeAttr.index) && prepAttr.index.isDefined)
                  Some(
                    AttributeIndexState.from(
                      prepAttr.index.get,
                      aliasNo = activeAttr.index
                        .map(_.aliasNo + 1)
                        .getOrElse(state.usedAliases.getOrElse(prepAttr.attributeId, -1) + 1)
                    )
                  )
                else if (isIndexChanged(prepAttr.index, activeAttr.index) && prepAttr.index.isEmpty)
                  None
                else
                  activeAttr.index
              Some(
                prepAttr
                  .into[ActiveAttributeUpdated]
                  .withFieldConst(_.id, cmd.payload.id)
                  .withFieldConst(_.index, index)
                  .withFieldConst(_.activatedBy, cmd.payload.activatedBy)
                  .withFieldConst(_.activatedAt, now)
                  .transform
              )
            } else None).flatten.toSeq

          val updateIndexEvents = (for {
            prepAttr   <- state.preparedAttributes.values
            activeAttr <- state.activeAttributes.get(prepAttr.attributeId)
          } yield
            if (
              isIndexChanged(prepAttr.index, activeAttr.index) && prepAttr.index.isDefined && activeAttr.index.isEmpty
            ) {
              val aliasNo = state.usedAliases.getOrElse(prepAttr.attributeId, -1) + 1
              Seq(
                prepAttr.index.get
                  .into[IndexAttributeCreated]
                  .withFieldConst(_.id, cmd.payload.id)
                  .withFieldConst(_.attributeId, prepAttr.attributeId)
                  .withFieldConst(_.index, AttributeIndexState.from(prepAttr.index.get, aliasNo))
                  .withFieldConst(_.alias, alias(cmd.payload.id, prepAttr.attributeId, aliasNo))
                  .withFieldConst(_.reindexAssignments, cmd.attributesWithAssignment.contains(activeAttr.attributeId))
                  .withFieldConst(_.activatedBy, cmd.payload.activatedBy)
                  .withFieldConst(_.activatedAt, now)
                  .transform
              )
            } else if (
              isIndexChanged(prepAttr.index, activeAttr.index) && prepAttr.index.isEmpty && activeAttr.index.isDefined
            )
              Seq(
                activeAttr.index.get
                  .into[IndexAttributeRemoved]
                  .withFieldConst(_.id, cmd.payload.id)
                  .withFieldConst(_.attributeId, prepAttr.attributeId)
                  .withFieldConst(_.alias, alias(cmd.payload.id, activeAttr.attributeId, activeAttr.index.get.aliasNo))
                  .withFieldConst(_.removeAssignments, cmd.attributesWithAssignment.contains(activeAttr.attributeId))
                  .withFieldConst(_.activatedBy, cmd.payload.activatedBy)
                  .withFieldConst(_.activatedAt, now)
                  .transform
              )
            else if (
              isIndexChanged(prepAttr.index, activeAttr.index) && prepAttr.index.isDefined && activeAttr.index.isDefined
            )
              Seq(
                prepAttr.index.get
                  .into[IndexAttributeRemoved]
                  .withFieldConst(_.id, cmd.payload.id)
                  .withFieldConst(_.alias, alias(cmd.payload.id, activeAttr.attributeId, activeAttr.index.get.aliasNo))
                  .withFieldConst(_.attributeId, prepAttr.attributeId)
                  .withFieldConst(_.removeAssignments, cmd.attributesWithAssignment.contains(activeAttr.attributeId))
                  .withFieldConst(_.activatedBy, cmd.payload.activatedBy)
                  .withFieldConst(_.activatedAt, now)
                  .transform,
                prepAttr.index.get
                  .into[IndexAttributeCreated]
                  .withFieldConst(_.id, cmd.payload.id)
                  .withFieldConst(
                    _.index,
                    AttributeIndexState.from(prepAttr.index.get, activeAttr.index.get.aliasNo + 1)
                  )
                  .withFieldConst(
                    _.alias,
                    alias(cmd.payload.id, activeAttr.attributeId, activeAttr.index.get.aliasNo + 1)
                  )
                  .withFieldConst(_.attributeId, prepAttr.attributeId)
                  .withFieldConst(_.reindexAssignments, cmd.attributesWithAssignment.contains(activeAttr.attributeId))
                  .withFieldConst(_.activatedBy, cmd.payload.activatedBy)
                  .withFieldConst(_.activatedAt, now)
                  .transform
              )
            else Seq.empty).flatten.toSeq

          val events =
            createAttrEvents ++ updateAttrEvents ++ removeAttrEvents ++ createIndexEvents ++ updateIndexEvents ++ removeIndexEvents
//          events.foreach(e => log.debug("activate event: {}", e))
          Effect
            .persist(events)
            .thenReply(cmd.replyTo)(_ => Success)
        }
    }
  }

  def deleteSchema(cmd: DeleteSchema): ReplyEffect[Event, SchemaEntity] =
    maybeState match {
      case None        => Effect.reply(cmd.replyTo)(SchemaNotFound)
      case Some(state) =>
        val now                           = OffsetDateTime.now
        val removeIndexedAttributesEvents = state.activeAttributes.values
          .filter(_.index.isDefined)
          .map(attr =>
            IndexAttributeRemoved(
              id = state.id,
              attributeId = attr.attributeId,
              alias = alias(state.id, attr.attributeId, attr.index.get.aliasNo),
              removeAssignments = false,
              activatedBy = cmd.payload.deletedBy,
              activatedAt = now
            )
          )
          .toSeq
        val deleteEvent                   = cmd.payload
          .into[SchemaDeleted]
          .withFieldConst(_.deletedAt, now)
          .transform
        Effect
          .persist(
            removeIndexedAttributesEvents :+ deleteEvent
          )
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def getSchema(cmd: GetSchema): ReplyEffect[Event, SchemaEntity] =
    maybeState match {
      case None        => Effect.reply(cmd.replyTo)(SchemaNotFound)
      case Some(state) =>
        val activeAttributes = state.activeAttributes.values.map { attr =>
          val newAttr = toActiveSchemaAttribute(state.id, attr)
          newAttr.attributeId -> newAttr
        }.toMap
        val schema           = state
          .into[Schema]
          .withFieldConst(_.activeAttributes, activeAttributes)
          .transform
        Effect.reply(cmd.replyTo)(SuccessSchema(schema))
    }

  def getSchemaAttribute(cmd: GetSchemaAttribute): ReplyEffect[Event, SchemaEntity] =
    maybeState match {
      case None        => Effect.reply(cmd.replyTo)(SchemaNotFound)
      case Some(state) =>
        val maybeAttribute =
          state.activeAttributes.get(cmd.attributeId).map(attrId => toActiveSchemaAttribute(state.id, attrId))
        Effect.reply(cmd.replyTo)(SuccessSchemaAttribute(maybeAttribute))
    }

  def getSchemaAttributes(cmd: GetSchemaAttributes): ReplyEffect[Event, SchemaEntity] =
    maybeState match {
      case None        => Effect.reply(cmd.replyTo)(SchemaNotFound)
      case Some(state) =>
        val attributes = state.activeAttributes.values.map(attrId => toActiveSchemaAttribute(state.id, attrId)).toSeq
        Effect.reply(cmd.replyTo)(SuccessSchemaAttributes(attributes))
    }

  def applyEvent(event: Event): SchemaEntity =
    event match {
      case event: SchemaCreated            => onSchemaCreated(event)
      case event: SchemaNameUpdated        => onSchemaNameUpdated(event)
      case event: ActiveAttributeCreated   => onActiveAttributeCreated(event)
      case event: ActiveAttributeUpdated   => onActiveAttributeUpdated(event)
      case event: ActiveAttributeRemoved   => onActiveAttributeRemoved(event)
      case _: IndexAttributeCreated        => this
      case _: IndexAttributeRemoved        => this
      case event: PreparedAttributeCreated => onPreparedAttributeCreated(event)
      case event: PreparedAttributeUpdated => onPreparedAttributeUpdated(event)
      case event: PreparedAttributeRemoved => onPreparedAttributeRemoved(event)
      case _: SchemaDeleted                => onSchemaDeleted()
    }

  def onSchemaCreated(event: SchemaCreated): SchemaEntity =
    SchemaEntity(
      Some(
        event
          .into[SchemaState]
          .withFieldConst(_.preparedAttributes, event.preparedAttributes.map(a => a.attributeId -> a).toMap)
          .transform
      )
    )

  def onSchemaNameUpdated(event: SchemaNameUpdated): SchemaEntity =
    SchemaEntity(
      maybeState.map(
        _.copy(
          name = event.name,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

  def onActiveAttributeCreated(event: ActiveAttributeCreated): SchemaEntity =
    SchemaEntity(
      maybeState.map { state =>
        val attribute = event.transformInto[AttributeState]
        state.copy(
          activeAttributes = state.activeAttributes + (attribute.attributeId -> attribute),
          activatedBy = Some(event.activatedBy),
          activatedAt = Some(event.activatedAt),
          updatedBy = event.activatedBy,
          updatedAt = event.activatedAt
        )
      }
    )

  def onActiveAttributeUpdated(event: ActiveAttributeUpdated): SchemaEntity =
    SchemaEntity(
      maybeState.map { state =>
        val usedAliases = state
          .activeAttributes(event.attributeId)
          .index
          .map(ind => state.usedAliases + (event.attributeId -> ind.aliasNo))
          .getOrElse(state.usedAliases)
        val attribute   = event
          .into[AttributeState]
          .transform
        state.copy(
          activeAttributes = state.activeAttributes + (attribute.attributeId -> attribute),
          activatedBy = Some(event.activatedBy),
          activatedAt = Some(event.activatedAt),
          usedAliases = usedAliases,
          updatedBy = event.activatedBy,
          updatedAt = event.activatedAt
        )
      }
    )

  def onActiveAttributeRemoved(event: ActiveAttributeRemoved): SchemaEntity =
    SchemaEntity(
      maybeState.map { state =>
        val usedAliases = state
          .activeAttributes(event.attributeId)
          .index
          .map(ind => state.usedAliases + (event.attributeId -> ind.aliasNo))
          .getOrElse(state.usedAliases)
        state.copy(
          activeAttributes = state.activeAttributes - event.attributeId,
          activatedBy = Some(event.activatedBy),
          activatedAt = Some(event.activatedAt),
          usedAliases = usedAliases,
          updatedBy = event.activatedBy,
          updatedAt = event.activatedAt
        )
      }
    )

  def onPreparedAttributeCreated(event: PreparedAttributeCreated): SchemaEntity =
    SchemaEntity(
      maybeState.map { state =>
        val attribute = event.transformInto[PreparedAttribute]
        state.copy(
          preparedAttributes = state.preparedAttributes + (attribute.attributeId -> attribute),
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      }
    )

  def onPreparedAttributeUpdated(event: PreparedAttributeUpdated): SchemaEntity =
    SchemaEntity(
      maybeState.map { state =>
        val attribute = event
          .into[PreparedAttribute]
          .transform
        state.copy(
          preparedAttributes = state.preparedAttributes + (attribute.attributeId -> attribute),
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      }
    )

  def onPreparedAttributeRemoved(event: PreparedAttributeRemoved): SchemaEntity =
    SchemaEntity(
      maybeState.map { state =>
        state.copy(
          preparedAttributes = state.preparedAttributes - event.attributeId,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      }
    )

  def onSchemaDeleted(): SchemaEntity =
    SchemaEntity(None)

  def isIndexChanged(
    prepIndexOpt: Option[PreparedAttributeIndex],
    activeIndexOpt: Option[AttributeIndexState]
  ): Boolean =
    prepIndexOpt match {
      case Some(prepIndex) =>
        activeIndexOpt match {
          case Some(activeIndex) =>
            AttributeIndexState.from(prepIndex, activeIndex.aliasNo) != activeIndex
          case None              => true
        }
      case None            =>
        activeIndexOpt match {
          case Some(_) => true
          case None    => false
        }
    }

  private def isAttributeChanged(prepAttr: PreparedAttribute, activeAttr: AttributeState): Boolean =
    isIndexChanged(
      prepAttr.index,
      activeAttr.index
    ) || prepAttr.name != activeAttr.name || prepAttr.caption != activeAttr.caption

  private def toActiveSchemaAttribute(id: SchemaId, attr: AttributeState) =
    attr
      .into[Attribute]
      .withFieldConst(
        _.index,
        attr.index.map(_.toAttributeIndex(id, attr.attributeId))
      )
      .transform

}
