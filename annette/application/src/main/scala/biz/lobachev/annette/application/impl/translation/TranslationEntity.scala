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

import java.time.OffsetDateTime
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl.{EntityContext, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import biz.lobachev.annette.application.api.language.LanguageId
import biz.lobachev.annette.application.api.translation._
import biz.lobachev.annette.application.impl.translation.model.TranslationState
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import com.lightbend.lagom.scaladsl.persistence._
import io.scalaland.chimney.dsl._
import org.slf4j.LoggerFactory
import play.api.libs.json._

import scala.util.Try

object TranslationEntity {

  trait CommandSerializable
  sealed trait Command                                                                                   extends CommandSerializable
  final case class CreateTranslation(payload: CreateTranslationPayload, replyTo: ActorRef[Confirmation]) extends Command
  final case class UpdateTranslationName(payload: UpdateTranslationNamePayload, replyTo: ActorRef[Confirmation])
      extends Command
  final case class DeleteTranslation(payload: DeleteTranslationPayload, replyTo: ActorRef[Confirmation]) extends Command
  final case class CreateTranslationBranch(payload: CreateTranslationBranchPayload, replyTo: ActorRef[Confirmation])
      extends Command
  final case class UpdateTranslationText(payload: UpdateTranslationTextPayload, replyTo: ActorRef[Confirmation])
      extends Command
  final case class DeleteTranslationItem(payload: DeleteTranslationItemPayload, replyTo: ActorRef[Confirmation])
      extends Command
  final case class DeleteTranslationText(payload: DeleteTranslationTextPayload, replyTo: ActorRef[Confirmation])
      extends Command
  final case class GetTranslation(id: TranslationId, replyTo: ActorRef[Confirmation])                    extends Command
  final case class GetTranslationJson(id: TranslationId, languageId: LanguageId, replyTo: ActorRef[Confirmation])
      extends Command

  sealed trait Confirmation
  final case object Success                                                 extends Confirmation
  final case class SuccessTranslation(translation: Translation)             extends Confirmation
  final case class SuccessTranslationJson(translationJson: TranslationJson) extends Confirmation
  final case object TranslationAlreadyExist                                 extends Confirmation
  final case object TranslationNotFound                                     extends Confirmation
  final case object IncorrectTranslationId                                  extends Confirmation

  implicit val confirmationSuccessFormat: Format[Success.type]                                 = Json.format
  implicit val confirmationSuccessTranslationFormat: Format[SuccessTranslation]                = Json.format
  implicit val confirmationSuccessTranslationJsonFormat: Format[SuccessTranslationJson]        = Json.format
  implicit val confirmationTranslationAlreadyExistFormat: Format[TranslationAlreadyExist.type] = Json.format
  implicit val confirmationTranslationNotFoundFormat: Format[TranslationNotFound.type]         = Json.format
  implicit val confirmationIncorrectTranslationIdFormat: Format[IncorrectTranslationId.type]   = Json.format
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
  final case class TranslationNameUpdated(
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
  final case class TranslationBranchCreated(
    id: TranslationId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class TranslationTextUpdated(
    id: TranslationId,
    languageId: LanguageId,
    text: String,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class TranslationItemDeleted(
    id: TranslationId,
    deletedBy: AnnettePrincipal,
    deletedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class TranslationTextDeleted(
    id: TranslationId,
    languageId: LanguageId,
    deletedBy: AnnettePrincipal,
    deletedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class TranslationJsonChanged(
    id: TranslationId,
    languageId: LanguageId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class TranslationJsonDeleted(
    id: TranslationId,
    languageId: LanguageId
  ) extends Event

  implicit val eventTranslationCreatedFormat: Format[TranslationCreated]             = Json.format
  implicit val eventTranslationNameUpdatedFormat: Format[TranslationNameUpdated]     = Json.format
  implicit val eventTranslationDeletedFormat: Format[TranslationDeleted]             = Json.format
  implicit val eventTranslationBranchCreatedFormat: Format[TranslationBranchCreated] = Json.format
  implicit val eventTranslationTextUpdatedFormat: Format[TranslationTextUpdated]     = Json.format
  implicit val eventTranslationItemDeletedFormat: Format[TranslationItemDeleted]     = Json.format
  implicit val eventTranslationTextDeletedFormat: Format[TranslationTextDeleted]     = Json.format
  implicit val eventTranslationJsonChangedFormat: Format[TranslationJsonChanged]     = Json.format
  implicit val eventTranslationJsonDeletedFormat: Format[TranslationJsonDeleted]     = Json.format

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
      case cmd: CreateTranslation       => createTranslation(cmd)
      case cmd: UpdateTranslationName   => updateTranslationName(cmd)
      case cmd: DeleteTranslation       => deleteTranslation(cmd)
      case cmd: CreateTranslationBranch => createTranslationBranch(cmd)
      case cmd: UpdateTranslationText   => updateTranslationText(cmd)
      case cmd: DeleteTranslationItem   => deleteTranslationItem(cmd)
      case cmd: DeleteTranslationText   => deleteTranslationText(cmd)
      case cmd: GetTranslation          => getTranslation(cmd)
      case cmd: GetTranslationJson      => getTranslationJson(cmd)
    }

  def createTranslation(cmd: CreateTranslation): ReplyEffect[Event, TranslationEntity] =
    maybeState match {
      case Some(_) => Effect.reply(cmd.replyTo)(TranslationAlreadyExist)
      case _       =>
        val event = cmd.payload.transformInto[TranslationCreated]
        Effect.persist(event).thenReply(cmd.replyTo)(_ => Success)
    }

  def updateTranslationName(cmd: UpdateTranslationName): ReplyEffect[Event, TranslationEntity] =
    maybeState match {
      case None => Effect.reply(cmd.replyTo)(TranslationNotFound)
      case _    =>
        val event = cmd.payload.transformInto[TranslationNameUpdated]
        Effect.persist(event).thenReply(cmd.replyTo)(_ => Success)
    }

  def deleteTranslation(cmd: DeleteTranslation): ReplyEffect[Event, TranslationEntity] =
    maybeState match {
      case None        => Effect.reply(cmd.replyTo)(TranslationNotFound)
      case Some(state) =>
        val events = Seq(cmd.payload.transformInto[TranslationDeleted]) ++
          state
            .languages()
            .map(languageId =>
              cmd.payload
                .into[TranslationJsonDeleted]
                .withFieldConst(_.languageId, languageId)
                .transform
            )
            .toSeq
        Effect.persist(events).thenReply(cmd.replyTo)(_ => Success)
    }

  def createTranslationBranch(cmd: CreateTranslationBranch): ReplyEffect[Event, TranslationEntity] =
    maybeState match {
      case None        => Effect.reply(cmd.replyTo)(TranslationNotFound)
      case Some(state) =>
        val maybeNewState = Try {
          state.createBranch(cmd.payload.id, cmd.payload.updatedBy, OffsetDateTime.now())
        }.toOption
        maybeNewState match {
          case Some(_) =>
            val event = cmd.payload.transformInto[TranslationBranchCreated]
            Effect.persist(event).thenReply(cmd.replyTo)(_ => Success)
          case _       => Effect.reply(cmd.replyTo)(IncorrectTranslationId)
        }

    }

  def updateTranslationText(cmd: UpdateTranslationText): ReplyEffect[Event, TranslationEntity] =
    maybeState match {
      case None        => Effect.reply(cmd.replyTo)(TranslationNotFound)
      case Some(state) =>
        val maybeNewState = Try {
          state.updateText(
            cmd.payload.id,
            cmd.payload.languageId,
            cmd.payload.text,
            cmd.payload.updatedBy,
            OffsetDateTime.now()
          )
        }.toOption
        maybeNewState match {
          case Some(_) =>
            val languageEvent = cmd.payload
              .into[TranslationJsonChanged]
              .withFieldConst(_.id, state.id)
              .withFieldConst(_.languageId, cmd.payload.languageId)
              .transform
            val event         = cmd.payload.transformInto[TranslationTextUpdated]
            Effect.persist(event :: languageEvent :: Nil).thenReply(cmd.replyTo)(_ => Success)
          case _       => Effect.reply(cmd.replyTo)(IncorrectTranslationId)
        }

    }

  def deleteTranslationItem(cmd: DeleteTranslationItem): ReplyEffect[Event, TranslationEntity] =
    maybeState match {
      case None        => Effect.reply(cmd.replyTo)(TranslationNotFound)
      case Some(state) =>
        val maybeNewState = Try {
          state.deleteItem(
            cmd.payload.id,
            cmd.payload.deletedBy,
            OffsetDateTime.now()
          )
        }.toOption
        maybeNewState match {
          case Some(newState) =>
            val languageEvents = newState
              .languages()
              .map(languageId =>
                cmd.payload
                  .into[TranslationJsonChanged]
                  .withFieldConst(_.id, state.id)
                  .withFieldConst(_.languageId, languageId)
                  .withFieldConst(_.updatedBy, cmd.payload.deletedBy)
                  .transform
              )
              .toList
            val event          = cmd.payload.transformInto[TranslationItemDeleted]
            Effect.persist(event :: languageEvents).thenReply(cmd.replyTo)(_ => Success)
          case _              => Effect.reply(cmd.replyTo)(IncorrectTranslationId)
        }
    }

  def deleteTranslationText(cmd: DeleteTranslationText): ReplyEffect[Event, TranslationEntity] =
    maybeState match {
      case None        => Effect.reply(cmd.replyTo)(TranslationNotFound)
      case Some(state) =>
        val maybeNewState = Try {
          state.deleteText(
            cmd.payload.id,
            cmd.payload.languageId,
            cmd.payload.deletedBy,
            OffsetDateTime.now()
          )
        }.toOption
        maybeNewState match {
          case Some(_) =>
            val event         = cmd.payload.transformInto[TranslationTextDeleted]
            val languageEvent = cmd.payload
              .into[TranslationJsonChanged]
              .withFieldConst(_.id, state.id)
              .withFieldConst(_.languageId, cmd.payload.languageId)
              .withFieldConst(_.updatedBy, cmd.payload.deletedBy)
              .transform
            Effect.persist(event :: languageEvent :: Nil).thenReply(cmd.replyTo)(_ => Success)
          case _       => Effect.reply(cmd.replyTo)(IncorrectTranslationId)
        }
    }

  def getTranslation(cmd: GetTranslation): ReplyEffect[Event, TranslationEntity] =
    maybeState match {
      case None        => Effect.reply(cmd.replyTo)(TranslationNotFound)
      case Some(state) =>
        val translation = state.transformInto[Translation]
        Effect.reply(cmd.replyTo)(SuccessTranslation(translation))
    }

  def getTranslationJson(cmd: GetTranslationJson): ReplyEffect[Event, TranslationEntity] =
    maybeState match {
      case None        => Effect.reply(cmd.replyTo)(TranslationNotFound)
      case Some(state) =>
        val translationJson = state
          .into[TranslationJson]
          .withFieldConst(_.languageId, cmd.languageId)
          .withFieldConst(_.json, state.json(cmd.languageId))
          .transform
        Effect.reply(cmd.replyTo)(SuccessTranslationJson(translationJson))
    }

  def applyEvent(event: Event): TranslationEntity =
    event match {
      case event: TranslationCreated       => onTranslationCreated(event)
      case event: TranslationNameUpdated   => onTranslationNameUpdated(event)
      case _: TranslationDeleted           => onTranslationDeleted()
      case event: TranslationBranchCreated => onTranslationBranchCreated(event)
      case event: TranslationTextUpdated   => onTranslationTextUpdated(event)
      case event: TranslationItemDeleted   => onTranslationItemDeleted(event)
      case event: TranslationTextDeleted   => onTranslationTextDeleted(event)
      case _: TranslationJsonChanged       => this
      case _: TranslationJsonDeleted       => this
    }

  def onTranslationCreated(event: TranslationCreated): TranslationEntity =
    TranslationEntity(
      Some(
        TranslationState.create(event.id, event.name, event.createdBy, event.createdAt)
      )
    )

  def onTranslationNameUpdated(event: TranslationNameUpdated): TranslationEntity =
    TranslationEntity(
      maybeState.map(
        _.updateName(
          name = event.name,
          principal = event.updatedBy,
          timestamp = event.updatedAt
        )
      )
    )

  def onTranslationDeleted(): TranslationEntity = TranslationEntity(None)

  def onTranslationBranchCreated(event: TranslationBranchCreated): TranslationEntity =
    TranslationEntity(
      maybeState.map(state =>
        Try {
          state.createBranch(
            id = event.id,
            principal = event.updatedBy,
            timestamp = event.updatedAt
          )
        }.getOrElse(state)
      )
    )

  def onTranslationTextUpdated(event: TranslationTextUpdated): TranslationEntity =
    TranslationEntity(
      maybeState.map(state =>
        Try {
          state.updateText(
            id = event.id,
            languageId = event.languageId,
            text = event.text,
            principal = event.updatedBy,
            timestamp = event.updatedAt
          )
        }.getOrElse(state)
      )
    )

  def onTranslationItemDeleted(event: TranslationItemDeleted): TranslationEntity =
    TranslationEntity(
      maybeState.map(state =>
        Try {
          state.deleteItem(
            id = event.id,
            principal = event.deletedBy,
            timestamp = event.deletedAt
          )
        }.getOrElse(state)
      )
    )

  def onTranslationTextDeleted(event: TranslationTextDeleted): TranslationEntity =
    TranslationEntity(
      maybeState.map(state =>
        Try {
          state.deleteText(
            id = event.id,
            languageId = event.languageId,
            principal = event.deletedBy,
            timestamp = event.deletedAt
          )
        }.getOrElse(state)
      )
    )

}
