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
import biz.lobachev.annette.core.model._
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.org_structure.api.hierarchy._
import biz.lobachev.annette.org_structure.api.role.OrgRoleId
import biz.lobachev.annette.org_structure.impl.hierarchy.HierarchyEntity
import com.datastax.driver.core.BoundStatement

import scala.collection.immutable.{Seq, _}
import scala.concurrent.Future

trait HierarchyDbDao {

  def createTables(): Future[Done]

  def prepareStatements(): Future[Done]

  def createOrganization(event: HierarchyEntity.OrganizationCreated): BoundStatement

  def deleteOrganization(event: HierarchyEntity.OrganizationDeleted): BoundStatement

  def createUnit(event: HierarchyEntity.UnitCreated): BoundStatement

  def deleteUnit(event: HierarchyEntity.UnitDeleted): BoundStatement

  def updateChildren(
    unitId: OrgItemId,
    children: Seq[OrgItemId],
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime
  ): BoundStatement

  def assignCategory(event: HierarchyEntity.CategoryAssigned): List[BoundStatement]

  def assignChief(event: HierarchyEntity.ChiefAssigned): List[BoundStatement]

  def unassignChief(event: HierarchyEntity.ChiefUnassigned): List[BoundStatement]

  def createPosition(event: HierarchyEntity.PositionCreated): BoundStatement

  def deletePosition(event: HierarchyEntity.PositionDeleted): BoundStatement

  def updateName(event: HierarchyEntity.NameUpdated): BoundStatement

  def updateShortName(event: HierarchyEntity.ShortNameUpdated): BoundStatement

  def changePositionLimit(event: HierarchyEntity.PositionLimitChanged): BoundStatement

  def assignPerson(event: HierarchyEntity.PersonAssigned, persons: Set[OrgItemId]): List[BoundStatement]

  def unassignPerson(event: HierarchyEntity.PersonUnassigned, persons: Set[OrgItemId]): List[BoundStatement]

  def updatePersons(
    positionId: OrgItemId,
    persons: Set[OrgItemId],
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime
  ): BoundStatement

  def updateRoles(
    positionId: OrgItemId,
    roles: Set[OrgRoleId],
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime
  ): BoundStatement

  def updateRootPaths(
    rootPaths: Map[OrgItemId, Seq[OrgItemId]],
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime
  ): Seq[BoundStatement]

  def getOrgItemById(id: OrgItemId): Future[Option[OrgItem]]

  def getOrgItemsById(ids: Set[OrgItemId]): Future[Seq[OrgItem]]

  def getPersonPrincipals(personId: PersonId): Future[Set[AnnettePrincipal]]

  def getPersonPositions(personId: PersonId): Future[Set[PersonPosition]]

}
