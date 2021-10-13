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

import biz.lobachev.annette.org_structure.impl.hierarchy.dao.HierarchyCassandraDbDao
import biz.lobachev.annette.org_structure.impl.hierarchy.entity.HierarchyEntity
import com.datastax.driver.core.BoundStatement
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}

import scala.concurrent.Future

private[impl] class HierarchyDbEventProcessor(
  readSide: CassandraReadSide,
  dbDao: HierarchyCassandraDbDao
)
//(implicit ec: ExecutionContext)
    extends ReadSideProcessor[HierarchyEntity.Event] {

  def buildHandler(): ReadSideProcessor.ReadSideHandler[HierarchyEntity.Event] =
    readSide
      .builder[HierarchyEntity.Event]("hierarchy-cassandra")
      .setGlobalPrepare(dbDao.createTables)
      .setPrepare(_ => dbDao.prepareStatements())
      .setEventHandler[HierarchyEntity.OrganizationCreated](e => createOrganization(e.event))
      .setEventHandler[HierarchyEntity.UnitCreated](e => createUnit(e.event))
      .setEventHandler[HierarchyEntity.PositionCreated](e => createPosition(e.event))
      .setEventHandler[HierarchyEntity.NameUpdated](e => updateName(e.event))
      .setEventHandler[HierarchyEntity.CategoryAssigned](e => assignCategory(e.event))
      .setEventHandler[HierarchyEntity.SourceUpdated](e => updateSource(e.event))
      .setEventHandler[HierarchyEntity.ExternalIdUpdated](e => updateExternalId(e.event))
      .setEventHandler[HierarchyEntity.ItemMoved](e => moveItem(e.event))
      .setEventHandler[HierarchyEntity.ItemOrderChanged](e => changeItemOrder(e.event))
      .setEventHandler[HierarchyEntity.RootPathUpdated](e => updateRootPath(e.event))
      .setEventHandler[HierarchyEntity.ChiefAssigned](e => assignChief(e.event))
      .setEventHandler[HierarchyEntity.ChiefUnassigned](e => unassignChief(e.event))
      .setEventHandler[HierarchyEntity.PositionLimitChanged](e => changePositionLimit(e.event))
      .setEventHandler[HierarchyEntity.PersonAssigned](e => assignPerson(e.event))
      .setEventHandler[HierarchyEntity.PersonUnassigned](e => unassignPerson(e.event))
      .setEventHandler[HierarchyEntity.OrgRoleAssigned](e => assignOrgRole(e.event))
      .setEventHandler[HierarchyEntity.OrgRoleUnassigned](e => unassignOrgRole(e.event))
      .setEventHandler[HierarchyEntity.OrganizationDeleted](e => deleteOrganization(e.event))
      .setEventHandler[HierarchyEntity.UnitDeleted](e => deleteUnit(e.event))
      .setEventHandler[HierarchyEntity.PositionDeleted](e => deletePosition(e.event))
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
    Future.successful(
      dbDao.createUnit(event)
    )

  def deleteUnit(event: HierarchyEntity.UnitDeleted): Future[Seq[BoundStatement]] =
    Future.successful(
      dbDao.deleteUnit(event)
    )

  def assignCategory(event: HierarchyEntity.CategoryAssigned): Future[Seq[BoundStatement]] =
    Future.successful(
      dbDao.assignCategory(event)
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
    Future.successful(
      dbDao.createPosition(event)
    )

  def deletePosition(event: HierarchyEntity.PositionDeleted): Future[Seq[BoundStatement]] =
    Future.successful(
      dbDao.deletePosition(event)
    )

  def updateName(event: HierarchyEntity.NameUpdated): Future[Seq[BoundStatement]] =
    Future.successful(
      List(
        dbDao.updateName(event)
      )
    )

  def updateSource(event: HierarchyEntity.SourceUpdated): Future[Seq[BoundStatement]] =
    Future.successful(
      List(
        dbDao.updateSource(event)
      )
    )

  def updateExternalId(event: HierarchyEntity.ExternalIdUpdated): Future[Seq[BoundStatement]] =
    Future.successful(
      dbDao.updateExternalId(event)
    )

  def changePositionLimit(event: HierarchyEntity.PositionLimitChanged): Future[Seq[BoundStatement]] =
    Future.successful(
      List(
        dbDao.changePositionLimit(event)
      )
    )

  def assignPerson(event: HierarchyEntity.PersonAssigned): Future[Seq[BoundStatement]] =
    Future.successful(dbDao.assignPerson(event))

  def unassignPerson(event: HierarchyEntity.PersonUnassigned): Future[Seq[BoundStatement]] =
    Future.successful(dbDao.unassignPerson(event))

  def assignOrgRole(event: HierarchyEntity.OrgRoleAssigned): Future[Seq[BoundStatement]] =
    Future.successful(Seq(dbDao.assignOrgRole(event)))

  def unassignOrgRole(event: HierarchyEntity.OrgRoleUnassigned): Future[Seq[BoundStatement]] =
    Future.successful(Seq(dbDao.unassignOrgRole(event)))

  def moveItem(event: HierarchyEntity.ItemMoved): Future[Seq[BoundStatement]] =
    Future.successful(dbDao.moveItem(event))

  def changeItemOrder(event: HierarchyEntity.ItemOrderChanged): Future[Seq[BoundStatement]] =
    Future.successful(dbDao.changeItemOrder(event))

  def updateRootPath(event: HierarchyEntity.RootPathUpdated): Future[Seq[BoundStatement]] =
    Future.successful(
      Seq(
        dbDao.updateRootPath(event)
      )
    )

}
