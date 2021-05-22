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

package biz.lobachev.annette.cms.impl.space

import java.time.OffsetDateTime
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl.{EntityContext, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import com.lightbend.lagom.scaladsl.persistence._
import play.api.libs.json._
import org.slf4j.LoggerFactory
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.cms.api.space._
import biz.lobachev.annette.cms.api.category.CategoryId
import biz.lobachev.annette.cms.impl.space.model.SpaceState

object SpaceEntity {

  trait CommandSerializable
  sealed trait Command                                                              extends CommandSerializable
  final case class CreateSpace(
    id: SpaceId,
    name: String,
    description: String,
    categoryId: CategoryId,
    targets: Set[AnnettePrincipal] = Set.empty,
    createdBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  )                                                                                 extends Command
  final case class UpdateSpaceName(
    id: SpaceId,
    name: String,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  )                                                                                 extends Command
  final case class UpdateSpaceDescription(
    id: SpaceId,
    description: String,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  )                                                                                 extends Command
  final case class UpdateSpaceCategory(
    id: SpaceId,
    categoryId: CategoryId,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  )                                                                                 extends Command
  final case class AssignSpaceTargetPrincipal(
    id: SpaceId,
    principal: AnnettePrincipal,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  )                                                                                 extends Command
  final case class UnassignSpaceTargetPrincipal(
    id: SpaceId,
    principal: AnnettePrincipal,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  )                                                                                 extends Command
  final case class ActivateSpace(id: SpaceId, updatedBy: AnnettePrincipal, replyTo: ActorRef[Confirmation])
      extends Command
  final case class DeactivateSpace(id: SpaceId, updatedBy: AnnettePrincipal, replyTo: ActorRef[Confirmation])
      extends Command
  final case class DeleteSpace(id: SpaceId, deletedBy: AnnettePrincipal, replyTo: ActorRef[Confirmation])
      extends Command
  final case class GetSpace(id: SpaceId, replyTo: ActorRef[Confirmation])           extends Command
  final case class GetSpaceAnnotation(id: SpaceId, replyTo: ActorRef[Confirmation]) extends Command

  sealed trait Confirmation
  final case object Success                                                 extends Confirmation
  final case class SuccessSpace(space: Space)                               extends Confirmation
  final case class SuccessSpaceAnnotation(spaceAnnotation: SpaceAnnotation) extends Confirmation
  final case object SpaceAlreadyExist                                       extends Confirmation
  final case object SpaceNotFound                                           extends Confirmation

  implicit val confirmationSuccessFormat: Format[Success.type]                          = Json.format
  implicit val confirmationSuccessSpaceFormat: Format[SuccessSpace]                     = Json.format
  implicit val confirmationSuccessSpaceAnnotationFormat: Format[SuccessSpaceAnnotation] = Json.format
  implicit val confirmationSpaceAlreadyExistFormat: Format[SpaceAlreadyExist.type]      = Json.format
  implicit val confirmationSpaceNotFoundFormat: Format[SpaceNotFound.type]              = Json.format

  sealed trait Event extends AggregateEvent[Event] {
    override def aggregateTag: AggregateEventTagger[Event] = Event.Tag
  }

  object Event {
    val Tag: AggregateEventShards[Event] = AggregateEventTag.sharded[Event](numShards = 10)
  }

  final case class SpaceCreated(
    id: SpaceId,
    name: String,
    description: String,
    categoryId: CategoryId,
    targets: Set[AnnettePrincipal] = Set.empty,
    createdBy: AnnettePrincipal,
    createdAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class SpaceNameUpdated(
    id: SpaceId,
    name: String,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class SpaceDescriptionUpdated(
    id: SpaceId,
    description: String,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class SpaceCategoryUpdated(
    id: SpaceId,
    categoryId: CategoryId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class SpaceTargetPrincipalAssigned(
    id: SpaceId,
    principal: AnnettePrincipal,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class SpaceTargetPrincipalUnassigned(
    id: SpaceId,
    principal: AnnettePrincipal,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class SpaceActivated(
    id: SpaceId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class SpaceDeactivated(
    id: SpaceId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class SpaceDeleted(id: SpaceId, deleteBy: AnnettePrincipal, deleteAt: OffsetDateTime = OffsetDateTime.now)
      extends Event

  implicit val eventSpaceCreatedFormat: Format[SpaceCreated]                                     = Json.format
  implicit val eventSpaceNameUpdatedFormat: Format[SpaceNameUpdated]                             = Json.format
  implicit val eventSpaceDescriptionUpdatedFormat: Format[SpaceDescriptionUpdated]               = Json.format
  implicit val eventSpaceCategoryUpdatedFormat: Format[SpaceCategoryUpdated]                     = Json.format
  implicit val eventSpaceTargetPrincipalAssignedFormat: Format[SpaceTargetPrincipalAssigned]     = Json.format
  implicit val eventSpaceTargetPrincipalUnassignedFormat: Format[SpaceTargetPrincipalUnassigned] = Json.format
  implicit val eventSpaceActivatedFormat: Format[SpaceActivated]                                 = Json.format
  implicit val eventSpaceDeactivatedFormat: Format[SpaceDeactivated]                             = Json.format
  implicit val eventSpaceDeletedFormat: Format[SpaceDeleted]                                     = Json.format

  val empty = SpaceEntity()

  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Cms_Space")

  def apply(persistenceId: PersistenceId): EventSourcedBehavior[Command, Event, SpaceEntity] =
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, SpaceEntity](
        persistenceId = persistenceId,
        emptyState = SpaceEntity.empty,
        commandHandler = (entity, cmd) => entity.applyCommand(cmd),
        eventHandler = (entity, evt) => entity.applyEvent(evt)
      )

  def apply(entityContext: EntityContext[Command]): Behavior[Command] =
    apply(PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId))
      .withTagger(AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2))

  implicit val entityFormat: Format[SpaceEntity] = Json.format

}

final case class SpaceEntity(maybeState: Option[SpaceState] = None) {
  import SpaceEntity._

  val log = LoggerFactory.getLogger(this.getClass)

  def applyCommand(cmd: Command): ReplyEffect[Event, SpaceEntity] =
    cmd match {
      case cmd: CreateSpace                  => createSpace(cmd)
      case cmd: UpdateSpaceName              => updateSpaceName(cmd)
      case cmd: UpdateSpaceDescription       => updateSpaceDescription(cmd)
      case cmd: UpdateSpaceCategory          => updateSpaceCategory(cmd)
      case cmd: AssignSpaceTargetPrincipal   => assignSpaceTargetPrincipal(cmd)
      case cmd: UnassignSpaceTargetPrincipal => unassignSpaceTargetPrincipal(cmd)
      case cmd: ActivateSpace                => activateSpace(cmd)
      case cmd: DeactivateSpace              => deactivateSpace(cmd)
      case cmd: DeleteSpace                  => deleteSpace(cmd)
      case cmd: GetSpace                     => getSpace(cmd)
      case cmd: GetSpaceAnnotation           => getSpaceAnnotation(cmd)
    }

  def createSpace(cmd: CreateSpace): ReplyEffect[Event, SpaceEntity] = ???

  def updateSpaceName(cmd: UpdateSpaceName): ReplyEffect[Event, SpaceEntity] = ???

  def updateSpaceDescription(cmd: UpdateSpaceDescription): ReplyEffect[Event, SpaceEntity] = ???

  def updateSpaceCategory(cmd: UpdateSpaceCategory): ReplyEffect[Event, SpaceEntity] = ???

  def assignSpaceTargetPrincipal(cmd: AssignSpaceTargetPrincipal): ReplyEffect[Event, SpaceEntity] = ???

  def unassignSpaceTargetPrincipal(cmd: UnassignSpaceTargetPrincipal): ReplyEffect[Event, SpaceEntity] = ???

  def activateSpace(cmd: ActivateSpace): ReplyEffect[Event, SpaceEntity] = ???

  def deactivateSpace(cmd: DeactivateSpace): ReplyEffect[Event, SpaceEntity] = ???

  def deleteSpace(cmd: DeleteSpace): ReplyEffect[Event, SpaceEntity] = ???

  def getSpace(cmd: GetSpace): ReplyEffect[Event, SpaceEntity] = ???

  def getSpaceAnnotation(cmd: GetSpaceAnnotation): ReplyEffect[Event, SpaceEntity] = ???

  def applyEvent(event: Event): SpaceEntity =
    event match {
      case event: SpaceCreated                   => onSpaceCreated(event)
      case event: SpaceNameUpdated               => onSpaceNameUpdated(event)
      case event: SpaceDescriptionUpdated        => onSpaceDescriptionUpdated(event)
      case event: SpaceCategoryUpdated           => onSpaceCategoryUpdated(event)
      case event: SpaceTargetPrincipalAssigned   => onSpaceTargetPrincipalAssigned(event)
      case event: SpaceTargetPrincipalUnassigned => onSpaceTargetPrincipalUnassigned(event)
      case event: SpaceActivated                 => onSpaceActivated(event)
      case event: SpaceDeactivated               => onSpaceDeactivated(event)
      case event: SpaceDeleted                   => onSpaceDeleted(event)
    }

  def onSpaceCreated(event: SpaceCreated): SpaceEntity = ???

  def onSpaceNameUpdated(event: SpaceNameUpdated): SpaceEntity = ???

  def onSpaceDescriptionUpdated(event: SpaceDescriptionUpdated): SpaceEntity = ???

  def onSpaceCategoryUpdated(event: SpaceCategoryUpdated): SpaceEntity = ???

  def onSpaceTargetPrincipalAssigned(event: SpaceTargetPrincipalAssigned): SpaceEntity = ???

  def onSpaceTargetPrincipalUnassigned(event: SpaceTargetPrincipalUnassigned): SpaceEntity = ???

  def onSpaceActivated(event: SpaceActivated): SpaceEntity = ???

  def onSpaceDeactivated(event: SpaceDeactivated): SpaceEntity = ???

  def onSpaceDeleted(event: SpaceDeleted): SpaceEntity = ???

}
