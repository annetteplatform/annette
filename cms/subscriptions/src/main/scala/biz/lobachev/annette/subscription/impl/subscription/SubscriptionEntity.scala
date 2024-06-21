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

package biz.lobachev.annette.subscription.impl.subscription

import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl._
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.subscription.api.subscription._
import biz.lobachev.annette.subscription.api.subscription_type.SubscriptionTypeId
import biz.lobachev.annette.subscription.impl.subscription.model.SubscriptionState
import com.lightbend.lagom.scaladsl.persistence._
import biz.lobachev.annette.core.utils.ChimneyCommons._
import io.scalaland.chimney.dsl._
import play.api.libs.json.{Format, _}

import java.time.OffsetDateTime

object SubscriptionEntity {

  trait CommandSerializable

  sealed trait Command extends CommandSerializable

  final case class CreateSubscription(
    subscriptionType: SubscriptionTypeId,
    objectId: ObjectId,
    principal: AnnettePrincipal,
    createdBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  )                                                                                      extends Command
  final case class DeleteSubscription(
    subscriptionType: SubscriptionTypeId,
    objectId: ObjectId,
    principal: AnnettePrincipal,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  )                                                                                      extends Command
  final case class GetSubscription(id: SubscriptionKey, replyTo: ActorRef[Confirmation]) extends Command

  sealed trait Confirmation
  final case object Success                                  extends Confirmation
  final case class SuccessSubscription(entity: Subscription) extends Confirmation
  final case object NotFound                                 extends Confirmation
  final case object AlreadyExist                             extends Confirmation

  implicit val confirmationSuccessFormat: Format[Success.type]                    = Json.format
  implicit val confirmationSuccessSubscriptionFormat: Format[SuccessSubscription] = Json.format
  implicit val confirmationNotFoundFormat: Format[NotFound.type]                  = Json.format
  implicit val confirmationAlreadyExistFormat: Format[AlreadyExist.type]          = Json.format
  implicit val confirmationFormat: Format[Confirmation]                           = Json.format[Confirmation]

  sealed trait Event extends AggregateEvent[Event] {
    override def aggregateTag: AggregateEventTagger[Event] = Event.Tag
  }

  object Event {
    val Tag: AggregateEventShards[Event] = AggregateEventTag.sharded[Event](numShards = 10)
  }

  final case class SubscriptionCreated(
    subscriptionType: SubscriptionTypeId,
    objectId: ObjectId,
    principal: AnnettePrincipal,
    createdBy: AnnettePrincipal,
    createdAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  final case class SubscriptionDeleted(
    subscriptionType: SubscriptionTypeId,
    objectId: ObjectId,
    principal: AnnettePrincipal,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  implicit val subscriptionCreatedFormat: Format[SubscriptionCreated] = Json.format
  implicit val subscriptionDeletedFormat: Format[SubscriptionDeleted] = Json.format

  val empty                           = SubscriptionEntity(None)
  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Subscription")

  def apply(persistenceId: PersistenceId): EventSourcedBehavior[Command, Event, SubscriptionEntity] =
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, SubscriptionEntity](
        persistenceId = persistenceId,
        emptyState = SubscriptionEntity.empty,
        commandHandler = (entity, cmd) => entity.applyCommand(cmd),
        eventHandler = (entity, evt) => entity.applyEvent(evt)
      )

  def apply(entityContext: EntityContext[Command]): Behavior[Command] =
    apply(PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId))
      .withTagger(AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2))

  implicit val subscriptionEntityFormat: Format[SubscriptionEntity] = Json.format
}

final case class SubscriptionEntity(maybeState: Option[SubscriptionState]) {

  import SubscriptionEntity._

  def applyCommand(cmd: Command): ReplyEffect[Event, SubscriptionEntity] =
    cmd match {
      case cmd: CreateSubscription     => createSubscription(cmd)
      case cmd: DeleteSubscription     => deleteSubscription(cmd)
      case GetSubscription(_, replyTo) => getSubscription(replyTo)
    }

  def createSubscription(
    cmd: CreateSubscription
  ): ReplyEffect[Event, SubscriptionEntity] =
    maybeState match {
      case None    =>
        val event = cmd.transformInto[SubscriptionCreated]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
      case Some(_) => Effect.reply(cmd.replyTo)(AlreadyExist)
    }

  def deleteSubscription(
    cmd: DeleteSubscription
  ): ReplyEffect[Event, SubscriptionEntity] =
    maybeState match {
      case None    => Effect.reply(cmd.replyTo)(NotFound)
      case Some(_) =>
        val event = cmd.transformInto[SubscriptionDeleted]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def getSubscription(replyTo: ActorRef[Confirmation]): ReplyEffect[Event, SubscriptionEntity] =
    maybeState match {
      case Some(state) => Effect.reply(replyTo)(SuccessSubscription(state.toSubscription))
      case None        => Effect.reply(replyTo)(NotFound)
    }

  def onSubscriptionCreated(event: SubscriptionCreated): SubscriptionEntity =
    SubscriptionEntity(
      Some(
        event
          .into[SubscriptionState]
          .withFieldConst(_.updatedAt, event.createdAt)
          .withFieldConst(_.updatedBy, event.createdBy)
          .transform
      )
    )

  def onSubscriptionDeleted(): SubscriptionEntity =
    SubscriptionEntity(None)

  def applyEvent(event: Event): SubscriptionEntity =
    event match {
      case event: SubscriptionCreated => onSubscriptionCreated(event)
      case _: SubscriptionDeleted     => onSubscriptionDeleted()
    }

}
