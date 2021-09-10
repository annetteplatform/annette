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

package biz.lobachev.annette.cms.impl.hierarchy

import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl.{EntityContext, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import biz.lobachev.annette.cms.api.post.PostId
import biz.lobachev.annette.cms.api.space._
import biz.lobachev.annette.cms.impl.hierarchy.model.HierarchyState
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import com.lightbend.lagom.scaladsl.persistence._
import io.scalaland.chimney.dsl._
import org.slf4j.LoggerFactory
import play.api.libs.json.{Format, Json}

import java.time.OffsetDateTime

object HierarchyEntity {

  trait CommandSerializable
  sealed trait Command extends CommandSerializable

  case class CreateSpace(
    spaceId: SpaceId,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  case class AddPost(
    spaceId: SpaceId,
    postId: PostId,
    parent: Option[PostId],
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  case class RemovePost(
    spaceId: SpaceId,
    postId: PostId,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  case class MovePost(
    spaceId: SpaceId,
    postId: PostId,
    newParent: PostId,
    newPosition: Option[Int],
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  case class DeleteSpace(
    spaceId: SpaceId,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class GetHierarchy(id: SpaceId, replyTo: ActorRef[Confirmation]) extends Command

  sealed trait Confirmation
  final case object Success                                   extends Confirmation
  final case class SuccessHierarchy(hierarchy: WikiHierarchy) extends Confirmation
  final case object SpaceNotFound                             extends Confirmation
  final case object PostNotFound                              extends Confirmation
  final case object InvalidParent                             extends Confirmation
  final case object PostAlreadyExist                          extends Confirmation
  final case object PostHasChild                              extends Confirmation

  implicit val confirmationSuccessFormat: Format[Success.type]                   = Json.format
  implicit val confirmationSuccessSpaceFormat: Format[SuccessHierarchy]          = Json.format
  implicit val confirmationSpaceNotFoundFormat: Format[SpaceNotFound.type]       = Json.format
  implicit val confirmationPostNotFoundFormat: Format[PostNotFound.type]         = Json.format
  implicit val confirmationInvalidParentFormat: Format[InvalidParent.type]       = Json.format
  implicit val confirmationPostAlreadyExistFormat: Format[PostAlreadyExist.type] = Json.format
  implicit val confirmationPostHasChildrenFormat: Format[PostHasChild.type]      = Json.format

  sealed trait Event extends AggregateEvent[Event] {
    override def aggregateTag: AggregateEventTagger[Event] = Event.Tag
  }

  object Event {
    val Tag: AggregateEventShards[Event] = AggregateEventTag.sharded[Event](numShards = 10)
  }

  case class SpaceCreated(
    spaceId: SpaceId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  final case class RootPostAdded(
    spaceId: SpaceId,
    postId: PostId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  final case class PostAdded(
    spaceId: SpaceId,
    postId: PostId,
    parent: PostId,
    children: Seq[PostId],
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  case class RootPostRemoved(
    spaceId: SpaceId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  case class PostRemoved(
    spaceId: SpaceId,
    postId: PostId,
    parent: PostId,
    children: Seq[PostId],
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  case class PostMoved(
    spaceId: SpaceId,
    postId: PostId,
    newParent: PostId,
    newChildren: Seq[PostId],
    oldParent: PostId,
    oldChildren: Seq[PostId],
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  case class SpaceDeleted(
    spaceId: SpaceId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  implicit val eventSpaceCreatedFormat: Format[SpaceCreated]       = Json.format
  implicit val eventRootPostAddedFormat: Format[RootPostAdded]     = Json.format
  implicit val eventPostAddedFormat: Format[PostAdded]             = Json.format
  implicit val eventRootPostRemovedFormat: Format[RootPostRemoved] = Json.format
  implicit val eventPostRemovedFormat: Format[PostRemoved]         = Json.format
  implicit val eventPostMovedFormat: Format[PostMoved]             = Json.format
  implicit val eventSpaceDeletedFormat: Format[SpaceDeleted]       = Json.format

  val empty = HierarchyEntity()

  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Cms_WikiHierarchy")

  def apply(persistenceId: PersistenceId): EventSourcedBehavior[Command, Event, HierarchyEntity] =
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, HierarchyEntity](
        persistenceId = persistenceId,
        emptyState = HierarchyEntity.empty,
        commandHandler = (entity, cmd) => entity.applyCommand(cmd),
        eventHandler = (entity, evt) => entity.applyEvent(evt)
      )

  def apply(entityContext: EntityContext[Command]): Behavior[Command] =
    apply(PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId))
      .withTagger(AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2))

  implicit val entityFormat: Format[HierarchyEntity] = Json.format

}
final case class HierarchyEntity(maybeState: Option[HierarchyState] = None) {
  import HierarchyEntity._

  val log = LoggerFactory.getLogger(this.getClass)

  def applyCommand(cmd: Command): ReplyEffect[Event, HierarchyEntity] =
    cmd match {
      case cmd: CreateSpace  => createSpace(cmd)
      case cmd: AddPost      => addPost(cmd)
      case cmd: MovePost     => movePost(cmd)
      case cmd: RemovePost   => removePost(cmd)
      case cmd: DeleteSpace  => deleteSpace(cmd)
      case cmd: GetHierarchy => getHierarchy(cmd)
    }

  def createSpace(cmd: CreateSpace): ReplyEffect[Event, HierarchyEntity] = {
    val event = cmd.transformInto[SpaceCreated]
    Effect
      .persist(event)
      .thenReply(cmd.replyTo)(_ => Success)
  }

  def addPost(cmd: AddPost): ReplyEffect[Event, HierarchyEntity] =
    maybeState match {
      case None                                                                                           =>
        Effect.reply(cmd.replyTo)(SpaceNotFound)

      // PostAlreadyExist
      case Some(state) if (state.rootPostId == Some(cmd.postId) || state.parentPost.contains(cmd.postId)) =>
        Effect.reply(cmd.replyTo)(PostAlreadyExist)

      // Parent id does not exist
      case Some(state)
          if cmd.parent
            .map(parent => !(state.parentPost.keySet ++ state.rootPostId.toSet).contains(parent))
            .getOrElse(false) =>
        Effect.reply(cmd.replyTo)(InvalidParent)

      case Some(state) if state.rootPostId.isEmpty                                                        =>
        val event = cmd.transformInto[RootPostAdded]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)

      case Some(state) if state.rootPostId.nonEmpty && cmd.parent.isEmpty                                 =>
        Effect.reply(cmd.replyTo)(InvalidParent)

      case Some(state)                                                                                    =>
        val event = cmd
          .into[PostAdded]
          .withFieldConst(_.parent, cmd.parent.get)
          .withFieldConst(_.children, state.childPosts.get(cmd.parent.get).getOrElse(Seq.empty) :+ cmd.postId)
          .transform
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)

    }

  def movePost(cmd: MovePost): ReplyEffect[Event, HierarchyEntity] =
    maybeState match {
      case None                                                                                        =>
        Effect.reply(cmd.replyTo)(SpaceNotFound)
      // Parent id does not exist
      case Some(state) if !(state.parentPost.keySet ++ state.rootPostId.toSet).contains(cmd.newParent) =>
        Effect.reply(cmd.replyTo)(InvalidParent)
      // Post not found
      case Some(state) if !(state.parentPost.keySet ++ state.rootPostId.toSet).contains(cmd.postId)    =>
        Effect.reply(cmd.replyTo)(PostNotFound)
      // newParent is descendant of moving post
      case Some(state) if rootPath(cmd.newParent, state).toSet.contains(cmd.postId)                    =>
        Effect.reply(cmd.replyTo)(InvalidParent)
      case Some(state)                                                                                 =>
        val oldParent        = state.parentPost(cmd.postId)
        val oldChildren      =
          if (oldParent == cmd.newParent) Seq.empty
          else state.childPosts.get(oldParent).getOrElse(Seq.empty).filterNot(_ == cmd.postId)
        val children         = state.childPosts.get(cmd.newParent).getOrElse(Seq.empty)
        val filteredChildren = children.filterNot(_ == cmd.postId)
        val newPosition      = cmd.newPosition.map { position =>
          if (position < 0) 0
          else if (position > filteredChildren.length) filteredChildren.length
          else position
        }.getOrElse(filteredChildren.length)
        val newChildren      =
          (filteredChildren.take(newPosition) :+ cmd.postId) ++ filteredChildren.drop(newPosition)

        val event = cmd
          .into[PostMoved]
          .withFieldConst(_.oldParent, oldParent)
          .withFieldConst(_.oldChildren, oldChildren)
          .withFieldConst(_.newChildren, newChildren)
          .transform
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)

    }

  private def rootPath(id: PostId, state: HierarchyState): Seq[PostId] =
    state.parentPost.get(id) match {
      case Some(parentId) => rootPath(parentId, state) ++ Seq(id)
      case None           => Seq(id)
    }

  def removePost(cmd: RemovePost): ReplyEffect[Event, HierarchyEntity] =
    maybeState match {
      case None                                                                                     =>
        Effect.reply(cmd.replyTo)(SpaceNotFound)
      // Post not found
      case Some(state) if !(state.parentPost.keySet ++ state.rootPostId.toSet).contains(cmd.postId) =>
        Effect.reply(cmd.replyTo)(PostNotFound)
      // post has child
      case Some(state) if state.childPosts.get(cmd.postId).map(_.nonEmpty).getOrElse(false)         =>
        Effect.reply(cmd.replyTo)(PostHasChild)
      case Some(state) if state.rootPostId == Some(cmd.postId)                                      =>
        val event = cmd.transformInto[RootPostRemoved]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
      case Some(state)                                                                              =>
        val parent      = state.parentPost(cmd.postId)
        val newChildren = state.childPosts.get(parent).getOrElse(Seq.empty).filterNot(_ == cmd.postId)
        val event       = cmd
          .into[PostRemoved]
          .withFieldConst(_.parent, parent)
          .withFieldConst(_.children, newChildren)
          .transform
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)

    }

  def deleteSpace(cmd: DeleteSpace): ReplyEffect[Event, HierarchyEntity] =
    maybeState match {
      case None    => Effect.reply(cmd.replyTo)(SpaceNotFound)
      case Some(_) =>
        val event = cmd.transformInto[SpaceDeleted]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def getHierarchy(cmd: GetHierarchy): ReplyEffect[Event, HierarchyEntity] =
    maybeState match {
      case None        => Effect.reply(cmd.replyTo)(SpaceNotFound)
      case Some(state) => Effect.reply(cmd.replyTo)(SuccessHierarchy(state.transformInto[WikiHierarchy]))
    }

  def applyEvent(event: Event): HierarchyEntity = {
    val state = event match {
      case event: SpaceCreated    => onSpaceCreated(event)
      case event: RootPostAdded   => onRootPostAdded(event)
      case event: PostAdded       => onPostAdded(event)
      case event: PostMoved       => onPostMoved(event)
      case event: RootPostRemoved => onRootPostRemoved(event)
      case event: PostRemoved     => onPostRemoved(event)
      case _: SpaceDeleted        => onSpaceDeleted()
    }
    println()
    println()
    println(state)
    println()
    println()
    state
  }

  def onSpaceCreated(event: SpaceCreated): HierarchyEntity =
    HierarchyEntity(
      Some(
        HierarchyState(
          event.spaceId,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

  def onRootPostAdded(event: RootPostAdded): HierarchyEntity =
    HierarchyEntity(
      Some(
        HierarchyState(
          spaceId = event.spaceId,
          rootPostId = Some(event.postId),
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

  def onPostAdded(event: PostAdded): HierarchyEntity =
    HierarchyEntity(
      maybeState.map { state =>
        state.copy(
          childPosts = state.childPosts + (event.parent -> event.children),
          parentPost = state.parentPost + (event.postId -> event.parent),
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      }
    )
  def onPostMoved(event: PostMoved): HierarchyEntity =
    HierarchyEntity(
      maybeState.map { state =>
        state.copy(
          childPosts =
            if (event.oldParent == event.newParent)
              state.childPosts + (event.newParent -> event.newChildren)
            else if (event.oldChildren.isEmpty)
              state.childPosts - event.oldParent + (event.newParent -> event.newChildren)
            else
              state.childPosts + (event.oldParent                   -> event.oldChildren) + (event.newParent -> event.newChildren),
          parentPost =
            state.parentPost + (event.postId -> event.newParent),
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      }
    )

  def onRootPostRemoved(event: RootPostRemoved): HierarchyEntity =
    HierarchyEntity(
      Some(
        HierarchyState(
          spaceId = event.spaceId,
          rootPostId = None,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

  def onPostRemoved(event: PostRemoved): HierarchyEntity =
    HierarchyEntity(
      maybeState.map { state =>
        state.copy(
          childPosts =
            if (event.children.isEmpty) state.childPosts - event.parent
            else state.childPosts + (event.parent -> event.children),
          parentPost = state.parentPost - event.postId,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      }
    )

  def onSpaceDeleted(): HierarchyEntity =
    HierarchyEntity(None)

}
