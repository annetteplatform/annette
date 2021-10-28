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

package biz.lobachev.annette.org_structure.impl.hierarchy.entity
import akka.persistence.typed.scaladsl.{Effect, ReplyEffect}
import biz.lobachev.annette.core.model.PersonId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.org_structure.api.hierarchy._
import biz.lobachev.annette.org_structure.api.role.OrgRoleId
import biz.lobachev.annette.org_structure.impl.hierarchy.HierarchyMetadata
import io.scalaland.chimney.dsl._
import play.api.libs.json.{Format, Json, JsonConfiguration, JsonNaming}

import java.time.OffsetDateTime

sealed trait HierarchyState {
  import HierarchyEntity._

  def applyCommand(cmd: Command): ReplyEffect[Event, HierarchyState]
  def applyEvent(event: Event): HierarchyState
}

object EmptyHierarchy extends HierarchyState {
  import HierarchyEntity._

  override def applyCommand(cmd: Command): ReplyEffect[Event, HierarchyState] =
    cmd match {
      case cmd: CreateOrganization      => createOrganization(cmd)
      case cmd: CreateUnit              => Effect.reply(cmd.replyTo)(OrganizationNotFound)
      case cmd: CreatePosition          => Effect.reply(cmd.replyTo)(OrganizationNotFound)
      case cmd: UpdateName              => Effect.reply(cmd.replyTo)(OrganizationNotFound)
      case cmd: AssignCategory          => Effect.reply(cmd.replyTo)(OrganizationNotFound)
      case cmd: UpdateSource            => Effect.reply(cmd.replyTo)(OrganizationNotFound)
      case cmd: UpdateExternalId        => Effect.reply(cmd.replyTo)(OrganizationNotFound)
      case cmd: MoveItem                => Effect.reply(cmd.replyTo)(OrganizationNotFound)
      case cmd: AssignChief             => Effect.reply(cmd.replyTo)(OrganizationNotFound)
      case cmd: UnassignChief           => Effect.reply(cmd.replyTo)(OrganizationNotFound)
      case cmd: ChangePositionLimit     => Effect.reply(cmd.replyTo)(OrganizationNotFound)
      case cmd: AssignPerson            => Effect.reply(cmd.replyTo)(OrganizationNotFound)
      case cmd: UnassignPerson          => Effect.reply(cmd.replyTo)(OrganizationNotFound)
      case cmd: AssignOrgRole           => Effect.reply(cmd.replyTo)(OrganizationNotFound)
      case cmd: UnassignOrgRole         => Effect.reply(cmd.replyTo)(OrganizationNotFound)
      case cmd: DeleteOrgItem           => Effect.reply(cmd.replyTo)(OrganizationNotFound)
      case cmd: GetOrgItem              => Effect.reply(cmd.replyTo)(OrganizationNotFound)
      case cmd: GetOrganization         => Effect.reply(cmd.replyTo)(OrganizationNotFound)
      case cmd: GetOrganizationTree     => Effect.reply(cmd.replyTo)(OrganizationNotFound)
      case cmd: GetChildren             => Effect.reply(cmd.replyTo)(OrganizationNotFound)
      case cmd: GetPersons              => Effect.reply(cmd.replyTo)(OrganizationNotFound)
      case cmd: GetRoles                => Effect.reply(cmd.replyTo)(OrganizationNotFound)
      case cmd: GetRootPaths            => Effect.reply(cmd.replyTo)(OrganizationNotFound)
      case cmd: GetOrgItemAttributes    => Effect.reply(cmd.replyTo)(OrganizationNotFound)
      case cmd: UpdateOrgItemAttributes => Effect.reply(cmd.replyTo)(OrganizationNotFound)
    }

  def createOrganization(cmd: CreateOrganization): ReplyEffect[Event, HierarchyState] = {
    val event = cmd
      .into[OrganizationCreated]
      .transform
    Effect
      .persist(event)
      .thenReply(cmd.replyTo)(_ => Success)
  }

  override def applyEvent(event: Event): HierarchyState =
    event match {
      case event: OrganizationCreated => onOrganizationCreated(event)
      case _                          => this
    }

  def onOrganizationCreated(event: OrganizationCreated): HierarchyState =
    ActiveHierarchy(
      orgId = event.orgId,
      units = Map(
        event.orgId ->
          HierarchyUnit(
            id = event.orgId,
            parentId = ROOT,
            name = event.name,
            categoryId = event.categoryId,
            source = event.source,
            externalId = event.externalId,
            updatedAt = event.createdAt,
            updatedBy = event.createdBy
          )
      ),
      positions = Map.empty,
      chiefAssignments = Map.empty,
      personAssignments = Map.empty,
      orgRoleAssignments = Map.empty,
      updatedAt = event.createdAt,
      updatedBy = event.createdBy
    )

  implicit val format = Json.format[EmptyHierarchy.type]
}

final case class ActiveHierarchy(
  orgId: CompositeOrgItemId,
  units: Map[CompositeOrgItemId, HierarchyUnit],
  positions: Map[CompositeOrgItemId, HierarchyPosition],
  chiefAssignments: Map[CompositeOrgItemId, Set[CompositeOrgItemId]], // PositionId -> UnitId[]
  personAssignments: Map[PersonId, Set[CompositeOrgItemId]],          // PersonId -> PositionId[]
  orgRoleAssignments: Map[OrgRoleId, Set[CompositeOrgItemId]],        // OrgRoleId -> PositionId[]
  updatedAt: OffsetDateTime = OffsetDateTime.now(),
  updatedBy: AnnettePrincipal
) extends HierarchyState {

  import HierarchyEntity._

  override def applyCommand(cmd: Command): ReplyEffect[Event, HierarchyState] =
    cmd match {
      case cmd: CreateOrganization      => Effect.reply(cmd.replyTo)(OrganizationAlreadyExist)
      case cmd: CreateUnit              => createUnit(cmd)
      case cmd: CreatePosition          => createPosition(cmd)
      case cmd: UpdateName              => updateName(cmd)
      case cmd: AssignCategory          => assignCategory(cmd)
      case cmd: UpdateSource            => updateSource(cmd)
      case cmd: UpdateExternalId        => updateExternalId(cmd)
      case cmd: MoveItem                => moveItem(cmd)
      case cmd: UpdateOrgItemAttributes => updateOrgItemAttributes(cmd)
      case cmd: AssignChief             => assignChief(cmd)
      case cmd: UnassignChief           => unassignChief(cmd)
      case cmd: ChangePositionLimit     => changePositionLimit(cmd)
      case cmd: AssignPerson            => assignPerson(cmd)
      case cmd: UnassignPerson          => unassignPerson(cmd)
      case cmd: AssignOrgRole           => assignOrgRole(cmd)
      case cmd: UnassignOrgRole         => unassignOrgRole(cmd)
      case cmd: DeleteOrgItem           => deleteOrgItem(cmd)

      case cmd: GetOrgItem           => getOrgItem(cmd)
      case cmd: GetOrgItemAttributes => getOrgItemAttributes(cmd)
      case cmd: GetOrganization      => getOrganization(cmd)
      case cmd: GetOrganizationTree  => getOrganizationTree(cmd)

      case cmd: GetChildren  => getChildren(cmd)
      case cmd: GetPersons   => getPersons(cmd)
      case cmd: GetRoles     => getRoles(cmd)
      case cmd: GetRootPaths => getRootPaths(cmd)

    }

  def createUnit(cmd: CreateUnit): ReplyEffect[Event, HierarchyState] =
    if (hasItem(cmd.unitId)) Effect.reply(cmd.replyTo)(AlreadyExist)
    else if (!hasUnit(cmd.parentId)) Effect.reply(cmd.replyTo)(ParentNotFound)
    else {
      val parent                                  = units(cmd.parentId)
      val children                                = parent.children
      val parentChildren: Seq[CompositeOrgItemId] = cmd.order match {
        case Some(pos) if pos >= 0 && pos < children.size =>
          (children.take(pos) :+ cmd.unitId) ++ children.drop(pos)
        case _                                            => children :+ cmd.unitId
      }
      val event                                   = cmd
        .into[UnitCreated]
        .withFieldConst(_.rootPath, getRootPath(cmd.parentId) :+ cmd.unitId)
        .withFieldConst(_.parentChildren, parentChildren)
        .transform
      Effect
        .persist(event)
        .thenReply(cmd.replyTo)(_ => Success)
    }

  def createPosition(cmd: CreatePosition): ReplyEffect[Event, HierarchyState] =
    if (hasItem(cmd.positionId)) Effect.reply(cmd.replyTo)(AlreadyExist)
    else if (!hasUnit(cmd.parentId)) Effect.reply(cmd.replyTo)(ParentNotFound)
    else {
      val parent                                  = units(cmd.parentId)
      val children                                = parent.children
      val parentChildren: Seq[CompositeOrgItemId] = cmd.order match {
        case Some(pos) if pos >= 0 && pos < children.size =>
          (children.take(pos) :+ cmd.positionId) ++ children.drop(pos)
        case _                                            => children :+ cmd.positionId
      }
      val event                                   = cmd
        .into[PositionCreated]
        .withFieldConst(_.rootPath, getRootPath(cmd.parentId) :+ cmd.positionId)
        .withFieldConst(_.parentChildren, parentChildren)
        .transform
      Effect
        .persist(event)
        .thenReply(cmd.replyTo)(_ => Success)
    }

  def updateName(cmd: UpdateName): ReplyEffect[Event, HierarchyState] =
    if (hasItem(cmd.itemId)) {
      val item = positions.getOrElse(cmd.itemId, units(cmd.itemId))
      if (item.name != cmd.name) {
        val event = cmd
          .into[NameUpdated]
          .transform
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
      } else
        Effect.reply(cmd.replyTo)(Success)
    } else Effect.reply(cmd.replyTo)(ItemNotFound)

  def assignCategory(cmd: AssignCategory): ReplyEffect[Event, HierarchyState] =
    if (hasUnit(cmd.itemId)) {
      val unit = units(cmd.itemId)
      if (
        (unit.parentId == ROOT && cmd.category.forOrganization) ||
        (unit.parentId != ROOT && cmd.category.forUnit)
      ) {
        val event = cmd
          .into[CategoryAssigned]
          .transform
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
      } else
        Effect.reply(cmd.replyTo)(IncorrectCategory)
    } else if (hasPosition(cmd.itemId))
      if (cmd.category.forPosition) {
        val event = cmd
          .into[CategoryAssigned]
          .transform
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
      } else Effect.reply(cmd.replyTo)(IncorrectCategory)
    else Effect.reply(cmd.replyTo)(ItemNotFound)

  def updateSource(cmd: UpdateSource): ReplyEffect[Event, HierarchyState] =
    if (hasItem(cmd.itemId)) {
      val item = positions.getOrElse(cmd.itemId, units(cmd.itemId))
      if (item.source != cmd.source) {
        val event = cmd
          .into[SourceUpdated]
          .transform
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
      } else
        Effect.reply(cmd.replyTo)(Success)
    } else Effect.reply(cmd.replyTo)(ItemNotFound)

  def updateExternalId(cmd: UpdateExternalId): ReplyEffect[Event, HierarchyState] =
    if (hasItem(cmd.itemId)) {
      val item = positions.getOrElse(cmd.itemId, units(cmd.itemId))
      if (item.externalId != cmd.externalId) {
        val event = cmd
          .into[ExternalIdUpdated]
          .withFieldConst(_.oldExternalId, item.externalId)
          .transform
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
      } else
        Effect.reply(cmd.replyTo)(Success)
    } else Effect.reply(cmd.replyTo)(ItemNotFound)

  def moveItem(cmd: MoveItem): ReplyEffect[Event, HierarchyState] =
    if (hasPosition(cmd.newParentId)) Effect.reply(cmd.replyTo)(IncorrectMoveItemArguments)
    else if (hasItem(cmd.itemId)) {
      val oldParentId = units
        .get(cmd.itemId)
        .map(_.parentId)
        .getOrElse(positions.get(cmd.itemId).map(_.parentId).get)
      if (oldParentId == cmd.newParentId) changeItemOrder(cmd)
      else {
        val rootPath = getRootPath(cmd.newParentId)
        if (rootPath.contains(cmd.itemId) || cmd.itemId == cmd.newParentId)
          Effect.reply(cmd.replyTo)(IncorrectMoveItemArguments)
        else {
          val oldParent                = units(oldParentId)
          val oldParentChildren        = oldParent.children.filter(_ != cmd.itemId)
          val newParent                = units(cmd.newParentId)
          val newParentChildren        = newParent.children.filter(_ != cmd.itemId)
          val updatedNewParentChildren = cmd.order match {
            case Some(pos) if pos >= 0 && pos < newParentChildren.size =>
              (newParentChildren.take(pos) :+ cmd.itemId) ++ newParentChildren.drop(pos)
            case _                                                     => newParentChildren :+ cmd.itemId
          }
          val oldRootPath              = getRootPath(oldParentId)
          val dropNum                  = oldRootPath.length
          val newRootPath              = getRootPath(cmd.newParentId)
          val updateRootPathEvents     = (cmd.itemId +: getDescendants(cmd.itemId)).map { itemId =>
            val rootPath = getRootPath(itemId)
            RootPathUpdated(
              itemId,
              newRootPath ++ rootPath.drop(dropNum)
            )
          }
          val event                    = cmd
            .into[ItemMoved]
            .withFieldConst(_.oldParentId, oldParentId)
            .withFieldConst(_.oldParentChildren, oldParentChildren)
            .withFieldConst(_.newParentChildren, updatedNewParentChildren)
            .transform
          Effect
            .persist(event +: updateRootPathEvents)
            .thenReply(cmd.replyTo)(_ => Success)
        }
      }
    } else Effect.reply(cmd.replyTo)(ItemNotFound)

  def updateOrgItemAttributes(cmd: UpdateOrgItemAttributes): ReplyEffect[Event, HierarchyState] =
    if (hasItem(cmd.itemId)) {
      val event = cmd
        .into[OrgItemAttributesUpdated]
        .transform
      Effect
        .persist(event)
        .thenReply(cmd.replyTo)(_ => Success)
    } else Effect.reply(cmd.replyTo)(ItemNotFound)

  def changeItemOrder(cmd: MoveItem): ReplyEffect[Event, HierarchyState] =
    if (cmd.order.isEmpty) Effect.reply(cmd.replyTo)(IncorrectMoveItemArguments)
    else {
      val parent         = units(cmd.newParentId)
      val children       = parent.children.filter(_ != cmd.itemId)
      val parentChildren = cmd.order.get match {
        case pos if pos >= 0 && pos < children.size =>
          (children.take(pos) :+ cmd.itemId) ++ children.drop(pos)
        case _                                      => children :+ cmd.itemId
      }
      val event          = ItemOrderChanged(
        cmd.itemId,
        cmd.newParentId,
        cmd.order.get,
        parentChildren,
        cmd.updatedBy
      )
      Effect
        .persist(event)
        .thenReply(cmd.replyTo)(_ => Success)
    }

  def assignChief(cmd: AssignChief): ReplyEffect[Event, HierarchyState] =
    if (!hasUnit(cmd.unitId)) Effect.reply(cmd.replyTo)(ItemNotFound)
    else if (!hasPosition(cmd.chiefId)) Effect.reply(cmd.replyTo)(ChiefNotFound)
    else if (positions(cmd.chiefId).limit != 1) Effect.reply(cmd.replyTo)(PositionLimitExceeded)
    else if (units(cmd.unitId).chief.isEmpty) {
      val event = cmd
        .into[ChiefAssigned]
        .transform
      Effect
        .persist(event)
        .thenReply(cmd.replyTo)(_ => Success)
    } else
      Effect.reply(cmd.replyTo)(ChiefAlreadyAssigned)

  def unassignChief(cmd: UnassignChief): ReplyEffect[Event, HierarchyState] =
    if (hasUnit(cmd.unitId))
      units(cmd.unitId).chief.map { chief =>
        val event = cmd
          .into[ChiefUnassigned]
          .withFieldConst(_.chiefId, chief)
          .transform
        Effect
          .persist[Event, HierarchyState](event)
          .thenReply(cmd.replyTo)(_ => Success)
      }
        .getOrElse(
          Effect.reply(cmd.replyTo)(ChiefNotAssigned)
        )
    else
      Effect.reply(cmd.replyTo)(ItemNotFound)

  def changePositionLimit(cmd: ChangePositionLimit): ReplyEffect[Event, HierarchyState] =
    if (!hasPosition(cmd.positionId)) Effect.reply(cmd.replyTo)(ItemNotFound)
    else if (chiefAssignments.isDefinedAt(cmd.positionId)) Effect.reply(cmd.replyTo)(ChiefAlreadyAssigned)
    else if (positions(cmd.positionId).persons.size > cmd.limit) Effect.reply(cmd.replyTo)(PositionLimitExceeded)
    else if (cmd.limit <= 0) Effect.reply(cmd.replyTo)(PositionLimitExceeded)
    else {
      val event = cmd
        .into[PositionLimitChanged]
        .transform
      Effect
        .persist(event)
        .thenReply(cmd.replyTo)(_ => Success)
    }

  def assignPerson(cmd: AssignPerson): ReplyEffect[Event, HierarchyState] =
    if (!hasPosition(cmd.positionId)) Effect.reply(cmd.replyTo)(ItemNotFound)
    else if (positions(cmd.positionId).persons.contains(cmd.personId)) Effect.reply(cmd.replyTo)(PersonAlreadyAssigned)
    else if (positions(cmd.positionId).limit == positions(cmd.positionId).persons.size)
      Effect.reply(cmd.replyTo)(PositionLimitExceeded)
    else {
      val event = cmd
        .into[PersonAssigned]
        .withFieldComputed(_.persons, positions(cmd.positionId).persons + _.personId)
        .transform
      Effect
        .persist(event)
        .thenReply(cmd.replyTo)(_ => Success)
    }

  def unassignPerson(cmd: UnassignPerson): ReplyEffect[Event, HierarchyState] =
    if (!hasPosition(cmd.positionId)) Effect.reply(cmd.replyTo)(ItemNotFound)
    else if (positions(cmd.positionId).persons.contains(cmd.personId)) {
      val event = cmd
        .into[PersonUnassigned]
        .withFieldComputed(_.persons, positions(cmd.positionId).persons - _.personId)
        .transform
      Effect
        .persist(event)
        .thenReply(cmd.replyTo)(_ => Success)
    } else
      Effect.reply(cmd.replyTo)(PersonNotAssigned)

  def assignOrgRole(cmd: AssignOrgRole): ReplyEffect[Event, HierarchyState] =
    if (!hasPosition(cmd.positionId)) Effect.reply(cmd.replyTo)(ItemNotFound)
    else {
      val event = cmd
        .into[OrgRoleAssigned]
        .withFieldComputed(_.orgRoles, positions(cmd.positionId).orgRoles + _.orgRoleId)
        .transform
      Effect
        .persist(event)
        .thenReply(cmd.replyTo)(_ => Success)
    }

  def unassignOrgRole(cmd: UnassignOrgRole): ReplyEffect[Event, HierarchyState] =
    if (!hasPosition(cmd.positionId)) Effect.reply(cmd.replyTo)(ItemNotFound)
    else if (positions(cmd.positionId).orgRoles.contains(cmd.orgRoleId)) {
      val event = cmd
        .into[OrgRoleUnassigned]
        .withFieldComputed(_.orgRoles, positions(cmd.positionId).orgRoles - _.orgRoleId)
        .transform
      Effect
        .persist(event)
        .thenReply(cmd.replyTo)(_ => Success)
    } else
      Effect.reply(cmd.replyTo)(Success)

  def deleteOrgItem(cmd: DeleteOrgItem): ReplyEffect[Event, HierarchyState] =
    if (OrgItemKey.isOrg(cmd.itemId)) // organization
      if (units.size == 1 && hasUnit(orgId) && positions.isEmpty) {
        val event = cmd
          .into[OrganizationDeleted]
          .withFieldConst(_.orgId, cmd.itemId)
          .transform
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
      } else
        Effect.reply(cmd.replyTo)(OrganizationNotEmpty)
    else if (hasUnit(cmd.itemId)) { // unit
      val unit = units(cmd.itemId)
      if (unit.children.nonEmpty) Effect.reply(cmd.replyTo)(UnitNotEmpty)
      else if (unit.chief.nonEmpty) Effect.reply(cmd.replyTo)(ChiefAlreadyAssigned)
      else {
        val parent         = units(unit.parentId)
        val parentChildren = parent.children.filter(_ != cmd.itemId)
        val event          = cmd
          .into[UnitDeleted]
          .withFieldConst(_.unitId, cmd.itemId)
          .withFieldConst(_.parentId, unit.parentId)
          .withFieldConst(_.parentChildren, parentChildren)
          .transform
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
      }
    } else if (hasPosition(cmd.itemId)) { // position
      val position = positions(cmd.itemId)
      if (chiefAssignments.isDefinedAt(cmd.itemId))
        Effect.reply(cmd.replyTo)(ChiefAlreadyAssigned)
      else if (position.persons.isEmpty) {
        val parent         = units(position.parentId)
        val parentChildren = parent.children.filter(_ != cmd.itemId)
        val event          = cmd
          .into[PositionDeleted]
          .withFieldConst(_.positionId, cmd.itemId)
          .withFieldConst(_.parentId, position.parentId)
          .withFieldConst(_.parentChildren, parentChildren)
          .transform
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
      } else
        Effect.reply(cmd.replyTo)(PositionNotEmpty)
    } else Effect.reply(cmd.replyTo)(ItemNotFound)

  def getOrganization(cmd: GetOrganization): ReplyEffect[Event, HierarchyState] = {
    val organization = this
      .into[Organization]
      .withFieldConst(_.orgId, cmd.orgId)
      .transform

    Effect.reply(cmd.replyTo)(SuccessOrganization(organization))
  }

  def getOrganizationTree(cmd: GetOrganizationTree): ReplyEffect[Event, HierarchyState] =
    if (hasUnit(cmd.itemId)) {
      val organizationTree = OrganizationTree(orgId, getOrgTreeItem(cmd.itemId))
      Effect.reply(cmd.replyTo)(SuccessOrganizationTree(organizationTree))
    } else Effect.reply(cmd.replyTo)(ItemNotFound)

  def getOrgItem(cmd: GetOrgItem): ReplyEffect[Event, HierarchyState] =
    if (hasPosition(cmd.id)) {
      val position   = positions(cmd.id)
      val rootPath   = getRootPath(cmd.id)
      val attributes = position.attributes.filter { case attribute -> _ => cmd.withAttributes.contains(attribute) }
      val orgItem    = position
        .into[OrgPosition]
        .withFieldConst(_.orgId, orgId)
        .withFieldConst(_.rootPath, rootPath)
        .withFieldConst(_.level, rootPath.length - 1)
        .withFieldConst(_.attributes, attributes)
        .transform
      Effect.reply(cmd.replyTo)(SuccessOrgItem(orgItem))
    } else if (hasUnit(cmd.id)) {
      val unit       = units(cmd.id)
      val rootPath   = getRootPath(cmd.id)
      val attributes = unit.attributes.filter { case attribute -> _ => cmd.withAttributes.contains(attribute) }
      val orgItem    = unit
        .into[OrgUnit]
        .withFieldConst(_.orgId, orgId)
        .withFieldConst(_.rootPath, rootPath)
        .withFieldConst(_.level, rootPath.length - 1)
        .withFieldConst(_.attributes, attributes)
        .transform
      Effect.reply(cmd.replyTo)(SuccessOrgItem(orgItem))
    } else Effect.reply(cmd.replyTo)(ItemNotFound)

  def getOrgItemAttributes(cmd: GetOrgItemAttributes): ReplyEffect[Event, HierarchyState] =
    if (hasPosition(cmd.id)) {
      val position   = positions(cmd.id)
      val attributes = position.attributes.filter { case attribute -> _ => cmd.withAttributes.contains(attribute) }
      Effect.reply(cmd.replyTo)(SuccessAttributes(attributes))
    } else if (hasUnit(cmd.id)) {
      val unit       = units(cmd.id)
      val attributes = unit.attributes.filter { case attribute -> _ => cmd.withAttributes.contains(attribute) }
      Effect.reply(cmd.replyTo)(SuccessAttributes(attributes))
    } else Effect.reply(cmd.replyTo)(ItemNotFound)

  def getChildren(cmd: GetChildren): ReplyEffect[Event, HierarchyState] =
    if (hasUnit(cmd.unitId))
      Effect.reply(cmd.replyTo)(SuccessChildren(units(cmd.unitId).children))
    else Effect.reply(cmd.replyTo)(ItemNotFound)

  def getPersons(cmd: GetPersons): ReplyEffect[Event, HierarchyState] =
    if (hasPosition(cmd.positionId))
      Effect.reply(cmd.replyTo)(SuccessPersons(positions(cmd.positionId).persons))
    else Effect.reply(cmd.replyTo)(ItemNotFound)

  def getRoles(cmd: GetRoles): ReplyEffect[Event, HierarchyState] =
    if (hasPosition(cmd.positionId))
      Effect.reply(cmd.replyTo)(SuccessRoles(positions(cmd.positionId).orgRoles))
    else Effect.reply(cmd.replyTo)(ItemNotFound)

  def getRootPaths(cmd: GetRootPaths): ReplyEffect[Event, HierarchyState] = {
    val rootPaths = cmd.itemIds.flatMap { itemId =>
      val rootPath = getRootPath(itemId)
      if (rootPath.nonEmpty)
        Some(itemId -> rootPath)
      else
        None
    }.toMap
    Effect.reply(cmd.replyTo)(SuccessRootPaths(rootPaths))
  }

  override def applyEvent(event: Event): HierarchyState =
    event match {
      case _: OrganizationCreated          => this
      case event: UnitCreated              => onUnitCreated(event)
      case event: PositionCreated          => onPositionCreated(event)
      case event: NameUpdated              => onNameUpdated(event)
      case event: CategoryAssigned         => onCategoryAssigned(event)
      case event: SourceUpdated            => onSourceUpdated(event)
      case event: ExternalIdUpdated        => onExternalIdUpdated(event)
      case event: ItemMoved                => onItemMoved(event)
      case event: ItemOrderChanged         => onItemOrderChanged(event)
      case event: OrgItemAttributesUpdated => onOrgItemAttributesUpdated(event)
      case _: RootPathUpdated              => this
      case event: ChiefAssigned            => onChiefAssigned(event)
      case event: ChiefUnassigned          => onChiefUnassigned(event)
      case event: PositionLimitChanged     => onPositionLimitChanged(event)
      case event: PersonAssigned           => onPersonAssigned(event)
      case event: PersonUnassigned         => onPersonUnassigned(event)
      case event: OrgRoleAssigned          => onOrgRolesAssigned(event)
      case event: OrgRoleUnassigned        => onOrgRolesUnassigned(event)
      case _: OrganizationDeleted          => EmptyHierarchy
      case event: UnitDeleted              => onUnitDeleted(event)
      case event: PositionDeleted          => onPositionDeleted(event)
    }

  def onUnitCreated(event: UnitCreated): HierarchyState = {
    val newUnit       = HierarchyUnit(
      id = event.unitId,
      parentId = event.parentId,
      name = event.name,
      categoryId = event.categoryId,
      source = event.source,
      externalId = event.externalId,
      attributes = event.attributes.getOrElse(Map.empty),
      updatedAt = event.createdAt,
      updatedBy = event.createdBy
    )
    val parent        = units(event.parentId)
    val updatedParent = parent.copy(
      children = event.parentChildren,
      updatedAt = event.createdAt,
      updatedBy = event.createdBy
    )

    copy(
      units = units + (updatedParent.id -> updatedParent) + (newUnit.id -> newUnit),
      updatedAt = event.createdAt,
      updatedBy = event.createdBy
    )
  }

  def onUnitDeleted(event: UnitDeleted): HierarchyState = {
    val parentId      = units(event.unitId).parentId
    val parent        = units(parentId)
    val updatedParent = parent.copy(
      children = event.parentChildren,
      updatedAt = event.deletedAt,
      updatedBy = event.deletedBy
    )
    copy(
      units = units + (updatedParent.id -> updatedParent) - event.unitId,
      updatedAt = event.deletedAt,
      updatedBy = event.deletedBy
    )
  }

  def onCategoryAssigned(event: CategoryAssigned): HierarchyState = {
    val updatedUnit     = units
      .get(event.itemId)
      .map(
        _.copy(
          categoryId = event.categoryId,
          updatedAt = event.updatedAt,
          updatedBy = event.updatedBy
        )
      )
    val updatedPosition = positions
      .get(event.itemId)
      .map(
        _.copy(
          categoryId = event.categoryId,
          updatedAt = event.updatedAt,
          updatedBy = event.updatedBy
        )
      )
    copy(
      units = updatedUnit.map(unit => units + (unit.id -> unit)).getOrElse(units),
      positions = updatedPosition.map(position => positions + (position.id -> position)).getOrElse(positions),
      updatedAt = event.updatedAt,
      updatedBy = event.updatedBy
    )
  }

  def onChiefAssigned(event: ChiefAssigned): HierarchyState = {
    val updatedUnit        = units(event.unitId)
      .copy(
        chief = Some(event.chiefId),
        updatedAt = event.updatedAt,
        updatedBy = event.updatedBy
      )
    val chiefAssignmentSet = chiefAssignments.getOrElse(event.chiefId, Set.empty) + event.unitId
    copy(
      units = units + (updatedUnit.id                      -> updatedUnit),
      chiefAssignments = chiefAssignments + (event.chiefId -> chiefAssignmentSet),
      updatedAt = event.updatedAt,
      updatedBy = event.updatedBy
    )
  }

  def onChiefUnassigned(event: ChiefUnassigned): HierarchyState = {
    val unit                    = units(event.unitId)
    val chiefId                 = unit.chief.get
    val updatedUnit             = unit.copy(
      chief = None,
      updatedAt = event.updatedAt,
      updatedBy = event.updatedBy
    )
    val chiefAssignmentSet      = chiefAssignments.getOrElse(chiefId, Set.empty) - event.unitId
    val updatedChiefAssignments =
      if (chiefAssignmentSet.isEmpty)
        chiefAssignments - chiefId
      else
        chiefAssignments + (chiefId -> chiefAssignmentSet)

    copy(
      units = units + (updatedUnit.id -> updatedUnit),
      chiefAssignments = updatedChiefAssignments,
      updatedAt = event.updatedAt,
      updatedBy = event.updatedBy
    )
  }

  def onPositionCreated(event: PositionCreated): HierarchyState = {
    val newPosition   = HierarchyPosition(
      id = event.positionId,
      parentId = event.parentId,
      name = event.name,
      limit = event.limit,
      categoryId = event.categoryId,
      source = event.source,
      externalId = event.externalId,
      attributes = event.attributes.getOrElse(Map.empty),
      updatedAt = event.createdAt,
      updatedBy = event.createdBy
    )
    val parent        = units(event.parentId)
    val updatedParent = parent.copy(
      children = event.parentChildren,
      updatedAt = event.createdAt,
      updatedBy = event.createdBy
    )

    copy(
      units = units + (updatedParent.id       -> updatedParent),
      positions = positions + (newPosition.id -> newPosition),
      updatedAt = event.createdAt,
      updatedBy = event.createdBy
    )
  }

  def onPositionDeleted(event: PositionDeleted): HierarchyState = {
    val parentId      = positions(event.positionId).parentId
    val parent        = units(parentId)
    val updatedParent = parent.copy(
      children = event.parentChildren,
      updatedAt = event.deletedAt,
      updatedBy = event.deletedBy
    )
    copy(
      units = units + (updatedParent.id -> updatedParent),
      positions = positions - event.positionId,
      updatedAt = event.deletedAt,
      updatedBy = event.deletedBy
    )
  }

  def onNameUpdated(event: NameUpdated): HierarchyState =
    positions
      .get(event.itemId)
      .map { position =>
        val updatedPosition = position.copy(
          name = event.name,
          updatedAt = event.updatedAt,
          updatedBy = event.updatedBy
        )
        copy(
          positions = positions + (updatedPosition.id -> updatedPosition),
          updatedAt = event.updatedAt,
          updatedBy = event.updatedBy
        )
      }
      .getOrElse {
        units
          .get(event.itemId)
          .map { unit =>
            val updatedUnit = unit.copy(
              name = event.name,
              updatedAt = event.updatedAt,
              updatedBy = event.updatedBy
            )
            copy(
              units = units + (updatedUnit.id -> updatedUnit),
              updatedAt = event.updatedAt,
              updatedBy = event.updatedBy
            )
          }
          .getOrElse(this)
      }

  def onSourceUpdated(event: SourceUpdated): HierarchyState =
    positions
      .get(event.itemId)
      .map { position =>
        val updatedPosition = position.copy(
          source = event.source,
          updatedAt = event.updatedAt,
          updatedBy = event.updatedBy
        )
        copy(
          positions = positions + (updatedPosition.id -> updatedPosition),
          updatedAt = event.updatedAt,
          updatedBy = event.updatedBy
        )
      }
      .getOrElse {
        units
          .get(event.itemId)
          .map { unit =>
            val updatedUnit = unit.copy(
              source = event.source,
              updatedAt = event.updatedAt,
              updatedBy = event.updatedBy
            )
            copy(
              units = units + (updatedUnit.id -> updatedUnit),
              updatedAt = event.updatedAt,
              updatedBy = event.updatedBy
            )
          }
          .getOrElse(this)
      }

  def onExternalIdUpdated(event: ExternalIdUpdated): HierarchyState =
    positions
      .get(event.itemId)
      .map { position =>
        val updatedPosition = position.copy(
          externalId = event.externalId,
          updatedAt = event.updatedAt,
          updatedBy = event.updatedBy
        )
        copy(
          positions = positions + (updatedPosition.id -> updatedPosition),
          updatedAt = event.updatedAt,
          updatedBy = event.updatedBy
        )
      }
      .getOrElse {
        units
          .get(event.itemId)
          .map { unit =>
            val updatedUnit = unit.copy(
              externalId = event.externalId,
              updatedAt = event.updatedAt,
              updatedBy = event.updatedBy
            )
            copy(
              units = units + (updatedUnit.id -> updatedUnit),
              updatedAt = event.updatedAt,
              updatedBy = event.updatedBy
            )
          }
          .getOrElse(this)
      }

  def onPositionLimitChanged(event: PositionLimitChanged): HierarchyState = {
    val position        = positions(event.positionId)
    val updatedPosition = position.copy(
      limit = event.limit,
      updatedAt = event.updatedAt,
      updatedBy = event.updatedBy
    )
    copy(
      positions = positions + (updatedPosition.id -> updatedPosition),
      updatedAt = event.updatedAt,
      updatedBy = event.updatedBy
    )
  }

  def onPersonAssigned(event: PersonAssigned): HierarchyState = {
    val position            = positions(event.positionId)
    val updatedPosition     = position.copy(
      persons = event.persons,
      updatedAt = event.updatedAt,
      updatedBy = event.updatedBy
    )
    val personAssignmentSet = personAssignments.getOrElse(event.personId, Set.empty) + event.positionId
    copy(
      positions = positions + (updatedPosition.id             -> updatedPosition),
      personAssignments = personAssignments + (event.personId -> personAssignmentSet),
      updatedAt = event.updatedAt,
      updatedBy = event.updatedBy
    )
  }

  def onPersonUnassigned(event: PersonUnassigned): HierarchyState = {
    val position                 = positions(event.positionId)
    val personId                 = event.personId
    val updatedPosition          = position.copy(
      persons = event.persons,
      updatedAt = event.updatedAt,
      updatedBy = event.updatedBy
    )
    val personAssignmentSet      = orgRoleAssignments.getOrElse(personId, Set.empty) - event.positionId
    val updatedPersonAssignments =
      if (personAssignmentSet.isEmpty)
        personAssignments - personId
      else
        personAssignments + (personId -> personAssignmentSet)

    copy(
      positions = positions + (updatedPosition.id -> updatedPosition),
      personAssignments = updatedPersonAssignments,
      updatedAt = event.updatedAt,
      updatedBy = event.updatedBy
    )
  }

  def onOrgRolesAssigned(event: OrgRoleAssigned): HierarchyState = {
    val position             = positions(event.positionId)
    val updatedPosition      = position.copy(
      orgRoles = event.orgRoles,
      updatedAt = event.updatedAt,
      updatedBy = event.updatedBy
    )
    val orgRoleAssignmentSet = orgRoleAssignments.getOrElse(event.orgRoleId, Set.empty) + event.positionId
    copy(
      positions = positions + (updatedPosition.id                -> updatedPosition),
      orgRoleAssignments = orgRoleAssignments + (event.orgRoleId -> orgRoleAssignmentSet),
      updatedAt = event.updatedAt,
      updatedBy = event.updatedBy
    )
  }

  def onOrgRolesUnassigned(event: OrgRoleUnassigned): HierarchyState = {
    val position                  = positions(event.positionId)
    val orgRoleId                 = event.orgRoleId
    val updatedPosition           = position.copy(
      orgRoles = event.orgRoles,
      updatedAt = event.updatedAt,
      updatedBy = event.updatedBy
    )
    val orgRoleAssignmentSet      = orgRoleAssignments.getOrElse(orgRoleId, Set.empty) - event.positionId
    val updatedOrgRoleAssignments =
      if (orgRoleAssignmentSet.isEmpty)
        orgRoleAssignments - orgRoleId
      else
        orgRoleAssignments + (orgRoleId -> orgRoleAssignmentSet)
    copy(
      positions = positions + (updatedPosition.id -> updatedPosition),
      orgRoleAssignments = updatedOrgRoleAssignments,
      updatedAt = event.updatedAt,
      updatedBy = event.updatedBy
    )
  }

  def onItemMoved(event: ItemMoved): HierarchyState = {
    val oldParent        = units(event.oldParentId)
    val updatedOldParent = oldParent.copy(
      children = event.oldParentChildren,
      updatedAt = event.updatedAt,
      updatedBy = event.updatedBy
    )
    val newParent        = units(event.newParentId)

    val updatedNewParent         = newParent.copy(
      children = event.newParentChildren,
      updatedAt = event.updatedAt,
      updatedBy = event.updatedBy
    )
    val (newUnits, newPositions) = if (hasUnit(event.itemId)) {
      val updatedUnit = units(event.itemId)
        .copy(
          parentId = event.newParentId,
          updatedAt = event.updatedAt,
          updatedBy = event.updatedBy
        )
      val newUnits    = units +
        (updatedOldParent.id -> updatedOldParent) +
        (updatedNewParent.id -> updatedNewParent) +
        (updatedUnit.id      -> updatedUnit)

      (newUnits, positions)

    } else {
      val updatedPosition = positions(event.itemId)
        .copy(
          parentId = event.newParentId,
          updatedAt = event.updatedAt,
          updatedBy = event.updatedBy
        )
      val newUnits        = units +
        (updatedOldParent.id -> updatedOldParent) +
        (updatedNewParent.id -> updatedNewParent)
      val newPositions = positions + (updatedPosition.id -> updatedPosition)
      (newUnits, newPositions)
    }
    copy(
      units = newUnits,
      positions = newPositions,
      updatedAt = event.updatedAt,
      updatedBy = event.updatedBy
    )
  }

  def onItemOrderChanged(event: ItemOrderChanged): HierarchyState = {
    val parent        = units(event.parentId)
    val updatedParent = parent.copy(
      children = event.parentChildren,
      updatedAt = event.updatedAt,
      updatedBy = event.updatedBy
    )
    copy(
      units = units + (updatedParent.id -> updatedParent),
      updatedAt = event.updatedAt,
      updatedBy = event.updatedBy
    )
  }

  def onOrgItemAttributesUpdated(event: OrgItemAttributesUpdated): HierarchyState = {
    val removedAttributes =
      event.attributes.filter { case _ -> value => value.isEmpty }.keys.toSet
    val updatedAttributes = event.attributes.filter {
      case attribute -> value =>
        value.nonEmpty &&
          HierarchyMetadata.metadata.get(attribute).map(!_.readSidePersistence).getOrElse(false)
    }
    positions
      .get(event.itemId)
      .map { position =>
        val updatedPosition = position.copy(
          attributes = position.attributes -- removedAttributes ++ updatedAttributes,
          updatedAt = event.updatedAt,
          updatedBy = event.updatedBy
        )
        copy(
          positions = positions + (updatedPosition.id -> updatedPosition),
          updatedAt = event.updatedAt,
          updatedBy = event.updatedBy
        )
      }
      .getOrElse {
        units
          .get(event.itemId)
          .map { unit =>
            val updatedUnit = unit.copy(
              attributes = unit.attributes -- removedAttributes ++ updatedAttributes,
              updatedAt = event.updatedAt,
              updatedBy = event.updatedBy
            )
            copy(
              units = units + (updatedUnit.id -> updatedUnit),
              updatedAt = event.updatedAt,
              updatedBy = event.updatedBy
            )
          }
          .getOrElse(this)
      }
  }

  // *****************************************************

  private def hasItem(itemId: CompositeOrgItemId) = positions.isDefinedAt(itemId) || units.isDefinedAt(itemId)

  private def hasPosition(itemId: CompositeOrgItemId) = positions.isDefinedAt(itemId)

  private def hasUnit(itemId: CompositeOrgItemId) = units.isDefinedAt(itemId)

  private def getOrgTreeItem(itemId: CompositeOrgItemId): OrgTreeItem =
    units
      .get(itemId)
      .map(unit =>
        unit
          .into[UnitTreeItem]
          .withFieldConst(_.children, unit.children.map(getOrgTreeItem))
          .transform
      )
      .getOrElse {
        positions
          .get(itemId)
          .map(_.transformInto[PositionTreeItem])
          .get
      }

  private def getRootPath(itemId: CompositeOrgItemId): Seq[CompositeOrgItemId] = {
    val maybeParentId = units.get(itemId).map(u => Some(u.parentId)).getOrElse(positions.get(itemId).map(_.parentId))
    maybeParentId match {
      case None                      => Seq()
      case Some(_) if itemId == ROOT => Seq()
      case Some(parentId)            =>
        getRootPath(parentId) :+ itemId
    }
  }

  private def getDescendants(itemId: CompositeOrgItemId): Seq[CompositeOrgItemId] = {
    val children    = units.get(itemId).map(u => u.children).getOrElse(Seq.empty)
    val descendants = for {
      childId <- children
      descId  <- getDescendants(childId)
    } yield descId
    children ++ descendants
  }

}

object ActiveHierarchy {
  implicit val format = Json.format[ActiveHierarchy]
}

object HierarchyState {
  implicit val config                         = JsonConfiguration(
    discriminator = "type",
    typeNaming = JsonNaming { fullName =>
      fullName.split("\\.").toSeq.last
    }
  )
  implicit val format: Format[HierarchyState] = Json.format
}
