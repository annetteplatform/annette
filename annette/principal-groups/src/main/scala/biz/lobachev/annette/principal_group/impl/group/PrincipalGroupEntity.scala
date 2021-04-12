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

package biz.lobachev.annette.principal_group.impl.group

import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl._
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.principal_group.api.category.CategoryId
import biz.lobachev.annette.principal_group.api.group._
import biz.lobachev.annette.principal_group.impl.group.model.PrincipalGroupState
import com.lightbend.lagom.scaladsl.persistence._
import io.scalaland.chimney.dsl._
import play.api.libs.json.{Format, _}

import java.time.OffsetDateTime

object PrincipalGroupEntity {

  trait CommandSerializable

  sealed trait Command extends CommandSerializable

  final case class CreatePrincipalGroup(
    id: PrincipalGroupId,
    name: String,
    description: String,
    categoryId: CategoryId,
    createdBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  )                                                                                         extends Command
  final case class UpdatePrincipalGroupName(
    id: PrincipalGroupId,
    name: String,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  )                                                                                         extends Command
  final case class UpdatePrincipalGroupDescription(
    id: PrincipalGroupId,
    description: String,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  )                                                                                         extends Command
  final case class UpdatePrincipalGroupCategory(
    id: PrincipalGroupId,
    categoryId: CategoryId,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  )                                                                                         extends Command
  final case class DeletePrincipalGroup(
    id: PrincipalGroupId,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  )                                                                                         extends Command
  final case class AssignPrincipal(
    id: PrincipalGroupId,
    principal: AnnettePrincipal,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  )                                                                                         extends Command
  final case class UnassignPrincipal(
    id: PrincipalGroupId,
    principal: AnnettePrincipal,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  )                                                                                         extends Command
  final case class GetPrincipalGroup(id: PrincipalGroupId, replyTo: ActorRef[Confirmation]) extends Command

  sealed trait Confirmation
  final case object Success                                      extends Confirmation
  final case class SuccessPrincipalGroup(entity: PrincipalGroup) extends Confirmation
  final case object NotFound                                     extends Confirmation
  final case object AlreadyExist                                 extends Confirmation

  implicit val confirmationSuccessFormat: Format[Success.type]                        = Json.format
  implicit val confirmationSuccessPrincipalGroupFormat: Format[SuccessPrincipalGroup] = Json.format
  implicit val confirmationNotFoundFormat: Format[NotFound.type]                      = Json.format
  implicit val confirmationAlreadyExistFormat: Format[AlreadyExist.type]              = Json.format
  implicit val confirmationFormat: Format[Confirmation]                               = Json.format[Confirmation]

  sealed trait Event extends AggregateEvent[Event] {
    override def aggregateTag: AggregateEventTagger[Event] = Event.Tag
  }

  object Event {
    val Tag: AggregateEventShards[Event] = AggregateEventTag.sharded[Event](numShards = 10)
  }

  final case class PrincipalGroupCreated(
    id: PrincipalGroupId,
    name: String,
    description: String,
    categoryId: CategoryId,
    createdBy: AnnettePrincipal,
    createdAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class PrincipalGroupNameUpdated(
    id: PrincipalGroupId,
    name: String,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  final case class PrincipalGroupDescriptionUpdated(
    id: PrincipalGroupId,
    description: String,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  final case class PrincipalGroupCategoryUpdated(
    id: PrincipalGroupId,
    categoryId: CategoryId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  final case class PrincipalGroupDeleted(
    id: PrincipalGroupId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class PrincipalAssigned(
    id: PrincipalGroupId,
    principal: AnnettePrincipal,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class PrincipalUnassigned(
    id: PrincipalGroupId,
    principal: AnnettePrincipal,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  implicit val principalGroupCreatedFormat: Format[PrincipalGroupCreated]                       = Json.format
  implicit val principalGroupNameUpdatedFormat: Format[PrincipalGroupNameUpdated]               = Json.format
  implicit val principalGroupDescriptionUpdatedFormat: Format[PrincipalGroupDescriptionUpdated] = Json.format
  implicit val principalGroupCategoryUpdatedFormat: Format[PrincipalGroupCategoryUpdated]       = Json.format
  implicit val principalGroupDeletedFormat: Format[PrincipalGroupDeleted]                       = Json.format
  implicit val principalAssignedFormat: Format[PrincipalAssigned]                               = Json.format
  implicit val principalUnassignedFormat: Format[PrincipalUnassigned]                           = Json.format

  val empty                           = PrincipalGroupEntity(None)
  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("PrincipalGroup")

  def apply(persistenceId: PersistenceId): EventSourcedBehavior[Command, Event, PrincipalGroupEntity] =
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, PrincipalGroupEntity](
        persistenceId = persistenceId,
        emptyState = PrincipalGroupEntity.empty,
        commandHandler = (entity, cmd) => entity.applyCommand(cmd),
        eventHandler = (entity, evt) => entity.applyEvent(evt)
      )

  def apply(entityContext: EntityContext[Command]): Behavior[Command] =
    apply(PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId))
      .withTagger(AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2))

  implicit val principalGroupEntityFormat: Format[PrincipalGroupEntity] = Json.format
}

final case class PrincipalGroupEntity(maybeState: Option[PrincipalGroupState]) {

  import PrincipalGroupEntity._

  def applyCommand(cmd: Command): ReplyEffect[Event, PrincipalGroupEntity] =
    cmd match {
      case cmd: CreatePrincipalGroup            => createPrincipalGroup(cmd)
      case cmd: UpdatePrincipalGroupName        => updatePrincipalGroupName(cmd)
      case cmd: UpdatePrincipalGroupDescription => updatePrincipalGroupDescription(cmd)
      case cmd: UpdatePrincipalGroupCategory    => updatePrincipalGroupCategory(cmd)
      case cmd: DeletePrincipalGroup            => deletePrincipalGroup(cmd)
      case cmd: AssignPrincipal                 => assignPrincipal(cmd)
      case cmd: UnassignPrincipal               => unassignPrincipal(cmd)
      case GetPrincipalGroup(_, replyTo)        => getPrincipalGroup(replyTo)
    }

  def createPrincipalGroup(
    cmd: CreatePrincipalGroup
  ): ReplyEffect[Event, PrincipalGroupEntity] =
    maybeState match {
      case None    =>
        val event = cmd.transformInto[PrincipalGroupCreated]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
      case Some(_) => Effect.reply(cmd.replyTo)(AlreadyExist)
    }

  def updatePrincipalGroupName(
    cmd: UpdatePrincipalGroupName
  ): ReplyEffect[Event, PrincipalGroupEntity] =
    maybeState match {
      case None    => Effect.reply(cmd.replyTo)(NotFound)
      case Some(_) =>
        val event = cmd.transformInto[PrincipalGroupNameUpdated]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def updatePrincipalGroupDescription(
    cmd: UpdatePrincipalGroupDescription
  ): ReplyEffect[Event, PrincipalGroupEntity] =
    maybeState match {
      case None    => Effect.reply(cmd.replyTo)(NotFound)
      case Some(_) =>
        val event = cmd.transformInto[PrincipalGroupDescriptionUpdated]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def updatePrincipalGroupCategory(
    cmd: UpdatePrincipalGroupCategory
  ): ReplyEffect[Event, PrincipalGroupEntity] =
    maybeState match {
      case None    => Effect.reply(cmd.replyTo)(NotFound)
      case Some(_) =>
        val event = cmd.transformInto[PrincipalGroupCategoryUpdated]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def deletePrincipalGroup(
    cmd: DeletePrincipalGroup
  ): ReplyEffect[Event, PrincipalGroupEntity] =
    maybeState match {
      case None    => Effect.reply(cmd.replyTo)(NotFound)
      case Some(_) =>
        val event = cmd.transformInto[PrincipalGroupDeleted]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def assignPrincipal(
    cmd: AssignPrincipal
  ): ReplyEffect[Event, PrincipalGroupEntity] =
    maybeState match {
      case None    => Effect.reply(cmd.replyTo)(NotFound)
      case Some(_) =>
        val event = cmd.transformInto[PrincipalAssigned]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def unassignPrincipal(
    cmd: UnassignPrincipal
  ): ReplyEffect[Event, PrincipalGroupEntity] =
    maybeState match {
      case None    => Effect.reply(cmd.replyTo)(NotFound)
      case Some(_) =>
        val event = cmd.transformInto[PrincipalUnassigned]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def getPrincipalGroup(replyTo: ActorRef[Confirmation]): ReplyEffect[Event, PrincipalGroupEntity] =
    maybeState match {
      case Some(state) => Effect.reply(replyTo)(SuccessPrincipalGroup(state.toPrincipalGroup))
      case None        => Effect.reply(replyTo)(NotFound)
    }

  def onPrincipalGroupCreated(event: PrincipalGroupCreated): PrincipalGroupEntity =
    PrincipalGroupEntity(
      Some(
        event
          .into[PrincipalGroupState]
          .withFieldConst(_.updatedAt, event.createdAt)
          .withFieldConst(_.updatedBy, event.createdBy)
          .transform
      )
    )

  def onPrincipalGroupNameUpdated(event: PrincipalGroupNameUpdated): PrincipalGroupEntity =
    PrincipalGroupEntity(
      maybeState.map(state =>
        state.copy(
          name = event.name,
          updatedAt = event.updatedAt
        )
      )
    )

  def onPrincipalGroupDescriptionUpdated(event: PrincipalGroupDescriptionUpdated): PrincipalGroupEntity =
    PrincipalGroupEntity(
      maybeState.map(state =>
        state.copy(
          description = event.description,
          updatedAt = event.updatedAt
        )
      )
    )

  def onPrincipalGroupCategoryUpdated(event: PrincipalGroupCategoryUpdated): PrincipalGroupEntity =
    PrincipalGroupEntity(
      maybeState.map(state =>
        state.copy(
          categoryId = event.categoryId,
          updatedAt = event.updatedAt
        )
      )
    )

  def onPrincipalGroupDeleted(): PrincipalGroupEntity =
    PrincipalGroupEntity(None)

  def onPrincipalAssigned(): PrincipalGroupEntity = this

  def onPrincipalUnassigned(): PrincipalGroupEntity = this

  def applyEvent(event: Event): PrincipalGroupEntity =
    event match {
      case event: PrincipalGroupCreated            => onPrincipalGroupCreated(event)
      case event: PrincipalGroupNameUpdated        => onPrincipalGroupNameUpdated(event)
      case event: PrincipalGroupDescriptionUpdated => onPrincipalGroupDescriptionUpdated(event)
      case event: PrincipalGroupCategoryUpdated    => onPrincipalGroupCategoryUpdated(event)
      case _: PrincipalGroupDeleted                => onPrincipalGroupDeleted()
      case _: PrincipalAssigned                    => onPrincipalAssigned()
      case _: PrincipalUnassigned                  => onPrincipalUnassigned()
    }

}
