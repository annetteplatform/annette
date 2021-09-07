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

import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl.{EntityContext, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{EventSourcedBehavior, RetentionCriteria}
import biz.lobachev.annette.core.model.PersonId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.org_structure.api.category.{OrgCategory, OrgCategoryId}
import biz.lobachev.annette.org_structure.api.hierarchy.{CompositeOrgItemId, OrgItem, Organization, OrganizationTree}
import biz.lobachev.annette.org_structure.api.role.OrgRoleId
import com.lightbend.lagom.scaladsl.persistence._
import play.api.libs.json.{Format, Json}

import java.time.OffsetDateTime

object HierarchyEntity {

  trait CommandSerializable

  sealed trait Command extends CommandSerializable

  final case class CreateOrganization(
    orgId: CompositeOrgItemId,
    name: String,
    categoryId: OrgCategoryId,
    source: Option[String] = None,
    externalId: Option[String] = None,
    createdBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class CreateUnit(
    unitId: CompositeOrgItemId,
    parentId: CompositeOrgItemId,
    name: String,
    categoryId: OrgCategoryId,
    order: Option[Int] = None,
    source: Option[String] = None,
    externalId: Option[String] = None,
    createdBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class CreatePosition(
    positionId: CompositeOrgItemId,
    parentId: CompositeOrgItemId,
    name: String,
    limit: Int = 1,
    categoryId: OrgCategoryId,
    order: Option[Int] = None,
    source: Option[String] = None,
    externalId: Option[String] = None,
    createdBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class UpdateName(
    itemId: CompositeOrgItemId,
    name: String,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class AssignCategory(
    itemId: CompositeOrgItemId,
    categoryId: OrgCategoryId,
    updatedBy: AnnettePrincipal,
    category: OrgCategory,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class UpdateSource(
    itemId: CompositeOrgItemId,
    source: Option[String],
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class UpdateExternalId(
    itemId: CompositeOrgItemId,
    externalId: Option[String],
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class MoveItem(
    itemId: CompositeOrgItemId,
    newParentId: CompositeOrgItemId,
    order: Option[Int] = None,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class AssignChief(
    unitId: CompositeOrgItemId,
    chiefId: CompositeOrgItemId,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class UnassignChief(
    unitId: CompositeOrgItemId,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class ChangePositionLimit(
    positionId: CompositeOrgItemId,
    limit: Int,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class AssignPerson(
    positionId: CompositeOrgItemId,
    personId: PersonId,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class UnassignPerson(
    positionId: CompositeOrgItemId,
    personId: PersonId,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class AssignOrgRole(
    positionId: CompositeOrgItemId,
    orgRoleId: OrgRoleId,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class UnassignOrgRole(
    positionId: CompositeOrgItemId,
    orgRoleId: OrgRoleId,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class DeleteOrgItem(
    itemId: CompositeOrgItemId,
    deletedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class GetOrganization(orgId: CompositeOrgItemId, replyTo: ActorRef[Confirmation]) extends Command

  final case class GetOrganizationTree(
    itemId: CompositeOrgItemId,
    replyTo: ActorRef[Confirmation]
  ) extends Command

  final case class GetOrgItem(id: CompositeOrgItemId, replyTo: ActorRef[Confirmation]) extends Command

  final case class GetChildren(unitId: CompositeOrgItemId, replyTo: ActorRef[Confirmation]) extends Command

  final case class GetPersons(positionId: CompositeOrgItemId, replyTo: ActorRef[Confirmation]) extends Command

  final case class GetRoles(positionId: CompositeOrgItemId, replyTo: ActorRef[Confirmation]) extends Command

  final case class GetRootPaths(itemIds: Set[CompositeOrgItemId], replyTo: ActorRef[Confirmation]) extends Command

  sealed trait Confirmation
  final case class SuccessOrganization(organization: Organization)                               extends Confirmation
  final case class SuccessOrganizationTree(tree: OrganizationTree)                               extends Confirmation
  final case class SuccessOrgItem(orgItem: OrgItem)                                              extends Confirmation
  final case class SuccessChildren(children: Seq[CompositeOrgItemId])                            extends Confirmation
  final case class SuccessPersons(persons: Set[CompositeOrgItemId])                              extends Confirmation
  final case class SuccessRoles(roles: Set[OrgRoleId])                                           extends Confirmation
  final case class SuccessRootPaths(rootPaths: Map[CompositeOrgItemId, Seq[CompositeOrgItemId]]) extends Confirmation
  final case object Success                                                                      extends Confirmation
  final case object OrganizationAlreadyExist                                                     extends Confirmation
  final case object OrganizationNotFound                                                         extends Confirmation
  final case object OrganizationNotEmpty                                                         extends Confirmation
  final case object UnitNotEmpty                                                                 extends Confirmation
  final case object PositionNotEmpty                                                             extends Confirmation
  final case object ItemNotFound                                                                 extends Confirmation
  final case object AlreadyExist                                                                 extends Confirmation
  final case object ParentNotFound                                                               extends Confirmation
  final case object ChiefNotFound                                                                extends Confirmation
  final case object ChiefAlreadyAssigned                                                         extends Confirmation
  final case object ChiefNotAssigned                                                             extends Confirmation
  final case object PositionLimitExceeded                                                        extends Confirmation
  final case object PersonAlreadyAssigned                                                        extends Confirmation
  final case object PersonNotAssigned                                                            extends Confirmation
  final case object IncorrectOrder                                                               extends Confirmation
  final case object IncorrectMoveItemArguments                                                   extends Confirmation
  final case object IncorrectCategory                                                            extends Confirmation

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
    orgId: CompositeOrgItemId,
    name: String,
    source: Option[String],
    externalId: Option[String],
    categoryId: OrgCategoryId,
    createdBy: AnnettePrincipal,
    createdAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  final case class UnitCreated(
    parentId: CompositeOrgItemId,
    unitId: CompositeOrgItemId,
    name: String,
    order: Option[Int],
    rootPath: Seq[CompositeOrgItemId],
    categoryId: OrgCategoryId,
    source: Option[String],
    externalId: Option[String],
    createdBy: AnnettePrincipal,
    createdAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  final case class PositionCreated(
    parentId: CompositeOrgItemId,
    positionId: CompositeOrgItemId,
    name: String,
    order: Option[Int],
    rootPath: Seq[CompositeOrgItemId],
    limit: Int,
    categoryId: OrgCategoryId,
    source: Option[String],
    externalId: Option[String],
    createdBy: AnnettePrincipal,
    createdAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  final case class NameUpdated(
    itemId: CompositeOrgItemId,
    name: String,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  final case class CategoryAssigned(
    itemId: CompositeOrgItemId,
    categoryId: OrgCategoryId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  final case class SourceUpdated(
    itemId: CompositeOrgItemId,
    source: Option[String],
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  final case class ExternalIdUpdated(
    itemId: CompositeOrgItemId,
    externalId: Option[String],
    oldExternalId: Option[String],
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  final case class ItemMoved(
    itemId: CompositeOrgItemId,
    oldParentId: CompositeOrgItemId,
    newParentId: CompositeOrgItemId,
    order: Option[Int] = None,
    affectedItemIds: Set[CompositeOrgItemId],
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  final case class ChiefAssigned(
    unitId: CompositeOrgItemId,
    chiefId: CompositeOrgItemId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  final case class ChiefUnassigned(
    unitId: CompositeOrgItemId,
    chiefId: CompositeOrgItemId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  final case class PositionLimitChanged(
    positionId: CompositeOrgItemId,
    limit: Int,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  final case class PersonAssigned(
    positionId: CompositeOrgItemId,
    personId: CompositeOrgItemId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  final case class PersonUnassigned(
    positionId: CompositeOrgItemId,
    personId: PersonId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  final case class OrgRoleAssigned(
    positionId: CompositeOrgItemId,
    orgRoleId: OrgRoleId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  final case class OrgRoleUnassigned(
    positionId: CompositeOrgItemId,
    orgRoleId: OrgRoleId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  final case class OrganizationDeleted(
    orgId: CompositeOrgItemId,
    deletedBy: AnnettePrincipal,
    deletedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  final case class UnitDeleted(
    parentId: CompositeOrgItemId,
    unitId: CompositeOrgItemId,
    deletedBy: AnnettePrincipal,
    deletedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  final case class PositionDeleted(
    parentId: CompositeOrgItemId,
    positionId: CompositeOrgItemId,
    deletedBy: AnnettePrincipal,
    deletedAt: OffsetDateTime = OffsetDateTime.now
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
  implicit val sourceUpdatedFormat: Format[SourceUpdated]               = Json.format
  implicit val externalIdUpdatedFormat: Format[ExternalIdUpdated]       = Json.format
  implicit val positionLimitChangedFormat: Format[PositionLimitChanged] = Json.format
  implicit val personAssignedFormat: Format[PersonAssigned]             = Json.format
  implicit val personUnassignedFormat: Format[PersonUnassigned]         = Json.format
  implicit val orgRoleAssignedFormat: Format[OrgRoleAssigned]           = Json.format
  implicit val orgRoleUnassignedFormat: Format[OrgRoleUnassigned]       = Json.format
  implicit val itemMovedFormat: Format[ItemMoved]                       = Json.format
//  implicit val itemOrderChangedFormat: Format[ItemOrderChanged]         = Json.format

  val empty = EmptyHierarchy

  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Hierarchy")

  def apply(persistenceId: PersistenceId): EventSourcedBehavior[Command, Event, HierarchyState] =
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, HierarchyState](
        persistenceId = persistenceId,
        emptyState = HierarchyEntity.empty,
        commandHandler = (entity, cmd) => entity.applyCommand(cmd),
        eventHandler = (entity, evt) => entity.applyEvent(evt)
      )

  def apply(entityContext: EntityContext[Command]): Behavior[Command] =
    apply(PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId))
      .withTagger(AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2))
}
