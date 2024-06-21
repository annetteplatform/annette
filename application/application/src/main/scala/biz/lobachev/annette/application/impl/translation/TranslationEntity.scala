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

package biz.lobachev.annette.application.impl.translation

import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl.{EntityContext, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import biz.lobachev.annette.application.api.translation._
import biz.lobachev.annette.application.impl.translation.model.TranslationState
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import com.lightbend.lagom.scaladsl.persistence._
import biz.lobachev.annette.core.utils.ChimneyCommons._
import io.scalaland.chimney.dsl._
import org.slf4j.LoggerFactory
import play.api.libs.json._

import java.time.OffsetDateTime

object TranslationEntity {

  trait CommandSerializable
  sealed trait Command                                                                extends CommandSerializable
  final case class CreateTranslation(
    id: TranslationId,
    name: String,
    createdBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  )                                                                                   extends Command
  final case class UpdateTranslation(
    id: TranslationId,
    name: String,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  )                                                                                   extends Command
  final case class DeleteTranslation(id: TranslationId, deletedBy: AnnettePrincipal, replyTo: ActorRef[Confirmation])
      extends Command
  final case class GetTranslation(id: TranslationId, replyTo: ActorRef[Confirmation]) extends Command

  sealed trait Confirmation
  final case object Success                                     extends Confirmation
  final case class SuccessTranslation(translation: Translation) extends Confirmation
  final case object TranslationAlreadyExist                     extends Confirmation
  final case object TranslationNotFound                         extends Confirmation

  implicit val confirmationSuccessFormat: Format[Success.type]                                 = Json.format
  implicit val confirmationSuccessTranslationFormat: Format[SuccessTranslation]                = Json.format
  implicit val confirmationTranslationAlreadyExistFormat: Format[TranslationAlreadyExist.type] = Json.format
  implicit val confirmationTranslationNotFoundFormat: Format[TranslationNotFound.type]         = Json.format
  implicit val confirmationFormat: Format[Confirmation]                                        = Json.format

  sealed trait Event extends AggregateEvent[Event] {
    override def aggregateTag: AggregateEventTagger[Event] = Event.Tag
  }

  object Event {
    val Tag: AggregateEventShards[Event] = AggregateEventTag.sharded[Event](numShards = 10)
  }

  final case class TranslationCreated(
    id: TranslationId,
    name: String,
    createdBy: AnnettePrincipal,
    createdAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class TranslationUpdated(
    id: TranslationId,
    name: String,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class TranslationDeleted(
    id: TranslationId,
    deletedBy: AnnettePrincipal,
    deletedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  implicit val eventTranslationCreatedFormat: Format[TranslationCreated]     = Json.format
  implicit val eventTranslationNameUpdatedFormat: Format[TranslationUpdated] = Json.format
  implicit val eventTranslationDeletedFormat: Format[TranslationDeleted]     = Json.format

  val empty = TranslationEntity()

  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Application_Translation")

  def apply(persistenceId: PersistenceId): EventSourcedBehavior[Command, Event, TranslationEntity] =
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, TranslationEntity](
        persistenceId = persistenceId,
        emptyState = TranslationEntity.empty,
        commandHandler = (entity, cmd) => entity.applyCommand(cmd),
        eventHandler = (entity, evt) => entity.applyEvent(evt)
      )

  def apply(entityContext: EntityContext[Command]): Behavior[Command] =
    apply(PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId))
      .withTagger(AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2))

  implicit val entityFormat: Format[TranslationEntity] = Json.format

}

final case class TranslationEntity(maybeState: Option[TranslationState] = None) {
  import TranslationEntity._

  val log = LoggerFactory.getLogger(this.getClass)

  def applyCommand(cmd: Command): ReplyEffect[Event, TranslationEntity] =
    cmd match {
      case cmd: CreateTranslation => createTranslation(cmd)
      case cmd: UpdateTranslation => updateTranslationName(cmd)
      case cmd: DeleteTranslation => deleteTranslation(cmd)
      case cmd: GetTranslation    => getTranslation(cmd)
    }

  def createTranslation(cmd: CreateTranslation): ReplyEffect[Event, TranslationEntity] =
    maybeState match {
      case Some(_) => Effect.reply(cmd.replyTo)(TranslationAlreadyExist)
      case _       =>
        val event = cmd.transformInto[TranslationCreated]
        Effect.persist(event).thenReply(cmd.replyTo)(_ => Success)
    }

  def updateTranslationName(cmd: UpdateTranslation): ReplyEffect[Event, TranslationEntity] =
    maybeState match {
      case None => Effect.reply(cmd.replyTo)(TranslationNotFound)
      case _    =>
        val event = cmd.transformInto[TranslationUpdated]
        Effect.persist(event).thenReply(cmd.replyTo)(_ => Success)
    }

  def deleteTranslation(cmd: DeleteTranslation): ReplyEffect[Event, TranslationEntity] =
    maybeState match {
      case None    => Effect.reply(cmd.replyTo)(TranslationNotFound)
      case Some(_) =>
        val event = Seq(cmd.transformInto[TranslationDeleted])
        Effect.persist(event).thenReply(cmd.replyTo)(_ => Success)
    }

  def getTranslation(cmd: GetTranslation): ReplyEffect[Event, TranslationEntity] =
    maybeState match {
      case None        => Effect.reply(cmd.replyTo)(TranslationNotFound)
      case Some(state) =>
        val translation = state.transformInto[Translation]
        Effect.reply(cmd.replyTo)(SuccessTranslation(translation))
    }

  def applyEvent(event: Event): TranslationEntity =
    event match {
      case event: TranslationCreated => onTranslationCreated(event)
      case event: TranslationUpdated => onTranslationUpdated(event)
      case _: TranslationDeleted     => onTranslationDeleted()
    }

  def onTranslationCreated(event: TranslationCreated): TranslationEntity =
    TranslationEntity(
      Some(
        event
          .into[TranslationState]
          .withFieldConst(_.updatedAt, event.createdAt)
          .withFieldConst(_.updatedBy, event.createdBy)
          .transform
      )
    )

  def onTranslationUpdated(event: TranslationUpdated): TranslationEntity =
    TranslationEntity(
      Some(
        event.transformInto[TranslationState]
      )
    )

  def onTranslationDeleted(): TranslationEntity = TranslationEntity(None)

}
