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

package biz.lobachev.annette.cms.impl.blogs.blog

import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl.{EntityContext, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import biz.lobachev.annette.cms.api.blogs.blog._
import biz.lobachev.annette.cms.impl.blogs.blog.model.BlogState
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.category.CategoryId
import com.lightbend.lagom.scaladsl.persistence._
import biz.lobachev.annette.core.utils.ChimneyCommons._
import io.scalaland.chimney.dsl._
import org.slf4j.LoggerFactory
import play.api.libs.json._

import java.time.OffsetDateTime

object BlogEntity {

  trait CommandSerializable
  sealed trait Command extends CommandSerializable

  final case class CreateBlog(
    id: BlogId,
    name: String,
    description: String,
    categoryId: CategoryId,
    authors: Set[AnnettePrincipal] = Set.empty,
    targets: Set[AnnettePrincipal] = Set.empty,
    createdBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class UpdateBlogName(
    id: BlogId,
    name: String,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class UpdateBlogDescription(
    id: BlogId,
    description: String,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class UpdateBlogCategoryId(
    id: BlogId,
    categoryId: CategoryId,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class AssignBlogAuthorPrincipal(
    id: BlogId,
    principal: AnnettePrincipal,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class UnassignBlogAuthorPrincipal(
    id: BlogId,
    principal: AnnettePrincipal,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class AssignBlogTargetPrincipal(
    id: BlogId,
    principal: AnnettePrincipal,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class UnassignBlogTargetPrincipal(
    id: BlogId,
    principal: AnnettePrincipal,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class ActivateBlog(id: BlogId, updatedBy: AnnettePrincipal, replyTo: ActorRef[Confirmation])
      extends Command

  final case class DeactivateBlog(id: BlogId, updatedBy: AnnettePrincipal, replyTo: ActorRef[Confirmation])
      extends Command

  final case class DeleteBlog(id: BlogId, deletedBy: AnnettePrincipal, replyTo: ActorRef[Confirmation]) extends Command

  final case class GetBlog(id: BlogId, replyTo: ActorRef[Confirmation]) extends Command

  sealed trait Confirmation
  final case object Success                                       extends Confirmation
  final case class SuccessBlog(blog: Blog)                        extends Confirmation
  final case class SuccessTargets(targets: Set[AnnettePrincipal]) extends Confirmation
  final case object BlogAlreadyExist                              extends Confirmation
  final case object BlogNotFound                                  extends Confirmation

  implicit val confirmationSuccessFormat: Format[Success.type]                   = Json.format
  implicit val confirmationSuccessBlogFormat: Format[SuccessBlog]                = Json.format
  implicit val confirmationSuccessTargetsFormat: Format[SuccessTargets]          = Json.format
  implicit val confirmationBlogAlreadyExistFormat: Format[BlogAlreadyExist.type] = Json.format
  implicit val confirmationBlogNotFoundFormat: Format[BlogNotFound.type]         = Json.format

  sealed trait Event extends AggregateEvent[Event] {
    override def aggregateTag: AggregateEventTagger[Event] = Event.Tag
  }

  object Event {
    val Tag: AggregateEventShards[Event] = AggregateEventTag.sharded[Event](numShards = 10)
  }

  final case class BlogCreated(
    id: BlogId,
    name: String,
    description: String,
    categoryId: CategoryId,
    authors: Set[AnnettePrincipal] = Set.empty,
    targets: Set[AnnettePrincipal] = Set.empty,
    createdBy: AnnettePrincipal,
    createdAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class BlogNameUpdated(
    id: BlogId,
    name: String,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class BlogDescriptionUpdated(
    id: BlogId,
    description: String,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class BlogCategoryUpdated(
    id: BlogId,
    categoryId: CategoryId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class BlogAuthorPrincipalAssigned(
    id: BlogId,
    principal: AnnettePrincipal,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class BlogAuthorPrincipalUnassigned(
    id: BlogId,
    principal: AnnettePrincipal,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class BlogTargetPrincipalAssigned(
    id: BlogId,
    principal: AnnettePrincipal,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class BlogTargetPrincipalUnassigned(
    id: BlogId,
    principal: AnnettePrincipal,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class BlogActivated(
    id: BlogId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class BlogDeactivated(
    id: BlogId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class BlogDeleted(id: BlogId, deletedBy: AnnettePrincipal, deleteAt: OffsetDateTime = OffsetDateTime.now)
      extends Event

  implicit val eventBlogCreatedFormat: Format[BlogCreated]                                     = Json.format
  implicit val eventBlogNameUpdatedFormat: Format[BlogNameUpdated]                             = Json.format
  implicit val eventBlogDescriptionUpdatedFormat: Format[BlogDescriptionUpdated]               = Json.format
  implicit val eventBlogCategoryUpdatedFormat: Format[BlogCategoryUpdated]                     = Json.format
  implicit val eventBlogAuthorPrincipalAssignedFormat: Format[BlogAuthorPrincipalAssigned]     = Json.format
  implicit val eventBlogAuthorPrincipalUnassignedFormat: Format[BlogAuthorPrincipalUnassigned] = Json.format
  implicit val eventBlogTargetPrincipalAssignedFormat: Format[BlogTargetPrincipalAssigned]     = Json.format
  implicit val eventBlogTargetPrincipalUnassignedFormat: Format[BlogTargetPrincipalUnassigned] = Json.format
  implicit val eventBlogActivatedFormat: Format[BlogActivated]                                 = Json.format
  implicit val eventBlogDeactivatedFormat: Format[BlogDeactivated]                             = Json.format
  implicit val eventBlogDeletedFormat: Format[BlogDeleted]                                     = Json.format

  val empty = BlogEntity()

  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Cms_Blog")

  def apply(persistenceId: PersistenceId): EventSourcedBehavior[Command, Event, BlogEntity] =
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, BlogEntity](
        persistenceId = persistenceId,
        emptyState = BlogEntity.empty,
        commandHandler = (entity, cmd) => entity.applyCommand(cmd),
        eventHandler = (entity, evt) => entity.applyEvent(evt)
      )

  def apply(entityContext: EntityContext[Command]): Behavior[Command] =
    apply(PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId))
      .withTagger(AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2))

  implicit val entityFormat: Format[BlogEntity] = Json.format

}

final case class BlogEntity(maybeState: Option[BlogState] = None) {
  import BlogEntity._

  val log = LoggerFactory.getLogger(this.getClass)

  def applyCommand(cmd: Command): ReplyEffect[Event, BlogEntity] =
    cmd match {
      case cmd: CreateBlog                  => createBlog(cmd)
      case cmd: UpdateBlogName              => updateBlogName(cmd)
      case cmd: UpdateBlogDescription       => updateBlogDescription(cmd)
      case cmd: UpdateBlogCategoryId        => updateBlogCategory(cmd)
      case cmd: AssignBlogAuthorPrincipal   => assignBlogAuthorPrincipal(cmd)
      case cmd: UnassignBlogAuthorPrincipal => unassignBlogAuthorPrincipal(cmd)
      case cmd: AssignBlogTargetPrincipal   => assignBlogTargetPrincipal(cmd)
      case cmd: UnassignBlogTargetPrincipal => unassignBlogTargetPrincipal(cmd)
      case cmd: ActivateBlog                => activateBlog(cmd)
      case cmd: DeactivateBlog              => deactivateBlog(cmd)
      case cmd: DeleteBlog                  => deleteBlog(cmd)
      case cmd: GetBlog                     => getBlog(cmd)
    }

  def createBlog(cmd: CreateBlog): ReplyEffect[Event, BlogEntity] =
    maybeState match {
      case None    =>
        val event = cmd
          .transformInto[BlogCreated]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
      case Some(_) => Effect.reply(cmd.replyTo)(BlogAlreadyExist)
    }

  def updateBlogName(cmd: UpdateBlogName): ReplyEffect[Event, BlogEntity] =
    maybeState match {
      case None    => Effect.reply(cmd.replyTo)(BlogNotFound)
      case Some(_) =>
        val event = cmd.transformInto[BlogNameUpdated]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def updateBlogDescription(cmd: UpdateBlogDescription): ReplyEffect[Event, BlogEntity] =
    maybeState match {
      case None    => Effect.reply(cmd.replyTo)(BlogNotFound)
      case Some(_) =>
        val event = cmd.transformInto[BlogDescriptionUpdated]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def updateBlogCategory(cmd: UpdateBlogCategoryId): ReplyEffect[Event, BlogEntity] =
    maybeState match {
      case None    => Effect.reply(cmd.replyTo)(BlogNotFound)
      case Some(_) =>
        val event = cmd.transformInto[BlogCategoryUpdated]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def assignBlogAuthorPrincipal(cmd: AssignBlogAuthorPrincipal): ReplyEffect[Event, BlogEntity] =
    maybeState match {
      case None                                                 => Effect.reply(cmd.replyTo)(BlogNotFound)
      case Some(state) if state.authors.contains(cmd.principal) => Effect.reply(cmd.replyTo)(Success)
      case Some(_)                                              =>
        val event = cmd.transformInto[BlogAuthorPrincipalAssigned]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def unassignBlogAuthorPrincipal(cmd: UnassignBlogAuthorPrincipal): ReplyEffect[Event, BlogEntity] =
    maybeState match {
      case None                                                  => Effect.reply(cmd.replyTo)(BlogNotFound)
      case Some(state) if !state.authors.contains(cmd.principal) => Effect.reply(cmd.replyTo)(Success)
      case Some(_)                                               =>
        val event = cmd.transformInto[BlogAuthorPrincipalUnassigned]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }
  def assignBlogTargetPrincipal(cmd: AssignBlogTargetPrincipal): ReplyEffect[Event, BlogEntity]     =
    maybeState match {
      case None                                                 => Effect.reply(cmd.replyTo)(BlogNotFound)
      case Some(state) if state.targets.contains(cmd.principal) => Effect.reply(cmd.replyTo)(Success)
      case Some(_)                                              =>
        val event = cmd.transformInto[BlogTargetPrincipalAssigned]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def unassignBlogTargetPrincipal(cmd: UnassignBlogTargetPrincipal): ReplyEffect[Event, BlogEntity] =
    maybeState match {
      case None                                                  => Effect.reply(cmd.replyTo)(BlogNotFound)
      case Some(state) if !state.targets.contains(cmd.principal) => Effect.reply(cmd.replyTo)(Success)
      case Some(_)                                               =>
        val event = cmd.transformInto[BlogTargetPrincipalUnassigned]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def activateBlog(cmd: ActivateBlog): ReplyEffect[Event, BlogEntity] =
    maybeState match {
      case None                         => Effect.reply(cmd.replyTo)(BlogNotFound)
      case Some(state) if !state.active =>
        val event = cmd.transformInto[BlogActivated]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
      case _                            => Effect.reply(cmd.replyTo)(Success)
    }

  def deactivateBlog(cmd: DeactivateBlog): ReplyEffect[Event, BlogEntity] =
    maybeState match {
      case None                        => Effect.reply(cmd.replyTo)(BlogNotFound)
      case Some(state) if state.active =>
        val event = cmd.transformInto[BlogDeactivated]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
      case _                           => Effect.reply(cmd.replyTo)(Success)
    }

  def deleteBlog(cmd: DeleteBlog): ReplyEffect[Event, BlogEntity] =
    maybeState match {
      case None    => Effect.reply(cmd.replyTo)(BlogNotFound)
      case Some(_) =>
        val event = cmd.transformInto[BlogDeleted]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def getBlog(cmd: GetBlog): ReplyEffect[Event, BlogEntity] =
    maybeState match {
      case None        => Effect.reply(cmd.replyTo)(BlogNotFound)
      case Some(state) => Effect.reply(cmd.replyTo)(SuccessBlog(state.transformInto[Blog]))
    }

  def applyEvent(event: Event): BlogEntity =
    event match {
      case event: BlogCreated                   => onBlogCreated(event)
      case event: BlogNameUpdated               => onBlogNameUpdated(event)
      case event: BlogDescriptionUpdated        => onBlogDescriptionUpdated(event)
      case event: BlogCategoryUpdated           => onBlogCategoryUpdated(event)
      case event: BlogAuthorPrincipalAssigned   => onBlogAuthorPrincipalAssigned(event)
      case event: BlogAuthorPrincipalUnassigned => onBlogAuthorPrincipalUnassigned(event)
      case event: BlogTargetPrincipalAssigned   => onBlogTargetPrincipalAssigned(event)
      case event: BlogTargetPrincipalUnassigned => onBlogTargetPrincipalUnassigned(event)
      case event: BlogActivated                 => onBlogActivated(event)
      case event: BlogDeactivated               => onBlogDeactivated(event)
      case _: BlogDeleted                       => onBlogDeleted()
    }

  def onBlogCreated(event: BlogCreated): BlogEntity =
    BlogEntity(
      Some(
        event
          .into[BlogState]
          .withFieldConst(_.active, true)
          .withFieldConst(_.updatedBy, event.createdBy)
          .withFieldConst(_.updatedAt, event.createdAt)
          .transform
      )
    )

  def onBlogNameUpdated(event: BlogNameUpdated): BlogEntity =
    BlogEntity(
      maybeState.map(
        _.copy(
          name = event.name,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

  def onBlogDescriptionUpdated(event: BlogDescriptionUpdated): BlogEntity =
    BlogEntity(
      maybeState.map(
        _.copy(
          description = event.description,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

  def onBlogCategoryUpdated(event: BlogCategoryUpdated): BlogEntity =
    BlogEntity(
      maybeState.map(
        _.copy(
          categoryId = event.categoryId,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

  def onBlogAuthorPrincipalAssigned(event: BlogAuthorPrincipalAssigned): BlogEntity =
    BlogEntity(
      maybeState.map(state =>
        state.copy(
          authors = state.authors + event.principal,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

  def onBlogAuthorPrincipalUnassigned(event: BlogAuthorPrincipalUnassigned): BlogEntity =
    BlogEntity(
      maybeState.map(state =>
        state.copy(
          authors = state.authors - event.principal,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

  def onBlogTargetPrincipalAssigned(event: BlogTargetPrincipalAssigned): BlogEntity =
    BlogEntity(
      maybeState.map(state =>
        state.copy(
          targets = state.targets + event.principal,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

  def onBlogTargetPrincipalUnassigned(event: BlogTargetPrincipalUnassigned): BlogEntity =
    BlogEntity(
      maybeState.map(state =>
        state.copy(
          targets = state.targets - event.principal,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

  def onBlogActivated(event: BlogActivated): BlogEntity =
    BlogEntity(
      maybeState.map(state =>
        state.copy(
          active = true,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

  def onBlogDeactivated(event: BlogDeactivated): BlogEntity =
    BlogEntity(
      maybeState.map(state =>
        state.copy(
          active = false,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

  def onBlogDeleted(): BlogEntity =
    BlogEntity(None)

}
