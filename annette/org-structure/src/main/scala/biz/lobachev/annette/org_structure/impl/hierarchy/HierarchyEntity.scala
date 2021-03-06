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

package biz.lobachev.annette.org_structure.impl.hierarchy

import java.time.OffsetDateTime
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl._
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import biz.lobachev.annette.core.model.PersonId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.org_structure.api.category.{OrgCategory, OrgCategoryId}
import biz.lobachev.annette.org_structure.api.hierarchy._
import biz.lobachev.annette.org_structure.api.role.OrgRoleId
import biz.lobachev.annette.org_structure.impl.hierarchy.model.HierarchyState
import com.lightbend.lagom.scaladsl.persistence._
import io.scalaland.chimney.dsl._
import play.api.libs.json.{Format, _}

object HierarchyEntity {

  trait CommandSerializable

  sealed trait Command extends CommandSerializable

  final case class CreateOrganization(payload: CreateOrganizationPayload, replyTo: ActorRef[Confirmation])
      extends Command
  final case class DeleteOrganization(payload: DeleteOrganizationPayload, replyTo: ActorRef[Confirmation])
      extends Command
  final case class CreateUnit(payload: CreateUnitPayload, replyTo: ActorRef[Confirmation])           extends Command
  final case class DeleteUnit(payload: DeleteUnitPayload, replyTo: ActorRef[Confirmation])           extends Command
  final case class AssignCategory(
    payload: AssignCategoryPayload,
    category: OrgCategory,
    replyTo: ActorRef[Confirmation]
  )                                                                                                  extends Command
  final case class AssignChief(payload: AssignChiefPayload, replyTo: ActorRef[Confirmation])         extends Command
  final case class UnassignChief(payload: UnassignChiefPayload, replyTo: ActorRef[Confirmation])     extends Command
  final case class CreatePosition(payload: CreatePositionPayload, replyTo: ActorRef[Confirmation])   extends Command
  final case class DeletePosition(payload: DeletePositionPayload, replyTo: ActorRef[Confirmation])   extends Command
  final case class UpdateName(payload: UpdateNamePayload, replyTo: ActorRef[Confirmation])           extends Command
  final case class UpdateShortName(payload: UpdateShortNamePayload, replyTo: ActorRef[Confirmation]) extends Command
  final case class ChangePositionLimit(payload: ChangePositionLimitPayload, replyTo: ActorRef[Confirmation])
      extends Command
  final case class AssignPerson(payload: AssignPersonPayload, replyTo: ActorRef[Confirmation])       extends Command
  final case class UnassignPerson(payload: UnassignPersonPayload, replyTo: ActorRef[Confirmation])   extends Command
  final case class AssignOrgRole(payload: AssignOrgRolePayload, replyTo: ActorRef[Confirmation])     extends Command
  final case class UnassignOrgRole(payload: UnassignOrgRolePayload, replyTo: ActorRef[Confirmation]) extends Command
  final case class MoveItem(payload: MoveItemPayload, replyTo: ActorRef[Confirmation])               extends Command
  final case class ChangeItemOrder(payload: ChangeItemOrderPayload, replyTo: ActorRef[Confirmation]) extends Command

  final case class GetOrganization(orgId: OrgItemId, replyTo: ActorRef[Confirmation])     extends Command
  final case class GetOrganizationTree(orgId: OrgItemId, itemId: OrgItemId, replyTo: ActorRef[Confirmation])
      extends Command
  final case class GetOrgItem(id: OrgItemId, replyTo: ActorRef[Confirmation])             extends Command
  final case class GetChildren(unitId: OrgItemId, replyTo: ActorRef[Confirmation])        extends Command
  final case class GetPersons(positionId: OrgItemId, replyTo: ActorRef[Confirmation])     extends Command
  final case class GetRoles(positionId: OrgItemId, replyTo: ActorRef[Confirmation])       extends Command
  final case class GetRootPaths(itemIds: Set[OrgItemId], replyTo: ActorRef[Confirmation]) extends Command

  sealed trait Confirmation
  final case class SuccessOrganization(organization: Organization)             extends Confirmation
  final case class SuccessOrganizationTree(tree: OrganizationTree)             extends Confirmation
  final case class SuccessOrgItem(orgItem: OrgItem)                            extends Confirmation
  final case class SuccessChildren(children: Seq[OrgItemId])                   extends Confirmation
  final case class SuccessPersons(persons: Set[OrgItemId])                     extends Confirmation
  final case class SuccessRoles(roles: Set[OrgRoleId])                         extends Confirmation
  final case class SuccessRootPaths(rootPaths: Map[OrgItemId, Seq[OrgItemId]]) extends Confirmation
  final case object Success                                                    extends Confirmation
  final case object OrganizationAlreadyExist                                   extends Confirmation
  final case object OrganizationNotFound                                       extends Confirmation
  final case object OrganizationNotEmpty                                       extends Confirmation
  final case object UnitNotEmpty                                               extends Confirmation
  final case object PositionNotEmpty                                           extends Confirmation
  final case object ItemNotFound                                               extends Confirmation
  final case object AlreadyExist                                               extends Confirmation
  final case object ParentNotFound                                             extends Confirmation
  final case object ChiefNotFound                                              extends Confirmation
  final case object ChiefAlreadyAssigned                                       extends Confirmation
  final case object ChiefNotAssigned                                           extends Confirmation
  final case object PositionLimitExceeded                                      extends Confirmation
  final case object PersonAlreadyAssigned                                      extends Confirmation
  final case object PersonNotAssigned                                          extends Confirmation
  final case object IncorrectOrder                                             extends Confirmation
  final case object IncorrectMoveItemArguments                                 extends Confirmation
  final case object IncorrectCategory                                          extends Confirmation

  implicit val confirmationSuccessOrganizationFormat: Format[SuccessOrganization]                    = Json.format
  implicit val confirmationSuccessOrganizationTreeFormat: Format[SuccessOrganizationTree]            = Json.format
  implicit val confirmationSuccessOrgItemFormat: Format[SuccessOrgItem]                              = Json.format
  implicit val confirmationSuccessChildrenFormat: Format[SuccessChildren]                            = Json.format
  implicit val confirmationSuccessPersonsFormat: Format[SuccessPersons]                              = Json.format
  implicit val confirmationSuccessRolesFormat: Format[SuccessRoles]                                  = Json.format
  implicit val confirmationSuccessRootPathsFormat: Format[SuccessRootPaths]                          = Json.format
  implicit val confirmationSuccessFormat: Format[Success.type]                                       = Json.format
  implicit val confirmationOrganizationAlreadyExistFormat: Format[OrganizationAlreadyExist.type]     = Json.format
  implicit val confirmationOrganizationNotFoundFormat: Format[OrganizationNotFound.type]             = Json.format
  implicit val confirmationOrganizationNotEmptyFormat: Format[OrganizationNotEmpty.type]             = Json.format
  implicit val confirmationUnitNotEmptyFormat: Format[UnitNotEmpty.type]                             = Json.format
  implicit val confirmationPositionNotEmptyFormat: Format[PositionNotEmpty.type]                     = Json.format
  implicit val confirmationItemNotFoundFormat: Format[ItemNotFound.type]                             = Json.format
  implicit val confirmationAlreadyExistFormat: Format[AlreadyExist.type]                             = Json.format
  implicit val confirmationParentNotFoundFormat: Format[ParentNotFound.type]                         = Json.format
  implicit val confirmationChiefNotFoundFormat: Format[ChiefNotFound.type]                           = Json.format
  implicit val confirmationChiefAlreadyAssignedFormat: Format[ChiefAlreadyAssigned.type]             = Json.format
  implicit val confirmationChiefNotAssignedFormat: Format[ChiefNotAssigned.type]                     = Json.format
  implicit val confirmationPositionLimitExceededFormat: Format[PositionLimitExceeded.type]           = Json.format
  implicit val confirmationPersonAlreadyAssignedFormat: Format[PersonAlreadyAssigned.type]           = Json.format
  implicit val confirmationPersonNotAssignedFormat: Format[PersonNotAssigned.type]                   = Json.format
  implicit val confirmationIncorrectOrderFormat: Format[IncorrectOrder.type]                         = Json.format
  implicit val confirmationIncorrectMoveItemArgumentsFormat: Format[IncorrectMoveItemArguments.type] = Json.format
  implicit val confirmationInvalidCategoryFormat: Format[IncorrectCategory.type]                     = Json.format

  implicit val confirmationFormat: Format[Confirmation] = Json.format[Confirmation]

  sealed trait Event extends AggregateEvent[Event] {
    override def aggregateTag: AggregateEventTagger[Event] = Event.Tag
  }

  object Event {
    val Tag: AggregateEventShards[Event] = AggregateEventTag.sharded[Event](numShards = 10)
  }

  final case class OrganizationCreated(
    orgId: OrgItemId,
    name: String,
    shortName: String,
    categoryId: OrgCategoryId,
    createdBy: AnnettePrincipal,
    createdAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class OrganizationDeleted(
    orgId: OrgItemId,
    deletedBy: AnnettePrincipal,
    deletedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class UnitCreated(
    orgId: OrgItemId,
    parentId: OrgItemId,
    unitId: OrgItemId,
    name: String,
    shortName: String,
    order: Option[Int],
    rootPath: Seq[OrgItemId],
    categoryId: OrgCategoryId,
    createdBy: AnnettePrincipal,
    createdAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class UnitDeleted(
    orgId: OrgItemId,
    parentId: OrgItemId,
    unitId: OrgItemId,
    deletedBy: AnnettePrincipal,
    deletedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class CategoryAssigned(
    orgId: OrgItemId,
    itemId: OrgItemId,
    categoryId: OrgCategoryId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class ChiefAssigned(
    orgId: OrgItemId,
    unitId: OrgItemId,
    chiefId: OrgItemId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class ChiefUnassigned(
    orgId: OrgItemId,
    unitId: OrgItemId,
    chiefId: OrgItemId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class PositionCreated(
    orgId: OrgItemId,
    parentId: OrgItemId,
    positionId: OrgItemId,
    name: String,
    shortName: String,
    order: Option[Int],
    rootPath: Seq[OrgItemId],
    limit: Int,
    categoryId: OrgCategoryId,
    createdBy: AnnettePrincipal,
    createdAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class PositionDeleted(
    orgId: OrgItemId,
    parentId: OrgItemId,
    positionId: OrgItemId,
    deletedBy: AnnettePrincipal,
    deletedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  final case class NameUpdated(
    orgId: OrgItemId,
    orgItemId: OrgItemId,
    name: String,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  final case class ShortNameUpdated(
    orgId: OrgItemId,
    orgItemId: OrgItemId,
    shortName: String,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class PositionLimitChanged(
    orgId: OrgItemId,
    positionId: OrgItemId,
    limit: Int,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class PersonAssigned(
    orgId: OrgItemId,
    positionId: OrgItemId,
    personId: OrgItemId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class PersonUnassigned(
    orgId: OrgItemId,
    positionId: OrgItemId,
    personId: PersonId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  final case class OrgRoleAssigned(
    orgId: OrgItemId,
    positionId: OrgItemId,
    orgRoleId: OrgRoleId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class OrgRoleUnassigned(
    orgId: OrgItemId,
    positionId: OrgItemId,
    orgRoleId: OrgRoleId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  final case class ItemMoved(
    orgId: OrgItemId,
    orgItemId: OrgItemId,
    oldParentId: OrgItemId,
    newParentId: OrgItemId,
    order: Option[Int] = None,
    affectedItemIds: Set[OrgItemId],
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class ItemOrderChanged(
    orgId: OrgItemId,
    orgItemId: OrgItemId,
    parentId: OrgItemId,
    order: Int,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  implicit val organizationCreatedFormat: Format[OrganizationCreated]   = Json.format
  implicit val organizationDeletedFormat: Format[OrganizationDeleted]   = Json.format
  implicit val unitCreatedFormat: Format[UnitCreated]                   = Json.format
  implicit val unitDeletedFormat: Format[UnitDeleted]                   = Json.format
  implicit val categoryAssignedFormat: Format[CategoryAssigned]         = Json.format
  implicit val chiefAssignedFormat: Format[ChiefAssigned]               = Json.format
  implicit val chiefUnassignedFormat: Format[ChiefUnassigned]           = Json.format
  implicit val positionCreatedFormat: Format[PositionCreated]           = Json.format
  implicit val positionDeletedFormat: Format[PositionDeleted]           = Json.format
  implicit val nameUpdatedFormat: Format[NameUpdated]                   = Json.format
  implicit val shortNameUpdatedFormat: Format[ShortNameUpdated]         = Json.format
  implicit val positionLimitChangedFormat: Format[PositionLimitChanged] = Json.format
  implicit val personAssignedFormat: Format[PersonAssigned]             = Json.format
  implicit val personUnassignedFormat: Format[PersonUnassigned]         = Json.format
  implicit val orgRoleAssignedFormat: Format[OrgRoleAssigned]           = Json.format
  implicit val orgRoleUnassignedFormat: Format[OrgRoleUnassigned]       = Json.format
  implicit val itemMovedFormat: Format[ItemMoved]                       = Json.format
  implicit val itemOrderChangedFormat: Format[ItemOrderChanged]         = Json.format

  val empty = HierarchyEntity(None)

  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Hierarchy")

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

  implicit val orgStructureEntityFormat: Format[HierarchyEntity] = Json.format

}

final case class HierarchyEntity(
  maybeState: Option[HierarchyState]
) {

  import HierarchyEntity._

  def applyCommand(cmd: Command): ReplyEffect[Event, HierarchyEntity] =
    cmd match {
      case cmd: CreateOrganization  => createOrganization(cmd)
      case cmd: DeleteOrganization  => deleteOrganization(cmd)
      case cmd: UpdateName          => updateName(cmd)
      case cmd: UpdateShortName     => updateShortName(cmd)
      case cmd: CreateUnit          => createUnit(cmd)
      case cmd: DeleteUnit          => deleteUnit(cmd)
      case cmd: AssignCategory      => assignCategory(cmd)
      case cmd: AssignChief         => assignChief(cmd)
      case cmd: UnassignChief       => unassignChief(cmd)
      case cmd: CreatePosition      => createPosition(cmd)
      case cmd: DeletePosition      => deletePosition(cmd)
      case cmd: ChangePositionLimit => changePositionLimit(cmd)
      case cmd: AssignPerson        => assignPerson(cmd)
      case cmd: UnassignPerson      => unassignPerson(cmd)
      case cmd: AssignOrgRole       => assignOrgRole(cmd)
      case cmd: UnassignOrgRole     => unassignOrgRole(cmd)
      case cmd: MoveItem            => moveItem(cmd)
      case cmd: ChangeItemOrder     => changeItemOrder(cmd)

      case cmd: GetOrgItem          => getOrgItem(cmd)
      case cmd: GetOrganization     => getOrganization(cmd)
      case cmd: GetOrganizationTree => getOrganizationTree(cmd)

      case cmd: GetChildren  => getChildren(cmd)
      case cmd: GetPersons   => getPersons(cmd)
      case cmd: GetRoles     => getRoles(cmd)
      case cmd: GetRootPaths => getRootPaths(cmd)

    }

  def createOrganization(
    cmd: CreateOrganization
  ): ReplyEffect[Event, HierarchyEntity] =
    maybeState match {
      case None    =>
        val event = cmd.payload
          .into[OrganizationCreated]
          .transform
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
      case Some(_) => Effect.reply(cmd.replyTo)(OrganizationAlreadyExist)
    }

  def deleteOrganization(
    cmd: DeleteOrganization
  ): ReplyEffect[Event, HierarchyEntity] =
    maybeState match {
      case None => Effect.reply(cmd.replyTo)(OrganizationNotFound)

      case Some(state) if state.units.size == 1 && state.units.isDefinedAt(state.orgId) && state.positions.isEmpty =>
        val event = cmd.payload
          .into[OrganizationDeleted]
          .transform
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)

      case _                                                                                                       =>
        Effect.reply(cmd.replyTo)(OrganizationNotEmpty)
    }

  def createUnit(
    cmd: CreateUnit
  ): ReplyEffect[Event, HierarchyEntity] =
    maybeState match {
      case None                                                          => Effect.reply(cmd.replyTo)(OrganizationNotFound)
      case Some(state)
          if state.units.isDefinedAt(cmd.payload.unitId) ||
            state.positions.isDefinedAt(cmd.payload.unitId) =>
        Effect.reply(cmd.replyTo)(AlreadyExist)
      case Some(state) if !state.units.isDefinedAt(cmd.payload.parentId) => Effect.reply(cmd.replyTo)(ParentNotFound)
      case Some(state)                                                   =>
        val event = cmd.payload
          .into[UnitCreated]
          .withFieldConst(_.rootPath, state.getRootPath(cmd.payload.parentId) :+ cmd.payload.unitId)
          .transform
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def deleteUnit(
    cmd: DeleteUnit
  ): ReplyEffect[Event, HierarchyEntity] = {
    val replyTo = cmd.replyTo
    maybeState match {
      case None                                                        => Effect.reply(replyTo)(OrganizationNotFound)
      case Some(state) if !state.units.isDefinedAt(cmd.payload.unitId) => Effect.reply(replyTo)(ItemNotFound)
      case Some(state) if state.units.isDefinedAt(cmd.payload.unitId)  =>
        val unit = state.units(cmd.payload.unitId)
        if (unit.children.nonEmpty) Effect.reply(replyTo)(UnitNotEmpty)
        else if (unit.chief.nonEmpty) Effect.reply(replyTo)(ChiefAlreadyAssigned)
        else {
          val event = cmd.payload
            .into[UnitDeleted]
            .withFieldConst(_.parentId, unit.parentId)
            .transform
          Effect
            .persist(event)
            .thenReply(replyTo)(_ => Success)
        }
    }
  }

  def assignCategory(cmd: AssignCategory): ReplyEffect[Event, HierarchyEntity] =
    maybeState match {
      case None                                                           => Effect.reply(cmd.replyTo)(OrganizationNotFound)
      case Some(state) if state.units.isDefinedAt(cmd.payload.itemId)     =>
        val unit = state.units(cmd.payload.itemId)
        if (
          (unit.parentId == ROOT && cmd.category.forOrganization) ||
          (unit.parentId != ROOT && cmd.category.forUnit)
        ) {
          val event = cmd.payload
            .into[CategoryAssigned]
            .transform
          Effect
            .persist(event)
            .thenReply(cmd.replyTo)(_ => Success)
        } else
          Effect.reply(cmd.replyTo)(IncorrectCategory)

      case Some(state) if state.positions.isDefinedAt(cmd.payload.itemId) =>
        if (cmd.category.forPosition) {
          val event = cmd.payload
            .into[CategoryAssigned]
            .transform
          Effect
            .persist(event)
            .thenReply(cmd.replyTo)(_ => Success)
        } else
          Effect.reply(cmd.replyTo)(IncorrectCategory)
      case _                                                              =>
        Effect.reply(cmd.replyTo)(ItemNotFound)
    }
  def assignChief(cmd: AssignChief): ReplyEffect[Event, HierarchyEntity]       =
    maybeState match {
      case None                                                             => Effect.reply(cmd.replyTo)(OrganizationNotFound)
      case Some(state) if !state.units.isDefinedAt(cmd.payload.unitId)      => Effect.reply(cmd.replyTo)(ItemNotFound)
      case Some(state) if !state.positions.isDefinedAt(cmd.payload.chiefId) => Effect.reply(cmd.replyTo)(ChiefNotFound)
      case Some(state) if state.positions(cmd.payload.chiefId).limit != 1   =>
        Effect.reply(cmd.replyTo)(PositionLimitExceeded)
      case Some(state)                                                      =>
        if (state.units(cmd.payload.unitId).chief.isEmpty) {
          val event = cmd.payload
            .into[ChiefAssigned]
            .transform
          Effect
            .persist(event)
            .thenReply(cmd.replyTo)(_ => Success)
        } else
          Effect.reply(cmd.replyTo)(ChiefAlreadyAssigned)
    }

  def unassignChief(
    cmd: UnassignChief
  ): ReplyEffect[Event, HierarchyEntity] =
    maybeState match {
      case None                                                        => Effect.reply(cmd.replyTo)(OrganizationNotFound)
      case Some(state) if !state.units.isDefinedAt(cmd.payload.unitId) => Effect.reply(cmd.replyTo)(ItemNotFound)
      case Some(state)                                                 =>
        state
          .units(cmd.payload.unitId)
          .chief
          .map { chief =>
            val event = cmd.payload
              .into[ChiefUnassigned]
              .withFieldConst(_.chiefId, chief)
              .transform
            Effect
              .persist[Event, HierarchyEntity](event)
              .thenReply(cmd.replyTo)(_ => Success)
          }
          .getOrElse(
            Effect.reply(cmd.replyTo)(ChiefNotAssigned)
          )
    }

  def createPosition(
    cmd: CreatePosition
  ): ReplyEffect[Event, HierarchyEntity] =
    maybeState match {
      case None                                                          => Effect.reply(cmd.replyTo)(OrganizationNotFound)
      case Some(state)
          if state.units.isDefinedAt(cmd.payload.positionId) ||
            state.positions.isDefinedAt(cmd.payload.positionId) =>
        Effect.reply(cmd.replyTo)(AlreadyExist)
      case Some(state) if !state.units.isDefinedAt(cmd.payload.parentId) => Effect.reply(cmd.replyTo)(ParentNotFound)
      case Some(state)                                                   =>
        val event = cmd.payload
          .into[PositionCreated]
          .withFieldConst(_.rootPath, state.getRootPath(cmd.payload.parentId) :+ cmd.payload.positionId)
          .transform
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def deletePosition(
    cmd: DeletePosition
  ): ReplyEffect[Event, HierarchyEntity] =
    maybeState match {
      case None                                                                      => Effect.reply(cmd.replyTo)(OrganizationNotFound)
      case Some(state) if !state.positions.isDefinedAt(cmd.payload.positionId)       =>
        Effect.reply(cmd.replyTo)(ItemNotFound)
      case Some(state) if state.chiefAssignments.isDefinedAt(cmd.payload.positionId) =>
        Effect.reply(cmd.replyTo)(ChiefAlreadyAssigned)
      case Some(state) if state.positions.isDefinedAt(cmd.payload.positionId)        =>
        val position = state.positions(cmd.payload.positionId)
        if (position.persons.isEmpty) {
          val event = cmd.payload
            .into[PositionDeleted]
            .withFieldConst(_.parentId, position.parentId)
            .transform
          Effect
            .persist(event)
            .thenReply(cmd.replyTo)(_ => Success)
        } else
          Effect.reply(cmd.replyTo)(PositionNotEmpty)
    }

  def updateName(cmd: UpdateName): ReplyEffect[Event, HierarchyEntity] = {
    val replyTo = cmd.replyTo
    val payload = cmd.payload
    maybeState match {
      case None => Effect.reply(replyTo)(OrganizationNotFound)
      case Some(state)
          if state.positions.isDefinedAt(payload.orgItemId) || state.units.isDefinedAt(cmd.payload.orgItemId) =>
        val event = payload
          .into[NameUpdated]
          .transform
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
      case _    => Effect.reply(cmd.replyTo)(ItemNotFound)
    }
  }

  def updateShortName(cmd: UpdateShortName): ReplyEffect[Event, HierarchyEntity] = {
    val replyTo = cmd.replyTo
    val payload = cmd.payload
    maybeState match {
      case None => Effect.reply(replyTo)(OrganizationNotFound)
      case Some(state)
          if state.positions.isDefinedAt(payload.orgItemId) || state.units.isDefinedAt(cmd.payload.orgItemId) =>
        val event = payload
          .into[ShortNameUpdated]
          .transform
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
      case _    => Effect.reply(cmd.replyTo)(ItemNotFound)
    }
  }

  def changePositionLimit(
    cmd: ChangePositionLimit
  ): ReplyEffect[Event, HierarchyEntity] =
    maybeState match {
      case None                                                                                    => Effect.reply(cmd.replyTo)(OrganizationNotFound)
      case Some(state) if !state.positions.isDefinedAt(cmd.payload.positionId)                     =>
        Effect.reply(cmd.replyTo)(ItemNotFound)
      case Some(state) if state.chiefAssignments.isDefinedAt(cmd.payload.positionId)               =>
        Effect.reply(cmd.replyTo)(PositionLimitExceeded)
      case Some(state) if state.positions(cmd.payload.positionId).persons.size > cmd.payload.limit =>
        Effect.reply(cmd.replyTo)(PositionLimitExceeded)
      case Some(_) if cmd.payload.limit <= 0                                                       => Effect.reply(cmd.replyTo)(PositionLimitExceeded)
      case Some(_)                                                                                 =>
        val event = cmd.payload
          .into[PositionLimitChanged]
          .transform
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def assignPerson(cmd: AssignPerson): ReplyEffect[Event, HierarchyEntity] =
    maybeState match {
      case None                                                                                          => Effect.reply(cmd.replyTo)(OrganizationNotFound)
      case Some(state) if !state.positions.isDefinedAt(cmd.payload.positionId)                           =>
        Effect.reply(cmd.replyTo)(ItemNotFound)
      case Some(state) if state.positions(cmd.payload.positionId).persons.contains(cmd.payload.personId) =>
        Effect.reply(cmd.replyTo)(PersonAlreadyAssigned)
      case Some(state)
          if state.positions(cmd.payload.positionId).limit == state.positions(cmd.payload.positionId).persons.size =>
        Effect.reply(cmd.replyTo)(PositionLimitExceeded)
      case Some(_)                                                                                       =>
        val event = cmd.payload
          .into[PersonAssigned]
          .transform
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def unassignPerson(
    cmd: UnassignPerson
  ): ReplyEffect[Event, HierarchyEntity] =
    maybeState match {
      case None                                                                => Effect.reply(cmd.replyTo)(OrganizationNotFound)
      case Some(state) if !state.positions.isDefinedAt(cmd.payload.positionId) =>
        Effect.reply(cmd.replyTo)(ItemNotFound)
      case Some(state)                                                         =>
        if (state.positions(cmd.payload.positionId).persons.contains(cmd.payload.personId)) {
          val event = cmd.payload
            .into[PersonUnassigned]
            .transform
          Effect
            .persist(event)
            .thenReply(cmd.replyTo)(_ => Success)
        } else
          Effect.reply(cmd.replyTo)(PersonNotAssigned)
    }

  def assignOrgRole(cmd: AssignOrgRole): ReplyEffect[Event, HierarchyEntity] =
    maybeState match {
      case None                                                                => Effect.reply(cmd.replyTo)(OrganizationNotFound)
      case Some(state) if !state.positions.isDefinedAt(cmd.payload.positionId) =>
        Effect.reply(cmd.replyTo)(ItemNotFound)
      case Some(_)                                                             =>
        val event = cmd.payload
          .into[OrgRoleAssigned]
          .transform
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def unassignOrgRole(cmd: UnassignOrgRole): ReplyEffect[Event, HierarchyEntity] =
    maybeState match {
      case None                                                                => Effect.reply(cmd.replyTo)(OrganizationNotFound)
      case Some(state) if !state.positions.isDefinedAt(cmd.payload.positionId) =>
        Effect.reply(cmd.replyTo)(ItemNotFound)
      case Some(state)                                                         =>
        if (state.positions(cmd.payload.positionId).orgRoles.contains(cmd.payload.orgRoleId)) {
          val event = cmd.payload
            .into[OrgRoleUnassigned]
            .transform
          Effect
            .persist(event)
            .thenReply(cmd.replyTo)(_ => Success)
        } else
          Effect.reply(cmd.replyTo)(Success)
    }

  def moveItem(cmd: MoveItem): ReplyEffect[Event, HierarchyEntity] = {
    val replyTo = cmd.replyTo
    val payload = cmd.payload
    maybeState match {
      case None                                                            => Effect.reply(replyTo)(OrganizationNotFound)
      case Some(state) if state.positions.isDefinedAt(payload.newParentId) =>
        Effect.reply(replyTo)(IncorrectMoveItemArguments)
      case Some(state)
          if state.positions.isDefinedAt(payload.orgItemId) ||
            state.units.isDefinedAt(payload.orgItemId) =>
        val oldParentId = state.units
          .get(payload.orgItemId)
          .map(_.parentId)
          .getOrElse(state.positions.get(payload.orgItemId).map(_.parentId).get)
        val rootPath    = state.getRootPath(payload.newParentId)
        if (rootPath.contains(payload.orgItemId) || payload.orgItemId == payload.newParentId)
          Effect.reply(replyTo)(IncorrectMoveItemArguments)
        else {
          val affectedItemIds =
            state.getDescendants(payload.orgItemId) + payload.orgItemId + payload.newParentId + oldParentId
          val event           = payload
            .into[ItemMoved]
            .withFieldConst(_.affectedItemIds, affectedItemIds)
            .withFieldConst(_.oldParentId, oldParentId)
            .transform
          Effect
            .persist(event)
            .thenReply(replyTo)(_ => Success)
        }
      case _                                                               => Effect.reply(replyTo)(ItemNotFound)
    }
  }

  def changeItemOrder(
    cmd: ChangeItemOrder
  ): ReplyEffect[Event, HierarchyEntity] = {
    val replyTo = cmd.replyTo
    val payload = cmd.payload
    maybeState match {
      case None => Effect.reply(replyTo)(OrganizationNotFound)
      case Some(state)
          if state.positions.isDefinedAt(payload.orgItemId) ||
            state.units.isDefinedAt(payload.orgItemId) =>
        val parentId = state.positions
          .get(payload.orgItemId)
          .map(_.parentId)
          .getOrElse(state.units.get(payload.orgItemId).map(_.parentId).get)
        val children = state.units(parentId).children
        assert(
          children.contains(payload.orgItemId),
          s"Parent unit ($parentId) does not contain item (${payload.orgItemId}) in children $children"
        )
        if (payload.order >= 0 && payload.order < children.size) {
          val event = payload
            .into[ItemOrderChanged]
            .withFieldConst(_.parentId, parentId)
            .transform
          Effect
            .persist(event)
            .thenReply(replyTo)(_ => Success)
        } else
          Effect.reply(replyTo)(IncorrectOrder)
      case _    => Effect.reply(replyTo)(ItemNotFound)

    }
  }

  def getOrgItem(cmd: GetOrgItem): ReplyEffect[Event, HierarchyEntity] =
    maybeState match {
      case None                                               => Effect.reply(cmd.replyTo)(OrganizationNotFound)
      case Some(state) if state.positions.isDefinedAt(cmd.id) =>
        val position = state.positions(cmd.id)
        val rootPath = state.getRootPath(cmd.id)
        val orgItem  = position
          .into[OrgPosition]
          .withFieldConst(_.orgId, state.orgId)
          .withFieldConst(_.rootPath, rootPath)
          .withFieldConst(_.level, rootPath.length - 1)
          .transform
        Effect.reply(cmd.replyTo)(SuccessOrgItem(orgItem))
      case Some(state) if state.units.isDefinedAt(cmd.id)     =>
        val unit     = state.units(cmd.id)
        val rootPath = state.getRootPath(cmd.id)
        val orgItem  = unit
          .into[OrgUnit]
          .withFieldConst(_.orgId, state.orgId)
          .withFieldConst(_.rootPath, rootPath)
          .withFieldConst(_.level, rootPath.length - 1)
          .transform
        Effect.reply(cmd.replyTo)(SuccessOrgItem(orgItem))
      case _                                                  => Effect.reply(cmd.replyTo)(ItemNotFound)
    }

  def getOrganization(
    cmd: GetOrganization
  ): ReplyEffect[Event, HierarchyEntity] =
    maybeState match {
      case None        => Effect.reply(cmd.replyTo)(OrganizationNotFound)
      case Some(state) =>
        val organization = state
          .into[Organization]
          .withFieldConst(_.orgId, cmd.orgId)
          .transform

        Effect.reply(cmd.replyTo)(SuccessOrganization(organization))
    }

  def getOrganizationTree(
    cmd: GetOrganizationTree
  ): ReplyEffect[Event, HierarchyEntity] =
    maybeState match {
      case None                                               => Effect.reply(cmd.replyTo)(OrganizationNotFound)
      case Some(state) if state.units.isDefinedAt(cmd.itemId) =>
        val organizationTree = OrganizationTree(state.orgId, state.getOrgTreeItem(cmd.itemId))
        Effect.reply(cmd.replyTo)(SuccessOrganizationTree(organizationTree))
      case _                                                  => Effect.reply(cmd.replyTo)(ItemNotFound)
    }

  def getChildren(cmd: GetChildren): ReplyEffect[Event, HierarchyEntity] =
    maybeState match {
      case None                                               => Effect.reply(cmd.replyTo)(OrganizationNotFound)
      case Some(state) if state.units.isDefinedAt(cmd.unitId) =>
        Effect.reply(cmd.replyTo)(SuccessChildren(state.units(cmd.unitId).children))
      case _                                                  => Effect.reply(cmd.replyTo)(ItemNotFound)
    }

  def getPersons(cmd: GetPersons): ReplyEffect[Event, HierarchyEntity] =
    maybeState match {
      case None                                                       => Effect.reply(cmd.replyTo)(OrganizationNotFound)
      case Some(state) if state.positions.isDefinedAt(cmd.positionId) =>
        Effect.reply(cmd.replyTo)(SuccessPersons(state.positions(cmd.positionId).persons))
      case _                                                          => Effect.reply(cmd.replyTo)(ItemNotFound)
    }

  def getRoles(cmd: GetRoles): ReplyEffect[Event, HierarchyEntity] =
    maybeState match {
      case None                                                       => Effect.reply(cmd.replyTo)(OrganizationNotFound)
      case Some(state) if state.positions.isDefinedAt(cmd.positionId) =>
        Effect.reply(cmd.replyTo)(SuccessRoles(state.positions(cmd.positionId).orgRoles))
      case _                                                          => Effect.reply(cmd.replyTo)(ItemNotFound)
    }

  def getRootPaths(cmd: GetRootPaths): ReplyEffect[Event, HierarchyEntity] =
    maybeState match {
      case None        => Effect.reply(cmd.replyTo)(OrganizationNotFound)
      case Some(state) =>
        val rootPaths = cmd.itemIds.flatMap { itemId =>
          val rootPath = state.getRootPath(itemId)
          if (rootPath.nonEmpty)
            Some(itemId -> rootPath)
          else
            None
        }.toMap
        Effect.reply(cmd.replyTo)(SuccessRootPaths(rootPaths))
    }

  def applyEvent(event: Event): HierarchyEntity =
    event match {
      case event: OrganizationCreated  => onOrganizationCreated(event)
      case _: OrganizationDeleted      => onOrganizationDeleted()
      case event: NameUpdated          => onNameUpdated(event)
      case event: ShortNameUpdated     => onShortNameUpdated(event)
      case event: UnitCreated          => onUnitCreated(event)
      case event: UnitDeleted          => onUnitDeleted(event)
      case event: CategoryAssigned     => onCategoryAssigned(event)
      case event: ChiefAssigned        => onChiefAssigned(event)
      case event: ChiefUnassigned      => onChiefUnassigned(event)
      case event: PositionCreated      => onPositionCreated(event)
      case event: PositionDeleted      => onPositionDeleted(event)
      case event: PositionLimitChanged => onPositionLimitChanged(event)
      case event: PersonAssigned       => onPersonAssigned(event)
      case event: PersonUnassigned     => onPersonUnassigned(event)
      case event: OrgRoleAssigned      => onOrgRolesAssigned(event)
      case event: OrgRoleUnassigned    => onOrgRolesUnassigned(event)
      case event: ItemMoved            => onItemMoved(event)
      case event: ItemOrderChanged     => onItemOrderChanged(event)

    }

  def onOrganizationCreated(event: OrganizationCreated): HierarchyEntity = {
    val rootUnit = HierarchyUnit(
      id = event.orgId,
      parentId = ROOT,
      name = event.name,
      shortName = event.shortName,
      categoryId = event.categoryId,
      updatedAt = event.createdAt,
      updatedBy = event.createdBy
    )
    HierarchyEntity(
      Some(
        HierarchyState(
          orgId = event.orgId,
          units = Map(rootUnit.id -> rootUnit),
          positions = Map.empty,
          chiefAssignments = Map.empty,
          personAssignments = Map.empty,
          orgRoleAssignments = Map.empty,
          updatedAt = event.createdAt,
          updatedBy = event.createdBy
        )
      )
    )
  }

  def onOrganizationDeleted(): HierarchyEntity = HierarchyEntity(None)

  def onUnitCreated(event: UnitCreated): HierarchyEntity =
    HierarchyEntity(
      maybeState.map { state =>
        val newUnit                         = HierarchyUnit(
          id = event.unitId,
          parentId = event.parentId,
          name = event.name,
          shortName = event.shortName,
          categoryId = event.categoryId,
          updatedAt = event.createdAt,
          updatedBy = event.createdBy
        )
        val parent                          = state.units(event.parentId)
        val children                        = parent.children
        val updatedChildren: Seq[OrgItemId] = event.order match {
          case Some(pos) if pos >= 0 && pos < children.size =>
            (children.take(pos) :+ event.unitId) ++ children.drop(pos)
          case _                                            => children :+ event.unitId
        }
        val updatedParent                   = parent.copy(
          children = updatedChildren,
          updatedAt = event.createdAt,
          updatedBy = event.createdBy
        )

        state.copy(
          units = state.units + (updatedParent.id -> updatedParent) + (newUnit.id -> newUnit),
          updatedAt = event.createdAt,
          updatedBy = event.createdBy
        )
      }
    )

  def onUnitDeleted(event: UnitDeleted): HierarchyEntity =
    HierarchyEntity(
      maybeState.map { state =>
        val parentId        = state.units(event.unitId).parentId
        val parent          = state.units(parentId)
        val updatedChildren = parent.children.filter(_ != event.unitId)
        val updatedParent   = parent.copy(
          children = updatedChildren,
          updatedAt = event.deletedAt,
          updatedBy = event.deletedBy
        )
        state.copy(
          units = state.units + (updatedParent.id -> updatedParent) - event.unitId,
          updatedAt = event.deletedAt,
          updatedBy = event.deletedBy
        )
      }
    )

  def onCategoryAssigned(event: CategoryAssigned): HierarchyEntity =
    HierarchyEntity(
      maybeState.map { state =>
        val updatedUnit     = state.units
          .get(event.itemId)
          .map(
            _.copy(
              categoryId = event.categoryId,
              updatedAt = event.updatedAt,
              updatedBy = event.updatedBy
            )
          )
        val updatedPosition = state.positions
          .get(event.itemId)
          .map(
            _.copy(
              categoryId = event.categoryId,
              updatedAt = event.updatedAt,
              updatedBy = event.updatedBy
            )
          )
        state.copy(
          units = updatedUnit.map(unit => state.units + (unit.id -> unit)).getOrElse(state.units),
          positions =
            updatedPosition.map(position => state.positions + (position.id -> position)).getOrElse(state.positions),
          updatedAt = event.updatedAt,
          updatedBy = event.updatedBy
        )
      }
    )

  def onChiefAssigned(event: ChiefAssigned): HierarchyEntity =
    HierarchyEntity(
      maybeState.map { state =>
        val updatedUnit        = state
          .units(event.unitId)
          .copy(
            chief = Some(event.chiefId),
            updatedAt = event.updatedAt,
            updatedBy = event.updatedBy
          )
        val chiefAssignmentSet = state.chiefAssignments.getOrElse(event.chiefId, Set.empty) + event.unitId
        state.copy(
          units = state.units + (updatedUnit.id                      -> updatedUnit),
          chiefAssignments = state.chiefAssignments + (event.chiefId -> chiefAssignmentSet),
          updatedAt = event.updatedAt,
          updatedBy = event.updatedBy
        )
      }
    )

  def onChiefUnassigned(event: ChiefUnassigned): HierarchyEntity =
    HierarchyEntity(
      maybeState.map { state =>
        val unit                                                    = state.units(event.unitId)
        val chiefId                                                 = unit.chief.get
        val updatedUnit                                             = unit.copy(
          chief = None,
          updatedAt = event.updatedAt,
          updatedBy = event.updatedBy
        )
        val chiefAssignmentSet                                      = state.chiefAssignments.getOrElse(chiefId, Set.empty) - event.unitId
        val updatedChiefAssignments: Map[OrgItemId, Set[OrgItemId]] =
          if (chiefAssignmentSet.isEmpty)
            state.chiefAssignments - chiefId
          else
            state.chiefAssignments + (chiefId -> chiefAssignmentSet)
        state.copy(
          units = state.units + (updatedUnit.id -> updatedUnit),
          chiefAssignments = updatedChiefAssignments,
          updatedAt = event.updatedAt,
          updatedBy = event.updatedBy
        )
      }
    )

  def onPositionCreated(event: PositionCreated): HierarchyEntity =
    HierarchyEntity(
      maybeState.map { state =>
        val newPosition                     = HierarchyPosition(
          id = event.positionId,
          parentId = event.parentId,
          name = event.name,
          shortName = event.shortName,
          limit = event.limit,
          categoryId = event.categoryId,
          updatedAt = event.createdAt,
          updatedBy = event.createdBy
        )
        val parent                          = state.units(event.parentId)
        val children                        = parent.children
        val updatedChildren: Seq[OrgItemId] = event.order match {
          case Some(pos) if pos >= 0 && pos < children.size =>
            (children.take(pos) :+ event.positionId) ++ children.drop(pos)
          case _                                            => children :+ event.positionId
        }
        val updatedParent                   = parent.copy(
          children = updatedChildren,
          updatedAt = event.createdAt,
          updatedBy = event.createdBy
        )

        state.copy(
          units = state.units + (updatedParent.id       -> updatedParent),
          positions = state.positions + (newPosition.id -> newPosition),
          updatedAt = event.createdAt,
          updatedBy = event.createdBy
        )
      }
    )

  def onPositionDeleted(event: PositionDeleted): HierarchyEntity =
    HierarchyEntity(
      maybeState.map { state =>
        val parentId        = state.positions(event.positionId).parentId
        val parent          = state.units(parentId)
        val updatedChildren = parent.children.filter(_ != event.positionId)
        val updatedParent   = parent.copy(
          children = updatedChildren,
          updatedAt = event.deletedAt,
          updatedBy = event.deletedBy
        )
        state.copy(
          units = state.units + (updatedParent.id -> updatedParent),
          positions = state.positions - event.positionId,
          updatedAt = event.deletedAt,
          updatedBy = event.deletedBy
        )
      }
    )

  def onNameUpdated(event: NameUpdated): HierarchyEntity =
    HierarchyEntity(
      maybeState.map { state =>
        state.positions
          .get(event.orgItemId)
          .map { position =>
            val updatedPosition = position.copy(
              name = event.name,
              updatedAt = event.updatedAt,
              updatedBy = event.updatedBy
            )
            state.copy(
              positions = state.positions + (updatedPosition.id -> updatedPosition),
              updatedAt = event.updatedAt,
              updatedBy = event.updatedBy
            )
          }
          .getOrElse {
            state.units
              .get(event.orgItemId)
              .map { unit =>
                val updatedUnit = unit.copy(
                  name = event.name,
                  updatedAt = event.updatedAt,
                  updatedBy = event.updatedBy
                )
                state.copy(
                  units = state.units + (updatedUnit.id -> updatedUnit),
                  updatedAt = event.updatedAt,
                  updatedBy = event.updatedBy
                )
              }
              .getOrElse(state)
          }
      }
    )

  def onShortNameUpdated(event: ShortNameUpdated): HierarchyEntity =
    HierarchyEntity(
      maybeState.map { state =>
        state.positions
          .get(event.orgItemId)
          .map { position =>
            val updatedPosition = position.copy(
              shortName = event.shortName,
              updatedAt = event.updatedAt,
              updatedBy = event.updatedBy
            )
            state.copy(
              positions = state.positions + (updatedPosition.id -> updatedPosition),
              updatedAt = event.updatedAt,
              updatedBy = event.updatedBy
            )
          }
          .getOrElse {
            state.units
              .get(event.orgItemId)
              .map { unit =>
                val updatedUnit = unit.copy(
                  shortName = event.shortName,
                  updatedAt = event.updatedAt,
                  updatedBy = event.updatedBy
                )
                state.copy(
                  units = state.units + (updatedUnit.id -> updatedUnit),
                  updatedAt = event.updatedAt,
                  updatedBy = event.updatedBy
                )
              }
              .getOrElse(state)
          }
      }
    )

  def onPositionLimitChanged(event: PositionLimitChanged): HierarchyEntity =
    HierarchyEntity(
      maybeState.map { state =>
        val position        = state.positions(event.positionId)
        val updatedPosition = position.copy(
          limit = event.limit,
          updatedAt = event.updatedAt,
          updatedBy = event.updatedBy
        )
        state.copy(
          positions = state.positions + (updatedPosition.id -> updatedPosition),
          updatedAt = event.updatedAt,
          updatedBy = event.updatedBy
        )
      }
    )

  def onPersonAssigned(event: PersonAssigned): HierarchyEntity =
    HierarchyEntity(
      maybeState.map { state =>
        val position            = state.positions(event.positionId)
        val updatedPosition     = position.copy(
          persons = position.persons + event.personId,
          updatedAt = event.updatedAt,
          updatedBy = event.updatedBy
        )
        val personAssignmentSet = state.personAssignments.getOrElse(event.personId, Set.empty) + event.positionId
        state.copy(
          positions = state.positions + (updatedPosition.id             -> updatedPosition),
          personAssignments = state.personAssignments + (event.personId -> personAssignmentSet),
          updatedAt = event.updatedAt,
          updatedBy = event.updatedBy
        )
      }
    )

  def onPersonUnassigned(event: PersonUnassigned): HierarchyEntity =
    HierarchyEntity(
      maybeState.map { state =>
        val position                                                 = state.positions(event.positionId)
        val personId                                                 = event.personId
        val updatedPosition                                          = position.copy(
          persons = position.persons - personId,
          updatedAt = event.updatedAt,
          updatedBy = event.updatedBy
        )
        val personAssignmentSet                                      = state.orgRoleAssignments.getOrElse(personId, Set.empty) - event.positionId
        val updatedPersonAssignments: Map[OrgItemId, Set[OrgItemId]] =
          if (personAssignmentSet.isEmpty)
            state.personAssignments - personId
          else
            state.personAssignments + (personId -> personAssignmentSet)

        state.copy(
          positions = state.positions + (updatedPosition.id -> updatedPosition),
          personAssignments = updatedPersonAssignments,
          updatedAt = event.updatedAt,
          updatedBy = event.updatedBy
        )
      }
    )

  def onOrgRolesAssigned(event: OrgRoleAssigned): HierarchyEntity =
    HierarchyEntity(
      maybeState.map { state =>
        val position             = state.positions(event.positionId)
        val updatedPosition      = position.copy(
          orgRoles = position.orgRoles + event.orgRoleId,
          updatedAt = event.updatedAt,
          updatedBy = event.updatedBy
        )
        val orgRoleAssignmentSet = state.orgRoleAssignments.getOrElse(event.orgRoleId, Set.empty) + event.positionId
        state.copy(
          positions = state.positions + (updatedPosition.id                -> updatedPosition),
          orgRoleAssignments = state.orgRoleAssignments + (event.orgRoleId -> orgRoleAssignmentSet),
          updatedAt = event.updatedAt,
          updatedBy = event.updatedBy
        )
      }
    )

  def onOrgRolesUnassigned(event: OrgRoleUnassigned): HierarchyEntity =
    HierarchyEntity(
      maybeState.map { state =>
        val position                                                  = state.positions(event.positionId)
        val orgRoleId                                                 = event.orgRoleId
        val updatedPosition                                           = position.copy(
          orgRoles = position.orgRoles - orgRoleId,
          updatedAt = event.updatedAt,
          updatedBy = event.updatedBy
        )
        val orgRoleAssignmentSet                                      = state.orgRoleAssignments.getOrElse(orgRoleId, Set.empty) - event.positionId
        val updatedOrgRoleAssignments: Map[OrgItemId, Set[OrgItemId]] =
          if (orgRoleAssignmentSet.isEmpty)
            state.orgRoleAssignments - orgRoleId
          else
            state.orgRoleAssignments + (orgRoleId -> orgRoleAssignmentSet)
        state.copy(
          positions = state.positions + (updatedPosition.id -> updatedPosition),
          orgRoleAssignments = updatedOrgRoleAssignments,
          updatedAt = event.updatedAt,
          updatedBy = event.updatedBy
        )
      }
    )

  def onItemMoved(event: ItemMoved): HierarchyEntity =
    HierarchyEntity(
      maybeState.map { state =>
        val oldParent                = state.units(event.oldParentId)
        val updatedOldChildren       = oldParent.children.filter(_ != event.orgItemId)
        val updatedOldParent         = oldParent.copy(
          children = updatedOldChildren,
          updatedAt = event.updatedAt,
          updatedBy = event.updatedBy
        )
        val newParent                = state.units(event.newParentId)
        val updatedNewChildren       = event.order match {
          case Some(pos) if pos >= 0 && pos < newParent.children.size =>
            (newParent.children.take(pos) :+ event.orgItemId) ++ newParent.children.drop(pos)
          case _                                                      => newParent.children :+ event.orgItemId
        }
        val updatedNewParent         = newParent.copy(
          children = updatedNewChildren,
          updatedAt = event.updatedAt,
          updatedBy = event.updatedBy
        )
        val (newUnits, newPositions) = if (state.units.isDefinedAt(event.orgItemId)) {
          val updatedUnit = state
            .units(event.orgItemId)
            .copy(
              parentId = event.newParentId,
              updatedAt = event.updatedAt,
              updatedBy = event.updatedBy
            )
          val newUnits    = state.units +
            (updatedNewParent.id -> updatedNewParent) +
            (updatedOldParent.id -> updatedOldParent) +
            (updatedUnit.id      -> updatedUnit)

          (newUnits, state.positions)

        } else {
          val updatedPosition = state
            .positions(event.orgItemId)
            .copy(
              parentId = event.newParentId,
              updatedAt = event.updatedAt,
              updatedBy = event.updatedBy
            )
          val newUnits        = state.units +
            (updatedNewParent.id -> updatedNewParent) +
            (updatedOldParent.id -> updatedOldParent)
          val newPositions = state.positions + (updatedPosition.id -> updatedPosition)
          (newUnits, newPositions)
        }
        state.copy(
          units = newUnits,
          positions = newPositions,
          updatedAt = event.updatedAt,
          updatedBy = event.updatedBy
        )
      }
    )

  def onItemOrderChanged(event: ItemOrderChanged): HierarchyEntity =
    HierarchyEntity(
      maybeState.map { state =>
        val parent                          = state.units(event.parentId)
        val children                        = parent.children.filter(_ != event.orgItemId)
        val updatedChildren: Seq[OrgItemId] = event.order match {
          case pos if pos >= 0 && pos < children.size =>
            (children.take(pos) :+ event.orgItemId) ++ children.drop(pos)
          case _                                      => children :+ event.orgItemId
        }
        val updatedParent                   = parent.copy(
          children = updatedChildren,
          updatedAt = event.updatedAt,
          updatedBy = event.updatedBy
        )
        state.copy(
          units = state.units + (updatedParent.id -> updatedParent),
          updatedAt = event.updatedAt,
          updatedBy = event.updatedBy
        )
      }
    )

}
