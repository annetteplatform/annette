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

package biz.lobachev.annette.service_catalog.service.group

import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl._
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.translation.MultiLanguageText
import biz.lobachev.annette.service_catalog.api.common.Icon
import biz.lobachev.annette.service_catalog.api.group._
import biz.lobachev.annette.service_catalog.api.item.{CreateGroupPayload, ScopeItemId, UpdateGroupPayload}
import biz.lobachev.annette.service_catalog.service.group.model.GroupState
import com.lightbend.lagom.scaladsl.persistence._
import io.scalaland.chimney.dsl._
import play.api.libs.json._

import java.time.OffsetDateTime

object GroupEntity {

  trait CommandSerializable

  sealed trait Command extends CommandSerializable

  final case class CreateGroup(payload: CreateGroupPayload, replyTo: ActorRef[Confirmation])         extends Command
  final case class UpdateGroup(payload: UpdateGroupPayload, replyTo: ActorRef[Confirmation])         extends Command
  final case class ActivateGroup(payload: ActivateGroupPayload, replyTo: ActorRef[Confirmation])     extends Command
  final case class DeactivateGroup(payload: DeactivateGroupPayload, replyTo: ActorRef[Confirmation]) extends Command
  final case class DeleteGroup(payload: DeleteGroupPayload, replyTo: ActorRef[Confirmation])         extends Command
  final case class GetGroup(id: GroupId, replyTo: ActorRef[Confirmation])                            extends Command

  sealed trait Confirmation
  final case object Success                    extends Confirmation
  final case class SuccessGroup(entity: Group) extends Confirmation
  final case object NotFound                   extends Confirmation
  final case object AlreadyExist               extends Confirmation

  implicit val confirmationSuccessFormat: Format[Success.type]           = Json.format
  implicit val confirmationSuccessGroupFormat: Format[SuccessGroup]      = Json.format
  implicit val confirmationNotFoundFormat: Format[NotFound.type]         = Json.format
  implicit val confirmationAlreadyExistFormat: Format[AlreadyExist.type] = Json.format
  implicit val confirmationFormat: Format[Confirmation]                  = Json.format[Confirmation]

  sealed trait Event extends AggregateEvent[Event] {
    override def aggregateTag: AggregateEventTagger[Event] = Event.Tag
  }

  object Event {
    val Tag: AggregateEventShards[Event] = AggregateEventTag.sharded[Event](numShards = 10)
  }

  final case class GroupCreated(
    id: GroupId,
    name: String,
    description: String,
    icon: Icon,
    label: MultiLanguageText,
    labelDescription: MultiLanguageText,
    services: Seq[ScopeItemId] = Seq.empty,
    createdBy: AnnettePrincipal,
    createdAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class GroupUpdated(
    id: GroupId,
    name: Option[String],
    description: Option[String],
    icon: Option[Icon],
    label: Option[MultiLanguageText],
    labelDescription: Option[MultiLanguageText],
    services: Option[Seq[ScopeItemId]],
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class GroupActivated(
    id: GroupId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class GroupDeactivated(
    id: GroupId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class GroupDeleted(
    id: GroupId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  implicit val groupCreatedFormat: Format[GroupCreated]         = Json.format
  implicit val groupUpdatedFormat: Format[GroupUpdated]         = Json.format
  implicit val groupActivatedFormat: Format[GroupActivated]     = Json.format
  implicit val groupDeactivatedFormat: Format[GroupDeactivated] = Json.format
  implicit val groupDeletedFormat: Format[GroupDeleted]         = Json.format

  val empty                           = GroupEntity(None)
  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Group")

  def apply(persistenceId: PersistenceId): EventSourcedBehavior[Command, Event, GroupEntity] =
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, GroupEntity](
        persistenceId = persistenceId,
        emptyState = GroupEntity.empty,
        commandHandler = (entity, cmd) => entity.applyCommand(cmd),
        eventHandler = (entity, evt) => entity.applyEvent(evt)
      )

  def apply(entityContext: EntityContext[Command]): Behavior[Command] =
    apply(PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId))
      .withTagger(AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2))

  implicit val groupEntityFormat: Format[GroupEntity] = Json.format
}

final case class GroupEntity(maybeState: Option[GroupState]) {

  import GroupEntity._

  def applyCommand(cmd: Command): ReplyEffect[Event, GroupEntity] =
    cmd match {
      case CreateGroup(payload, replyTo)     => createGroup(payload, replyTo)
      case UpdateGroup(payload, replyTo)     => updateGroup(payload, replyTo)
      case ActivateGroup(payload, replyTo)   => activateGroup(payload, replyTo)
      case DeactivateGroup(payload, replyTo) => deactivateGroup(payload, replyTo)
      case DeleteGroup(payload, replyTo)     => deleteGroup(payload, replyTo)
      case GetGroup(_, replyTo)              => getGroup(replyTo)
    }

  def createGroup(payload: CreateGroupPayload, replyTo: ActorRef[Confirmation]): ReplyEffect[Event, GroupEntity] =
    maybeState match {
      case None    =>
        val event = payload.transformInto[GroupCreated]
        Effect
          .persist(event)
          .thenReply(replyTo)(_ => Success)
      case Some(_) => Effect.reply(replyTo)(AlreadyExist)
    }

  def updateGroup(payload: UpdateGroupPayload, replyTo: ActorRef[Confirmation]): ReplyEffect[Event, GroupEntity] =
    maybeState match {
      case None    => Effect.reply(replyTo)(NotFound)
      case Some(_) =>
        val event = payload.transformInto[GroupUpdated]
        Effect
          .persist(event)
          .thenReply(replyTo)(_ => Success)
    }

  def activateGroup(payload: ActivateGroupPayload, replyTo: ActorRef[Confirmation]): ReplyEffect[Event, GroupEntity] =
    maybeState match {
      case None    => Effect.reply(replyTo)(NotFound)
      case Some(_) =>
        val event = payload.transformInto[GroupActivated]
        Effect
          .persist(event)
          .thenReply(replyTo)(_ => Success)
    }

  def deactivateGroup(
    payload: DeactivateGroupPayload,
    replyTo: ActorRef[Confirmation]
  ): ReplyEffect[Event, GroupEntity] =
    maybeState match {
      case None    => Effect.reply(replyTo)(NotFound)
      case Some(_) =>
        val event = payload.transformInto[GroupDeactivated]
        Effect
          .persist(event)
          .thenReply(replyTo)(_ => Success)
    }

  def deleteGroup(
    payload: DeleteGroupPayload,
    replyTo: ActorRef[Confirmation]
  ): ReplyEffect[Event, GroupEntity] =
    maybeState match {
      case None    => Effect.reply(replyTo)(NotFound)
      case Some(_) =>
        val event = payload
          .into[GroupDeleted]
          .withFieldComputed(_.updatedBy, _.deletedBy)
          .transform
        Effect
          .persist(event)
          .thenReply(replyTo)(_ => Success)
    }

  def getGroup(replyTo: ActorRef[Confirmation]): ReplyEffect[Event, GroupEntity] =
    maybeState match {
      case Some(state) => Effect.reply(replyTo)(SuccessGroup(state.toGroup()))
      case None        => Effect.reply(replyTo)(NotFound)
    }

  def onGroupCreated(event: GroupCreated): GroupEntity =
    GroupEntity(
      Some(
        event
          .into[GroupState]
          .withFieldConst(_.active, true)
          .withFieldConst(_.updatedAt, event.createdAt)
          .withFieldConst(_.updatedBy, event.createdBy)
          .transform
      )
    )

  def onGroupUpdated(event: GroupUpdated): GroupEntity =
    GroupEntity(
      maybeState.map(s =>
        s.copy(
          name = event.name.getOrElse(s.name),
          description = event.description.getOrElse(s.description),
          icon = event.icon.getOrElse(s.icon),
          label = event.label.getOrElse(s.label),
          labelDescription = event.labelDescription.getOrElse(s.labelDescription),
          services = event.services.getOrElse(s.services),
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

  def onGroupActivated(event: GroupActivated): GroupEntity =
    GroupEntity(maybeState.map(_.copy(active = true, updatedBy = event.updatedBy, updatedAt = event.updatedAt)))

  def onGroupDeactivated(event: GroupDeactivated): GroupEntity =
    GroupEntity(maybeState.map(_.copy(active = false, updatedBy = event.updatedBy, updatedAt = event.updatedAt)))

  def onGroupDeleted(): GroupEntity =
    GroupEntity(None)

  def applyEvent(event: Event): GroupEntity =
    event match {
      case event: GroupCreated     => onGroupCreated(event)
      case event: GroupUpdated     => onGroupUpdated(event)
      case event: GroupActivated   => onGroupActivated(event)
      case event: GroupDeactivated => onGroupDeactivated(event)
      case _: GroupDeleted         => onGroupDeleted()
    }

}
