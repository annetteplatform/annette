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

package biz.lobachev.annette.cms.impl.home_pages

import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl.{EntityContext, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import biz.lobachev.annette.cms.api.home_page.{HomePage, HomePageId}
import biz.lobachev.annette.cms.api.pages.page.PageId
import biz.lobachev.annette.cms.impl.home_pages.model.HomePageState
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import com.lightbend.lagom.scaladsl.persistence._
import io.scalaland.chimney.dsl._
import org.slf4j.LoggerFactory
import play.api.libs.json._

import java.time.OffsetDateTime

object HomePageEntity {

  trait CommandSerializable
  sealed trait Command extends CommandSerializable

  final case class AssignHomePage(
    applicationId: String,
    principal: AnnettePrincipal,
    priority: Int,
    pageId: PageId,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class UnassignHomePage(
    applicationId: String,
    principal: AnnettePrincipal,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class GetHomePage(id: HomePageId, replyTo: ActorRef[Confirmation]) extends Command

  sealed trait Confirmation
  final case object Success                            extends Confirmation
  final case class SuccessHomePage(homePage: HomePage) extends Confirmation
  final case object HomePageNotFound                   extends Confirmation

  implicit val confirmationSuccessFormat: Format[Success.type]                   = Json.format
  implicit val confirmationSuccessHomePageFormat: Format[SuccessHomePage]        = Json.format
  implicit val confirmationHomePageNotFoundFormat: Format[HomePageNotFound.type] = Json.format

  sealed trait Event extends AggregateEvent[Event] {
    override def aggregateTag: AggregateEventTagger[Event] = Event.Tag
  }

  object Event {
    val Tag: AggregateEventShards[Event] = AggregateEventTag.sharded[Event](numShards = 10)
  }

  final case class HomePageAssigned(
    applicationId: String,
    principal: AnnettePrincipal,
    priority: Int,
    pageId: PageId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  final case class HomePageUnassigned(
    applicationId: String,
    principal: AnnettePrincipal,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  implicit val eventHomePageAssignedFormat: Format[HomePageAssigned]     = Json.format
  implicit val eventHomePageUnassignedFormat: Format[HomePageUnassigned] = Json.format

  val empty = HomePageEntity()

  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Cms_HomePage")

  def apply(persistenceId: PersistenceId): EventSourcedBehavior[Command, Event, HomePageEntity] =
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, HomePageEntity](
        persistenceId = persistenceId,
        emptyState = HomePageEntity.empty,
        commandHandler = (entity, cmd) => entity.applyCommand(cmd),
        eventHandler = (entity, evt) => entity.applyEvent(evt)
      )

  def apply(entityContext: EntityContext[Command]): Behavior[Command] =
    apply(PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId))
      .withTagger(AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2))

  implicit val entityFormat: Format[HomePageEntity] = Json.format

}

final case class HomePageEntity(maybeState: Option[HomePageState] = None) {
  import HomePageEntity._

  val log = LoggerFactory.getLogger(this.getClass)

  def applyCommand(cmd: Command): ReplyEffect[Event, HomePageEntity] =
    cmd match {
      case cmd: AssignHomePage   => assignHomePage(cmd)
      case cmd: UnassignHomePage => unassignHomePage(cmd)
      case cmd: GetHomePage      => getHomePage(cmd)
    }

  def assignHomePage(cmd: AssignHomePage): ReplyEffect[Event, HomePageEntity] = {
    val event = cmd
      .transformInto[HomePageAssigned]
    Effect
      .persist(event)
      .thenReply(cmd.replyTo)(_ => Success)
  }

  def unassignHomePage(cmd: UnassignHomePage): ReplyEffect[Event, HomePageEntity] =
    maybeState match {
      case None    => Effect.reply(cmd.replyTo)(HomePageNotFound)
      case Some(_) =>
        val event = cmd.transformInto[HomePageUnassigned]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def getHomePage(cmd: GetHomePage): ReplyEffect[Event, HomePageEntity] =
    maybeState match {
      case None        => Effect.reply(cmd.replyTo)(HomePageNotFound)
      case Some(state) => Effect.reply(cmd.replyTo)(SuccessHomePage(state.transformInto[HomePage]))
    }

  def applyEvent(event: Event): HomePageEntity =
    event match {
      case event: HomePageAssigned => onHomePageAssigned(event)
      case _: HomePageUnassigned   => onHomePageUnassigned()
    }

  def onHomePageAssigned(event: HomePageAssigned): HomePageEntity =
    HomePageEntity(
      Some(
        event
          .into[HomePageState]
          .withFieldConst(_.id, HomePage.toCompositeId(event.applicationId, event.principal))
          .withFieldConst(_.updatedBy, event.updatedBy)
          .withFieldConst(_.updatedAt, event.updatedAt)
          .transform
      )
    )

  def onHomePageUnassigned(): HomePageEntity =
    HomePageEntity(None)

}
