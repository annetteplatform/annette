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

package biz.lobachev.annette.cms.impl.files

import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl.{EntityContext, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import biz.lobachev.annette.cms.api.files.FileDescriptor
import biz.lobachev.annette.cms.api.files.FileTypes.FileType
import biz.lobachev.annette.cms.impl.files.model.FileState
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import com.lightbend.lagom.scaladsl.persistence._
import io.scalaland.chimney.dsl._
import org.slf4j.LoggerFactory
import play.api.libs.json.{Format, Json}

import java.time.OffsetDateTime

object FileEntity {

  trait CommandSerializable
  sealed trait Command extends CommandSerializable

  final case class StoreFile(
    objectId: String,
    fileType: FileType,
    fileId: String,
    name: String,
    filename: String,
    contentType: Option[String],
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class UpdateFileName(
    objectId: String,
    fileType: FileType,
    fileId: String,
    name: String,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class RemoveFile(
    objectId: String,
    fileType: FileType,
    fileId: String,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  sealed trait Confirmation
  final case object Success                          extends Confirmation
  final case class SuccessFile(file: FileDescriptor) extends Confirmation
  final case object FileNotFound                     extends Confirmation

  implicit val confirmationSuccessFormat: Format[Success.type]           = Json.format
  implicit val confirmationSuccessFileFormat: Format[SuccessFile]        = Json.format
  implicit val confirmationFileNotFoundFormat: Format[FileNotFound.type] = Json.format

  sealed trait Event extends AggregateEvent[Event] {
    override def aggregateTag: AggregateEventTagger[Event] = Event.Tag
  }

  object Event {
    val Tag: AggregateEventShards[Event] = AggregateEventTag.sharded[Event](numShards = 10)
  }

  final case class FileStored(
    objectId: String,
    fileType: FileType,
    fileId: String,
    name: String,
    filename: String,
    contentType: Option[String],
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class FileNameUpdated(
    objectId: String,
    fileType: FileType,
    fileId: String,
    name: String,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class FileRemoved(
    objectId: String,
    fileType: FileType,
    fileId: String,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  implicit val eventFileStoredFormat: Format[FileStored]           = Json.format
  implicit val eventFileNameUpdatedFormat: Format[FileNameUpdated] = Json.format
  implicit val eventFileRemovedFormat: Format[FileRemoved]         = Json.format

  val empty = FileEntity()

  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Cms_File")

  def apply(persistenceId: PersistenceId): EventSourcedBehavior[Command, Event, FileEntity] =
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, FileEntity](
        persistenceId = persistenceId,
        emptyState = FileEntity.empty,
        commandHandler = (entity, cmd) => entity.applyCommand(cmd),
        eventHandler = (entity, evt) => entity.applyEvent(evt)
      )

  def apply(entityContext: EntityContext[Command]): Behavior[Command] =
    apply(PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId))
      .withTagger(AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2))

  implicit val entityFormat: Format[FileEntity] = Json.format

}

final case class FileEntity(maybeState: Option[FileState] = None) {

  val log = LoggerFactory.getLogger(this.getClass)

  def applyCommand(cmd: FileEntity.Command): ReplyEffect[FileEntity.Event, FileEntity] =
    cmd match {
      case cmd: FileEntity.StoreFile      => storeFile(cmd)
      case cmd: FileEntity.UpdateFileName => updateFileName(cmd)
      case cmd: FileEntity.RemoveFile     => removeFile(cmd)

    }

  def storeFile(cmd: FileEntity.StoreFile): ReplyEffect[FileEntity.Event, FileEntity] = {
    val event = cmd.transformInto[FileEntity.FileStored]
    Effect
      .persist(event)
      .thenReply(cmd.replyTo)(_ => FileEntity.Success)
  }

  def updateFileName(cmd: FileEntity.UpdateFileName): ReplyEffect[FileEntity.Event, FileEntity] =
    maybeState match {
      case None    => Effect.reply(cmd.replyTo)(FileEntity.FileNotFound)
      case Some(_) =>
        val event = cmd.transformInto[FileEntity.FileNameUpdated]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => FileEntity.Success)
    }

  def removeFile(cmd: FileEntity.RemoveFile): ReplyEffect[FileEntity.Event, FileEntity] =
    maybeState match {
      case None    => Effect.reply(cmd.replyTo)(FileEntity.FileNotFound)
      case Some(_) =>
        val event = cmd.transformInto[FileEntity.FileRemoved]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => FileEntity.Success)
    }

  def applyEvent(event: FileEntity.Event): FileEntity =
    event match {
      case event: FileEntity.FileStored      => onFileStored(event)
      case event: FileEntity.FileNameUpdated => onFileNameUpdated(event)
      case _: FileEntity.FileRemoved         => onFileRemoved()

    }

  def onFileStored(event: FileEntity.FileStored): FileEntity =
    FileEntity(
      Some(
        event
          .transformInto[FileState]
      )
    )

  def onFileNameUpdated(event: FileEntity.FileNameUpdated): FileEntity =
    FileEntity(
      maybeState.map(
        _.copy(
          name = event.name,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

  def onFileRemoved(): FileEntity =
    FileEntity(None)

}
