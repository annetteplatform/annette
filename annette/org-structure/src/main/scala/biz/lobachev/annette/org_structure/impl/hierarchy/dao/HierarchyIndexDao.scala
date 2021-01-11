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

package biz.lobachev.annette.org_structure.impl.hierarchy.dao

import java.time.OffsetDateTime
import akka.Done
import biz.lobachev.annette.attributes.api.query.AttributeIndexDao
import biz.lobachev.annette.core.model.elastic.FindResult
import biz.lobachev.annette.org_structure.api.hierarchy.{OrgItemFindQuery, OrgItemId}
import biz.lobachev.annette.org_structure.api.role.OrgRoleId
import biz.lobachev.annette.org_structure.impl.hierarchy.HierarchyEntity

import scala.collection.immutable.Seq
import scala.concurrent.Future

trait HierarchyIndexDao extends AttributeIndexDao {

  def createEntityIndex(): Future[Done]

  def createOrganization(event: HierarchyEntity.OrganizationCreated): Future[Unit]

  def deleteOrganization(event: HierarchyEntity.OrganizationDeleted): Future[Unit]

  def createUnit(event: HierarchyEntity.UnitCreated): Future[Unit]

  def deleteUnit(event: HierarchyEntity.UnitDeleted): Future[Unit]

  def assignCategory(event: HierarchyEntity.CategoryAssigned): Future[Unit]

  def assignChief(event: HierarchyEntity.ChiefAssigned): Future[Unit]

  def unassignChief(event: HierarchyEntity.ChiefUnassigned): Future[Unit]

  def createPosition(event: HierarchyEntity.PositionCreated): Future[Unit]

  def deletePosition(event: HierarchyEntity.PositionDeleted): Future[Unit]

  def updateName(event: HierarchyEntity.NameUpdated): Future[Unit]

  def updateShortName(event: HierarchyEntity.ShortNameUpdated): Future[Unit]

  def changePositionLimit(event: HierarchyEntity.PositionLimitChanged): Future[Unit]

  def updatePersons(positionId: OrgItemId, persons: Set[OrgItemId], updatedAt: OffsetDateTime): Future[Unit]

  def updateRoles(positionId: OrgItemId, roles: Set[OrgRoleId], updatedAt: OffsetDateTime): Future[Unit]

  def updateChildren(itemId: OrgItemId, children: Seq[OrgItemId], updatedAt: OffsetDateTime): Future[Unit]

  def updateRootPaths(rootPaths: Map[OrgItemId, Seq[OrgItemId]], updatedAt: OffsetDateTime): Future[Unit]

  // *************************** Search API ***************************

  def findOrgItem(query: OrgItemFindQuery): Future[FindResult]
}
