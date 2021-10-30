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
import biz.lobachev.annette.cms.api.content.{SerialContent, WidgetContent}
import ContentTypes.ContentType
import biz.lobachev.annette.cms.impl.blogs.post.model.PostState
import biz.lobachev.annette.cms.impl.content.Content
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
    introContent: SerialContent,
    content: SerialContent,
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

  final case class UpdateWidgetContent(
    id: PostId,
    contentType: ContentType,
    widgetContent: WidgetContent,
    order: Option[Int] = None,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  case class ChangeWidgetContentOrder(
    id: PostId,
    contentType: ContentType,
    widgetContentId: String,
    order: Int,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  case class DeleteWidgetContent(
    id: PostId,
    contentType: ContentType,
    widgetContentId: String,
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

  final case class GetPost(id: PostId, replyTo: ActorRef[Confirmation])           extends Command
  final case class GetPostAnnotation(id: PostId, replyTo: ActorRef[Confirmation]) extends Command
  final case class AddPostMedia(
    postId: PostId,
    mediaId: MediaId,
    filename: String,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  )                                                                               extends Command

  final case class RemovePostMedia(
    postId: PostId,
    mediaId: MediaId,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class AddPostDoc(
    postId: PostId,
    docId: DocId,
    name: String,
    filename: String,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class UpdatePostDocName(
    postId: PostId,
    docId: DocId,
    name: String,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class RemovePostDoc(
    postId: PostId,
    docId: DocId,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  sealed trait Confirmation
  final case object Success                                              extends Confirmation
  final case class SuccessPost(post: Post)                               extends Confirmation
  final case class SuccessPostAnnotation(postAnnotation: PostAnnotation) extends Confirmation
  final case object PostAlreadyExist                                     extends Confirmation
  final case object PostNotFound                                         extends Confirmation
  final case object WidgetContentNotFound                                extends Confirmation
  final case object PostPublicationDateClearNotAllowed                   extends Confirmation
  final case object PostMediaAlreadyExist                                extends Confirmation
  final case object PostMediaNotFound                                    extends Confirmation
  final case object PostDocAlreadyExist                                  extends Confirmation
  final case object PostDocNotFound                                      extends Confirmation

  implicit val confirmationSuccessFormat: Format[Success.type]                             = Json.format
  implicit val confirmationSuccessPostFormat: Format[SuccessPost]                          = Json.format
  implicit val confirmationSuccessPostAnnotationFormat: Format[SuccessPostAnnotation]      = Json.format
  implicit val confirmationPostAlreadyExistFormat: Format[PostAlreadyExist.type]           = Json.format
  implicit val confirmationPostNotFoundFormat: Format[PostNotFound.type]                   = Json.format
  implicit val confirmationPostMediaAlreadyExistFormat: Format[PostMediaAlreadyExist.type] = Json.format
  implicit val confirmationPostMediaNotFoundFormat: Format[PostMediaNotFound.type]         = Json.format
  implicit val confirmationPostDocAlreadyExistFormat: Format[PostDocAlreadyExist.type]     = Json.format
  implicit val confirmationPostDocNotFoundFormat: Format[PostDocNotFound.type]             = Json.format

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
    introContent: Content,
    content: Content,
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
  final case class PostWidgetContentUpdated(
    id: PostId,
    contentType: ContentType,
    widgetContent: WidgetContent,
    contentOrder: Seq[String],
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  case class WidgetContentOrderChanged(
    id: PostId,
    contentType: ContentType,
    widgetContentId: String,
    contentOrder: Seq[String],
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  case class WidgetContentDeleted(
    id: PostId,
    contentType: ContentType,
    widgetContentId: String,
    contentOrder: Seq[String],
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
  final case class PostMediaAdded(
    postId: PostId,
    mediaId: MediaId,
    filename: String,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class PostMediaRemoved(
    postId: PostId,
    mediaId: MediaId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class PostDocAdded(
    postId: PostId,
    docId: DocId,
    name: String,
    filename: String,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class PostDocNameUpdated(
    postId: PostId,
    docId: DocId,
    name: String,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class PostDocRemoved(
    postId: PostId,
    docId: DocId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  implicit val eventPostCreatedFormat: Format[PostCreated]                                         = Json.format
  implicit val eventPostFeaturedUpdatedFormat: Format[PostFeaturedUpdated]                         = Json.format
  implicit val eventPostAuthorUpdatedFormat: Format[PostAuthorUpdated]                             = Json.format
  implicit val eventPostTitleUpdatedFormat: Format[PostTitleUpdated]                               = Json.format
  implicit val eventPostWidgetContentUpdatedFormat: Format[PostWidgetContentUpdated]               = Json.format
  implicit val eventWidgetContentOrderChangedFormat: Format[WidgetContentOrderChanged]             = Json.format
  implicit val eventWidgetContentDeletedFormat: Format[WidgetContentDeleted]                       = Json.format
  implicit val eventPostIndexChangedFormat: Format[PostIndexChanged]                               = Json.format
  implicit val eventPostPublicationTimestampUpdatedFormat: Format[PostPublicationTimestampUpdated] = Json.format
  implicit val eventPostPublishedFormat: Format[PostPublished]                                     = Json.format
  implicit val eventPostUnpublishedFormat: Format[PostUnpublished]                                 = Json.format
  implicit val eventPostTargetPrincipalAssignedFormat: Format[PostTargetPrincipalAssigned]         = Json.format
  implicit val eventPostTargetPrincipalUnassignedFormat: Format[PostTargetPrincipalUnassigned]     = Json.format
  implicit val eventPostDeletedFormat: Format[PostDeleted]                                         = Json.format
  implicit val eventPostMediaAddedFormat: Format[PostMediaAdded]                                   = Json.format
  implicit val eventPostMediaRemovedFormat: Format[PostMediaRemoved]                               = Json.format
  implicit val eventPostDocAddedFormat: Format[PostDocAdded]                                       = Json.format
  implicit val eventPostDocNameUpdatedFormat: Format[PostDocNameUpdated]                           = Json.format
  implicit val eventPostDocRemovedFormat: Format[PostDocRemoved]                                   = Json.format

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
      case cmd: UpdateWidgetContent            => updatePostWidgetContent(cmd)
      case cmd: ChangeWidgetContentOrder       => changeWidgetContentOrder(cmd)
      case cmd: DeleteWidgetContent            => deleteWidgetContent(cmd)
      case cmd: UpdatePostPublicationTimestamp => updatePostPublicationTimestamp(cmd)
      case cmd: PublishPost                    => publishPost(cmd)
      case cmd: UnpublishPost                  => unpublishPost(cmd)
      case cmd: AssignPostTargetPrincipal      => assignPostTargetPrincipal(cmd)
      case cmd: UnassignPostTargetPrincipal    => unassignPostTargetPrincipal(cmd)
      case cmd: DeletePost                     => deletePost(cmd)
      case cmd: GetPost                        => getPost(cmd)
      case cmd: GetPostAnnotation              => getPostAnnotation(cmd)
      case cmd: AddPostMedia                   => addPostMedia(cmd)
      case cmd: RemovePostMedia                => removePostMedia(cmd)
      case cmd: AddPostDoc                     => addPostDoc(cmd)
      case cmd: UpdatePostDocName              => updatePostDocName(cmd)
      case cmd: RemovePostDoc                  => removePostDoc(cmd)
    }

  def createPost(cmd: CreatePost): ReplyEffect[Event, PostEntity] =
    maybeState match {
      case None    =>
        val event = cmd
          .into[PostCreated]
          .withFieldComputed(_.introContent, c => Content.fromSerialContent(c.introContent))
          .withFieldComputed(_.content, c => Content.fromSerialContent(c.content))
          .transform

        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
      case Some(_) => Effect.reply(cmd.replyTo)(PostAlreadyExist)
    }

  def updatePostFeatured(cmd: UpdatePostFeatured): ReplyEffect[Event, PostEntity] =
    maybeState match {
      case None    => Effect.reply(cmd.replyTo)(PostNotFound)
      case Some(_) =>
        val event = cmd.transformInto[PostFeaturedUpdated]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def updatePostAuthor(cmd: UpdatePostAuthor): ReplyEffect[Event, PostEntity] =
    maybeState match {
      case None    => Effect.reply(cmd.replyTo)(PostNotFound)
      case Some(_) =>
        val event = cmd.transformInto[PostAuthorUpdated]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def updatePostTitle(cmd: UpdatePostTitle): ReplyEffect[Event, PostEntity] =
    maybeState match {
      case None    => Effect.reply(cmd.replyTo)(PostNotFound)
      case Some(_) =>
        val event = cmd.transformInto[PostTitleUpdated]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def updatePostWidgetContent(cmd: UpdateWidgetContent): ReplyEffect[Event, PostEntity] =
    maybeState match {
      case None        => Effect.reply(cmd.replyTo)(PostNotFound)
      // update
      case Some(state)
          if (cmd.contentType == ContentTypes.Intro && state.introContent.content.contains(cmd.widgetContent.id)) ||
            (cmd.contentType == ContentTypes.Post && state.content.content.contains(cmd.widgetContent.id)) =>
        val contentMap          = cmd.contentType match {
          case ContentTypes.Intro => state.introContent
          case ContentTypes.Post  => state.content
        }
        val updatedContentOrder = cmd.order.map { order =>
          val newContentOrder = contentMap.contentOrder.filter(_ != cmd.widgetContent.id)
          if (order >= 0 && order < newContentOrder.length)
            (newContentOrder.take(order) :+ cmd.widgetContent.id) ++ newContentOrder.drop(order)
          else newContentOrder :+ cmd.widgetContent.id
        }.getOrElse(contentMap.contentOrder)

        val updateEvent = cmd
          .into[PostWidgetContentUpdated]
          .withFieldConst(_.contentOrder, updatedContentOrder)
          .transform
        val indexData   =
          if (contentMap.content(cmd.widgetContent.id).indexData != cmd.widgetContent.indexData)
            Some(
              contentMap.content.map {
                case widgetContentId -> _ if widgetContentId == cmd.widgetContent.id => cmd.widgetContent.indexData
                case _ -> widgetContent                                              => widgetContent.indexData
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
          .thenReply(cmd.replyTo)(_ => Success)

      // create
      case Some(state) =>
        val contentMap          = cmd.contentType match {
          case ContentTypes.Intro => state.introContent
          case ContentTypes.Post  => state.content
        }
        val updatedContentOrder = cmd.order.map { order =>
          val newContentOrder = contentMap.contentOrder.filter(_ != cmd.widgetContent.id)
          if (order >= 0 && order < newContentOrder.length)
            (newContentOrder.take(order) :+ cmd.widgetContent.id) ++ newContentOrder.drop(order)
          else newContentOrder :+ cmd.widgetContent.id
        }.getOrElse(contentMap.contentOrder)
        val updateEvent         = cmd
          .into[PostWidgetContentUpdated]
          .withFieldConst(_.contentOrder, updatedContentOrder)
          .transform
        val indexData           =
          cmd.widgetContent.indexData.map { idx =>
            (contentMap.content.values.map(_.indexData).flatten.toSeq :+ idx).mkString("\n")
          }
        val indexEvent          = indexData.map(idx =>
          cmd
            .into[PostIndexChanged]
            .withFieldConst(_.indexData, idx)
            .transform
        )
        Effect
          .persist(Seq(updateEvent) ++ indexEvent.toSeq)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def changeWidgetContentOrder(cmd: ChangeWidgetContentOrder): ReplyEffect[Event, PostEntity] =
    maybeState match {
      case None => Effect.reply(cmd.replyTo)(PostNotFound)
      case Some(state)
          if (cmd.contentType == ContentTypes.Intro && state.introContent.content.contains(cmd.widgetContentId)) ||
            (cmd.contentType == ContentTypes.Post && state.content.content.contains(cmd.widgetContentId)) =>
        val (contentMap, contentOrder) = cmd.contentType match {
          case ContentTypes.Intro => state.introContent -> state.introContent.contentOrder
          case ContentTypes.Post  => state.content      -> state.content.contentOrder
        }
        val newOrder                   =
          if (cmd.order < 0) 0
          else if (cmd.order > contentMap.contentOrder.length - 1) contentMap.contentOrder.length - 1
          else cmd.order
        val currentOrder               = contentOrder.indexOf(cmd.widgetContentId)
        if (currentOrder != newOrder) {
          val newContentOrder     = contentOrder.filter(_ != cmd.widgetContentId)
          val updatedContentOrder =
            if (newOrder >= 0 && newOrder < newContentOrder.length)
              (newContentOrder.take(newOrder) :+ cmd.widgetContentId) ++ newContentOrder.drop(newOrder)
            else newContentOrder :+ cmd.widgetContentId
          val updateEvent         = cmd
            .into[WidgetContentOrderChanged]
            .withFieldConst(_.contentOrder, updatedContentOrder)
            .transform

          Effect
            .persist(updateEvent)
            .thenReply(cmd.replyTo)(_ => Success)
        } else Effect.reply(cmd.replyTo)(Success)
      case _    => Effect.reply(cmd.replyTo)(WidgetContentNotFound)

    }

  def deleteWidgetContent(cmd: DeleteWidgetContent): ReplyEffect[Event, PostEntity] =
    maybeState match {
      case None => Effect.reply(cmd.replyTo)(PostNotFound)
      case Some(state)
          if (cmd.contentType == ContentTypes.Intro && state.introContent.content.contains(cmd.widgetContentId)) ||
            (cmd.contentType == ContentTypes.Post && state.content.content.contains(cmd.widgetContentId)) =>
        val contentOrder = cmd.contentType match {
          case ContentTypes.Intro => state.introContent.contentOrder
          case ContentTypes.Post  => state.content.contentOrder
        }
        val updateEvent  = cmd
          .into[WidgetContentDeleted]
          .withFieldConst(_.contentOrder, contentOrder.filter(_ != cmd.widgetContentId))
          .transform

        Effect
          .persist(updateEvent)
          .thenReply(cmd.replyTo)(_ => Success)
      case _    => Effect.reply(cmd.replyTo)(WidgetContentNotFound)
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
          .thenReply(cmd.replyTo)(_ => Success)
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
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def unpublishPost(cmd: UnpublishPost): ReplyEffect[Event, PostEntity] =
    maybeState match {
      case None    => Effect.reply(cmd.replyTo)(PostNotFound)
      case Some(_) =>
        val event = cmd.transformInto[PostUnpublished]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def assignPostTargetPrincipal(cmd: AssignPostTargetPrincipal): ReplyEffect[Event, PostEntity] =
    maybeState match {
      case None                                                 => Effect.reply(cmd.replyTo)(PostNotFound)
      case Some(state) if state.targets.contains(cmd.principal) => Effect.reply(cmd.replyTo)(Success)
      case Some(_)                                              =>
        val event = cmd.transformInto[PostTargetPrincipalAssigned]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def unassignPostTargetPrincipal(cmd: UnassignPostTargetPrincipal): ReplyEffect[Event, PostEntity] =
    maybeState match {
      case None                                                  => Effect.reply(cmd.replyTo)(PostNotFound)
      case Some(state) if !state.targets.contains(cmd.principal) => Effect.reply(cmd.replyTo)(Success)
      case Some(_)                                               =>
        val event = cmd.transformInto[PostTargetPrincipalUnassigned]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def deletePost(cmd: DeletePost): ReplyEffect[Event, PostEntity] =
    maybeState match {
      case None    => Effect.reply(cmd.replyTo)(PostNotFound)
      case Some(_) =>
        val event = cmd.transformInto[PostDeleted]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def getPost(cmd: GetPost): ReplyEffect[Event, PostEntity] =
    maybeState match {
      case None        => Effect.reply(cmd.replyTo)(PostNotFound)
      case Some(state) =>
        Effect.reply(cmd.replyTo)(
          SuccessPost(
            state
              .into[Post]
              .withFieldComputed(_.introContent, _.introContent.toSerialContent)
              .withFieldComputed(_.content, _.content.toSerialContent)
              .transform
          )
        )
    }

  def getPostAnnotation(cmd: GetPostAnnotation): ReplyEffect[Event, PostEntity] =
    maybeState match {
      case None        => Effect.reply(cmd.replyTo)(PostNotFound)
      case Some(state) =>
        Effect.reply(cmd.replyTo)(
          SuccessPostAnnotation(
            state
              .into[PostAnnotation]
              .withFieldComputed(_.introContent, _.introContent.toSerialContent)
              .transform
          )
        )
    }

  def addPostMedia(cmd: AddPostMedia): ReplyEffect[Event, PostEntity] =
    maybeState match {
      case None                                             => Effect.reply(cmd.replyTo)(PostNotFound)
      case Some(state) if state.media.contains(cmd.mediaId) => Effect.reply(cmd.replyTo)(PostMediaAlreadyExist)
      case Some(_)                                          =>
        val event = cmd.transformInto[PostMediaAdded]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def removePostMedia(cmd: RemovePostMedia): ReplyEffect[Event, PostEntity] =
    maybeState match {
      case None                                              => Effect.reply(cmd.replyTo)(PostNotFound)
      case Some(state) if !state.media.contains(cmd.mediaId) => Effect.reply(cmd.replyTo)(PostMediaNotFound)
      case Some(_)                                           =>
        val event = cmd.transformInto[PostMediaRemoved]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def addPostDoc(cmd: AddPostDoc): ReplyEffect[Event, PostEntity] =
    maybeState match {
      case None                                          => Effect.reply(cmd.replyTo)(PostNotFound)
      case Some(state) if state.docs.contains(cmd.docId) => Effect.reply(cmd.replyTo)(PostDocAlreadyExist)
      case Some(_)                                       =>
        val event = cmd.transformInto[PostDocAdded]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def updatePostDocName(cmd: UpdatePostDocName): ReplyEffect[Event, PostEntity] =
    maybeState match {
      case None                                           => Effect.reply(cmd.replyTo)(PostNotFound)
      case Some(state) if !state.docs.contains(cmd.docId) => Effect.reply(cmd.replyTo)(PostDocNotFound)
      case Some(_)                                        =>
        val event = cmd.transformInto[PostDocNameUpdated]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def removePostDoc(cmd: RemovePostDoc): ReplyEffect[Event, PostEntity] =
    maybeState match {
      case None                                           => Effect.reply(cmd.replyTo)(PostNotFound)
      case Some(state) if !state.docs.contains(cmd.docId) => Effect.reply(cmd.replyTo)(PostDocNotFound)
      case Some(_)                                        =>
        val event = cmd.transformInto[PostDocRemoved]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def applyEvent(event: Event): PostEntity =
    event match {
      case event: PostCreated                     => onPostCreated(event)
      case event: PostFeaturedUpdated             => onPostFeaturedUpdated(event)
      case event: PostAuthorUpdated               => onPostAuthorUpdated(event)
      case event: PostTitleUpdated                => onPostTitleUpdated(event)
      case event: PostWidgetContentUpdated        => onPostWidgetContentUpdated(event)
      case event: WidgetContentOrderChanged       => onWidgetContentOrderChanged(event)
      case event: WidgetContentDeleted            => onWidgetContentDeleted(event)
      case _: PostIndexChanged                    => this
      case event: PostPublicationTimestampUpdated => onPostPublicationTimestampUpdated(event)
      case event: PostPublished                   => onPostPublished(event)
      case event: PostUnpublished                 => onPostUnpublished(event)
      case event: PostTargetPrincipalAssigned     => onPostTargetPrincipalAssigned(event)
      case event: PostTargetPrincipalUnassigned   => onPostTargetPrincipalUnassigned(event)
      case _: PostDeleted                         => onPostDeleted()
      case event: PostMediaAdded                  => onPostMediaAdded(event)
      case event: PostMediaRemoved                => onPostMediaRemoved(event)
      case event: PostDocAdded                    => onPostDocAdded(event)
      case event: PostDocNameUpdated              => onPostDocNameUpdated(event)
      case event: PostDocRemoved                  => onPostDocRemoved(event)
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

  def onPostWidgetContentUpdated(event: PostWidgetContentUpdated): PostEntity =
    PostEntity(
      maybeState.map {
        case state if event.contentType == ContentTypes.Intro =>
          state.copy(
            introContent = Content(
              contentOrder = event.contentOrder,
              content = state.introContent.content + (event.widgetContent.id -> event.widgetContent)
            ),
            updatedBy = event.updatedBy,
            updatedAt = event.updatedAt
          )

        case state if event.contentType == ContentTypes.Post  =>
          state.copy(
            content = Content(
              contentOrder = event.contentOrder,
              content = state.content.content + (event.widgetContent.id -> event.widgetContent)
            ),
            updatedBy = event.updatedBy,
            updatedAt = event.updatedAt
          )
      }
    )

  def onWidgetContentOrderChanged(event: WidgetContentOrderChanged): PostEntity =
    event.contentType match {
      case ContentTypes.Intro =>
        PostEntity(
          maybeState.map { state =>
            state.copy(
              introContent = state.introContent.copy(contentOrder = event.contentOrder),
              updatedBy = event.updatedBy,
              updatedAt = event.updatedAt
            )
          }
        )
      case ContentTypes.Post  =>
        PostEntity(
          maybeState.map { state =>
            state.copy(
              content = state.content.copy(contentOrder = event.contentOrder),
              updatedBy = event.updatedBy,
              updatedAt = event.updatedAt
            )
          }
        )
    }

  def onWidgetContentDeleted(event: WidgetContentDeleted): PostEntity =
    event.contentType match {
      case ContentTypes.Intro =>
        PostEntity(
          maybeState.map(state =>
            state.copy(
              introContent = Content(
                contentOrder = event.contentOrder,
                content = state.introContent.content - event.widgetContentId
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
              content = Content(
                contentOrder = event.contentOrder,
                content = state.content.content - event.widgetContentId
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

  def onPostMediaAdded(event: PostMediaAdded): PostEntity =
    PostEntity(
      maybeState.map(state =>
        state.copy(
          media = state.media + (event.mediaId -> Media(event.mediaId, event.filename)),
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

  def onPostMediaRemoved(event: PostMediaRemoved): PostEntity =
    PostEntity(
      maybeState.map(state =>
        state.copy(
          media = state.media - event.mediaId,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

  def onPostDocAdded(event: PostDocAdded): PostEntity =
    PostEntity(
      maybeState.map(state =>
        state.copy(
          docs = state.docs + (event.docId -> Doc(event.docId, event.name, event.filename)),
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

  def onPostDocNameUpdated(event: PostDocNameUpdated): PostEntity =
    PostEntity(
      maybeState.map { state =>
        val updatedDocs = state.docs
          .get(event.docId)
          .map(doc => state.docs + (event.docId -> doc.copy(name = event.name)))
          .getOrElse(state.docs)
        state.copy(
          docs = updatedDocs,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      }
    )

  def onPostDocRemoved(event: PostDocRemoved): PostEntity =
    PostEntity(
      maybeState.map(state =>
        state.copy(
          docs = state.docs - event.docId,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

}
