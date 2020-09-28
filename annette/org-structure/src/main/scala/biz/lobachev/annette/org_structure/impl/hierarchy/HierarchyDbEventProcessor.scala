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

import biz.lobachev.annette.core.model.AnnettePrincipal
import biz.lobachev.annette.org_structure.api.hierarchy.OrgItemId
import biz.lobachev.annette.org_structure.impl.hierarchy.dao.HierarchyDbDao
import com.datastax.driver.core.BoundStatement
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}

import scala.concurrent.{ExecutionContext, Future}

private[impl] class HierarchyDbEventProcessor(
  readSide: CassandraReadSide,
  dbDao: HierarchyDbDao,
  hierarchyEntityService: HierarchyEntityService
)(implicit
  ec: ExecutionContext
) extends ReadSideProcessor[HierarchyEntity.Event] {

  def buildHandler(): ReadSideProcessor.ReadSideHandler[HierarchyEntity.Event] =
    readSide
      .builder[HierarchyEntity.Event]("OrgStructure_Hierarchy_CasEventOffset")
      .setGlobalPrepare(dbDao.createTables)
      .setPrepare(_ => dbDao.prepareStatements())
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
        updateOrgRoles(e.event.orgId, e.event.positionId, e.event.updatedBy, e.event.updatedAt)
      )
      .setEventHandler[HierarchyEntity.OrgRoleUnassigned](e =>
        updateOrgRoles(e.event.orgId, e.event.positionId, e.event.updatedBy, e.event.updatedAt)
      )
      .setEventHandler[HierarchyEntity.ItemOrderChanged](e => changeItemOrder(e.event))
      .setEventHandler[HierarchyEntity.ItemMoved](e => moveItem(e.event))
      .build()

  def aggregateTags: Set[AggregateEventTag[HierarchyEntity.Event]] = HierarchyEntity.Event.Tag.allTags

  def createOrganization(event: HierarchyEntity.OrganizationCreated): Future[Seq[BoundStatement]] =
    Future.successful(
      List(
        dbDao.createOrganization(event)
      )
    )

  def deleteOrganization(event: HierarchyEntity.OrganizationDeleted): Future[Seq[BoundStatement]] =
    Future.successful(
      List(
        dbDao.deleteOrganization(event)
      )
    )

  def createUnit(event: HierarchyEntity.UnitCreated): Future[Seq[BoundStatement]] =
    for {
      children <- hierarchyEntityService.getChildren(event.orgId, event.parentId)
    } yield List(
      dbDao.updateChildren(event.parentId, children, event.createdBy, event.createdAt),
      dbDao.createUnit(event)
    )

  def deleteUnit(event: HierarchyEntity.UnitDeleted): Future[Seq[BoundStatement]] =
    for {
      children <- hierarchyEntityService.getChildren(event.orgId, event.parentId)
    } yield List(
      dbDao.updateChildren(event.parentId, children, event.deletedBy, event.deletedAt),
      dbDao.deleteUnit(event)
    )

  def assignChief(event: HierarchyEntity.ChiefAssigned): Future[Seq[BoundStatement]] =
    Future.successful(
      dbDao.assignChief(event)
    )

  def unassignChief(event: HierarchyEntity.ChiefUnassigned): Future[Seq[BoundStatement]] =
    Future.successful(
      dbDao.unassignChief(event)
    )

  def createPosition(event: HierarchyEntity.PositionCreated): Future[Seq[BoundStatement]] =
    for {
      children <- hierarchyEntityService.getChildren(event.orgId, event.parentId)
    } yield List(
      dbDao.updateChildren(event.parentId, children, event.createdBy, event.createdAt),
      dbDao.createPosition(event)
    )

  def deletePosition(event: HierarchyEntity.PositionDeleted): Future[Seq[BoundStatement]] =
    for {
      children <- hierarchyEntityService.getChildren(event.orgId, event.parentId)
    } yield List(
      dbDao.updateChildren(event.parentId, children, event.deletedBy, event.deletedAt),
      dbDao.deletePosition(event)
    )

  def updateName(event: HierarchyEntity.NameUpdated): Future[Seq[BoundStatement]] =
    Future.successful(
      List(
        dbDao.updateName(event)
      )
    )

  def updateShortName(event: HierarchyEntity.ShortNameUpdated): Future[Seq[BoundStatement]] =
    Future.successful(
      List(
        dbDao.updateShortName(event)
      )
    )

  def changePositionLimit(event: HierarchyEntity.PositionLimitChanged): Future[Seq[BoundStatement]] =
    Future.successful(
      List(
        dbDao.changePositionLimit(event)
      )
    )

  def assignPerson(event: HierarchyEntity.PersonAssigned): Future[Seq[BoundStatement]] =
    for {
      persons <- hierarchyEntityService.getPersons(event.orgId, event.positionId)
    } yield dbDao.assignPerson(event, persons)

  def unassignPerson(event: HierarchyEntity.PersonUnassigned): Future[Seq[BoundStatement]] =
    for {
      persons <- hierarchyEntityService.getPersons(event.orgId, event.positionId)
    } yield dbDao.unassignPerson(event, persons)

  def updateOrgRoles(
    orgId: OrgItemId,
    positionId: OrgItemId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime
  ): Future[Seq[BoundStatement]] =
    for {
      roles <- hierarchyEntityService.getRoles(orgId, positionId)
    } yield List(
      dbDao.updateRoles(positionId, roles, updatedBy, updatedAt)
    )

  def changeItemOrder(event: HierarchyEntity.ItemOrderChanged): Future[Seq[BoundStatement]] =
    for {
      children <- hierarchyEntityService.getChildren(event.orgId, event.parentId)
    } yield List(
      dbDao.updateChildren(event.parentId, children, event.updatedBy, event.updatedAt)
    )

  def moveItem(event: HierarchyEntity.ItemMoved): Future[Seq[BoundStatement]] =
    for {
      childrenFrom <- hierarchyEntityService.getChildren(event.orgId, event.oldParentId)
      childrenTo   <- hierarchyEntityService.getChildren(event.orgId, event.newParentId)
      rootPaths    <- hierarchyEntityService.getRootPaths(event.orgId, event.affectedItemIds)
    } yield List(
      dbDao.updateChildren(event.oldParentId, childrenFrom, event.updatedBy, event.updatedAt),
      dbDao.updateChildren(event.newParentId, childrenTo, event.updatedBy, event.updatedAt)
    ) ++
      dbDao.updateRootPaths(rootPaths, event.updatedBy, event.updatedAt)

}
