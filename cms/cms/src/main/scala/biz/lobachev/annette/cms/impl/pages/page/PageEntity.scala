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

package biz.lobachev.annette.cms.impl.pages.page

import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl.{EntityContext, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import biz.lobachev.annette.cms.api.pages.space.SpaceId
import biz.lobachev.annette.cms.api.pages.page._
import biz.lobachev.annette.cms.api.common.{SerialContent, WidgetContent}
import biz.lobachev.annette.cms.impl.pages.page.model.PageState
import biz.lobachev.annette.cms.impl.content.Content
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import com.lightbend.lagom.scaladsl.persistence._
import io.scalaland.chimney.dsl._
import org.slf4j.LoggerFactory
import play.api.libs.json._

import java.time.OffsetDateTime

object PageEntity {

  trait CommandSerializable
  sealed trait Command extends CommandSerializable
  final case class CreatePage(
    id: PageId,
    spaceId: SpaceId,
    authorId: AnnettePrincipal,
    title: String,
    content: SerialContent,
    targets: Set[AnnettePrincipal] = Set.empty,
    createdBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  )                    extends Command

  final case class UpdatePageAuthor(
    id: PageId,
    authorId: AnnettePrincipal,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class UpdatePageTitle(
    id: PageId,
    title: String,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class UpdateWidgetContent(
    id: PageId,
    widgetContent: WidgetContent,
    order: Option[Int] = None,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  case class ChangeWidgetContentOrder(
    id: PageId,
    widgetContentId: String,
    order: Int,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  case class DeleteWidgetContent(
    id: PageId,
    widgetContentId: String,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class UpdatePagePublicationTimestamp(
    id: PageId,
    publicationTimestamp: Option[OffsetDateTime],
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class PublishPage(id: PageId, updatedBy: AnnettePrincipal, replyTo: ActorRef[Confirmation]) extends Command

  final case class UnpublishPage(id: PageId, updatedBy: AnnettePrincipal, replyTo: ActorRef[Confirmation])
      extends Command

  final case class AssignPageTargetPrincipal(
    id: PageId,
    principal: AnnettePrincipal,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class UnassignPageTargetPrincipal(
    id: PageId,
    principal: AnnettePrincipal,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class DeletePage(
    id: PageId,
    deletedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class GetPage(
    id: PageId,
    withContent: Boolean,
    withTargets: Boolean,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  sealed trait Confirmation
  final case object Success                            extends Confirmation
  final case class SuccessPage(page: Page)             extends Confirmation
  final case object PageAlreadyExist                   extends Confirmation
  final case object PageNotFound                       extends Confirmation
  final case object WidgetContentNotFound              extends Confirmation
  final case object PagePublicationDateClearNotAllowed extends Confirmation

  implicit val confirmationSuccessFormat: Format[Success.type]                                                       = Json.format
  implicit val confirmationSuccessPageFormat: Format[SuccessPage]                                                    = Json.format
  implicit val confirmationPageAlreadyExistFormat: Format[PageAlreadyExist.type]                                     = Json.format
  implicit val confirmationPageNotFoundFormat: Format[PageNotFound.type]                                             = Json.format
  implicit val confirmationWidgetContentNotFoundFormat: Format[WidgetContentNotFound.type]                           = Json.format
  implicit val confirmationPagePublicationDateClearNotAllowedFormat: Format[PagePublicationDateClearNotAllowed.type] =
    Json.format

  sealed trait Event extends AggregateEvent[Event] {
    override def aggregateTag: AggregateEventTagger[Event] = Event.Tag
  }

  object Event {
    val Tag: AggregateEventShards[Event] = AggregateEventTag.sharded[Event](numShards = 10)
  }

  final case class PageCreated(
    id: PageId,
    spaceId: SpaceId,
    authorId: AnnettePrincipal,
    title: String,
    content: Content,
    targets: Set[AnnettePrincipal] = Set.empty,
    createdBy: AnnettePrincipal,
    createdAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class PageAuthorUpdated(
    id: PageId,
    authorId: AnnettePrincipal,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class PageTitleUpdated(
    id: PageId,
    title: String,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class PageWidgetContentUpdated(
    id: PageId,
    widgetContent: WidgetContent,
    contentOrder: Seq[String],
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  case class WidgetContentOrderChanged(
    id: PageId,
    widgetContentId: String,
    contentOrder: Seq[String],
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  case class WidgetContentDeleted(
    id: PageId,
    widgetContentId: String,
    contentOrder: Seq[String],
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  case class PageIndexChanged(
    id: PageId,
    indexData: String,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class PagePublicationTimestampUpdated(
    id: PageId,
    publicationTimestamp: Option[OffsetDateTime],
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class PagePublished(
    id: PageId,
    publicationTimestamp: OffsetDateTime,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class PageUnpublished(
    id: PageId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class PageTargetPrincipalAssigned(
    id: PageId,
    principal: AnnettePrincipal,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class PageTargetPrincipalUnassigned(
    id: PageId,
    principal: AnnettePrincipal,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class PageDeleted(
    id: PageId,
    deletedBy: AnnettePrincipal,
    deleteAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  implicit val eventPageCreatedFormat: Format[PageCreated]                                         = Json.format
  implicit val eventPageAuthorUpdatedFormat: Format[PageAuthorUpdated]                             = Json.format
  implicit val eventPageTitleUpdatedFormat: Format[PageTitleUpdated]                               = Json.format
  implicit val eventPageWidgetContentUpdatedFormat: Format[PageWidgetContentUpdated]               = Json.format
  implicit val eventWidgetContentOrderChangedFormat: Format[WidgetContentOrderChanged]             = Json.format
  implicit val eventWidgetContentDeletedFormat: Format[WidgetContentDeleted]                       = Json.format
  implicit val eventPageIndexChangedFormat: Format[PageIndexChanged]                               = Json.format
  implicit val eventPagePublicationTimestampUpdatedFormat: Format[PagePublicationTimestampUpdated] = Json.format
  implicit val eventPagePublishedFormat: Format[PagePublished]                                     = Json.format
  implicit val eventPageUnpublishedFormat: Format[PageUnpublished]                                 = Json.format
  implicit val eventPageTargetPrincipalAssignedFormat: Format[PageTargetPrincipalAssigned]         = Json.format
  implicit val eventPageTargetPrincipalUnassignedFormat: Format[PageTargetPrincipalUnassigned]     = Json.format
  implicit val eventPageDeletedFormat: Format[PageDeleted]                                         = Json.format

  val empty = PageEntity()

  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Cms_Page")

  def apply(persistenceId: PersistenceId): EventSourcedBehavior[Command, Event, PageEntity] =
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, PageEntity](
        persistenceId = persistenceId,
        emptyState = PageEntity.empty,
        commandHandler = (entity, cmd) => entity.applyCommand(cmd),
        eventHandler = (entity, evt) => entity.applyEvent(evt)
      )

  def apply(entityContext: EntityContext[Command]): Behavior[Command] =
    apply(PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId))
      .withTagger(AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2))

  implicit val entityFormat: Format[PageEntity] = Json.format

}

final case class PageEntity(maybeState: Option[PageState] = None) {
  import PageEntity._

  val log = LoggerFactory.getLogger(this.getClass)

  def applyCommand(cmd: Command): ReplyEffect[Event, PageEntity] =
    cmd match {
      case cmd: CreatePage                     => createPage(cmd)
      case cmd: UpdatePageAuthor               => updatePageAuthor(cmd)
      case cmd: UpdatePageTitle                => updatePageTitle(cmd)
      case cmd: UpdateWidgetContent            => updatePageWidgetContent(cmd)
      case cmd: ChangeWidgetContentOrder       => changeWidgetContentOrder(cmd)
      case cmd: DeleteWidgetContent            => deleteWidgetContent(cmd)
      case cmd: UpdatePagePublicationTimestamp => updatePagePublicationTimestamp(cmd)
      case cmd: PublishPage                    => publishPage(cmd)
      case cmd: UnpublishPage                  => unpublishPage(cmd)
      case cmd: AssignPageTargetPrincipal      => assignPageTargetPrincipal(cmd)
      case cmd: UnassignPageTargetPrincipal    => unassignPageTargetPrincipal(cmd)
      case cmd: DeletePage                     => deletePage(cmd)
      case cmd: GetPage                        => getPage(cmd)
    }

  def createPage(cmd: CreatePage): ReplyEffect[Event, PageEntity] =
    maybeState match {
      case None    =>
        val event = cmd
          .into[PageCreated]
          .withFieldComputed(_.content, c => Content.fromSerialContent(c.content))
          .transform

        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
      case Some(_) => Effect.reply(cmd.replyTo)(PageAlreadyExist)
    }

  def updatePageAuthor(cmd: UpdatePageAuthor): ReplyEffect[Event, PageEntity] =
    maybeState match {
      case None    => Effect.reply(cmd.replyTo)(PageNotFound)
      case Some(_) =>
        val event = cmd.transformInto[PageAuthorUpdated]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def updatePageTitle(cmd: UpdatePageTitle): ReplyEffect[Event, PageEntity] =
    maybeState match {
      case None    => Effect.reply(cmd.replyTo)(PageNotFound)
      case Some(_) =>
        val event = cmd.transformInto[PageTitleUpdated]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def updatePageWidgetContent(cmd: UpdateWidgetContent): ReplyEffect[Event, PageEntity] =
    maybeState match {
      case None                                                                => Effect.reply(cmd.replyTo)(PageNotFound)
      // update
      case Some(state) if state.content.content.contains(cmd.widgetContent.id) =>
        val updatedContentOrder = cmd.order.map { order =>
          val newContentOrder = state.content.contentOrder.filter(_ != cmd.widgetContent.id)
          if (order >= 0 && order < newContentOrder.length)
            (newContentOrder.take(order) :+ cmd.widgetContent.id) ++ newContentOrder.drop(order)
          else newContentOrder :+ cmd.widgetContent.id
        }.getOrElse(state.content.contentOrder)

        val updateEvent = cmd
          .into[PageWidgetContentUpdated]
          .withFieldConst(_.contentOrder, updatedContentOrder)
          .transform
        val indexData   =
          if (state.content.content(cmd.widgetContent.id).indexData != cmd.widgetContent.indexData)
            Some(
              state.content.content.map {
                case widgetContentId -> _ if widgetContentId == cmd.widgetContent.id => cmd.widgetContent.indexData
                case _ -> widgetContent                                              => widgetContent.indexData
              }.flatten.mkString("\n")
            )
          else None
        val indexEvent  = indexData.map(idx =>
          cmd
            .into[PageIndexChanged]
            .withFieldConst(_.indexData, idx)
            .transform
        )
        Effect
          .persist(Seq(updateEvent) ++ indexEvent.toSeq)
          .thenReply(cmd.replyTo)(_ => Success)

      // create
      case Some(state)                                                         =>
        val updatedContentOrder = cmd.order.map { order =>
          val newContentOrder = state.content.contentOrder.filter(_ != cmd.widgetContent.id)
          if (order >= 0 && order < newContentOrder.length)
            (newContentOrder.take(order) :+ cmd.widgetContent.id) ++ newContentOrder.drop(order)
          else newContentOrder :+ cmd.widgetContent.id
        }.getOrElse(state.content.contentOrder)
        val updateEvent         = cmd
          .into[PageWidgetContentUpdated]
          .withFieldConst(_.contentOrder, updatedContentOrder)
          .transform
        val indexData           =
          cmd.widgetContent.indexData.map { idx =>
            (state.content.content.values.map(_.indexData).flatten.toSeq :+ idx).mkString("\n")
          }
        val indexEvent          = indexData.map(idx =>
          cmd
            .into[PageIndexChanged]
            .withFieldConst(_.indexData, idx)
            .transform
        )
        Effect
          .persist(Seq(updateEvent) ++ indexEvent.toSeq)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def changeWidgetContentOrder(cmd: ChangeWidgetContentOrder): ReplyEffect[Event, PageEntity] =
    maybeState match {
      case None                                                               => Effect.reply(cmd.replyTo)(PageNotFound)
      case Some(state) if state.content.content.contains(cmd.widgetContentId) =>
//        val (contentMap, contentOrder) = cmd.contentType match {
//          case ContentTypes.Intro => state.introContent -> state.introContent.contentOrder
//          case ContentTypes.Page  => state.content      -> state.content.contentOrder
//        }
        val newOrder     =
          if (cmd.order < 0) 0
          else if (cmd.order > state.content.contentOrder.length - 1) state.content.contentOrder.length - 1
          else cmd.order
        val currentOrder = state.content.contentOrder.indexOf(cmd.widgetContentId)
        if (currentOrder != newOrder) {
          val newContentOrder     = state.content.contentOrder.filter(_ != cmd.widgetContentId)
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
      case _                                                                  => Effect.reply(cmd.replyTo)(WidgetContentNotFound)

    }

  def deleteWidgetContent(cmd: DeleteWidgetContent): ReplyEffect[Event, PageEntity] =
    maybeState match {
      case None                                                               => Effect.reply(cmd.replyTo)(PageNotFound)
      case Some(state) if state.content.content.contains(cmd.widgetContentId) =>
        val updateEvent = cmd
          .into[WidgetContentDeleted]
          .withFieldConst(_.contentOrder, state.content.contentOrder.filter(_ != cmd.widgetContentId))
          .transform

        Effect
          .persist(updateEvent)
          .thenReply(cmd.replyTo)(_ => Success)
      case _                                                                  => Effect.reply(cmd.replyTo)(WidgetContentNotFound)
    }

  def updatePagePublicationTimestamp(cmd: UpdatePagePublicationTimestamp): ReplyEffect[Event, PageEntity] =
    maybeState match {
      case None    => Effect.reply(cmd.replyTo)(PageNotFound)
      case Some(state)
          if state.publicationStatus == PublicationStatus.Published &&
            cmd.publicationTimestamp.isEmpty =>
        Effect.reply(cmd.replyTo)(PagePublicationDateClearNotAllowed)
      case Some(_) =>
        val event = cmd.transformInto[PagePublicationTimestampUpdated]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def publishPage(cmd: PublishPage): ReplyEffect[Event, PageEntity] =
    maybeState match {
      case None        => Effect.reply(cmd.replyTo)(PageNotFound)
      case Some(state) =>
        val event = cmd
          .into[PagePublished]
          .withFieldConst(_.publicationTimestamp, state.publicationTimestamp.getOrElse(OffsetDateTime.now))
          .transform
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def unpublishPage(cmd: UnpublishPage): ReplyEffect[Event, PageEntity] =
    maybeState match {
      case None    => Effect.reply(cmd.replyTo)(PageNotFound)
      case Some(_) =>
        val event = cmd.transformInto[PageUnpublished]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def assignPageTargetPrincipal(cmd: AssignPageTargetPrincipal): ReplyEffect[Event, PageEntity] =
    maybeState match {
      case None                                                 => Effect.reply(cmd.replyTo)(PageNotFound)
      case Some(state) if state.targets.contains(cmd.principal) => Effect.reply(cmd.replyTo)(Success)
      case Some(_)                                              =>
        val event = cmd.transformInto[PageTargetPrincipalAssigned]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def unassignPageTargetPrincipal(cmd: UnassignPageTargetPrincipal): ReplyEffect[Event, PageEntity] =
    maybeState match {
      case None                                                  => Effect.reply(cmd.replyTo)(PageNotFound)
      case Some(state) if !state.targets.contains(cmd.principal) => Effect.reply(cmd.replyTo)(Success)
      case Some(_)                                               =>
        val event = cmd.transformInto[PageTargetPrincipalUnassigned]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def deletePage(cmd: DeletePage): ReplyEffect[Event, PageEntity] =
    maybeState match {
      case None    => Effect.reply(cmd.replyTo)(PageNotFound)
      case Some(_) =>
        val event = cmd.transformInto[PageDeleted]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def getPage(cmd: GetPage): ReplyEffect[Event, PageEntity] =
    maybeState match {
      case None        => Effect.reply(cmd.replyTo)(PageNotFound)
      case Some(state) =>
        Effect.reply(cmd.replyTo)(
          SuccessPage(
            state
              .into[Page]
              .withFieldComputed(_.content, c => if (cmd.withContent) Some(c.content.toSerialContent) else None)
              .withFieldComputed(_.targets, c => if (cmd.withTargets) Some(c.targets) else None)
              .transform
          )
        )
    }

  def applyEvent(event: Event): PageEntity =
    event match {
      case event: PageCreated                     => onPageCreated(event)
      case event: PageAuthorUpdated               => onPageAuthorUpdated(event)
      case event: PageTitleUpdated                => onPageTitleUpdated(event)
      case event: PageWidgetContentUpdated        => onPageWidgetContentUpdated(event)
      case event: WidgetContentOrderChanged       => onWidgetContentOrderChanged(event)
      case event: WidgetContentDeleted            => onWidgetContentDeleted(event)
      case _: PageIndexChanged                    => this
      case event: PagePublicationTimestampUpdated => onPagePublicationTimestampUpdated(event)
      case event: PagePublished                   => onPagePublished(event)
      case event: PageUnpublished                 => onPageUnpublished(event)
      case event: PageTargetPrincipalAssigned     => onPageTargetPrincipalAssigned(event)
      case event: PageTargetPrincipalUnassigned   => onPageTargetPrincipalUnassigned(event)
      case _: PageDeleted                         => onPageDeleted()
    }

  def onPageCreated(event: PageCreated): PageEntity =
    PageEntity(
      Some(
        event
          .into[PageState]
          .withFieldConst(_.updatedBy, event.createdBy)
          .withFieldConst(_.updatedAt, event.createdAt)
          .transform
      )
    )

  def onPageAuthorUpdated(event: PageAuthorUpdated): PageEntity =
    PageEntity(
      maybeState.map(
        _.copy(
          authorId = event.authorId,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

  def onPageTitleUpdated(event: PageTitleUpdated): PageEntity =
    PageEntity(
      maybeState.map(
        _.copy(
          title = event.title,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

  def onPageWidgetContentUpdated(event: PageWidgetContentUpdated): PageEntity =
    PageEntity(
      maybeState.map {
        case state =>
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

  def onWidgetContentOrderChanged(event: WidgetContentOrderChanged): PageEntity =
    PageEntity(
      maybeState.map { state =>
        state.copy(
          content = state.content.copy(contentOrder = event.contentOrder),
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      }
    )

  def onWidgetContentDeleted(event: WidgetContentDeleted): PageEntity =
    PageEntity(
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

  def onPagePublicationTimestampUpdated(event: PagePublicationTimestampUpdated): PageEntity =
    PageEntity(
      maybeState.map(
        _.copy(
          publicationTimestamp = event.publicationTimestamp,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

  def onPagePublished(event: PagePublished): PageEntity =
    PageEntity(
      maybeState.map(
        _.copy(
          publicationStatus = PublicationStatus.Published,
          publicationTimestamp = Some(event.publicationTimestamp),
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

  def onPageUnpublished(event: PageUnpublished): PageEntity =
    PageEntity(
      maybeState.map(
        _.copy(
          publicationStatus = PublicationStatus.Draft,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

  def onPageTargetPrincipalAssigned(event: PageTargetPrincipalAssigned): PageEntity =
    PageEntity(
      maybeState.map(state =>
        state.copy(
          targets = state.targets + event.principal,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

  def onPageTargetPrincipalUnassigned(event: PageTargetPrincipalUnassigned): PageEntity =
    PageEntity(
      maybeState.map(state =>
        state.copy(
          targets = state.targets - event.principal,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      )
    )

  def onPageDeleted(): PageEntity =
    PageEntity(None)

}
