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
  OrgCategoryAlreadyExist,
  OrgCategoryFindQuery,
  OrgCategoryId,
  UpdateCategoryPayload
}
import biz.lobachev.annette.org_structure.api.hierarchy._
import biz.lobachev.annette.org_structure.api.role.{OrgRoleId, _}
import io.scalaland.chimney.dsl._

import scala.collection.immutable.Map
import scala.concurrent.{ExecutionContext, Future}

class OrgStructureServiceImpl(api: OrgStructureServiceApi, implicit val ec: ExecutionContext)
    extends OrgStructureService {

  // hierarchy methods

  def createOrganization(payload: CreateOrganizationPayload): Future[Done] =
    api.createOrganization.invoke(payload)

  def createUnit(payload: CreateUnitPayload): Future[Done] =
    api.createUnit.invoke(payload)

  def createPosition(payload: CreatePositionPayload): Future[Done] =
    api.createPosition.invoke(payload)

  def updateName(payload: UpdateNamePayload): Future[Done] =
    api.updateName.invoke(payload)

  def assignCategory(payload: AssignCategoryPayload): Future[Done] =
    api.assignCategory.invoke(payload)

  def updateSource(payload: UpdateSourcePayload): Future[Done] =
    api.updateSource.invoke(payload)

  def updateExternalId(payload: UpdateExternalIdPayload): Future[Done] =
    api.updateExternalId.invoke(payload)

  def moveItem(payload: MoveItemPayload): Future[Done] =
    api.moveItem.invoke(payload)

  def assignChief(payload: AssignChiefPayload): Future[Done] =
    api.assignChief.invoke(payload)

  def unassignChief(payload: UnassignChiefPayload): Future[Done] =
    api.unassignChief.invoke(payload)

  def changePositionLimit(payload: ChangePositionLimitPayload): Future[Done] =
    api.changePositionLimit.invoke(payload)

  def assignPerson(payload: AssignPersonPayload): Future[Done] =
    api.assignPerson.invoke(payload)

  def unassignPerson(payload: UnassignPersonPayload): Future[Done] =
    api.unassignPerson.invoke(payload)

  def assignOrgRole(payload: AssignOrgRolePayload): Future[Done] =
    api.assignOrgRole.invoke(payload)

  def unassignOrgRole(payload: UnassignOrgRolePayload): Future[Done] =
    api.unassignOrgRole.invoke(payload)

  def deleteOrgItem(payload: DeleteOrgItemPayload): Future[Done] =
    api.deleteOrgItem.invoke(payload)

  def getOrganizationById(orgId: CompositeOrgItemId): Future[Organization] =
    api.getOrganizationById(orgId).invoke()

  def getOrganizationTree(itemId: CompositeOrgItemId): Future[OrganizationTree] =
    api.getOrganizationTree(itemId).invoke()

  def getOrgItemById(itemId: CompositeOrgItemId, fromReadSide: Boolean): Future[OrgItem] =
    api.getOrgItemById(itemId, fromReadSide).invoke()

  def getOrgItemsById(ids: Set[CompositeOrgItemId], fromReadSide: Boolean): Future[Seq[OrgItem]] =
    api.getOrgItemsById(fromReadSide).invoke(ids)

  def getItemIdsByExternalId(externalIds: Set[String]): Future[Map[String, CompositeOrgItemId]] =
    api.getItemIdsByExternalId.invoke(externalIds)

  def getPersonPrincipals(personId: PersonId): Future[Set[AnnettePrincipal]] =
    api.getPersonPrincipals(personId).invoke()

  def getPersonPositions(personId: PersonId): Future[Set[PersonPosition]] =
    api.getPersonPositions(personId).invoke()

  def findOrgItems(query: OrgItemFindQuery): Future[FindResult] =
    api.findOrgItems.invoke(query)

  // org role methods

  def createOrgRole(payload: CreateOrgRolePayload): Future[Done] =
    api.createOrgRole.invoke(payload)

  def updateOrgRole(payload: UpdateOrgRolePayload): Future[Done] =
    api.updateOrgRole.invoke(payload)

  def createOrUpdateOrgRole(payload: CreateOrgRolePayload): Future[Done] =
    createOrgRole(payload).recoverWith {
      case OrgRoleAlreadyExist(_) =>
        val updatePayload = payload
          .into[UpdateOrgRolePayload]
          .withFieldComputed(_.updatedBy, _.createdBy)
          .transform
        updateOrgRole(updatePayload)
      case th                     => Future.failed(th)
    }

  def deleteOrgRole(payload: DeleteOrgRolePayload): Future[Done] =
    api.deleteOrgRole.invoke(payload)

  def getOrgRoleById(id: OrgRoleId, fromReadSide: Boolean): Future[OrgRole] =
    api.getOrgRoleById(id, fromReadSide).invoke()

  def getOrgRolesById(ids: Set[OrgRoleId], fromReadSide: Boolean): Future[Seq[OrgRole]] =
    api.getOrgRolesById(fromReadSide).invoke(ids)

  def findOrgRoles(query: OrgRoleFindQuery): Future[FindResult] =
    api.findOrgRoles.invoke(query)

  // org category methods

  def createCategory(payload: CreateCategoryPayload): Future[Done] =
    api.createCategory.invoke(payload)

  def updateCategory(payload: UpdateCategoryPayload): Future[Done] =
    api.updateCategory.invoke(payload)

  def createOrUpdateCategory(payload: CreateCategoryPayload): Future[Done] =
    createCategory(payload).recoverWith {
      case OrgCategoryAlreadyExist(_) =>
        val updatePayload = payload
          .into[UpdateCategoryPayload]
          .withFieldComputed(_.updatedBy, _.createdBy)
          .transform
        updateCategory(updatePayload)
      case th                         => Future.failed(th)
    }

  def deleteCategory(payload: DeleteCategoryPayload): Future[Done] =
    api.deleteCategory.invoke(payload)

  def getCategoryById(id: OrgCategoryId, fromReadSide: Boolean): Future[OrgCategory] =
    api.getCategoryById(id, fromReadSide).invoke()

  def getCategoriesById(ids: Set[OrgCategoryId], fromReadSide: Boolean): Future[Seq[OrgCategory]] =
    api.getCategoriesById(fromReadSide).invoke(ids)

  def findCategories(query: OrgCategoryFindQuery): Future[FindResult] =
    api.findCategories.invoke(query)

}
