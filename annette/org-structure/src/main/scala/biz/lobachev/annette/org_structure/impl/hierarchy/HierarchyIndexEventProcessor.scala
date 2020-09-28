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

import biz.lobachev.annette.org_structure.api.hierarchy.OrgItemId
import biz.lobachev.annette.org_structure.impl.hierarchy.dao.HierarchyIndexDao
import com.datastax.driver.core.BoundStatement
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}

import scala.concurrent.{ExecutionContext, Future}

private[impl] class HierarchyIndexEventProcessor(
  readSide: CassandraReadSide,
  indexDao: HierarchyIndexDao,
  hierarchyEntityService: HierarchyEntityService
)(implicit
  ec: ExecutionContext
) extends ReadSideProcessor[HierarchyEntity.Event] {

  def buildHandler(): ReadSideProcessor.ReadSideHandler[HierarchyEntity.Event] =
    readSide
      .builder[HierarchyEntity.Event]("OrgStructure_Hierarchy_ElasticEventOffset")
      .setGlobalPrepare(indexDao.createEntityIndex)
      .setEventHandler[HierarchyEntity.OrganizationCreated](e => createOrganization(e.event))
      .setEventHandler[HierarchyEntity.OrganizationDeleted](e => deleteOrganization(e.event))
      .setEventHandler[HierarchyEntity.NameUpdated](e => updateName(e.event))
      .setEventHandler[HierarchyEntity.ShortNameUpdated](e => updateShortName(e.event))
      .setEventHandler[HierarchyEntity.UnitCreated](e => createUnit(e.event))
      .setEventHandler[HierarchyEntity.UnitDeleted](e => deleteUnit(e.event))
      .setEventHandler[HierarchyEntity.PositionCreated](e => createPosition(e.event))
      .setEventHandler[HierarchyEntity.PositionDeleted](e => deletePosition(e.event))
      .setEventHandler[HierarchyEntity.ChiefAssigned](e => assignChief(e.event))
      .setEventHandler[HierarchyEntity.ChiefUnassigned](e => unassignChief(e.event))
      .setEventHandler[HierarchyEntity.PositionLimitChanged](e => changePositionLimit(e.event))
      .setEventHandler[HierarchyEntity.PersonAssigned](e => assignPerson(e.event))
      .setEventHandler[HierarchyEntity.PersonUnassigned](e => unassignPerson(e.event))
      .setEventHandler[HierarchyEntity.OrgRoleAssigned](e =>
        updateOrgRoles(e.event.orgId, e.event.positionId, e.event.updatedAt)
      )
      .setEventHandler[HierarchyEntity.OrgRoleUnassigned](e =>
        updateOrgRoles(e.event.orgId, e.event.positionId, e.event.updatedAt)
      )
      .setEventHandler[HierarchyEntity.ItemOrderChanged](e => changeItemOrder(e.event))
      .setEventHandler[HierarchyEntity.ItemMoved](e => moveItem(e.event))
      .build()

  def aggregateTags: Set[AggregateEventTag[HierarchyEntity.Event]] = HierarchyEntity.Event.Tag.allTags

  def createOrganization(event: HierarchyEntity.OrganizationCreated): Future[Seq[BoundStatement]] =
    for {
      _ <- indexDao.createOrganization(event)
    } yield List.empty

  def deleteOrganization(event: HierarchyEntity.OrganizationDeleted): Future[Seq[BoundStatement]] =
    for {
      _ <- indexDao.deleteOrganization(event)
    } yield List.empty

  def createUnit(event: HierarchyEntity.UnitCreated): Future[Seq[BoundStatement]] =
    for {
      children <- hierarchyEntityService.getChildren(event.orgId, event.parentId)
      _        <- indexDao.updateChildren(event.parentId, children, event.createdAt)
      _        <- indexDao.createUnit(event)
    } yield List.empty

  def deleteUnit(event: HierarchyEntity.UnitDeleted): Future[Seq[BoundStatement]] =
    for {
      children <- hierarchyEntityService.getChildren(event.orgId, event.parentId)
      _        <- indexDao.updateChildren(event.parentId, children, event.deletedAt)
      _        <- indexDao.deleteUnit(event)
    } yield List.empty

  def assignChief(event: HierarchyEntity.ChiefAssigned): Future[Seq[BoundStatement]] =
    for {
      _ <- indexDao.assignChief(event)
    } yield List.empty

  def unassignChief(event: HierarchyEntity.ChiefUnassigned): Future[Seq[BoundStatement]] =
    for {
      _ <- indexDao.unassignChief(event)
    } yield List.empty

  def createPosition(event: HierarchyEntity.PositionCreated): Future[Seq[BoundStatement]] =
    for {
      children <- hierarchyEntityService.getChildren(event.orgId, event.parentId)
      _        <- indexDao.updateChildren(event.parentId, children, event.createdAt)
      _        <- indexDao.createPosition(event)
    } yield List.empty

  def deletePosition(event: HierarchyEntity.PositionDeleted): Future[Seq[BoundStatement]] =
    for {
      children <- hierarchyEntityService.getChildren(event.orgId, event.parentId)
      _        <- indexDao.updateChildren(event.parentId, children, event.deletedAt)
      _        <- indexDao.deletePosition(event)
    } yield List.empty

  def updateName(event: HierarchyEntity.NameUpdated): Future[Seq[BoundStatement]] =
    for {
      _ <- indexDao.updateName(event)
    } yield List.empty

  def updateShortName(event: HierarchyEntity.ShortNameUpdated): Future[Seq[BoundStatement]] =
    for {
      _ <- indexDao.updateShortName(event)
    } yield List.empty

  def changePositionLimit(event: HierarchyEntity.PositionLimitChanged): Future[Seq[BoundStatement]] =
    for {
      _ <- indexDao.changePositionLimit(event)
    } yield List.empty

  def assignPerson(event: HierarchyEntity.PersonAssigned): Future[Seq[BoundStatement]] =
    for {
      persons <- hierarchyEntityService.getPersons(event.orgId, event.positionId)
      _       <- indexDao.updatePersons(event.positionId, persons, event.updatedAt)
    } yield List.empty

  def unassignPerson(event: HierarchyEntity.PersonUnassigned): Future[Seq[BoundStatement]] =
    for {
      persons <- hierarchyEntityService.getPersons(event.orgId, event.positionId)
      _       <- indexDao.updatePersons(event.positionId, persons, event.updatedAt)
    } yield List.empty

  def updateOrgRoles(
    orgId: OrgItemId,
    positionId: OrgItemId,
    updatedAt: OffsetDateTime
  ): Future[Seq[BoundStatement]] =
    for {
      roles <- hierarchyEntityService.getRoles(orgId, positionId)
      _     <- indexDao.updateRoles(positionId, roles, updatedAt)
    } yield List.empty

  def changeItemOrder(event: HierarchyEntity.ItemOrderChanged): Future[Seq[BoundStatement]] =
    for {
      children <- hierarchyEntityService.getChildren(event.orgId, event.parentId)
      _        <- indexDao.updateChildren(event.parentId, children, event.updatedAt)
    } yield List.empty

  def moveItem(event: HierarchyEntity.ItemMoved): Future[Seq[BoundStatement]] =
    for {
      childrenFrom <- hierarchyEntityService.getChildren(event.orgId, event.oldParentId)
      childrenTo   <- hierarchyEntityService.getChildren(event.orgId, event.newParentId)
      rootPaths    <- hierarchyEntityService.getRootPaths(event.orgId, event.affectedItemIds)
      _            <- indexDao.updateChildren(event.oldParentId, childrenFrom, event.updatedAt)
      _            <- indexDao.updateChildren(event.newParentId, childrenTo, event.updatedAt)
      _            <- indexDao.updateRootPaths(rootPaths, event.updatedAt)
    } yield List.empty

}
