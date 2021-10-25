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

import biz.lobachev.annette.microservice_core.event_processing.SimpleEventHandling
import biz.lobachev.annette.org_structure.impl.hierarchy.dao.HierarchyDbDao
import biz.lobachev.annette.org_structure.impl.hierarchy.entity.HierarchyEntity
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}

import scala.concurrent.ExecutionContext

private[impl] class HierarchyDbEventProcessor(
  readSide: CassandraReadSide,
  dbDao: HierarchyDbDao
)(implicit ec: ExecutionContext)
    extends ReadSideProcessor[HierarchyEntity.Event]
    with SimpleEventHandling {

  def buildHandler(): ReadSideProcessor.ReadSideHandler[HierarchyEntity.Event] =
    readSide
      .builder[HierarchyEntity.Event]("hierarchy-cassandra")
      .setGlobalPrepare(dbDao.createTables)
      .setEventHandler[HierarchyEntity.OrganizationCreated](handle(dbDao.createOrganization))
      .setEventHandler[HierarchyEntity.UnitCreated](handle(dbDao.createUnit))
      .setEventHandler[HierarchyEntity.PositionCreated](handle(dbDao.createPosition))
      .setEventHandler[HierarchyEntity.NameUpdated](handle(dbDao.updateName))
      .setEventHandler[HierarchyEntity.CategoryAssigned](handle(dbDao.assignCategory))
      .setEventHandler[HierarchyEntity.SourceUpdated](handle(dbDao.updateSource))
      .setEventHandler[HierarchyEntity.ExternalIdUpdated](handle(dbDao.updateExternalId))
      .setEventHandler[HierarchyEntity.ItemMoved](handle(dbDao.moveItem))
      .setEventHandler[HierarchyEntity.ItemOrderChanged](handle(dbDao.changeItemOrder))
      .setEventHandler[HierarchyEntity.RootPathUpdated](handle(dbDao.updateRootPath))
      .setEventHandler[HierarchyEntity.ChiefAssigned](handle(dbDao.assignChief))
      .setEventHandler[HierarchyEntity.ChiefUnassigned](handle(dbDao.unassignChief))
      .setEventHandler[HierarchyEntity.PositionLimitChanged](handle(dbDao.changePositionLimit))
      .setEventHandler[HierarchyEntity.PersonAssigned](handle(dbDao.assignPerson))
      .setEventHandler[HierarchyEntity.PersonUnassigned](handle(dbDao.unassignPerson))
      .setEventHandler[HierarchyEntity.OrgRoleAssigned](handle(dbDao.assignOrgRole))
      .setEventHandler[HierarchyEntity.OrgRoleUnassigned](handle(dbDao.unassignOrgRole))
      .setEventHandler[HierarchyEntity.OrganizationDeleted](handle(dbDao.deleteOrganization))
      .setEventHandler[HierarchyEntity.UnitDeleted](handle(dbDao.deleteUnit))
      .setEventHandler[HierarchyEntity.PositionDeleted](handle(dbDao.deletePosition))
      .setEventHandler[HierarchyEntity.OrgItemAttributesUpdated](handle(dbDao.updateOrgItemAttributes))
      .build()

  def aggregateTags: Set[AggregateEventTag[HierarchyEntity.Event]] = HierarchyEntity.Event.Tag.allTags
}
