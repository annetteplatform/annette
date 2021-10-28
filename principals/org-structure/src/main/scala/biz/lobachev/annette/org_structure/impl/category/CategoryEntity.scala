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

package biz.lobachev.annette.org_structure.impl.category

import java.time.OffsetDateTime
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl._
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.org_structure.api.category.CreateCategoryPayload
import biz.lobachev.annette.org_structure.api.category._
import biz.lobachev.annette.org_structure.impl.category.model.CategoryState
import com.lightbend.lagom.scaladsl.persistence._
import io.scalaland.chimney.dsl._
import play.api.libs.json.{Format, _}

object CategoryEntity {

  trait CommandSerializable

  sealed trait Command extends CommandSerializable

  final case class CreateCategory(payload: CreateCategoryPayload, replyTo: ActorRef[Confirmation]) extends Command
  final case class UpdateCategory(payload: UpdateCategoryPayload, replyTo: ActorRef[Confirmation]) extends Command
  final case class DeleteCategory(payload: DeleteCategoryPayload, replyTo: ActorRef[Confirmation]) extends Command
  final case class GetCategory(id: OrgCategoryId, replyTo: ActorRef[Confirmation])                 extends Command

  sealed trait Confirmation
  final case object Success                             extends Confirmation
  final case class SuccessCategory(entity: OrgCategory) extends Confirmation
  final case object NotFound                            extends Confirmation
  final case object AlreadyExist                        extends Confirmation

  implicit val confirmationSuccessFormat: Format[Success.type]            = Json.format
  implicit val confirmationSuccessCategoryFormat: Format[SuccessCategory] = Json.format
  implicit val confirmationNotFoundFormat: Format[NotFound.type]          = Json.format
  implicit val confirmationAlreadyExistFormat: Format[AlreadyExist.type]  = Json.format

  implicit val confirmationFormat: Format[Confirmation] = Json.format

  sealed trait Event extends AggregateEvent[Event] {
    override def aggregateTag: AggregateEventTagger[Event] = Event.Tag
  }

  object Event {
    val Tag: AggregateEventShards[Event] = AggregateEventTag.sharded[Event](numShards = 10)
  }

  final case class CategoryCreated(
    id: OrgCategoryId,
    name: String,
    forOrganization: Boolean = false,
    forUnit: Boolean = false,
    forPosition: Boolean = false,
    createdBy: AnnettePrincipal,
    createdAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class CategoryUpdated(
    id: OrgCategoryId,
    name: String,
    forOrganization: Boolean = false,
    forUnit: Boolean = false,
    forPosition: Boolean = false,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class CategoryDeleted(
    id: OrgCategoryId, // category id
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  implicit val categoryCreatedFormat: Format[CategoryCreated]     = Json.format
  implicit val categoryUpdatedFormat: Format[CategoryUpdated]     = Json.format
  implicit val categoryDeactivatedFormat: Format[CategoryDeleted] = Json.format

  val empty                           = CategoryEntity(None)
  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Category")

  def apply(persistenceId: PersistenceId): EventSourcedBehavior[Command, Event, CategoryEntity] =
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, CategoryEntity](
        persistenceId = persistenceId,
        emptyState = CategoryEntity.empty,
        commandHandler = (entity, cmd) => entity.applyCommand(cmd),
        eventHandler = (entity, evt) => entity.applyEvent(evt)
      )

  def apply(entityContext: EntityContext[Command]): Behavior[Command] =
    apply(PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId))
      .withTagger(AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2))

  implicit val categoryEntityFormat: Format[CategoryEntity] = Json.format
}

final case class CategoryEntity(maybeState: Option[CategoryState]) {

  import CategoryEntity._

  def applyCommand(cmd: Command): ReplyEffect[Event, CategoryEntity] =
    cmd match {
      case CreateCategory(payload, replyTo) => createCategory(payload, replyTo)
      case UpdateCategory(payload, replyTo) => updateCategory(payload, replyTo)
      case DeleteCategory(payload, replyTo) => deleteCategory(payload, replyTo)
      case GetCategory(_, replyTo)          => getCategory(replyTo)
    }

  def createCategory(
    payload: CreateCategoryPayload,
    replyTo: ActorRef[Confirmation]
  ): ReplyEffect[Event, CategoryEntity] =
    maybeState match {
      case None    =>
        val event = payload.transformInto[CategoryCreated]
        Effect
          .persist(event)
          .thenReply(replyTo)(_ => Success)
      case Some(_) => Effect.reply(replyTo)(AlreadyExist)
    }

  def updateCategory(
    payload: UpdateCategoryPayload,
    replyTo: ActorRef[Confirmation]
  ): ReplyEffect[Event, CategoryEntity] =
    maybeState match {
      case None    => Effect.reply(replyTo)(NotFound)
      case Some(_) =>
        val event = payload.transformInto[CategoryUpdated]
        Effect
          .persist(event)
          .thenReply(replyTo)(_ => Success)
    }

  def deleteCategory(
    payload: DeleteCategoryPayload,
    replyTo: ActorRef[Confirmation]
  ): ReplyEffect[Event, CategoryEntity] =
    maybeState match {
      case None    => Effect.reply(replyTo)(NotFound)
      case Some(_) =>
        val event = payload.transformInto[CategoryDeleted]
        Effect
          .persist(event)
          .thenReply(replyTo)(_ => Success)
    }

  def getCategory(replyTo: ActorRef[Confirmation]): ReplyEffect[Event, CategoryEntity] =
    maybeState match {
      case Some(state) => Effect.reply(replyTo)(SuccessCategory(state.toCategory))
      case None        => Effect.reply(replyTo)(NotFound)
    }

  def applyEvent(evt: Event): CategoryEntity =
    evt match {
      case event: CategoryCreated => onCategoryCreated(event)
      case event: CategoryUpdated => onCategoryUpdated(event)
      case _: CategoryDeleted     => onCategoryDeleted()
    }

  def onCategoryCreated(event: CategoryCreated): CategoryEntity =
    CategoryEntity(
      Some(
        event
          .into[CategoryState]
          .withFieldConst(_.updatedAt, event.createdAt)
          .withFieldConst(_.updatedBy, event.createdBy)
          .transform
      )
    )

  def onCategoryUpdated(event: CategoryUpdated): CategoryEntity =
    CategoryEntity(
      Some(
        event
          .into[CategoryState]
          .withFieldConst(_.updatedAt, event.updatedAt)
          .transform
      )
    )

  def onCategoryDeleted(): CategoryEntity =
    CategoryEntity(None)

}
