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
import biz.lobachev.annette.org_structure.impl.hierarchy.dao.HierarchyIndexDao
import biz.lobachev.annette.org_structure.impl.hierarchy.entity.HierarchyEntity
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}

import scala.concurrent.ExecutionContext

private[impl] class HierarchyIndexEventProcessor(
  readSide: CassandraReadSide,
  indexDao: HierarchyIndexDao
)(implicit
  ec: ExecutionContext
) extends ReadSideProcessor[HierarchyEntity.Event]
    with SimpleEventHandling {

  def buildHandler(): ReadSideProcessor.ReadSideHandler[HierarchyEntity.Event] =
    readSide
      .builder[HierarchyEntity.Event]("hierarchy-indexing")
      .setGlobalPrepare(indexDao.createEntityIndex)
      .setEventHandler[HierarchyEntity.OrganizationCreated](handle(indexDao.createOrganization))
      .setEventHandler[HierarchyEntity.UnitCreated](handle(indexDao.createUnit))
      .setEventHandler[HierarchyEntity.PositionCreated](handle(indexDao.createPosition))
      .setEventHandler[HierarchyEntity.NameUpdated](handle(indexDao.updateName))
      .setEventHandler[HierarchyEntity.CategoryAssigned](handle(indexDao.assignCategory))
      .setEventHandler[HierarchyEntity.SourceUpdated](handle(indexDao.updateSource))
      .setEventHandler[HierarchyEntity.ExternalIdUpdated](handle(indexDao.updateExternalId))
      .setEventHandler[HierarchyEntity.ItemMoved](handle(indexDao.moveItem))
      .setEventHandler[HierarchyEntity.ChiefAssigned](handle(indexDao.assignChief))
      .setEventHandler[HierarchyEntity.ChiefUnassigned](handle(indexDao.unassignChief))
      .setEventHandler[HierarchyEntity.PositionLimitChanged](handle(indexDao.changePositionLimit))
      .setEventHandler[HierarchyEntity.PersonAssigned](handle(indexDao.assignPerson))
      .setEventHandler[HierarchyEntity.PersonUnassigned](handle(indexDao.unassignPerson))
      .setEventHandler[HierarchyEntity.OrgRoleAssigned](handle(indexDao.assignOrgRole))
      .setEventHandler[HierarchyEntity.OrgRoleUnassigned](handle(indexDao.unassignOrgRole))
      .setEventHandler[HierarchyEntity.ItemOrderChanged](handle(indexDao.changeItemOrder))
      .setEventHandler[HierarchyEntity.RootPathUpdated](handle(indexDao.updateRootPath))
      .setEventHandler[HierarchyEntity.OrganizationDeleted](handle(indexDao.deleteOrganization))
      .setEventHandler[HierarchyEntity.UnitDeleted](handle(indexDao.deleteUnit))
      .setEventHandler[HierarchyEntity.PositionDeleted](handle(indexDao.deletePosition))
      .build()

  def aggregateTags: Set[AggregateEventTag[HierarchyEntity.Event]] = HierarchyEntity.Event.Tag.allTags

}
