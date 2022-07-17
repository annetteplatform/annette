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

package biz.lobachev.annette.service_catalog.service.service

import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl._
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.translation.MultiLanguageText
import biz.lobachev.annette.service_catalog.api.common.Icon
import biz.lobachev.annette.service_catalog.api.item._
import biz.lobachev.annette.service_catalog.service.service.model.ServiceState
import com.lightbend.lagom.scaladsl.persistence._
import io.scalaland.chimney.dsl._
import play.api.libs.json._

import java.time.OffsetDateTime

object ServiceEntity {

  trait CommandSerializable

  sealed trait Command extends CommandSerializable

  final case class CreateService(payload: CreateServicePayload, replyTo: ActorRef[Confirmation])       extends Command
  final case class UpdateService(payload: UpdateServicePayload, replyTo: ActorRef[Confirmation])       extends Command
  final case class ActivateService(payload: ActivateScopeItemPayload, replyTo: ActorRef[Confirmation]) extends Command
  final case class DeactivateService(payload: DeactivateScopeItemPayload, replyTo: ActorRef[Confirmation])
      extends Command
  final case class DeleteService(payload: DeleteScopeItemPayload, replyTo: ActorRef[Confirmation])     extends Command
  final case class GetService(id: ScopeItemId, replyTo: ActorRef[Confirmation])                        extends Command

  sealed trait Confirmation
  final case object Success                            extends Confirmation
  final case class SuccessService(entity: ServiceItem) extends Confirmation
  final case object NotFound                           extends Confirmation
  final case object AlreadyExist                       extends Confirmation

  implicit val confirmationSuccessFormat: Format[Success.type]           = Json.format
  implicit val confirmationSuccessServiceFormat: Format[SuccessService]  = Json.format
  implicit val confirmationNotFoundFormat: Format[NotFound.type]         = Json.format
  implicit val confirmationAlreadyExistFormat: Format[AlreadyExist.type] = Json.format
  implicit val confirmationFormat: Format[Confirmation]                  = Json.format[Confirmation]

  sealed trait Event extends AggregateEvent[Event] {
    override def aggregateTag: AggregateEventTagger[Event] = Event.Tag
  }

  object Event {
    val Tag: AggregateEventShards[Event] = AggregateEventTag.sharded[Event](numShards = 10)
  }

  final case class ServiceCreated(
    id: ScopeItemId,
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
    id: ScopeItemId,
    name: Option[String],
    description: Option[String],
    icon: Option[Icon],
    label: Option[MultiLanguageText],
    labelDescription: Option[MultiLanguageText],
    link: Option[ServiceLink],
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class ServiceActivated(
    id: ScopeItemId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class ServiceDeactivated(
    id: ScopeItemId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class ServiceDeleted(
    id: ScopeItemId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  implicit val serviceCreatedFormat: Format[ServiceCreated]         = Json.format
  implicit val serviceUpdatedFormat: Format[ServiceUpdated]         = Json.format
  implicit val serviceActivatedFormat: Format[ServiceActivated]     = Json.format
  implicit val serviceDeactivatedFormat: Format[ServiceDeactivated] = Json.format
  implicit val serviceDeletedFormat: Format[ServiceDeleted]         = Json.format

  val empty                           = ServiceEntity(None)
  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Service")

  def apply(persistenceId: PersistenceId): EventSourcedBehavior[Command, Event, ServiceEntity] =
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, ServiceEntity](
        persistenceId = persistenceId,
        emptyState = ServiceEntity.empty,
        commandHandler = (entity, cmd) => entity.applyCommand(cmd),
        eventHandler = (entity, evt) => entity.applyEvent(evt)
      )

  def apply(entityContext: EntityContext[Command]): Behavior[Command] =
    apply(PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId))
      .withTagger(AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2))

  implicit val serviceEntityFormat: Format[ServiceEntity] = Json.format
}

final case class ServiceEntity(maybeState: Option[ServiceState]) {

  import ServiceEntity._

  def applyCommand(cmd: Command): ReplyEffect[Event, ServiceEntity] =
    cmd match {
      case CreateService(payload, replyTo)     => createService(payload, replyTo)
      case UpdateService(payload, replyTo)     => updateService(payload, replyTo)
      case ActivateService(payload, replyTo)   => activateService(payload, replyTo)
      case DeactivateService(payload, replyTo) => deactivateService(payload, replyTo)
      case DeleteService(payload, replyTo)     => deleteService(payload, replyTo)
      case GetService(_, replyTo)              => getService(replyTo)
    }

  def createService(payload: CreateServicePayload, replyTo: ActorRef[Confirmation]): ReplyEffect[Event, ServiceEntity] =
    maybeState match {
      case None    =>
        val event = payload.transformInto[ServiceCreated]
        Effect
          .persist(event)
          .thenReply(replyTo)(_ => Success)
      case Some(_) => Effect.reply(replyTo)(AlreadyExist)
    }

  def updateService(payload: UpdateServicePayload, replyTo: ActorRef[Confirmation]): ReplyEffect[Event, ServiceEntity] =
    maybeState match {
      case None    => Effect.reply(replyTo)(NotFound)
      case Some(_) =>
        val event = payload.transformInto[ServiceUpdated]
        Effect
          .persist(event)
          .thenReply(replyTo)(_ => Success)
    }

  def activateService(
    payload: ActivateScopeItemPayload,
    replyTo: ActorRef[Confirmation]
  ): ReplyEffect[Event, ServiceEntity] =
    maybeState match {
      case None    => Effect.reply(replyTo)(NotFound)
      case Some(_) =>
        val event = payload.transformInto[ServiceActivated]
        Effect
          .persist(event)
          .thenReply(replyTo)(_ => Success)
    }

  def deactivateService(
    payload: DeactivateScopeItemPayload,
    replyTo: ActorRef[Confirmation]
  ): ReplyEffect[Event, ServiceEntity] =
    maybeState match {
      case None    => Effect.reply(replyTo)(NotFound)
      case Some(_) =>
        val event = payload.transformInto[ServiceDeactivated]
        Effect
          .persist(event)
          .thenReply(replyTo)(_ => Success)
    }

  def deleteService(
    payload: DeleteScopeItemPayload,
    replyTo: ActorRef[Confirmation]
  ): ReplyEffect[Event, ServiceEntity] =
    maybeState match {
      case None    => Effect.reply(replyTo)(NotFound)
      case Some(_) =>
        val event = payload
          .into[ServiceDeleted]
          .withFieldComputed(_.updatedBy, _.deletedBy)
          .transform
        Effect
          .persist(event)
          .thenReply(replyTo)(_ => Success)
    }

  def getService(replyTo: ActorRef[Confirmation]): ReplyEffect[Event, ServiceEntity] =
    maybeState match {
      case Some(state) => Effect.reply(replyTo)(SuccessService(state.toService()))
      case None        => Effect.reply(replyTo)(NotFound)
    }

  def onServiceCreated(event: ServiceCreated): ServiceEntity =
    ServiceEntity(
      Some(
        event
          .into[ServiceState]
          .withFieldConst(_.active, true)
          .withFieldConst(_.updatedAt, event.createdAt)
          .withFieldConst(_.updatedBy, event.createdBy)
          .transform
      )
    )

  def onServiceUpdated(event: ServiceUpdated): ServiceEntity =
    ServiceEntity(
      maybeState.map(s =>
        s.copy(
          name = event.name.getOrElse(s.name),
          description = event.description.getOrElse(s.description),
          icon = event.icon.getOrElse(s.icon),
          label = event.label.getOrElse(s.label),
          labelDescription = event.labelDescription.getOrElse(s.labelDescription),
          link = event.link.getOrElse(s.link),
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

  def onServiceActivated(event: ServiceActivated): ServiceEntity =
    ServiceEntity(maybeState.map(_.copy(active = true, updatedBy = event.updatedBy, updatedAt = event.updatedAt)))

  def onServiceDeactivated(event: ServiceDeactivated): ServiceEntity =
    ServiceEntity(maybeState.map(_.copy(active = false, updatedBy = event.updatedBy, updatedAt = event.updatedAt)))

  def onServiceDeleted(): ServiceEntity =
    ServiceEntity(None)

  def applyEvent(event: Event): ServiceEntity =
    event match {
      case event: ServiceCreated     => onServiceCreated(event)
      case event: ServiceUpdated     => onServiceUpdated(event)
      case event: ServiceActivated   => onServiceActivated(event)
      case event: ServiceDeactivated => onServiceDeactivated(event)
      case _: ServiceDeleted         => onServiceDeleted()
    }

}
