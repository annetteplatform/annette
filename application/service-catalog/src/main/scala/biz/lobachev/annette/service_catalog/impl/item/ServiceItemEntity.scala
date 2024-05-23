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

package biz.lobachev.annette.service_catalog.impl.item

import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl._
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.text.{Icon, MultiLanguageText}
import biz.lobachev.annette.service_catalog.api.item._
import biz.lobachev.annette.service_catalog.impl.item.model.{GroupState, ServiceItemState, ServiceState}
import com.lightbend.lagom.scaladsl.persistence._
import biz.lobachev.annette.core.utils.ChimneyCommons._
import io.scalaland.chimney.dsl._
import play.api.libs.json._

import java.time.OffsetDateTime

object ServiceItemEntity {

  trait CommandSerializable

  sealed trait Command extends CommandSerializable

  final case class CreateGroup(payload: CreateGroupPayload, replyTo: ActorRef[Confirmation])             extends Command
  final case class UpdateGroup(payload: UpdateGroupPayload, replyTo: ActorRef[Confirmation])             extends Command
  final case class CreateService(payload: CreateServicePayload, replyTo: ActorRef[Confirmation])         extends Command
  final case class UpdateService(payload: UpdateServicePayload, replyTo: ActorRef[Confirmation])         extends Command
  final case class ActivateServiceItem(payload: ActivateServiceItemPayload, replyTo: ActorRef[Confirmation])
      extends Command
  final case class DeactivateServiceItem(payload: DeactivateServiceItemPayload, replyTo: ActorRef[Confirmation])
      extends Command
  final case class DeleteServiceItem(payload: DeleteServiceItemPayload, replyTo: ActorRef[Confirmation]) extends Command
  final case class GetServiceItem(id: ServiceItemId, replyTo: ActorRef[Confirmation])                    extends Command

  sealed trait Confirmation
  final case object Success                                extends Confirmation
  final case class SuccessServiceItem(entity: ServiceItem) extends Confirmation
  final case object NotFound                               extends Confirmation
  final case object AlreadyExist                           extends Confirmation
  final case object IsNotGroup                             extends Confirmation
  final case object IsNotService                           extends Confirmation

  implicit val confirmationSuccessFormat: Format[Success.type]              = Json.format
  implicit val confirmationSuccessServiceFormat: Format[SuccessServiceItem] = Json.format
  implicit val confirmationNotFoundFormat: Format[NotFound.type]            = Json.format
  implicit val confirmationAlreadyExistFormat: Format[AlreadyExist.type]    = Json.format
  implicit val confirmationIsNotGroupFormat: Format[IsNotGroup.type]        = Json.format
  implicit val confirmationIsNotServiceFormat: Format[IsNotService.type]    = Json.format
  implicit val confirmationFormat: Format[Confirmation]                     = Json.format[Confirmation]

  sealed trait Event extends AggregateEvent[Event] {
    override def aggregateTag: AggregateEventTagger[Event] = Event.Tag
  }

  object Event {
    val Tag: AggregateEventShards[Event] = AggregateEventTag.sharded[Event](numShards = 10)
  }

  final case class GroupCreated(
    id: ServiceItemId,
    name: String,
    description: String,
    icon: Icon,
    label: MultiLanguageText,
    labelDescription: MultiLanguageText,
    children: Seq[ServiceItemId],
    createdBy: AnnettePrincipal,
    createdAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class GroupUpdated(
    id: ServiceItemId,
    name: Option[String],
    description: Option[String],
    icon: Option[Icon],
    label: Option[MultiLanguageText],
    labelDescription: Option[MultiLanguageText],
    children: Option[Seq[ServiceItemId]],
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class ServiceCreated(
    id: ServiceItemId,
    name: String,
    description: String,
    icon: Icon,
    label: MultiLanguageText,
    labelDescription: MultiLanguageText,
    link: ServiceLink,
    createdBy: AnnettePrincipal,
    createdAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class ServiceUpdated(
    id: ServiceItemId,
    name: Option[String],
    description: Option[String],
    icon: Option[Icon],
    label: Option[MultiLanguageText],
    labelDescription: Option[MultiLanguageText],
    link: Option[ServiceLink],
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class ServiceItemActivated(
    id: ServiceItemId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class ServiceItemDeactivated(
    id: ServiceItemId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class ServiceItemDeleted(
    id: ServiceItemId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  implicit val groupCreatedFormat: Format[GroupCreated]                 = Json.format
  implicit val groupUpdatedFormat: Format[GroupUpdated]                 = Json.format
  implicit val serviceCreatedFormat: Format[ServiceCreated]             = Json.format
  implicit val serviceUpdatedFormat: Format[ServiceUpdated]             = Json.format
  implicit val serviceActivatedFormat: Format[ServiceItemActivated]     = Json.format
  implicit val serviceDeactivatedFormat: Format[ServiceItemDeactivated] = Json.format
  implicit val serviceDeletedFormat: Format[ServiceItemDeleted]         = Json.format

  val empty                           = ServiceItemEntity(None)
  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Service")

  def apply(persistenceId: PersistenceId): EventSourcedBehavior[Command, Event, ServiceItemEntity] =
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, ServiceItemEntity](
        persistenceId = persistenceId,
        emptyState = ServiceItemEntity.empty,
        commandHandler = (entity, cmd) => entity.applyCommand(cmd),
        eventHandler = (entity, evt) => entity.applyEvent(evt)
      )

  def apply(entityContext: EntityContext[Command]): Behavior[Command] =
    apply(PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId))
      .withTagger(AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2))

  implicit val serviceEntityFormat: Format[ServiceItemEntity] = Json.format
}

final case class ServiceItemEntity(maybeState: Option[ServiceItemState]) {

  import ServiceItemEntity._

  def applyCommand(cmd: Command): ReplyEffect[Event, ServiceItemEntity] =
    cmd match {
      case CreateGroup(payload, replyTo)           => createGroup(payload, replyTo)
      case UpdateGroup(payload, replyTo)           => updateGroup(payload, replyTo)
      case CreateService(payload, replyTo)         => createService(payload, replyTo)
      case UpdateService(payload, replyTo)         => updateService(payload, replyTo)
      case ActivateServiceItem(payload, replyTo)   => activateService(payload, replyTo)
      case DeactivateServiceItem(payload, replyTo) => deactivateService(payload, replyTo)
      case DeleteServiceItem(payload, replyTo)     => deleteService(payload, replyTo)
      case GetServiceItem(_, replyTo)              => getService(replyTo)
    }

  def createGroup(payload: CreateGroupPayload, replyTo: ActorRef[Confirmation]): ReplyEffect[Event, ServiceItemEntity] =
    maybeState match {
      case None    =>
        val event = payload.transformInto[GroupCreated]
        Effect
          .persist(event)
          .thenReply(replyTo)(_ => Success)
      case Some(_) => Effect.reply(replyTo)(AlreadyExist)
    }

  def updateGroup(payload: UpdateGroupPayload, replyTo: ActorRef[Confirmation]): ReplyEffect[Event, ServiceItemEntity] =
    maybeState match {
      case None                   => Effect.reply(replyTo)(NotFound)
      case Some(item: GroupState) =>
        val event = GroupUpdated(
          id = payload.id,
          name = if (Some(item.name) == payload.name) None else payload.name,
          description = if (Some(item.description) == payload.description) None else payload.description,
          icon = if (Some(item.icon) == payload.icon) None else payload.icon,
          label = if (Some(item.label) == payload.label) None else payload.label,
          labelDescription =
            if (Some(item.labelDescription) == payload.labelDescription) None else payload.labelDescription,
          children = if (Some(item.children) == payload.children) None else payload.children,
          updatedBy = payload.updatedBy
        )
        Effect
          .persist(event)
          .thenReply(replyTo)(_ => Success)
      case _                      => Effect.reply(replyTo)(IsNotGroup)
    }

  def createService(
    payload: CreateServicePayload,
    replyTo: ActorRef[Confirmation]
  ): ReplyEffect[Event, ServiceItemEntity] =
    maybeState match {
      case None    =>
        val event = payload.transformInto[ServiceCreated]
        Effect
          .persist(event)
          .thenReply(replyTo)(_ => Success)
      case Some(_) => Effect.reply(replyTo)(AlreadyExist)
    }

  def updateService(
    payload: UpdateServicePayload,
    replyTo: ActorRef[Confirmation]
  ): ReplyEffect[Event, ServiceItemEntity] =
    maybeState match {
      case None                     => Effect.reply(replyTo)(NotFound)
      case Some(item: ServiceState) =>
        val event = ServiceUpdated(
          id = payload.id,
          name = if (Some(item.name) == payload.name) None else payload.name,
          description = if (Some(item.description) == payload.description) None else payload.description,
          icon = if (Some(item.icon) == payload.icon) None else payload.icon,
          label = if (Some(item.label) == payload.label) None else payload.label,
          labelDescription =
            if (Some(item.labelDescription) == payload.labelDescription) None else payload.labelDescription,
          link = if (Some(item.link) == payload.link) None else payload.link,
          updatedBy = payload.updatedBy
        )
        Effect
          .persist(event)
          .thenReply(replyTo)(_ => Success)
      case _                        => Effect.reply(replyTo)(IsNotService)
    }

  def activateService(
    payload: ActivateServiceItemPayload,
    replyTo: ActorRef[Confirmation]
  ): ReplyEffect[Event, ServiceItemEntity] =
    maybeState match {
      case None    => Effect.reply(replyTo)(NotFound)
      case Some(_) =>
        val event = payload.transformInto[ServiceItemActivated]
        Effect
          .persist(event)
          .thenReply(replyTo)(_ => Success)
    }

  def deactivateService(
    payload: DeactivateServiceItemPayload,
    replyTo: ActorRef[Confirmation]
  ): ReplyEffect[Event, ServiceItemEntity] =
    maybeState match {
      case None    => Effect.reply(replyTo)(NotFound)
      case Some(_) =>
        val event = payload.transformInto[ServiceItemDeactivated]
        Effect
          .persist(event)
          .thenReply(replyTo)(_ => Success)
    }

  def deleteService(
    payload: DeleteServiceItemPayload,
    replyTo: ActorRef[Confirmation]
  ): ReplyEffect[Event, ServiceItemEntity] =
    maybeState match {
      case None    => Effect.reply(replyTo)(NotFound)
      case Some(_) =>
        val event = payload
          .into[ServiceItemDeleted]
          .withFieldComputed(_.updatedBy, _.deletedBy)
          .transform
        Effect
          .persist(event)
          .thenReply(replyTo)(_ => Success)
    }

  def getService(replyTo: ActorRef[Confirmation]): ReplyEffect[Event, ServiceItemEntity] =
    maybeState match {
      case Some(state) => Effect.reply(replyTo)(SuccessServiceItem(state.toServiceItem()))
      case None        => Effect.reply(replyTo)(NotFound)
    }

  def onGroupCreated(event: GroupCreated): ServiceItemEntity =
    ServiceItemEntity(
      Some(
        GroupState(
          id = event.id,
          name = event.name,
          description = event.description,
          icon = event.icon,
          label = event.label,
          labelDescription = event.labelDescription,
          children = event.children,
          active = true,
          updatedBy = event.createdBy,
          updatedAt = event.createdAt
        )
      )
    )

  def onGroupUpdated(event: GroupUpdated): ServiceItemEntity =
    ServiceItemEntity(
      maybeState.map { s =>
        GroupState(
          id = event.id,
          name = event.name.getOrElse(s.name),
          description = event.description.getOrElse(s.description),
          icon = event.icon.getOrElse(s.icon),
          label = event.label.getOrElse(s.label),
          labelDescription = event.labelDescription.getOrElse(s.labelDescription),
          children = event.children.getOrElse {
            s match {
              case s: GroupState => s.children
              case _             => Seq.empty
            }
          },
          active = s.active,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      }
    )

  def onServiceCreated(event: ServiceCreated): ServiceItemEntity =
    ServiceItemEntity(
      Some(
        ServiceState(
          id = event.id,
          name = event.name,
          description = event.description,
          icon = event.icon,
          label = event.label,
          labelDescription = event.labelDescription,
          link = event.link,
          active = true,
          updatedBy = event.createdBy,
          updatedAt = event.createdAt
        )
      )
    )

  def onServiceUpdated(event: ServiceUpdated): ServiceItemEntity =
    ServiceItemEntity(
      maybeState.map { s =>
        ServiceState(
          id = event.id,
          name = event.name.getOrElse(s.name),
          description = event.description.getOrElse(s.description),
          icon = event.icon.getOrElse(s.icon),
          label = event.label.getOrElse(s.label),
          labelDescription = event.labelDescription.getOrElse(s.labelDescription),
          link = event.link.getOrElse(s.asInstanceOf[ServiceState].link),
          active = s.active,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      }
    )

  def onServiceActivated(event: ServiceItemActivated): ServiceItemEntity =
    ServiceItemEntity(
      maybeState.map {
        case s: ServiceState => s.copy(active = true, updatedBy = event.updatedBy, updatedAt = event.updatedAt)
        case g: GroupState   => g.copy(active = true, updatedBy = event.updatedBy, updatedAt = event.updatedAt)
      }
    )

  def onServiceDeactivated(event: ServiceItemDeactivated): ServiceItemEntity =
    ServiceItemEntity(
      maybeState.map {
        case s: ServiceState => s.copy(active = false, updatedBy = event.updatedBy, updatedAt = event.updatedAt)
        case g: GroupState   => g.copy(active = false, updatedBy = event.updatedBy, updatedAt = event.updatedAt)
      }
    )

  def onServiceDeleted(): ServiceItemEntity =
    ServiceItemEntity(None)

  def applyEvent(event: Event): ServiceItemEntity =
    event match {
      case event: GroupCreated           => onGroupCreated(event)
      case event: GroupUpdated           => onGroupUpdated(event)
      case event: ServiceCreated         => onServiceCreated(event)
      case event: ServiceUpdated         => onServiceUpdated(event)
      case event: ServiceItemActivated   => onServiceActivated(event)
      case event: ServiceItemDeactivated => onServiceDeactivated(event)
      case _: ServiceItemDeleted         => onServiceDeleted()
    }

}
