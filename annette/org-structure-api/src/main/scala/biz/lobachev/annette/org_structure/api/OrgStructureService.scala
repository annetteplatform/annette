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

package biz.lobachev.annette.org_structure.api

import akka.Done
import biz.lobachev.annette.core.model.elastic.FindResult
import biz.lobachev.annette.core.model.PersonId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.org_structure.api.category.{
  CreateCategoryPayload,
  DeleteCategoryPayload,
  OrgCategory,
  OrgCategoryFindQuery,
  OrgCategoryId,
  UpdateCategoryPayload
}
import biz.lobachev.annette.org_structure.api.hierarchy._
import biz.lobachev.annette.org_structure.api.role.{OrgRoleId, _}

import scala.concurrent.Future

trait OrgStructureService {

  // hierarchy methods

  def createOrganization(payload: CreateOrganizationPayload): Future[Done]
  def deleteOrganization(payload: DeleteOrganizationPayload): Future[Done]
  def getOrganizationById(orgId: OrgItemId): Future[Organization]
  def getOrganizationTree(orgId: OrgItemId, itemId: OrgItemId): Future[OrganizationTree]

  def createUnit(payload: CreateUnitPayload): Future[Done]
  def deleteUnit(payload: DeleteUnitPayload): Future[Done]
  def assignChief(payload: AssignChiefPayload): Future[Done]
  def unassignChief(payload: UnassignChiefPayload): Future[Done]

  def createPosition(payload: CreatePositionPayload): Future[Done]
  def deletePosition(payload: DeletePositionPayload): Future[Done]
  def updateName(payload: UpdateNamePayload): Future[Done]
  def updateShortName(payload: UpdateShortNamePayload): Future[Done]
  def assignCategory(payload: AssignCategoryPayload): Future[Done]
  def changePositionLimit(payload: ChangePositionLimitPayload): Future[Done]
  def assignPerson(payload: AssignPersonPayload): Future[Done]
  def unassignPerson(payload: UnassignPersonPayload): Future[Done]
  def assignOrgRole(payload: AssignOrgRolePayload): Future[Done]
  def unassignOrgRole(payload: UnassignOrgRolePayload): Future[Done]

  def getOrgItemById(orgId: OrgItemId, id: OrgItemId): Future[OrgItem]
  def getOrgItemsById(orgId: OrgItemId, ids: Set[OrgItemId]): Future[Map[OrgItemId, OrgItem]]
  def getOrgItemByIdFromReadSide(id: OrgItemId): Future[OrgItem]
  def getOrgItemsByIdFromReadSide(ids: Set[OrgItemId]): Future[Map[OrgItemId, OrgItem]]
  def findOrgItems(query: OrgItemFindQuery): Future[FindResult]

  def moveItem(payload: MoveItemPayload): Future[Done]
  def changeItemOrder(payload: ChangeItemOrderPayload): Future[Done]

  def getPersonPrincipals(personId: PersonId): Future[Set[AnnettePrincipal]]
  def getPersonPositions(personId: PersonId): Future[Set[PersonPosition]]

  // org role methods

  def createOrgRole(payload: CreateOrgRolePayload): Future[Done]
  def createOrUpdateOrgRole(payload: CreateOrgRolePayload): Future[Done]
  def updateOrgRole(payload: UpdateOrgRolePayload): Future[Done]
  def deleteOrgRole(payload: DeleteOrgRolePayload): Future[Done]
  def getOrgRoleById(id: OrgRoleId, fromReadSide: Boolean): Future[OrgRole]
  def getOrgRolesById(ids: Set[OrgRoleId], fromReadSide: Boolean): Future[Map[OrgRoleId, OrgRole]]
  def findOrgRoles(query: OrgRoleFindQuery): Future[FindResult]

  // category methods

  def createCategory(payload: CreateCategoryPayload): Future[Done]
  def createOrUpdateCategory(payload: CreateCategoryPayload): Future[Done]
  def updateCategory(payload: UpdateCategoryPayload): Future[Done]
  def deleteCategory(payload: DeleteCategoryPayload): Future[Done]
  def getCategoryById(id: OrgCategoryId, fromReadSide: Boolean): Future[OrgCategory]
  def getCategoriesById(ids: Set[OrgCategoryId], fromReadSide: Boolean): Future[Map[OrgCategoryId, OrgCategory]]
  def findCategories(query: OrgCategoryFindQuery): Future[FindResult]

}
