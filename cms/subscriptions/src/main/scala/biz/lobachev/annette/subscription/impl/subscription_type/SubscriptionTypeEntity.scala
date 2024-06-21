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

package biz.lobachev.annette.subscription.impl.subscription_type

import java.time.OffsetDateTime
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl._
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.subscription.api.subscription_type._
import biz.lobachev.annette.subscription.impl.subscription_type.model.SubscriptionTypeState
import com.lightbend.lagom.scaladsl.persistence._
import biz.lobachev.annette.core.utils.ChimneyCommons._
import io.scalaland.chimney.dsl._
import play.api.libs.json.{Format, _}

object SubscriptionTypeEntity {

  trait CommandSerializable

  sealed trait Command extends CommandSerializable

  final case class CreateSubscriptionType(payload: CreateSubscriptionTypePayload, replyTo: ActorRef[Confirmation])
      extends Command
  final case class UpdateSubscriptionType(payload: UpdateSubscriptionTypePayload, replyTo: ActorRef[Confirmation])
      extends Command
  final case class DeleteSubscriptionType(payload: DeleteSubscriptionTypePayload, replyTo: ActorRef[Confirmation])
      extends Command
  final case class GetSubscriptionType(id: SubscriptionTypeId, replyTo: ActorRef[Confirmation]) extends Command

  sealed trait Confirmation
  final case object Success                                          extends Confirmation
  final case class SuccessSubscriptionType(entity: SubscriptionType) extends Confirmation
  final case object NotFound                                         extends Confirmation
  final case object AlreadyExist                                     extends Confirmation

  implicit val confirmationSuccessFormat: Format[Success.type]                            = Json.format
  implicit val confirmationSuccessSubscriptionTypeFormat: Format[SuccessSubscriptionType] = Json.format
  implicit val confirmationNotFoundFormat: Format[NotFound.type]                          = Json.format
  implicit val confirmationAlreadyExistFormat: Format[AlreadyExist.type]                  = Json.format

  implicit val confirmationFormat: Format[Confirmation] = Json.format

  sealed trait Event extends AggregateEvent[Event] {
    override def aggregateTag: AggregateEventTagger[Event] = Event.Tag
  }

  object Event {
    val Tag: AggregateEventShards[Event] = AggregateEventTag.sharded[Event](numShards = 10)
  }

  final case class SubscriptionTypeCreated(
    id: SubscriptionTypeId,
    name: String,
    createdBy: AnnettePrincipal,
    createdAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class SubscriptionTypeUpdated(
    id: SubscriptionTypeId,
    name: String,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class SubscriptionTypeDeleted(
    id: SubscriptionTypeId, // subscriptionType id
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  implicit val subscriptionTypeCreatedFormat: Format[SubscriptionTypeCreated]     = Json.format
  implicit val subscriptionTypeUpdatedFormat: Format[SubscriptionTypeUpdated]     = Json.format
  implicit val subscriptionTypeDeactivatedFormat: Format[SubscriptionTypeDeleted] = Json.format

  val empty                           = SubscriptionTypeEntity(None)
  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("SubscriptionType")

  def apply(persistenceId: PersistenceId): EventSourcedBehavior[Command, Event, SubscriptionTypeEntity] =
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, SubscriptionTypeEntity](
        persistenceId = persistenceId,
        emptyState = SubscriptionTypeEntity.empty,
        commandHandler = (entity, cmd) => entity.applyCommand(cmd),
        eventHandler = (entity, evt) => entity.applyEvent(evt)
      )

  def apply(entityContext: EntityContext[Command]): Behavior[Command] =
    apply(PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId))
      .withTagger(AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2))

  implicit val subscriptionTypeEntityFormat: Format[SubscriptionTypeEntity] = Json.format
}

final case class SubscriptionTypeEntity(maybeState: Option[SubscriptionTypeState]) {

  import SubscriptionTypeEntity._

  def applyCommand(cmd: Command): ReplyEffect[Event, SubscriptionTypeEntity] =
    cmd match {
      case CreateSubscriptionType(payload, replyTo) => createSubscriptionType(payload, replyTo)
      case UpdateSubscriptionType(payload, replyTo) => updateSubscriptionType(payload, replyTo)
      case DeleteSubscriptionType(payload, replyTo) => deleteSubscriptionType(payload, replyTo)
      case GetSubscriptionType(_, replyTo)          => getSubscriptionType(replyTo)
    }

  def createSubscriptionType(
    payload: CreateSubscriptionTypePayload,
    replyTo: ActorRef[Confirmation]
  ): ReplyEffect[Event, SubscriptionTypeEntity] =
    maybeState match {
      case None    =>
        val event = payload.transformInto[SubscriptionTypeCreated]
        Effect
          .persist(event)
          .thenReply(replyTo)(_ => Success)
      case Some(_) => Effect.reply(replyTo)(AlreadyExist)
    }

  def updateSubscriptionType(
    payload: UpdateSubscriptionTypePayload,
    replyTo: ActorRef[Confirmation]
  ): ReplyEffect[Event, SubscriptionTypeEntity] =
    maybeState match {
      case None    => Effect.reply(replyTo)(NotFound)
      case Some(_) =>
        val event = payload.transformInto[SubscriptionTypeUpdated]
        Effect
          .persist(event)
          .thenReply(replyTo)(_ => Success)
    }

  def deleteSubscriptionType(
    payload: DeleteSubscriptionTypePayload,
    replyTo: ActorRef[Confirmation]
  ): ReplyEffect[Event, SubscriptionTypeEntity] =
    maybeState match {
      case None    => Effect.reply(replyTo)(NotFound)
      case Some(_) =>
        val event = payload.transformInto[SubscriptionTypeDeleted]
        Effect
          .persist(event)
          .thenReply(replyTo)(_ => Success)
    }

  def getSubscriptionType(replyTo: ActorRef[Confirmation]): ReplyEffect[Event, SubscriptionTypeEntity] =
    maybeState match {
      case Some(state) => Effect.reply(replyTo)(SuccessSubscriptionType(state.toSubscriptionType))
      case None        => Effect.reply(replyTo)(NotFound)
    }

  def applyEvent(evt: Event): SubscriptionTypeEntity =
    evt match {
      case event: SubscriptionTypeCreated => onSubscriptionTypeCreated(event)
      case event: SubscriptionTypeUpdated => onSubscriptionTypeUpdated(event)
      case _: SubscriptionTypeDeleted     => onSubscriptionTypeDeleted()
    }

  def onSubscriptionTypeCreated(event: SubscriptionTypeCreated): SubscriptionTypeEntity =
    SubscriptionTypeEntity(
      Some(
        event
          .into[SubscriptionTypeState]
          .withFieldConst(_.updatedAt, event.createdAt)
          .withFieldConst(_.updatedBy, event.createdBy)
          .transform
      )
    )

  def onSubscriptionTypeUpdated(event: SubscriptionTypeUpdated): SubscriptionTypeEntity =
    SubscriptionTypeEntity(
      Some(
        event
          .into[SubscriptionTypeState]
          .withFieldConst(_.updatedAt, event.updatedAt)
          .transform
      )
    )

  def onSubscriptionTypeDeleted(): SubscriptionTypeEntity =
    SubscriptionTypeEntity(None)

}
