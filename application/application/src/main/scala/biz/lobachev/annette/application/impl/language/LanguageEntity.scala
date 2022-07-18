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

package biz.lobachev.annette.application.impl.language

import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl.{EntityContext, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import biz.lobachev.annette.application.api.language._
import biz.lobachev.annette.application.impl.language.model.LanguageState
import biz.lobachev.annette.core.model.LanguageId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import com.lightbend.lagom.scaladsl.persistence._
import io.scalaland.chimney.dsl._
import org.slf4j.LoggerFactory
import play.api.libs.json._

import java.time.OffsetDateTime

object LanguageEntity {

  trait CommandSerializable
  sealed trait Command                                                          extends CommandSerializable
  final case class CreateLanguage(
    id: LanguageId,
    name: String,
    createdBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  )                                                                             extends Command
  final case class UpdateLanguage(
    id: LanguageId,
    name: String,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  )                                                                             extends Command
  final case class DeleteLanguage(id: LanguageId, deletedBy: AnnettePrincipal, replyTo: ActorRef[Confirmation])
      extends Command
  final case class GetLanguage(id: LanguageId, replyTo: ActorRef[Confirmation]) extends Command

  sealed trait Confirmation
  final case object Success                            extends Confirmation
  final case class SuccessLanguage(language: Language) extends Confirmation
  final case object LanguageAlreadyExist               extends Confirmation
  final case object LanguageNotFound                   extends Confirmation

  implicit val confirmationSuccessFormat: Format[Success.type]                           = Json.format
  implicit val confirmationSuccessLanguageFormat: Format[SuccessLanguage]                = Json.format
  implicit val confirmationLanguageAlreadyExistFormat: Format[LanguageAlreadyExist.type] = Json.format
  implicit val confirmationLanguageNotFoundFormat: Format[LanguageNotFound.type]         = Json.format
  implicit val confirmationFormat: Format[Confirmation]                                  = Json.format

  sealed trait Event extends AggregateEvent[Event] {
    override def aggregateTag: AggregateEventTagger[Event] = Event.Tag
  }

  object Event {
    val Tag: AggregateEventShards[Event] = AggregateEventTag.sharded[Event](numShards = 10)
  }

  final case class LanguageCreated(
    id: LanguageId,
    name: String,
    createdBy: AnnettePrincipal,
    createdAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class LanguageUpdated(
    id: LanguageId,
    name: String,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class LanguageDeleted(
    id: LanguageId,
    deletedBy: AnnettePrincipal,
    deletedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  implicit val eventLanguageCreatedFormat: Format[LanguageCreated] = Json.format
  implicit val eventLanguageUpdatedFormat: Format[LanguageUpdated] = Json.format
  implicit val eventLanguageDeletedFormat: Format[LanguageDeleted] = Json.format

  val empty = LanguageEntity()

  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Application_Language")

  def apply(persistenceId: PersistenceId): EventSourcedBehavior[Command, Event, LanguageEntity] =
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, LanguageEntity](
        persistenceId = persistenceId,
        emptyState = LanguageEntity.empty,
        commandHandler = (entity, cmd) => entity.applyCommand(cmd),
        eventHandler = (entity, evt) => entity.applyEvent(evt)
      )

  def apply(entityContext: EntityContext[Command]): Behavior[Command] =
    apply(PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId))
      .withTagger(AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2))

  implicit val entityFormat: Format[LanguageEntity] = Json.format

}

final case class LanguageEntity(maybeState: Option[LanguageState] = None) {
  import LanguageEntity._

  val log = LoggerFactory.getLogger(this.getClass)

  def applyCommand(cmd: Command): ReplyEffect[Event, LanguageEntity] =
    cmd match {
      case cmd: CreateLanguage => createLanguage(cmd)
      case cmd: UpdateLanguage => updateLanguage(cmd)
      case cmd: DeleteLanguage => deleteLanguage(cmd)
      case cmd: GetLanguage    => getLanguage(cmd)
    }

  def createLanguage(cmd: CreateLanguage): ReplyEffect[Event, LanguageEntity] =
    maybeState match {
      case Some(_) => Effect.reply(cmd.replyTo)(LanguageAlreadyExist)
      case _       =>
        val event = cmd.transformInto[LanguageCreated]
        Effect.persist(event).thenReply(cmd.replyTo)(_ => Success)
    }

  def updateLanguage(cmd: UpdateLanguage): ReplyEffect[Event, LanguageEntity] =
    maybeState match {
      case None => Effect.reply(cmd.replyTo)(LanguageNotFound)
      case _    =>
        val event = cmd.transformInto[LanguageUpdated]
        Effect.persist(event).thenReply(cmd.replyTo)(_ => Success)
    }

  def deleteLanguage(cmd: DeleteLanguage): ReplyEffect[Event, LanguageEntity] =
    maybeState match {
      case None => Effect.reply(cmd.replyTo)(LanguageNotFound)
      case _    =>
        val event = cmd.transformInto[LanguageDeleted]
        Effect.persist(event).thenReply(cmd.replyTo)(_ => Success)
    }

  def getLanguage(cmd: GetLanguage): ReplyEffect[Event, LanguageEntity] =
    maybeState match {
      case None        => Effect.reply(cmd.replyTo)(LanguageNotFound)
      case Some(state) =>
        val language = state.transformInto[Language]
        Effect.reply(cmd.replyTo)(SuccessLanguage(language))
    }

  def applyEvent(event: Event): LanguageEntity =
    event match {
      case event: LanguageCreated => onLanguageCreated(event)
      case event: LanguageUpdated => onLanguageUpdated(event)
      case _: LanguageDeleted     => onLanguageDeleted()
    }

  def onLanguageCreated(event: LanguageCreated): LanguageEntity =
    LanguageEntity(
      Some(
        event
          .into[LanguageState]
          .withFieldConst(_.updatedAt, event.createdAt)
          .withFieldConst(_.updatedBy, event.createdBy)
          .transform
      )
    )

  def onLanguageUpdated(event: LanguageUpdated): LanguageEntity =
    LanguageEntity(Some(event.transformInto[LanguageState]))

  def onLanguageDeleted(): LanguageEntity =
    LanguageEntity(None)

}
