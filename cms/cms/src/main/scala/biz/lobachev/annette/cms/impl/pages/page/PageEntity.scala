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
import biz.lobachev.annette.cms.api.common.article.PublicationStatus
import biz.lobachev.annette.cms.api.content.{Content, Widget}
import biz.lobachev.annette.cms.api.pages.page._
import biz.lobachev.annette.cms.api.pages.space.SpaceId
import biz.lobachev.annette.cms.impl.pages.page.model.PageState
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
    content: Content,
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

  final case class UpdateContentSettings(
    id: String,
    settings: JsValue,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class UpdateWidget(
    id: PageId,
    widget: Widget,
    order: Option[Int] = None,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  case class ChangeWidgetOrder(
    id: PageId,
    widgetId: String,
    order: Int,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  case class DeleteWidget(
    id: PageId,
    widgetId: String,
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
  final case class Success(updatedBy: AnnettePrincipal, updatedAt: OffsetDateTime) extends Confirmation
  final case class SuccessPage(page: Page)                                         extends Confirmation
  final case object PageAlreadyExist                                               extends Confirmation
  final case object PageNotFound                                                   extends Confirmation
  final case object WidgetNotFound                                                 extends Confirmation
  final case object PagePublicationDateClearNotAllowed                             extends Confirmation

  implicit val confirmationSuccessFormat: Format[Success.type]                                                       = Json.format
  implicit val confirmationSuccessPageFormat: Format[SuccessPage]                                                    = Json.format
  implicit val confirmationPageAlreadyExistFormat: Format[PageAlreadyExist.type]                                     = Json.format
  implicit val confirmationPageNotFoundFormat: Format[PageNotFound.type]                                             = Json.format
  implicit val confirmationWidgetNotFoundFormat: Format[WidgetNotFound.type]                                         = Json.format
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
  final case class ContentSettingsUpdated(
    id: String,
    settings: JsValue,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class PageWidgetUpdated(
    id: PageId,
    widget: Widget,
    widgetOrder: Seq[String],
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  case class WidgetOrderChanged(
    id: PageId,
    widgetId: String,
    widgetOrder: Seq[String],
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  case class WidgetDeleted(
    id: PageId,
    widgetId: String,
    widgetOrder: Seq[String],
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
  implicit val eventContentSettingsUpdatedFormat: Format[ContentSettingsUpdated]                   = Json.format
  implicit val eventPageWidgetUpdatedFormat: Format[PageWidgetUpdated]                             = Json.format
  implicit val eventWidgetOrderChangedFormat: Format[WidgetOrderChanged]                           = Json.format
  implicit val eventWidgetDeletedFormat: Format[WidgetDeleted]                                     = Json.format
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
      case cmd: UpdateContentSettings          => updateContentSettings(cmd)
      case cmd: UpdateWidget                   => updatePageWidget(cmd)
      case cmd: ChangeWidgetOrder              => changeWidgetOrder(cmd)
      case cmd: DeleteWidget                   => deleteWidget(cmd)
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
          .transform

        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(pageEntity =>
            SuccessPage(
              pageEntity.maybeState.get
                .into[Page]
                .withFieldComputed(_.content, c => Some(c.content))
                .withFieldComputed(_.targets, c => Some(c.targets))
                .transform
            )
          )
      case Some(_) => Effect.reply(cmd.replyTo)(PageAlreadyExist)
    }

  def updatePageAuthor(cmd: UpdatePageAuthor): ReplyEffect[Event, PageEntity] =
    maybeState match {
      case None    => Effect.reply(cmd.replyTo)(PageNotFound)
      case Some(_) =>
        val event = cmd.transformInto[PageAuthorUpdated]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success(event.updatedBy, event.updatedAt))
    }

  def updatePageTitle(cmd: UpdatePageTitle): ReplyEffect[Event, PageEntity] =
    maybeState match {
      case None    => Effect.reply(cmd.replyTo)(PageNotFound)
      case Some(_) =>
        val event = cmd.transformInto[PageTitleUpdated]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success(event.updatedBy, event.updatedAt))
    }

  def updateContentSettings(cmd: UpdateContentSettings): ReplyEffect[Event, PageEntity] =
    maybeState match {
      case None    => Effect.reply(cmd.replyTo)(PageNotFound)
      case Some(_) =>
        val updateEvent = cmd
          .into[ContentSettingsUpdated]
          .transform
        Effect
          .persist(updateEvent)
          .thenReply(cmd.replyTo)(_ => Success(updateEvent.updatedBy, updateEvent.updatedAt))
      case _       => Effect.reply(cmd.replyTo)(WidgetNotFound)
    }

  def updatePageWidget(cmd: UpdateWidget): ReplyEffect[Event, PageEntity] =
    maybeState match {
      case None                                                         => Effect.reply(cmd.replyTo)(PageNotFound)
      // update
      case Some(state) if state.content.widgets.contains(cmd.widget.id) =>
        val updatedContentOrder = cmd.order.map { order =>
          val newContentOrder = state.content.widgetOrder.filter(_ != cmd.widget.id)
          if (order >= 0 && order < newContentOrder.length)
            (newContentOrder.take(order) :+ cmd.widget.id) ++ newContentOrder.drop(order)
          else newContentOrder :+ cmd.widget.id
        }.getOrElse(state.content.widgetOrder)

        val updateEvent = cmd
          .into[PageWidgetUpdated]
          .withFieldConst(_.widgetOrder, updatedContentOrder)
          .transform
        val indexData   =
          if (state.content.widgets(cmd.widget.id).indexData != cmd.widget.indexData)
            Some(
              state.content.widgets.map {
                case widgetContentId -> _ if widgetContentId == cmd.widget.id => cmd.widget.indexData
                case _ -> widgetContent                                       => widgetContent.indexData
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
          .thenReply(cmd.replyTo)(_ => Success(updateEvent.updatedBy, updateEvent.updatedAt))

      // create
      case Some(state)                                                  =>
        val updatedContentOrder = cmd.order.map { order =>
          val newContentOrder = state.content.widgetOrder.filter(_ != cmd.widget.id)
          if (order >= 0 && order < newContentOrder.length)
            (newContentOrder.take(order) :+ cmd.widget.id) ++ newContentOrder.drop(order)
          else newContentOrder :+ cmd.widget.id
        }.getOrElse(state.content.widgetOrder)
        val updateEvent         = cmd
          .into[PageWidgetUpdated]
          .withFieldConst(_.widgetOrder, updatedContentOrder)
          .transform
        val indexData           =
          cmd.widget.indexData.map { idx =>
            (state.content.widgets.values.map(_.indexData).flatten.toSeq :+ idx).mkString("\n")
          }
        val indexEvent          = indexData.map(idx =>
          cmd
            .into[PageIndexChanged]
            .withFieldConst(_.indexData, idx)
            .transform
        )
        Effect
          .persist(Seq(updateEvent) ++ indexEvent.toSeq)
          .thenReply(cmd.replyTo)(_ => Success(updateEvent.updatedBy, updateEvent.updatedAt))
    }

  def changeWidgetOrder(cmd: ChangeWidgetOrder): ReplyEffect[Event, PageEntity] =
    maybeState match {
      case None                                                        => Effect.reply(cmd.replyTo)(PageNotFound)
      case Some(state) if state.content.widgets.contains(cmd.widgetId) =>
        val newOrder     =
          if (cmd.order < 0) 0
          else if (cmd.order > state.content.widgetOrder.length - 1) state.content.widgetOrder.length - 1
          else cmd.order
        val currentOrder = state.content.widgetOrder.indexOf(cmd.widgetId)
        if (currentOrder != newOrder) {
          val newContentOrder     = state.content.widgetOrder.filter(_ != cmd.widgetId)
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
      case _                                                           => Effect.reply(cmd.replyTo)(WidgetNotFound)

    }

  def deleteWidget(cmd: DeleteWidget): ReplyEffect[Event, PageEntity] =
    maybeState match {
      case None                                                        => Effect.reply(cmd.replyTo)(PageNotFound)
      case Some(state) if state.content.widgets.contains(cmd.widgetId) =>
        val updateEvent = cmd
          .into[WidgetDeleted]
          .withFieldConst(_.widgetOrder, state.content.widgetOrder.filter(_ != cmd.widgetId))
          .transform

        Effect
          .persist(updateEvent)
          .thenReply(cmd.replyTo)(_ => Success(updateEvent.updatedBy, updateEvent.updatedAt))
      case _                                                           => Effect.reply(cmd.replyTo)(WidgetNotFound)
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
          .thenReply(cmd.replyTo)(_ => Success(event.updatedBy, event.updatedAt))
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
          .thenReply(cmd.replyTo)(_ => Success(event.updatedBy, event.updatedAt))
    }

  def unpublishPage(cmd: UnpublishPage): ReplyEffect[Event, PageEntity] =
    maybeState match {
      case None    => Effect.reply(cmd.replyTo)(PageNotFound)
      case Some(_) =>
        val event = cmd.transformInto[PageUnpublished]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success(event.updatedBy, event.updatedAt))
    }

  def assignPageTargetPrincipal(cmd: AssignPageTargetPrincipal): ReplyEffect[Event, PageEntity] =
    maybeState match {
      case None                                                 => Effect.reply(cmd.replyTo)(PageNotFound)
      case Some(state) if state.targets.contains(cmd.principal) =>
        Effect.reply(cmd.replyTo)(Success(state.updatedBy, state.updatedAt))
      case Some(_)                                              =>
        val event = cmd.transformInto[PageTargetPrincipalAssigned]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success(event.updatedBy, event.updatedAt))
    }

  def unassignPageTargetPrincipal(cmd: UnassignPageTargetPrincipal): ReplyEffect[Event, PageEntity] =
    maybeState match {
      case None                                                  => Effect.reply(cmd.replyTo)(PageNotFound)
      case Some(state) if !state.targets.contains(cmd.principal) =>
        Effect.reply(cmd.replyTo)(Success(state.updatedBy, state.updatedAt))
      case Some(_)                                               =>
        val event = cmd.transformInto[PageTargetPrincipalUnassigned]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success(event.updatedBy, event.updatedAt))
    }

  def deletePage(cmd: DeletePage): ReplyEffect[Event, PageEntity] =
    maybeState match {
      case None    => Effect.reply(cmd.replyTo)(PageNotFound)
      case Some(_) =>
        val event = cmd.transformInto[PageDeleted]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success(event.deletedBy, event.deleteAt))
    }

  def getPage(cmd: GetPage): ReplyEffect[Event, PageEntity] =
    maybeState match {
      case None        => Effect.reply(cmd.replyTo)(PageNotFound)
      case Some(state) =>
        Effect.reply(cmd.replyTo)(
          SuccessPage(
            state
              .into[Page]
              .withFieldComputed(_.content, c => if (cmd.withContent) Some(c.content) else None)
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
      case event: ContentSettingsUpdated          => onContentSettingsUpdated(event)
      case event: PageWidgetUpdated               => onPageWidgetUpdated(event)
      case event: WidgetOrderChanged              => onWidgetOrderChanged(event)
      case event: WidgetDeleted                   => onWidgetDeleted(event)
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

  def onContentSettingsUpdated(event: ContentSettingsUpdated): PageEntity =
    PageEntity(
      maybeState.map {
        case state =>
          state.copy(
            content = state.content.copy(
              settings = event.settings
            ),
            updatedBy = event.updatedBy,
            updatedAt = event.updatedAt
          )
      }
    )

  def onPageWidgetUpdated(event: PageWidgetUpdated): PageEntity =
    PageEntity(
      maybeState.map {
        case state =>
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

  def onWidgetOrderChanged(event: WidgetOrderChanged): PageEntity =
    PageEntity(
      maybeState.map { state =>
        state.copy(
          content = state.content.copy(widgetOrder = event.widgetOrder),
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      }
    )

  def onWidgetDeleted(event: WidgetDeleted): PageEntity =
    PageEntity(
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
