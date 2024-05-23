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

package biz.lobachev.annette.service_catalog.impl.scope

import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl._
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.category.CategoryId
import biz.lobachev.annette.service_catalog.api.item.ServiceItemId
import biz.lobachev.annette.service_catalog.api.scope._
import biz.lobachev.annette.service_catalog.impl.scope.model.ScopeState
import com.lightbend.lagom.scaladsl.persistence._
import biz.lobachev.annette.core.utils.ChimneyCommons._
import io.scalaland.chimney.dsl._
import play.api.libs.json._

import java.time.OffsetDateTime

object ScopeEntity {

  trait CommandSerializable

  sealed trait Command extends CommandSerializable

  final case class CreateScope(payload: CreateScopePayload, replyTo: ActorRef[Confirmation])         extends Command
  final case class UpdateScope(payload: UpdateScopePayload, replyTo: ActorRef[Confirmation])         extends Command
  final case class ActivateScope(payload: ActivateScopePayload, replyTo: ActorRef[Confirmation])     extends Command
  final case class DeactivateScope(payload: DeactivateScopePayload, replyTo: ActorRef[Confirmation]) extends Command
  final case class DeleteScope(payload: DeleteScopePayload, replyTo: ActorRef[Confirmation])         extends Command
  final case class GetScope(id: ScopeId, replyTo: ActorRef[Confirmation])                            extends Command

  sealed trait Confirmation
  final case object Success                    extends Confirmation
  final case class SuccessScope(entity: Scope) extends Confirmation
  final case object NotFound                   extends Confirmation
  final case object AlreadyExist               extends Confirmation

  implicit val confirmationSuccessFormat: Format[Success.type]           = Json.format
  implicit val confirmationSuccessScopeFormat: Format[SuccessScope]      = Json.format
  implicit val confirmationNotFoundFormat: Format[NotFound.type]         = Json.format
  implicit val confirmationAlreadyExistFormat: Format[AlreadyExist.type] = Json.format
  implicit val confirmationFormat: Format[Confirmation]                  = Json.format[Confirmation]

  sealed trait Event extends AggregateEvent[Event] {
    override def aggregateTag: AggregateEventTagger[Event] = Event.Tag
  }

  object Event {
    val Tag: AggregateEventShards[Event] = AggregateEventTag.sharded[Event](numShards = 10)
  }

  final case class ScopeCreated(
    id: ScopeId,
    name: String,
    description: String,
    categoryId: CategoryId,
    children: Seq[ServiceItemId] = Seq.empty,
    createdBy: AnnettePrincipal,
    createdAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class ScopeUpdated(
    id: ScopeId,
    name: Option[String],
    description: Option[String],
    categoryId: Option[CategoryId],
    children: Option[Seq[ServiceItemId]],
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class ScopeActivated(
    id: ScopeId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class ScopeDeactivated(
    id: ScopeId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class ScopeDeleted(
    id: ScopeId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  implicit val scopeCreatedFormat: Format[ScopeCreated]         = Json.format
  implicit val scopeUpdatedFormat: Format[ScopeUpdated]         = Json.format
  implicit val scopeActivatedFormat: Format[ScopeActivated]     = Json.format
  implicit val scopeDeactivatedFormat: Format[ScopeDeactivated] = Json.format
  implicit val scopeDeletedFormat: Format[ScopeDeleted]         = Json.format

  val empty                           = ScopeEntity(None)
  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Scope")

  def apply(persistenceId: PersistenceId): EventSourcedBehavior[Command, Event, ScopeEntity] =
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, ScopeEntity](
        persistenceId = persistenceId,
        emptyState = ScopeEntity.empty,
        commandHandler = (entity, cmd) => entity.applyCommand(cmd),
        eventHandler = (entity, evt) => entity.applyEvent(evt)
      )

  def apply(entityContext: EntityContext[Command]): Behavior[Command] =
    apply(PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId))
      .withTagger(AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2))

  implicit val scopeEntityFormat: Format[ScopeEntity] = Json.format
}

final case class ScopeEntity(maybeState: Option[ScopeState]) {

  import ScopeEntity._

  def applyCommand(cmd: Command): ReplyEffect[Event, ScopeEntity] =
    cmd match {
      case CreateScope(payload, replyTo)     => createScope(payload, replyTo)
      case UpdateScope(payload, replyTo)     => updateScope(payload, replyTo)
      case ActivateScope(payload, replyTo)   => activateScope(payload, replyTo)
      case DeactivateScope(payload, replyTo) => deactivateScope(payload, replyTo)
      case DeleteScope(payload, replyTo)     => deleteScope(payload, replyTo)
      case GetScope(_, replyTo)              => getScope(replyTo)
    }

  def createScope(payload: CreateScopePayload, replyTo: ActorRef[Confirmation]): ReplyEffect[Event, ScopeEntity] =
    maybeState match {
      case None    =>
        val event = payload.transformInto[ScopeCreated]
        Effect
          .persist(event)
          .thenReply(replyTo)(_ => Success)
      case Some(_) => Effect.reply(replyTo)(AlreadyExist)
    }

  def updateScope(payload: UpdateScopePayload, replyTo: ActorRef[Confirmation]): ReplyEffect[Event, ScopeEntity] =
    maybeState match {
      case None    => Effect.reply(replyTo)(NotFound)
      case Some(_) =>
        val event = payload.transformInto[ScopeUpdated]
        Effect
          .persist(event)
          .thenReply(replyTo)(_ => Success)
    }

  def activateScope(payload: ActivateScopePayload, replyTo: ActorRef[Confirmation]): ReplyEffect[Event, ScopeEntity] =
    maybeState match {
      case None    => Effect.reply(replyTo)(NotFound)
      case Some(_) =>
        val event = payload.transformInto[ScopeActivated]
        Effect
          .persist(event)
          .thenReply(replyTo)(_ => Success)
    }

  def deactivateScope(
    payload: DeactivateScopePayload,
    replyTo: ActorRef[Confirmation]
  ): ReplyEffect[Event, ScopeEntity] =
    maybeState match {
      case None    => Effect.reply(replyTo)(NotFound)
      case Some(_) =>
        val event = payload.transformInto[ScopeDeactivated]
        Effect
          .persist(event)
          .thenReply(replyTo)(_ => Success)
    }

  def deleteScope(
    payload: DeleteScopePayload,
    replyTo: ActorRef[Confirmation]
  ): ReplyEffect[Event, ScopeEntity] =
    maybeState match {
      case None    => Effect.reply(replyTo)(NotFound)
      case Some(_) =>
        val event = payload
          .into[ScopeDeleted]
          .withFieldComputed(_.updatedBy, _.deletedBy)
          .transform
        Effect
          .persist(event)
          .thenReply(replyTo)(_ => Success)
    }

  def getScope(replyTo: ActorRef[Confirmation]): ReplyEffect[Event, ScopeEntity] =
    maybeState match {
      case Some(state) => Effect.reply(replyTo)(SuccessScope(state.toScope()))
      case None        => Effect.reply(replyTo)(NotFound)
    }

  def onScopeCreated(event: ScopeCreated): ScopeEntity =
    ScopeEntity(
      Some(
        event
          .into[ScopeState]
          .withFieldConst(_.active, true)
          .withFieldConst(_.updatedAt, event.createdAt)
          .withFieldConst(_.updatedBy, event.createdBy)
          .transform
      )
    )

  def onScopeUpdated(event: ScopeUpdated): ScopeEntity =
    ScopeEntity(
      maybeState.map(s =>
        s.copy(
          name = event.name.getOrElse(s.name),
          description = event.description.getOrElse(s.description),
          categoryId = event.categoryId.getOrElse(s.categoryId),
          children = event.children.getOrElse(s.children),
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

  def onScopeActivated(event: ScopeActivated): ScopeEntity =
    ScopeEntity(maybeState.map(_.copy(active = true, updatedBy = event.updatedBy, updatedAt = event.updatedAt)))

  def onScopeDeactivated(event: ScopeDeactivated): ScopeEntity =
    ScopeEntity(maybeState.map(_.copy(active = false, updatedBy = event.updatedBy, updatedAt = event.updatedAt)))

  def onScopeDeleted(): ScopeEntity =
    ScopeEntity(None)

  def applyEvent(event: Event): ScopeEntity =
    event match {
      case event: ScopeCreated     => onScopeCreated(event)
      case event: ScopeUpdated     => onScopeUpdated(event)
      case event: ScopeActivated   => onScopeActivated(event)
      case event: ScopeDeactivated => onScopeDeactivated(event)
      case _: ScopeDeleted         => onScopeDeleted()
    }

}
