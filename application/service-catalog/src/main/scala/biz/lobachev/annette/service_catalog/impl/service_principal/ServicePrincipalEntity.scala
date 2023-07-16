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

package biz.lobachev.annette.service_catalog.impl.service_principal

import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl.{EntityContext, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.service_catalog.api.item.ServiceItemId
import biz.lobachev.annette.service_catalog.api.service_principal.{
  AssignServicePrincipalPayload,
  UnassignServicePrincipalPayload
}
import biz.lobachev.annette.service_catalog.impl.service_principal.model.ServicePrincipalState
import com.lightbend.lagom.scaladsl.persistence._
import io.scalaland.chimney.dsl._
import play.api.libs.json._

import java.time.OffsetDateTime

object ServicePrincipalEntity {

  trait CommandSerializable
  sealed trait Command extends CommandSerializable
  final case class AssignPrincipal(payload: AssignServicePrincipalPayload, replyTo: ActorRef[Confirmation])
      extends Command
  final case class UnassignPrincipal(payload: UnassignServicePrincipalPayload, replyTo: ActorRef[Confirmation])
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

  final case class ServicePrincipalAssigned(
    serviceId: ServiceItemId,
    principal: AnnettePrincipal,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class ServicePrincipalUnassigned(
    serviceId: ServiceItemId,
    principal: AnnettePrincipal,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  implicit val eventPrincipalAssignedFormat: Format[ServicePrincipalAssigned]     = Json.format
  implicit val eventPrincipalUnassignedFormat: Format[ServicePrincipalUnassigned] = Json.format

  val empty = ServicePrincipalEntity()

  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Authorization_ServicePrincipal")

  def apply(persistenceId: PersistenceId): EventSourcedBehavior[Command, Event, ServicePrincipalEntity] =
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, ServicePrincipalEntity](
        persistenceId = persistenceId,
        emptyState = ServicePrincipalEntity.empty,
        commandHandler = (entity, cmd) => entity.applyCommand(cmd),
        eventHandler = (entity, evt) => entity.applyEvent(evt)
      )

  def apply(entityContext: EntityContext[Command]): Behavior[Command] =
    apply(PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId))
      .withTagger(AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2))

  implicit val entityFormat: Format[ServicePrincipalEntity] = Json.format

  def compositeId(
    serviceId: ServiceItemId,
    principal: AnnettePrincipal
  ): String =
    s"$serviceId/${principal.code}"

  def fromCompositeId(compositeId: String): (ServiceItemId, AnnettePrincipal) = {
    val split = compositeId.split("/")
    split(0) -> AnnettePrincipal(split(1))
  }
}

final case class ServicePrincipalEntity(maybeState: Option[ServicePrincipalState] = None) {
  import ServicePrincipalEntity._

  def applyCommand(cmd: Command): ReplyEffect[Event, ServicePrincipalEntity] =
    cmd match {
      case cmd: AssignPrincipal   => assignPrincipal(cmd)
      case cmd: UnassignPrincipal => unassignPrincipal(cmd)
    }

  def assignPrincipal(cmd: AssignPrincipal): ReplyEffect[Event, ServicePrincipalEntity] = {
    val event = cmd.payload
      .into[ServicePrincipalAssigned]
      .transform
    Effect
      .persist(event)
      .thenReply(cmd.replyTo)(_ => Success)
  }

  def unassignPrincipal(cmd: UnassignPrincipal): ReplyEffect[Event, ServicePrincipalEntity] = {
    val event = cmd.payload
      .into[ServicePrincipalUnassigned]
      .transform
    Effect
      .persist(event)
      .thenReply(cmd.replyTo)(_ => Success)
  }

  def applyEvent(event: Event): ServicePrincipalEntity =
    event match {
      case event: ServicePrincipalAssigned => onPrincipalAssigned(event)
      case _: ServicePrincipalUnassigned   => onPrincipalUnassigned()
    }

  def onPrincipalAssigned(event: ServicePrincipalAssigned): ServicePrincipalEntity =
    ServicePrincipalEntity(
      Some(
        event.transformInto[ServicePrincipalState]
      )
    )

  def onPrincipalUnassigned(): ServicePrincipalEntity =
    ServicePrincipalEntity(None)

}
