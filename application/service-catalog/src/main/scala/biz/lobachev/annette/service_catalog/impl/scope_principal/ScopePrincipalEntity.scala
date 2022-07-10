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

package biz.lobachev.annette.service_catalog.impl.scope_principal

import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl.{EntityContext, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.service_catalog.api.scope.ScopeId
import biz.lobachev.annette.service_catalog.api.scope_principal.{
  AssignScopePrincipalPayload,
  UnassignScopePrincipalPayload
}
import biz.lobachev.annette.service_catalog.impl.scope_principal.model.ScopePrincipalState
import com.lightbend.lagom.scaladsl.persistence._
import io.scalaland.chimney.dsl._
import play.api.libs.json._

import java.time.OffsetDateTime

object ScopePrincipalEntity {

  trait CommandSerializable
  sealed trait Command extends CommandSerializable
  final case class AssignPrincipal(payload: AssignScopePrincipalPayload, replyTo: ActorRef[Confirmation])
      extends Command
  final case class UnassignPrincipal(payload: UnassignScopePrincipalPayload, replyTo: ActorRef[Confirmation])
      extends Command

  sealed trait Confirmation
  final case object Success extends Confirmation

  implicit val confirmationSuccessFormat: Format[Success.type] = Json.format
  implicit val confirmationFormat: Format[Confirmation]        = Json.format[Confirmation]

  sealed trait Event extends AggregateEvent[Event] {
    override def aggregateTag: AggregateEventTagger[Event] = Event.Tag
  }

  object Event {
    val Tag: AggregateEventShards[Event] = AggregateEventTag.sharded[Event](numShards = 10)
  }

  final case class ScopePrincipalAssigned(
    scopeId: ScopeId,
    principal: AnnettePrincipal,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class ScopePrincipalUnassigned(
    scopeId: ScopeId,
    principal: AnnettePrincipal,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  implicit val eventPrincipalAssignedFormat: Format[ScopePrincipalAssigned]     = Json.format
  implicit val eventPrincipalUnassignedFormat: Format[ScopePrincipalUnassigned] = Json.format

  val empty = ScopePrincipalEntity()

  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Authorization_ScopePrincipal")

  def apply(persistenceId: PersistenceId): EventSourcedBehavior[Command, Event, ScopePrincipalEntity] =
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, ScopePrincipalEntity](
        persistenceId = persistenceId,
        emptyState = ScopePrincipalEntity.empty,
        commandHandler = (entity, cmd) => entity.applyCommand(cmd),
        eventHandler = (entity, evt) => entity.applyEvent(evt)
      )

  def apply(entityContext: EntityContext[Command]): Behavior[Command] =
    apply(PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId))
      .withTagger(AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2))

  implicit val entityFormat: Format[ScopePrincipalEntity] = Json.format

  def scopePrincipalId(
    scopeId: ScopeId,
    principal: AnnettePrincipal
  ): String =
    // TODO: review scopePrincipal id calculation (replace whitespace with another symbol)
    s"$scopeId ${principal.code}"

}

final case class ScopePrincipalEntity(maybeState: Option[ScopePrincipalState] = None) {
  import ScopePrincipalEntity._

  def applyCommand(cmd: Command): ReplyEffect[Event, ScopePrincipalEntity] =
    cmd match {
      case cmd: AssignPrincipal   => assignPrincipal(cmd)
      case cmd: UnassignPrincipal => unassignPrincipal(cmd)
    }

  def assignPrincipal(cmd: AssignPrincipal): ReplyEffect[Event, ScopePrincipalEntity] = {
    val event = cmd.payload
      .into[ScopePrincipalAssigned]
      .transform
    Effect
      .persist(event)
      .thenReply(cmd.replyTo)(_ => Success)
  }

  def unassignPrincipal(cmd: UnassignPrincipal): ReplyEffect[Event, ScopePrincipalEntity] = {
    val event = cmd.payload
      .into[ScopePrincipalUnassigned]
      .transform
    Effect
      .persist(event)
      .thenReply(cmd.replyTo)(_ => Success)
  }

  def applyEvent(event: Event): ScopePrincipalEntity =
    event match {
      case event: ScopePrincipalAssigned => onPrincipalAssigned(event)
      case _: ScopePrincipalUnassigned   => onPrincipalUnassigned()
    }

  def onPrincipalAssigned(event: ScopePrincipalAssigned): ScopePrincipalEntity =
    ScopePrincipalEntity(
      Some(
        event.transformInto[ScopePrincipalState]
      )
    )

  def onPrincipalUnassigned(): ScopePrincipalEntity =
    ScopePrincipalEntity(None)

}
