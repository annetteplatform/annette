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

package biz.lobachev.annette.cms.impl.pages.space

import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl.{EntityContext, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import biz.lobachev.annette.cms.api.pages.space._
import biz.lobachev.annette.cms.impl.pages.space.model.SpaceState
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.category.CategoryId
import com.lightbend.lagom.scaladsl.persistence._
import io.scalaland.chimney.dsl._
import org.slf4j.LoggerFactory
import play.api.libs.json._

import java.time.OffsetDateTime

object SpaceEntity {

  trait CommandSerializable
  sealed trait Command extends CommandSerializable

  final case class CreateSpace(
    id: SpaceId,
    name: String,
    description: String,
    categoryId: CategoryId,
    targets: Set[AnnettePrincipal] = Set.empty,
    createdBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class UpdateSpaceName(
    id: SpaceId,
    name: String,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class UpdateSpaceDescription(
    id: SpaceId,
    description: String,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class UpdateSpaceCategoryId(
    id: SpaceId,
    categoryId: CategoryId,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class AssignSpaceTargetPrincipal(
    id: SpaceId,
    principal: AnnettePrincipal,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class UnassignSpaceTargetPrincipal(
    id: SpaceId,
    principal: AnnettePrincipal,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class ActivateSpace(id: SpaceId, updatedBy: AnnettePrincipal, replyTo: ActorRef[Confirmation])
      extends Command

  final case class DeactivateSpace(id: SpaceId, updatedBy: AnnettePrincipal, replyTo: ActorRef[Confirmation])
      extends Command

  final case class DeleteSpace(id: SpaceId, deletedBy: AnnettePrincipal, replyTo: ActorRef[Confirmation])
      extends Command

  final case class GetSpace(id: SpaceId, replyTo: ActorRef[Confirmation]) extends Command

  sealed trait Confirmation
  final case object Success                                       extends Confirmation
  final case class SuccessSpace(space: Space)                     extends Confirmation
  final case class SuccessTargets(targets: Set[AnnettePrincipal]) extends Confirmation
  final case object SpaceAlreadyExist                             extends Confirmation
  final case object SpaceNotFound                                 extends Confirmation

  implicit val confirmationSuccessFormat: Format[Success.type]                     = Json.format
  implicit val confirmationSuccessSpaceFormat: Format[SuccessSpace]                = Json.format
  implicit val confirmationSuccessTargetsFormat: Format[SuccessTargets]            = Json.format
  implicit val confirmationSpaceAlreadyExistFormat: Format[SpaceAlreadyExist.type] = Json.format
  implicit val confirmationSpaceNotFoundFormat: Format[SpaceNotFound.type]         = Json.format

  sealed trait Event extends AggregateEvent[Event] {
    override def aggregateTag: AggregateEventTagger[Event] = Event.Tag
  }

  object Event {
    val Tag: AggregateEventShards[Event] = AggregateEventTag.sharded[Event](numShards = 10)
  }

  final case class SpaceCreated(
    id: SpaceId,
    name: String,
    description: String,
    categoryId: CategoryId,
    targets: Set[AnnettePrincipal] = Set.empty,
    createdBy: AnnettePrincipal,
    createdAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class SpaceNameUpdated(
    id: SpaceId,
    name: String,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class SpaceDescriptionUpdated(
    id: SpaceId,
    description: String,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class SpaceCategoryUpdated(
    id: SpaceId,
    categoryId: CategoryId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class SpaceTargetPrincipalAssigned(
    id: SpaceId,
    principal: AnnettePrincipal,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class SpaceTargetPrincipalUnassigned(
    id: SpaceId,
    principal: AnnettePrincipal,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class SpaceActivated(
    id: SpaceId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class SpaceDeactivated(
    id: SpaceId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class SpaceDeleted(id: SpaceId, deletedBy: AnnettePrincipal, deleteAt: OffsetDateTime = OffsetDateTime.now)
      extends Event

  implicit val eventSpaceCreatedFormat: Format[SpaceCreated]                                     = Json.format
  implicit val eventSpaceNameUpdatedFormat: Format[SpaceNameUpdated]                             = Json.format
  implicit val eventSpaceDescriptionUpdatedFormat: Format[SpaceDescriptionUpdated]               = Json.format
  implicit val eventSpaceCategoryUpdatedFormat: Format[SpaceCategoryUpdated]                     = Json.format
  implicit val eventSpaceTargetPrincipalAssignedFormat: Format[SpaceTargetPrincipalAssigned]     = Json.format
  implicit val eventSpaceTargetPrincipalUnassignedFormat: Format[SpaceTargetPrincipalUnassigned] = Json.format
  implicit val eventSpaceActivatedFormat: Format[SpaceActivated]                                 = Json.format
  implicit val eventSpaceDeactivatedFormat: Format[SpaceDeactivated]                             = Json.format
  implicit val eventSpaceDeletedFormat: Format[SpaceDeleted]                                     = Json.format

  val empty = SpaceEntity()

  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Cms_Space")

  def apply(persistenceId: PersistenceId): EventSourcedBehavior[Command, Event, SpaceEntity] =
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, SpaceEntity](
        persistenceId = persistenceId,
        emptyState = SpaceEntity.empty,
        commandHandler = (entity, cmd) => entity.applyCommand(cmd),
        eventHandler = (entity, evt) => entity.applyEvent(evt)
      )

  def apply(entityContext: EntityContext[Command]): Behavior[Command] =
    apply(PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId))
      .withTagger(AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2))

  implicit val entityFormat: Format[SpaceEntity] = Json.format

}

final case class SpaceEntity(maybeState: Option[SpaceState] = None) {
  import SpaceEntity._

  val log = LoggerFactory.getLogger(this.getClass)

  def applyCommand(cmd: Command): ReplyEffect[Event, SpaceEntity] =
    cmd match {
      case cmd: CreateSpace                  => createSpace(cmd)
      case cmd: UpdateSpaceName              => updateSpaceName(cmd)
      case cmd: UpdateSpaceDescription       => updateSpaceDescription(cmd)
      case cmd: UpdateSpaceCategoryId        => updateSpaceCategory(cmd)
      case cmd: AssignSpaceTargetPrincipal   => assignSpaceTargetPrincipal(cmd)
      case cmd: UnassignSpaceTargetPrincipal => unassignSpaceTargetPrincipal(cmd)
      case cmd: ActivateSpace                => activateSpace(cmd)
      case cmd: DeactivateSpace              => deactivateSpace(cmd)
      case cmd: DeleteSpace                  => deleteSpace(cmd)
      case cmd: GetSpace                     => getSpace(cmd)
    }

  def createSpace(cmd: CreateSpace): ReplyEffect[Event, SpaceEntity] =
    maybeState match {
      case None    =>
        val event = cmd
          .transformInto[SpaceCreated]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
      case Some(_) => Effect.reply(cmd.replyTo)(SpaceAlreadyExist)
    }

  def updateSpaceName(cmd: UpdateSpaceName): ReplyEffect[Event, SpaceEntity] =
    maybeState match {
      case None    => Effect.reply(cmd.replyTo)(SpaceNotFound)
      case Some(_) =>
        val event = cmd.transformInto[SpaceNameUpdated]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def updateSpaceDescription(cmd: UpdateSpaceDescription): ReplyEffect[Event, SpaceEntity] =
    maybeState match {
      case None    => Effect.reply(cmd.replyTo)(SpaceNotFound)
      case Some(_) =>
        val event = cmd.transformInto[SpaceDescriptionUpdated]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def updateSpaceCategory(cmd: UpdateSpaceCategoryId): ReplyEffect[Event, SpaceEntity] =
    maybeState match {
      case None    => Effect.reply(cmd.replyTo)(SpaceNotFound)
      case Some(_) =>
        val event = cmd.transformInto[SpaceCategoryUpdated]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def assignSpaceTargetPrincipal(cmd: AssignSpaceTargetPrincipal): ReplyEffect[Event, SpaceEntity] =
    maybeState match {
      case None                                                 => Effect.reply(cmd.replyTo)(SpaceNotFound)
      case Some(state) if state.targets.contains(cmd.principal) => Effect.reply(cmd.replyTo)(Success)
      case Some(_)                                              =>
        val event = cmd.transformInto[SpaceTargetPrincipalAssigned]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def unassignSpaceTargetPrincipal(cmd: UnassignSpaceTargetPrincipal): ReplyEffect[Event, SpaceEntity] =
    maybeState match {
      case None                                                  => Effect.reply(cmd.replyTo)(SpaceNotFound)
      case Some(state) if !state.targets.contains(cmd.principal) => Effect.reply(cmd.replyTo)(Success)
      case Some(_)                                               =>
        val event = cmd.transformInto[SpaceTargetPrincipalUnassigned]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def activateSpace(cmd: ActivateSpace): ReplyEffect[Event, SpaceEntity] =
    maybeState match {
      case None                         => Effect.reply(cmd.replyTo)(SpaceNotFound)
      case Some(state) if !state.active =>
        val event = cmd.transformInto[SpaceActivated]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
      case _                            => Effect.reply(cmd.replyTo)(Success)
    }

  def deactivateSpace(cmd: DeactivateSpace): ReplyEffect[Event, SpaceEntity] =
    maybeState match {
      case None                        => Effect.reply(cmd.replyTo)(SpaceNotFound)
      case Some(state) if state.active =>
        val event = cmd.transformInto[SpaceDeactivated]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
      case _                           => Effect.reply(cmd.replyTo)(Success)
    }

  def deleteSpace(cmd: DeleteSpace): ReplyEffect[Event, SpaceEntity] =
    maybeState match {
      case None    => Effect.reply(cmd.replyTo)(SpaceNotFound)
      case Some(_) =>
        val event = cmd.transformInto[SpaceDeleted]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def getSpace(cmd: GetSpace): ReplyEffect[Event, SpaceEntity] =
    maybeState match {
      case None        => Effect.reply(cmd.replyTo)(SpaceNotFound)
      case Some(state) => Effect.reply(cmd.replyTo)(SuccessSpace(state.transformInto[Space]))
    }

  def applyEvent(event: Event): SpaceEntity =
    event match {
      case event: SpaceCreated                   => onSpaceCreated(event)
      case event: SpaceNameUpdated               => onSpaceNameUpdated(event)
      case event: SpaceDescriptionUpdated        => onSpaceDescriptionUpdated(event)
      case event: SpaceCategoryUpdated           => onSpaceCategoryUpdated(event)
      case event: SpaceTargetPrincipalAssigned   => onSpaceTargetPrincipalAssigned(event)
      case event: SpaceTargetPrincipalUnassigned => onSpaceTargetPrincipalUnassigned(event)
      case event: SpaceActivated                 => onSpaceActivated(event)
      case event: SpaceDeactivated               => onSpaceDeactivated(event)
      case _: SpaceDeleted                       => onSpaceDeleted()
    }

  def onSpaceCreated(event: SpaceCreated): SpaceEntity =
    SpaceEntity(
      Some(
        event
          .into[SpaceState]
          .withFieldConst(_.active, true)
          .withFieldConst(_.updatedBy, event.createdBy)
          .withFieldConst(_.updatedAt, event.createdAt)
          .transform
      )
    )

  def onSpaceNameUpdated(event: SpaceNameUpdated): SpaceEntity =
    SpaceEntity(
      maybeState.map(
        _.copy(
          name = event.name,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

  def onSpaceDescriptionUpdated(event: SpaceDescriptionUpdated): SpaceEntity =
    SpaceEntity(
      maybeState.map(
        _.copy(
          description = event.description,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

  def onSpaceCategoryUpdated(event: SpaceCategoryUpdated): SpaceEntity =
    SpaceEntity(
      maybeState.map(
        _.copy(
          categoryId = event.categoryId,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

  def onSpaceTargetPrincipalAssigned(event: SpaceTargetPrincipalAssigned): SpaceEntity =
    SpaceEntity(
      maybeState.map(state =>
        state.copy(
          targets = state.targets + event.principal,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

  def onSpaceTargetPrincipalUnassigned(event: SpaceTargetPrincipalUnassigned): SpaceEntity =
    SpaceEntity(
      maybeState.map(state =>
        state.copy(
          targets = state.targets - event.principal,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

  def onSpaceActivated(event: SpaceActivated): SpaceEntity =
    SpaceEntity(
      maybeState.map(state =>
        state.copy(
          active = true,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

  def onSpaceDeactivated(event: SpaceDeactivated): SpaceEntity =
    SpaceEntity(
      maybeState.map(state =>
        state.copy(
          active = false,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

  def onSpaceDeleted(): SpaceEntity =
    SpaceEntity(None)

}
