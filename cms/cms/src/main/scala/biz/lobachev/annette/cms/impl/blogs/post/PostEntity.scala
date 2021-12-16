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

package biz.lobachev.annette.cms.impl.blogs.post

import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl.{EntityContext, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import biz.lobachev.annette.cms.api.blogs.blog.BlogId
import biz.lobachev.annette.cms.api.blogs.post._
import biz.lobachev.annette.cms.api.common.article.PublicationStatus
import biz.lobachev.annette.cms.api.content.ContentTypes
import biz.lobachev.annette.cms.api.content.ContentTypes.ContentType
import biz.lobachev.annette.cms.impl.blogs.post.model.{PostInt, PostState}
import biz.lobachev.annette.cms.impl.content.{ContentInt, WidgetInt}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import com.lightbend.lagom.scaladsl.persistence._
import io.scalaland.chimney.dsl._
import org.slf4j.LoggerFactory
import play.api.libs.json._

import java.time.OffsetDateTime

object PostEntity {

  trait CommandSerializable
  sealed trait Command extends CommandSerializable
  final case class CreatePost(
    id: PostId,
    blogId: BlogId,
    featured: Boolean,
    authorId: AnnettePrincipal,
    title: String,
    introContent: ContentInt,
    content: ContentInt,
    targets: Set[AnnettePrincipal] = Set.empty,
    createdBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  )                    extends Command

  final case class UpdatePostFeatured(
    id: PostId,
    featured: Boolean,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class UpdatePostAuthor(
    id: PostId,
    authorId: AnnettePrincipal,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class UpdatePostTitle(
    id: PostId,
    title: String,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class UpdateContentSettings(
    id: String,
    contentType: ContentType,
    settings: String,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class UpdateWidget(
    id: PostId,
    contentType: ContentType,
    widget: WidgetInt,
    order: Option[Int] = None,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  case class ChangeWidgetOrder(
    id: PostId,
    contentType: ContentType,
    widgetId: String,
    order: Int,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  case class DeleteWidget(
    id: PostId,
    contentType: ContentType,
    widgetId: String,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class UpdatePostPublicationTimestamp(
    id: PostId,
    publicationTimestamp: Option[OffsetDateTime],
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class PublishPost(id: PostId, updatedBy: AnnettePrincipal, replyTo: ActorRef[Confirmation]) extends Command

  final case class UnpublishPost(id: PostId, updatedBy: AnnettePrincipal, replyTo: ActorRef[Confirmation])
      extends Command

  final case class AssignPostTargetPrincipal(
    id: PostId,
    principal: AnnettePrincipal,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class UnassignPostTargetPrincipal(
    id: PostId,
    principal: AnnettePrincipal,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class DeletePost(
    id: PostId,
    deletedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class GetPost(
    id: PostId,
    withIntro: Boolean,
    withContent: Boolean,
    withTargets: Boolean,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  sealed trait Confirmation
  final case class Success(updatedBy: AnnettePrincipal, updatedAt: OffsetDateTime) extends Confirmation
  final case class SuccessPost(post: PostInt)                                      extends Confirmation
  final case object PostAlreadyExist                                               extends Confirmation
  final case object PostNotFound                                                   extends Confirmation
  final case object WidgetNotFound                                                 extends Confirmation
  final case object PostPublicationDateClearNotAllowed                             extends Confirmation

  implicit val confirmationSuccessFormat: Format[Success]                                                            = Json.format
  implicit val confirmationSuccessPostFormat: Format[SuccessPost]                                                    = Json.format
  implicit val confirmationPostAlreadyExistFormat: Format[PostAlreadyExist.type]                                     = Json.format
  implicit val confirmationPostNotFoundFormat: Format[PostNotFound.type]                                             = Json.format
  implicit val confirmationWidgetContentNotFoundFormat: Format[WidgetNotFound.type]                                  = Json.format
  implicit val confirmationPostPublicationDateClearNotAllowedFormat: Format[PostPublicationDateClearNotAllowed.type] =
    Json.format

  sealed trait Event extends AggregateEvent[Event] {
    override def aggregateTag: AggregateEventTagger[Event] = Event.Tag
  }

  object Event {
    val Tag: AggregateEventShards[Event] = AggregateEventTag.sharded[Event](numShards = 10)
  }

  final case class PostCreated(
    id: PostId,
    blogId: BlogId,
    featured: Boolean,
    authorId: AnnettePrincipal,
    title: String,
    introContent: ContentInt,
    content: ContentInt,
    targets: Set[AnnettePrincipal] = Set.empty,
    createdBy: AnnettePrincipal,
    createdAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class PostFeaturedUpdated(
    id: PostId,
    featured: Boolean,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class PostAuthorUpdated(
    id: PostId,
    authorId: AnnettePrincipal,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class PostTitleUpdated(
    id: PostId,
    title: String,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class ContentSettingsUpdated(
    id: String,
    contentType: ContentType,
    settings: String,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class PostWidgetUpdated(
    id: PostId,
    contentType: ContentType,
    widget: WidgetInt,
    widgetOrder: Seq[String],
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  case class WidgetOrderChanged(
    id: PostId,
    contentType: ContentType,
    widgetId: String,
    widgetOrder: Seq[String],
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  case class WidgetDeleted(
    id: PostId,
    contentType: ContentType,
    widgetId: String,
    widgetOrder: Seq[String],
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  case class PostIndexChanged(
    id: PostId,
    contentType: ContentType,
    indexData: String,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class PostPublicationTimestampUpdated(
    id: PostId,
    publicationTimestamp: Option[OffsetDateTime],
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class PostPublished(
    id: PostId,
    publicationTimestamp: OffsetDateTime,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class PostUnpublished(
    id: PostId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class PostTargetPrincipalAssigned(
    id: PostId,
    principal: AnnettePrincipal,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class PostTargetPrincipalUnassigned(
    id: PostId,
    principal: AnnettePrincipal,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class PostDeleted(
    id: PostId,
    deletedBy: AnnettePrincipal,
    deleteAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  implicit val eventPostCreatedFormat: Format[PostCreated]                                         = Json.format
  implicit val eventPostFeaturedUpdatedFormat: Format[PostFeaturedUpdated]                         = Json.format
  implicit val eventPostAuthorUpdatedFormat: Format[PostAuthorUpdated]                             = Json.format
  implicit val eventPostTitleUpdatedFormat: Format[PostTitleUpdated]                               = Json.format
  implicit val eventContentSettingsUpdatedFormat: Format[ContentSettingsUpdated]                   = Json.format
  implicit val eventPostWidgetUpdatedFormat: Format[PostWidgetUpdated]                             = Json.format
  implicit val eventWidgetOrderChangedFormat: Format[WidgetOrderChanged]                           = Json.format
  implicit val eventWidgetDeletedFormat: Format[WidgetDeleted]                                     = Json.format
  implicit val eventPostIndexChangedFormat: Format[PostIndexChanged]                               = Json.format
  implicit val eventPostPublicationTimestampUpdatedFormat: Format[PostPublicationTimestampUpdated] = Json.format
  implicit val eventPostPublishedFormat: Format[PostPublished]                                     = Json.format
  implicit val eventPostUnpublishedFormat: Format[PostUnpublished]                                 = Json.format
  implicit val eventPostTargetPrincipalAssignedFormat: Format[PostTargetPrincipalAssigned]         = Json.format
  implicit val eventPostTargetPrincipalUnassignedFormat: Format[PostTargetPrincipalUnassigned]     = Json.format
  implicit val eventPostDeletedFormat: Format[PostDeleted]                                         = Json.format

  val empty = PostEntity()

  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Cms_Post")

  def apply(persistenceId: PersistenceId): EventSourcedBehavior[Command, Event, PostEntity] =
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, PostEntity](
        persistenceId = persistenceId,
        emptyState = PostEntity.empty,
        commandHandler = (entity, cmd) => entity.applyCommand(cmd),
        eventHandler = (entity, evt) => entity.applyEvent(evt)
      )

  def apply(entityContext: EntityContext[Command]): Behavior[Command] =
    apply(PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId))
      .withTagger(AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2))

  implicit val entityFormat: Format[PostEntity] = Json.format

}

final case class PostEntity(maybeState: Option[PostState] = None) {
  import PostEntity._

  val log = LoggerFactory.getLogger(this.getClass)

  def applyCommand(cmd: Command): ReplyEffect[Event, PostEntity] =
    cmd match {
      case cmd: CreatePost                     => createPost(cmd)
      case cmd: UpdatePostFeatured             => updatePostFeatured(cmd)
      case cmd: UpdatePostAuthor               => updatePostAuthor(cmd)
      case cmd: UpdatePostTitle                => updatePostTitle(cmd)
      case cmd: UpdateContentSettings          => updateContentSettings(cmd)
      case cmd: UpdateWidget                   => updatePostWidgetContent(cmd)
      case cmd: ChangeWidgetOrder              => changeWidgetContentOrder(cmd)
      case cmd: DeleteWidget                   => deleteWidgetContent(cmd)
      case cmd: UpdatePostPublicationTimestamp => updatePostPublicationTimestamp(cmd)
      case cmd: PublishPost                    => publishPost(cmd)
      case cmd: UnpublishPost                  => unpublishPost(cmd)
      case cmd: AssignPostTargetPrincipal      => assignPostTargetPrincipal(cmd)
      case cmd: UnassignPostTargetPrincipal    => unassignPostTargetPrincipal(cmd)
      case cmd: DeletePost                     => deletePost(cmd)
      case cmd: GetPost                        => getPost(cmd)
    }

  def createPost(cmd: CreatePost): ReplyEffect[Event, PostEntity] =
    maybeState match {
      case None    =>
        val event = cmd
          .into[PostCreated]
          .transform
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(postEntity =>
            SuccessPost(
              postEntity.maybeState.get
                .into[PostInt]
                .withFieldComputed(_.introContent, c => Some(c.introContent))
                .withFieldComputed(_.content, c => Some(c.content))
                .withFieldComputed(_.targets, c => Some(c.targets))
                .transform
            )
          )
      case Some(_) => Effect.reply(cmd.replyTo)(PostAlreadyExist)
    }

  def updatePostFeatured(cmd: UpdatePostFeatured): ReplyEffect[Event, PostEntity] =
    maybeState match {
      case None    => Effect.reply(cmd.replyTo)(PostNotFound)
      case Some(_) =>
        val event = cmd.transformInto[PostFeaturedUpdated]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success(event.updatedBy, event.updatedAt))
    }

  def updatePostAuthor(cmd: UpdatePostAuthor): ReplyEffect[Event, PostEntity] =
    maybeState match {
      case None    => Effect.reply(cmd.replyTo)(PostNotFound)
      case Some(_) =>
        val event = cmd.transformInto[PostAuthorUpdated]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success(event.updatedBy, event.updatedAt))
    }

  def updatePostTitle(cmd: UpdatePostTitle): ReplyEffect[Event, PostEntity] =
    maybeState match {
      case None    => Effect.reply(cmd.replyTo)(PostNotFound)
      case Some(_) =>
        val event = cmd.transformInto[PostTitleUpdated]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success(event.updatedBy, event.updatedAt))
    }

  def updateContentSettings(cmd: UpdateContentSettings): ReplyEffect[Event, PostEntity] =
    maybeState match {
      case None    => Effect.reply(cmd.replyTo)(PostNotFound)
      case Some(_) =>
        val updateEvent = cmd
          .into[ContentSettingsUpdated]
          .transform
        Effect
          .persist(updateEvent)
          .thenReply(cmd.replyTo)(_ => Success(updateEvent.updatedBy, updateEvent.updatedAt))
      case _       => Effect.reply(cmd.replyTo)(WidgetNotFound)
    }

  def updatePostWidgetContent(cmd: UpdateWidget): ReplyEffect[Event, PostEntity] =
    maybeState match {
      case None        => Effect.reply(cmd.replyTo)(PostNotFound)
      // update
      case Some(state)
          if (cmd.contentType == ContentTypes.Intro && state.introContent.widgets.contains(cmd.widget.id)) ||
            (cmd.contentType == ContentTypes.Post && state.content.widgets.contains(cmd.widget.id)) =>
        val contentMap          = cmd.contentType match {
          case ContentTypes.Intro => state.introContent
          case ContentTypes.Post  => state.content
        }
        val updatedContentOrder = cmd.order.map { order =>
          val newContentOrder = contentMap.widgetOrder.filter(_ != cmd.widget.id)
          if (order >= 0 && order < newContentOrder.length)
            (newContentOrder.take(order) :+ cmd.widget.id) ++ newContentOrder.drop(order)
          else newContentOrder :+ cmd.widget.id
        }.getOrElse(contentMap.widgetOrder)

        val updateEvent = cmd
          .into[PostWidgetUpdated]
          .withFieldConst(_.widgetOrder, updatedContentOrder)
          .transform
        val indexData   =
          if (contentMap.widgets(cmd.widget.id).indexData != cmd.widget.indexData)
            Some(
              contentMap.widgets.map {
                case widgetContentId -> _ if widgetContentId == cmd.widget.id => cmd.widget.indexData
                case _ -> widgetContent                                       => widgetContent.indexData
              }.flatten.mkString("\n")
            )
          else None
        val indexEvent  = indexData.map(idx =>
          cmd
            .into[PostIndexChanged]
            .withFieldConst(_.indexData, idx)
            .transform
        )
        Effect
          .persist(Seq(updateEvent) ++ indexEvent.toSeq)
          .thenReply(cmd.replyTo)(_ => Success(updateEvent.updatedBy, updateEvent.updatedAt))

      // create
      case Some(state) =>
        val contentMap          = cmd.contentType match {
          case ContentTypes.Intro => state.introContent
          case ContentTypes.Post  => state.content
        }
        val updatedContentOrder = cmd.order.map { order =>
          val newContentOrder = contentMap.widgetOrder.filter(_ != cmd.widget.id)
          if (order >= 0 && order < newContentOrder.length)
            (newContentOrder.take(order) :+ cmd.widget.id) ++ newContentOrder.drop(order)
          else newContentOrder :+ cmd.widget.id
        }.getOrElse(contentMap.widgetOrder)
        val updateEvent         = cmd
          .into[PostWidgetUpdated]
          .withFieldConst(_.widgetOrder, updatedContentOrder)
          .transform
        val indexData           =
          cmd.widget.indexData.map { idx =>
            (contentMap.widgets.values.map(_.indexData).flatten.toSeq :+ idx).mkString("\n")
          }
        val indexEvent          = indexData.map(idx =>
          cmd
            .into[PostIndexChanged]
            .withFieldConst(_.indexData, idx)
            .transform
        )
        Effect
          .persist(Seq(updateEvent) ++ indexEvent.toSeq)
          .thenReply(cmd.replyTo)(_ => Success(updateEvent.updatedBy, updateEvent.updatedAt))
    }

  def changeWidgetContentOrder(cmd: ChangeWidgetOrder): ReplyEffect[Event, PostEntity] =
    maybeState match {
      case None => Effect.reply(cmd.replyTo)(PostNotFound)
      case Some(state)
          if (cmd.contentType == ContentTypes.Intro && state.introContent.widgets.contains(cmd.widgetId)) ||
            (cmd.contentType == ContentTypes.Post && state.content.widgets.contains(cmd.widgetId)) =>
        val (contentMap, contentOrder) = cmd.contentType match {
          case ContentTypes.Intro => state.introContent -> state.introContent.widgetOrder
          case ContentTypes.Post  => state.content      -> state.content.widgetOrder
        }
        val newOrder                   =
          if (cmd.order < 0) 0
          else if (cmd.order > contentMap.widgetOrder.length - 1) contentMap.widgetOrder.length - 1
          else cmd.order
        val currentOrder               = contentOrder.indexOf(cmd.widgetId)
        if (currentOrder != newOrder) {
          val newContentOrder     = contentOrder.filter(_ != cmd.widgetId)
          val updatedContentOrder =
            if (newOrder >= 0 && newOrder < newContentOrder.length)
              (newContentOrder.take(newOrder) :+ cmd.widgetId) ++ newContentOrder.drop(newOrder)
            else newContentOrder :+ cmd.widgetId
          val updateEvent         = cmd
            .into[WidgetOrderChanged]
            .withFieldConst(_.widgetOrder, updatedContentOrder)
            .transform

          Effect
            .persist(updateEvent)
            .thenReply(cmd.replyTo)(_ => Success(updateEvent.updatedBy, updateEvent.updatedAt))
        } else Effect.reply(cmd.replyTo)(Success(state.updatedBy, state.updatedAt))
      case _    => Effect.reply(cmd.replyTo)(WidgetNotFound)

    }

  def deleteWidgetContent(cmd: DeleteWidget): ReplyEffect[Event, PostEntity] =
    maybeState match {
      case None => Effect.reply(cmd.replyTo)(PostNotFound)
      case Some(state)
          if (cmd.contentType == ContentTypes.Intro && state.introContent.widgets.contains(cmd.widgetId)) ||
            (cmd.contentType == ContentTypes.Post && state.content.widgets.contains(cmd.widgetId)) =>
        val contentOrder = cmd.contentType match {
          case ContentTypes.Intro => state.introContent.widgetOrder
          case ContentTypes.Post  => state.content.widgetOrder
        }
        val updateEvent  = cmd
          .into[WidgetDeleted]
          .withFieldConst(_.widgetOrder, contentOrder.filter(_ != cmd.widgetId))
          .transform

        Effect
          .persist(updateEvent)
          .thenReply(cmd.replyTo)(_ => Success(updateEvent.updatedBy, updateEvent.updatedAt))
      case _    => Effect.reply(cmd.replyTo)(WidgetNotFound)
    }

  def updatePostPublicationTimestamp(cmd: UpdatePostPublicationTimestamp): ReplyEffect[Event, PostEntity] =
    maybeState match {
      case None    => Effect.reply(cmd.replyTo)(PostNotFound)
      case Some(state)
          if state.publicationStatus == PublicationStatus.Published &&
            cmd.publicationTimestamp.isEmpty =>
        Effect.reply(cmd.replyTo)(PostPublicationDateClearNotAllowed)
      case Some(_) =>
        val event = cmd.transformInto[PostPublicationTimestampUpdated]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success(event.updatedBy, event.updatedAt))
    }

  def publishPost(cmd: PublishPost): ReplyEffect[Event, PostEntity] =
    maybeState match {
      case None        => Effect.reply(cmd.replyTo)(PostNotFound)
      case Some(state) =>
        val event = cmd
          .into[PostPublished]
          .withFieldConst(_.publicationTimestamp, state.publicationTimestamp.getOrElse(OffsetDateTime.now))
          .transform
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success(event.updatedBy, event.updatedAt))
    }

  def unpublishPost(cmd: UnpublishPost): ReplyEffect[Event, PostEntity] =
    maybeState match {
      case None    => Effect.reply(cmd.replyTo)(PostNotFound)
      case Some(_) =>
        val event = cmd.transformInto[PostUnpublished]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success(event.updatedBy, event.updatedAt))
    }

  def assignPostTargetPrincipal(cmd: AssignPostTargetPrincipal): ReplyEffect[Event, PostEntity] =
    maybeState match {
      case None                                                 => Effect.reply(cmd.replyTo)(PostNotFound)
      case Some(state) if state.targets.contains(cmd.principal) =>
        Effect.reply(cmd.replyTo)(Success(state.updatedBy, state.updatedAt))
      case Some(_)                                              =>
        val event = cmd.transformInto[PostTargetPrincipalAssigned]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success(event.updatedBy, event.updatedAt))
    }

  def unassignPostTargetPrincipal(cmd: UnassignPostTargetPrincipal): ReplyEffect[Event, PostEntity] =
    maybeState match {
      case None                                                  => Effect.reply(cmd.replyTo)(PostNotFound)
      case Some(state) if !state.targets.contains(cmd.principal) =>
        Effect.reply(cmd.replyTo)(Success(state.updatedBy, state.updatedAt))
      case Some(_)                                               =>
        val event = cmd.transformInto[PostTargetPrincipalUnassigned]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success(event.updatedBy, event.updatedAt))
    }

  def deletePost(cmd: DeletePost): ReplyEffect[Event, PostEntity] =
    maybeState match {
      case None    => Effect.reply(cmd.replyTo)(PostNotFound)
      case Some(_) =>
        val event = cmd.transformInto[PostDeleted]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success(event.deletedBy, event.deleteAt))
    }

  def getPost(cmd: GetPost): ReplyEffect[Event, PostEntity] =
    maybeState match {
      case None        => Effect.reply(cmd.replyTo)(PostNotFound)
      case Some(state) =>
        Effect.reply(cmd.replyTo)(
          SuccessPost(
            state
              .into[PostInt]
              .withFieldComputed(_.introContent, c => if (cmd.withIntro) Some(c.introContent) else None)
              .withFieldComputed(_.content, c => if (cmd.withContent) Some(c.content) else None)
              .withFieldComputed(_.targets, c => if (cmd.withTargets) Some(c.targets) else None)
              .transform
          )
        )
    }

  def applyEvent(event: Event): PostEntity =
    event match {
      case event: PostCreated                     => onPostCreated(event)
      case event: PostFeaturedUpdated             => onPostFeaturedUpdated(event)
      case event: PostAuthorUpdated               => onPostAuthorUpdated(event)
      case event: PostTitleUpdated                => onPostTitleUpdated(event)
      case event: ContentSettingsUpdated          => onContentSettingsUpdated(event)
      case event: PostWidgetUpdated               => onPostWidgetUpdated(event)
      case event: WidgetOrderChanged              => onWidgetOrderChanged(event)
      case event: WidgetDeleted                   => onWidgetDeleted(event)
      case _: PostIndexChanged                    => this
      case event: PostPublicationTimestampUpdated => onPostPublicationTimestampUpdated(event)
      case event: PostPublished                   => onPostPublished(event)
      case event: PostUnpublished                 => onPostUnpublished(event)
      case event: PostTargetPrincipalAssigned     => onPostTargetPrincipalAssigned(event)
      case event: PostTargetPrincipalUnassigned   => onPostTargetPrincipalUnassigned(event)
      case _: PostDeleted                         => onPostDeleted()
    }

  def onPostCreated(event: PostCreated): PostEntity =
    PostEntity(
      Some(
        event
          .into[PostState]
          .withFieldConst(_.updatedBy, event.createdBy)
          .withFieldConst(_.updatedAt, event.createdAt)
          .transform
      )
    )

  def onPostFeaturedUpdated(event: PostFeaturedUpdated): PostEntity =
    PostEntity(
      maybeState.map(
        _.copy(
          featured = event.featured,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

  def onPostAuthorUpdated(event: PostAuthorUpdated): PostEntity =
    PostEntity(
      maybeState.map(
        _.copy(
          authorId = event.authorId,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

  def onPostTitleUpdated(event: PostTitleUpdated): PostEntity =
    PostEntity(
      maybeState.map(
        _.copy(
          title = event.title,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

  def onContentSettingsUpdated(event: ContentSettingsUpdated): PostEntity =
    PostEntity(
      maybeState.map {
        case state if event.contentType == ContentTypes.Intro =>
          state.copy(
            introContent = state.introContent.copy(
              settings = event.settings
            ),
            updatedBy = event.updatedBy,
            updatedAt = event.updatedAt
          )

        case state if event.contentType == ContentTypes.Post  =>
          state.copy(
            content = state.content.copy(
              settings = event.settings
            ),
            updatedBy = event.updatedBy,
            updatedAt = event.updatedAt
          )
      }
    )

  def onPostWidgetUpdated(event: PostWidgetUpdated): PostEntity =
    PostEntity(
      maybeState.map {
        case state if event.contentType == ContentTypes.Intro =>
          state.copy(
            introContent = state.introContent.copy(
              widgetOrder = event.widgetOrder,
              widgets = state.introContent.widgets + (event.widget.id -> event.widget)
            ),
            updatedBy = event.updatedBy,
            updatedAt = event.updatedAt
          )

        case state if event.contentType == ContentTypes.Post  =>
          state.copy(
            content = state.content.copy(
              widgetOrder = event.widgetOrder,
              widgets = state.content.widgets + (event.widget.id -> event.widget)
            ),
            updatedBy = event.updatedBy,
            updatedAt = event.updatedAt
          )
      }
    )

  def onWidgetOrderChanged(event: WidgetOrderChanged): PostEntity =
    event.contentType match {
      case ContentTypes.Intro =>
        PostEntity(
          maybeState.map { state =>
            state.copy(
              introContent = state.introContent.copy(widgetOrder = event.widgetOrder),
              updatedBy = event.updatedBy,
              updatedAt = event.updatedAt
            )
          }
        )
      case ContentTypes.Post  =>
        PostEntity(
          maybeState.map { state =>
            state.copy(
              content = state.content.copy(widgetOrder = event.widgetOrder),
              updatedBy = event.updatedBy,
              updatedAt = event.updatedAt
            )
          }
        )
    }

  def onWidgetDeleted(event: WidgetDeleted): PostEntity =
    event.contentType match {
      case ContentTypes.Intro =>
        PostEntity(
          maybeState.map(state =>
            state.copy(
              introContent = state.introContent.copy(
                widgetOrder = event.widgetOrder,
                widgets = state.introContent.widgets - event.widgetId
              ),
              updatedBy = event.updatedBy,
              updatedAt = event.updatedAt
            )
          )
        )
      case ContentTypes.Post  =>
        PostEntity(
          maybeState.map(state =>
            state.copy(
              content = state.content.copy(
                widgetOrder = event.widgetOrder,
                widgets = state.content.widgets - event.widgetId
              ),
              updatedBy = event.updatedBy,
              updatedAt = event.updatedAt
            )
          )
        )
    }

  def onPostPublicationTimestampUpdated(event: PostPublicationTimestampUpdated): PostEntity =
    PostEntity(
      maybeState.map(
        _.copy(
          publicationTimestamp = event.publicationTimestamp,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

  def onPostPublished(event: PostPublished): PostEntity =
    PostEntity(
      maybeState.map(
        _.copy(
          publicationStatus = PublicationStatus.Published,
          publicationTimestamp = Some(event.publicationTimestamp),
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

  def onPostUnpublished(event: PostUnpublished): PostEntity =
    PostEntity(
      maybeState.map(
        _.copy(
          publicationStatus = PublicationStatus.Draft,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

  def onPostTargetPrincipalAssigned(event: PostTargetPrincipalAssigned): PostEntity =
    PostEntity(
      maybeState.map(state =>
        state.copy(
          targets = state.targets + event.principal,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

  def onPostTargetPrincipalUnassigned(event: PostTargetPrincipalUnassigned): PostEntity =
    PostEntity(
      maybeState.map(state =>
        state.copy(
          targets = state.targets - event.principal,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

  def onPostDeleted(): PostEntity =
    PostEntity(None)

}
