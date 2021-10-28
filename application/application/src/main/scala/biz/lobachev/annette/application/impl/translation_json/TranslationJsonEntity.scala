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

package biz.lobachev.annette.application.impl.translation_json

import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl.{EntityContext, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import biz.lobachev.annette.application.api.translation._
import biz.lobachev.annette.application.impl.translation_json.model.TranslationJsonState
import biz.lobachev.annette.core.model.LanguageId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import com.lightbend.lagom.scaladsl.persistence._
import io.scalaland.chimney.dsl._
import org.slf4j.LoggerFactory
import play.api.libs.json._

import java.time.OffsetDateTime

object TranslationJsonEntity {

  trait CommandSerializable
  sealed trait Command extends CommandSerializable

  final case class UpdateTranslationJson(payload: UpdateTranslationJsonPayload, replyTo: ActorRef[Confirmation])
      extends Command
  final case class DeleteTranslationJson(payload: DeleteTranslationJsonPayload, replyTo: ActorRef[Confirmation])
      extends Command
  final case class GetTranslationJson(id: TranslationId, languageId: LanguageId, replyTo: ActorRef[Confirmation])
      extends Command

  sealed trait Confirmation
  final case object Success                                                 extends Confirmation
  final case class SuccessTranslationJson(translationJson: TranslationJson) extends Confirmation
  final case object TranslationNotFound                                     extends Confirmation

  implicit val confirmationSuccessFormat: Format[Success.type]                          = Json.format
  implicit val confirmationSuccessTranslationJsonFormat: Format[SuccessTranslationJson] = Json.format
  implicit val confirmationTranslationNotFoundFormat: Format[TranslationNotFound.type]  = Json.format
  implicit val confirmationFormat: Format[Confirmation]                                 = Json.format

  sealed trait Event extends AggregateEvent[Event] {
    override def aggregateTag: AggregateEventTagger[Event] = Event.Tag
  }

  object Event {
    val Tag: AggregateEventShards[Event] = AggregateEventTag.sharded[Event](numShards = 10)
  }

  final case class TranslationJsonUpdated(
    translationId: TranslationId,
    languageId: LanguageId,
    json: JsObject,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  final case class TranslationJsonDeleted(
    translationId: TranslationId,
    languageId: LanguageId,
    deletedBy: AnnettePrincipal,
    deletedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  implicit val eventTranslationNameUpdatedFormat: Format[TranslationJsonUpdated] = Json.format
  implicit val eventTranslationJsonDeletedFormat: Format[TranslationJsonDeleted] = Json.format

  val empty = TranslationJsonEntity()

  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Application_TranslationJson")

  def apply(persistenceId: PersistenceId): EventSourcedBehavior[Command, Event, TranslationJsonEntity] =
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, TranslationJsonEntity](
        persistenceId = persistenceId,
        emptyState = TranslationJsonEntity.empty,
        commandHandler = (entity, cmd) => entity.applyCommand(cmd),
        eventHandler = (entity, evt) => entity.applyEvent(evt)
      )

  def apply(entityContext: EntityContext[Command]): Behavior[Command] =
    apply(PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId))
      .withTagger(AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2))

  implicit val entityFormat: Format[TranslationJsonEntity] = Json.format

}

final case class TranslationJsonEntity(maybeState: Option[TranslationJsonState] = None) {
  import TranslationJsonEntity._

  val log = LoggerFactory.getLogger(this.getClass)

  def applyCommand(cmd: Command): ReplyEffect[Event, TranslationJsonEntity] =
    cmd match {
      case cmd: UpdateTranslationJson => updateTranslationJson(cmd)
      case cmd: DeleteTranslationJson => deleteTranslationJson(cmd)
      case cmd: GetTranslationJson    => getTranslationJson(cmd)
    }

  def updateTranslationJson(cmd: UpdateTranslationJson): ReplyEffect[Event, TranslationJsonEntity] = {
    val event = cmd.payload.transformInto[TranslationJsonUpdated]
    Effect.persist(event).thenReply(cmd.replyTo)(_ => Success)
  }

  def deleteTranslationJson(cmd: DeleteTranslationJson): ReplyEffect[Event, TranslationJsonEntity] =
    maybeState match {
      case None    => Effect.reply(cmd.replyTo)(TranslationNotFound)
      case Some(_) =>
        val event = Seq(cmd.payload.transformInto[TranslationJsonDeleted])
        Effect.persist(event).thenReply(cmd.replyTo)(_ => Success)
    }

  def getTranslationJson(cmd: GetTranslationJson): ReplyEffect[Event, TranslationJsonEntity] =
    maybeState match {
      case None        => Effect.reply(cmd.replyTo)(TranslationNotFound)
      case Some(state) =>
        Effect.reply(cmd.replyTo)(
          SuccessTranslationJson(
            state.transformInto[TranslationJson]
          )
        )
    }

  def applyEvent(event: Event): TranslationJsonEntity =
    event match {
      case event: TranslationJsonUpdated => onTranslationJsonUpdated(event)
      case _: TranslationJsonDeleted     => onTranslationDeleted()
    }

  def onTranslationJsonUpdated(event: TranslationJsonUpdated): TranslationJsonEntity =
    TranslationJsonEntity(
      Some(event.transformInto[TranslationJsonState])
    )

  def onTranslationDeleted(): TranslationJsonEntity = TranslationJsonEntity(None)

}
