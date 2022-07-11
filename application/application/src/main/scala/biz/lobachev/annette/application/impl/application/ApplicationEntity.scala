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

package biz.lobachev.annette.application.impl.application

import java.time.OffsetDateTime
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl.{EntityContext, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import com.lightbend.lagom.scaladsl.persistence._
import play.api.libs.json._
import org.slf4j.LoggerFactory
import biz.lobachev.annette.application.api.application._
import biz.lobachev.annette.application.api.translation.TranslationId
import biz.lobachev.annette.application.impl.application.model.ApplicationState
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.translation.Caption
import io.scalaland.chimney.dsl._

object ApplicationEntity {

  trait CommandSerializable
  sealed trait Command extends CommandSerializable
  final case class CreateApplication(
    id: ApplicationId,
    name: String,
    caption: Caption,
    translations: Set[TranslationId] = Set.empty,
    frontendUrl: Option[String],
    backendUrl: Option[String],
    createdBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  )                    extends Command
  final case class UpdateApplication(
    id: ApplicationId,
    name: String,
    caption: Caption,
    translations: Set[TranslationId] = Set.empty,
    frontendUrl: Option[String],
    backendUrl: Option[String],
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  )                    extends Command
  final case class DeleteApplication(
    id: ApplicationId,
    deletedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  )                    extends Command
  final case class GetApplication(
    id: ApplicationId,
    replyTo: ActorRef[Confirmation]
  )                    extends Command

  sealed trait Confirmation
  final case object Success                                     extends Confirmation
  final case class SuccessApplication(application: Application) extends Confirmation
  final case object ApplicationAlreadyExist                     extends Confirmation
  final case object ApplicationNotFound                         extends Confirmation

  implicit val confirmationSuccessFormat: Format[Success.type]                                 = Json.format
  implicit val confirmationSuccessApplicationFormat: Format[SuccessApplication]                = Json.format
  implicit val confirmationApplicationAlreadyExistFormat: Format[ApplicationAlreadyExist.type] = Json.format
  implicit val confirmationApplicationNotFoundFormat: Format[ApplicationNotFound.type]         = Json.format

  implicit val confirmationFormat: Format[Confirmation] = Json.format[Confirmation]

  sealed trait Event extends AggregateEvent[Event] {
    override def aggregateTag: AggregateEventTagger[Event] = Event.Tag
  }

  object Event {
    val Tag: AggregateEventShards[Event] = AggregateEventTag.sharded[Event](numShards = 10)
  }

  final case class ApplicationCreated(
    id: ApplicationId,
    name: String,
    caption: Caption,
    translations: Set[TranslationId] = Set.empty,
    frontendUrl: Option[String],
    backendUrl: Option[String],
    createdBy: AnnettePrincipal,
    createdAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class ApplicationNameUpdated(
    id: ApplicationId,
    name: String,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class ApplicationCaptionUpdated(
    id: ApplicationId,
    caption: Caption,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class ApplicationTranslationsUpdated(
    id: ApplicationId,
    translations: Set[TranslationId] = Set.empty,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class ApplicationBackendUrlUpdated(
    id: ApplicationId,
    backendUrl: Option[String],
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class ApplicationFrontendUrlUpdated(
    id: ApplicationId,
    frontendUrl: Option[String],
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class ApplicationDeleted(
    id: ApplicationId,
    deletedBy: AnnettePrincipal,
    deletedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  implicit val eventApplicationCreatedFormat: Format[ApplicationCreated]                         = Json.format
  implicit val eventApplicationNameUpdatedFormat: Format[ApplicationNameUpdated]                 = Json.format
  implicit val eventApplicationCaptionUpdatedFormat: Format[ApplicationCaptionUpdated]           = Json.format
  implicit val eventApplicationTranslationsUpdatedFormat: Format[ApplicationTranslationsUpdated] = Json.format
  implicit val eventApplicationBackendUrlUpdatedFormat: Format[ApplicationBackendUrlUpdated]     = Json.format
  implicit val eventApplicationFrontendUrlUpdatedFormat: Format[ApplicationFrontendUrlUpdated]   = Json.format
  implicit val eventApplicationDeletedFormat: Format[ApplicationDeleted]                         = Json.format

  val empty = ApplicationEntity()

  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Application_Application")

  def apply(persistenceId: PersistenceId): EventSourcedBehavior[Command, Event, ApplicationEntity] =
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, ApplicationEntity](
        persistenceId = persistenceId,
        emptyState = ApplicationEntity.empty,
        commandHandler = (entity, cmd) => entity.applyCommand(cmd),
        eventHandler = (entity, evt) => entity.applyEvent(evt)
      )

  def apply(entityContext: EntityContext[Command]): Behavior[Command] =
    apply(PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId))
      .withTagger(AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2))

  implicit val entityFormat: Format[ApplicationEntity] = Json.format

}

final case class ApplicationEntity(maybeState: Option[ApplicationState] = None) {
  import ApplicationEntity._

  val log = LoggerFactory.getLogger(this.getClass)

  def applyCommand(cmd: Command): ReplyEffect[Event, ApplicationEntity] =
    cmd match {
      case cmd: CreateApplication => createApplication(cmd)
      case cmd: UpdateApplication => updateApplication(cmd)
      case cmd: DeleteApplication => deleteApplication(cmd)
      case cmd: GetApplication    => getApplication(cmd)
    }

  def createApplication(cmd: CreateApplication): ReplyEffect[Event, ApplicationEntity] =
    maybeState match {
      case Some(_) => Effect.reply(cmd.replyTo)(ApplicationAlreadyExist)
      case _       =>
        val event = cmd.transformInto[ApplicationCreated]
        Effect.persist(event).thenReply(cmd.replyTo)(_ => Success)
    }

  def updateApplication(cmd: UpdateApplication): ReplyEffect[Event, ApplicationEntity] =
    maybeState match {
      case None        => Effect.reply(cmd.replyTo)(ApplicationNotFound)
      case Some(state) =>
        val events =
          Seq(
            if (state.name != cmd.name) Some(cmd.transformInto[ApplicationNameUpdated])
            else None,
            if (state.caption != cmd.caption) Some(cmd.transformInto[ApplicationCaptionUpdated])
            else None,
            if (state.translations != cmd.translations)
              Some(cmd.transformInto[ApplicationTranslationsUpdated])
            else None,
            if (state.backendUrl != cmd.backendUrl) Some(cmd.transformInto[ApplicationBackendUrlUpdated])
            else None,
            if (state.frontendUrl != cmd.frontendUrl) Some(cmd.transformInto[ApplicationFrontendUrlUpdated])
            else None
          ).flatten
        Effect.persist(events).thenReply(cmd.replyTo)(_ => Success)
    }

  def deleteApplication(cmd: DeleteApplication): ReplyEffect[Event, ApplicationEntity] =
    maybeState match {
      case None => Effect.reply(cmd.replyTo)(ApplicationNotFound)
      case _    =>
        val event = cmd.transformInto[ApplicationDeleted]
        Effect.persist(event).thenReply(cmd.replyTo)(_ => Success)
    }

  def getApplication(cmd: GetApplication): ReplyEffect[Event, ApplicationEntity] =
    maybeState match {
      case None        => Effect.reply(cmd.replyTo)(ApplicationNotFound)
      case Some(state) =>
        val application = state.transformInto[Application]
        Effect.reply(cmd.replyTo)(SuccessApplication(application))
    }

  def applyEvent(event: Event): ApplicationEntity =
    event match {
      case event: ApplicationCreated             => onApplicationCreated(event)
      case event: ApplicationNameUpdated         => onApplicationNameUpdated(event)
      case event: ApplicationCaptionUpdated      => onApplicationCaptionUpdated(event)
      case event: ApplicationTranslationsUpdated => onApplicationTranslationsUpdated(event)
      case event: ApplicationBackendUrlUpdated   => onApplicationBackendUrlUpdated(event)
      case event: ApplicationFrontendUrlUpdated  => onApplicationFrontendUrlUpdated(event)
      case _: ApplicationDeleted                 => onApplicationDeleted()
    }

  def onApplicationCreated(event: ApplicationCreated): ApplicationEntity =
    ApplicationEntity(
      Some(
        event
          .into[ApplicationState]
          .withFieldConst(_.updatedAt, event.createdAt)
          .withFieldConst(_.updatedBy, event.createdBy)
          .transform
      )
    )

  def onApplicationNameUpdated(event: ApplicationNameUpdated): ApplicationEntity =
    ApplicationEntity(
      maybeState.map(state =>
        state.copy(
          name = event.name,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

  def onApplicationCaptionUpdated(event: ApplicationCaptionUpdated): ApplicationEntity =
    ApplicationEntity(
      maybeState.map(state =>
        state.copy(
          caption = event.caption,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

  def onApplicationTranslationsUpdated(event: ApplicationTranslationsUpdated): ApplicationEntity =
    ApplicationEntity(
      maybeState.map(state =>
        state.copy(
          translations = event.translations,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

  def onApplicationBackendUrlUpdated(event: ApplicationBackendUrlUpdated): ApplicationEntity   =
    ApplicationEntity(
      maybeState.map(state =>
        state.copy(
          backendUrl = event.backendUrl,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )
  def onApplicationFrontendUrlUpdated(event: ApplicationFrontendUrlUpdated): ApplicationEntity =
    ApplicationEntity(
      maybeState.map(state =>
        state.copy(
          frontendUrl = event.frontendUrl,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

  def onApplicationDeleted(): ApplicationEntity =
    ApplicationEntity(None)

}
