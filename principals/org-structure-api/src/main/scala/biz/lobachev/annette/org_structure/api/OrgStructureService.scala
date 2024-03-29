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
import biz.lobachev.annette.core.attribute.{AttributeMetadata, AttributeValues, UpdateAttributesPayload}
import biz.lobachev.annette.core.model.PersonId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.indexing.FindResult
import biz.lobachev.annette.org_structure.api.category._
import biz.lobachev.annette.org_structure.api.hierarchy._
import biz.lobachev.annette.org_structure.api.role._

import scala.collection.immutable.Map
import scala.concurrent.Future

trait OrgStructureService {

  // hierarchy methods

  def createOrganization(payload: CreateOrganizationPayload): Future[Done]
  def createUnit(payload: CreateUnitPayload): Future[Done]
  def createPosition(payload: CreatePositionPayload): Future[Done]

  def updateName(payload: UpdateNamePayload): Future[Done]
  def assignCategory(payload: AssignCategoryPayload): Future[Done]
  def updateSource(payload: UpdateSourcePayload): Future[Done]
  def updateExternalId(payload: UpdateExternalIdPayload): Future[Done]
  def moveItem(payload: MoveItemPayload): Future[Done]

  def assignChief(payload: AssignChiefPayload): Future[Done]
  def unassignChief(payload: UnassignChiefPayload): Future[Done]

  def changePositionLimit(payload: ChangePositionLimitPayload): Future[Done]
  def assignPerson(payload: AssignPersonPayload): Future[Done]
  def unassignPerson(payload: UnassignPersonPayload): Future[Done]
  def assignOrgRole(payload: AssignOrgRolePayload): Future[Done]
  def unassignOrgRole(payload: UnassignOrgRolePayload): Future[Done]

  def deleteOrgItem(payload: DeleteOrgItemPayload): Future[Done]

  def getOrganization(orgId: CompositeOrgItemId): Future[Organization]
  def getOrganizationTree(itemId: CompositeOrgItemId): Future[OrganizationTree]

  def getOrgItem(
    itemId: CompositeOrgItemId,
    source: Option[String] = None,
    attributes: Option[String] = None
  ): Future[OrgItem]
  def getOrgItems(
    ids: Set[CompositeOrgItemId],
    source: Option[String] = None,
    attributes: Option[String] = None
  ): Future[Seq[OrgItem]]

  def getItemIdsByExternalId(externalIds: Set[String]): Future[Map[String, CompositeOrgItemId]]

  def getPersonPrincipals(personId: PersonId): Future[Set[AnnettePrincipal]]
  def getPersonPositions(personId: PersonId): Future[Set[PersonPosition]]

  def findOrgItems(query: OrgItemFindQuery): Future[FindResult]

  // OrgItem attribute methods
  def getOrgItemMetadata: Future[Map[String, AttributeMetadata]]
  def updateOrgItemAttributes(payload: UpdateAttributesPayload): Future[Done]
  def getOrgItemAttributes(
    id: CompositeOrgItemId,
    source: Option[String] = None,
    attributes: Option[String] = None
  ): Future[AttributeValues]
  def getOrgItemsAttributes(
    ids: Set[CompositeOrgItemId],
    source: Option[String] = None,
    attributes: Option[String] = None
  ): Future[Map[String, AttributeValues]]

  // org role methods

  def createOrgRole(payload: CreateOrgRolePayload): Future[Done]
  def createOrUpdateOrgRole(payload: CreateOrgRolePayload): Future[Done]
  def updateOrgRole(payload: UpdateOrgRolePayload): Future[Done]
  def deleteOrgRole(payload: DeleteOrgRolePayload): Future[Done]
  def getOrgRole(id: OrgRoleId, source: Option[String]): Future[OrgRole]
  def getOrgRoles(ids: Set[OrgRoleId], source: Option[String]): Future[Seq[OrgRole]]
  def findOrgRoles(query: OrgRoleFindQuery): Future[FindResult]

  // category methods

  def createCategory(payload: CreateCategoryPayload): Future[Done]
  def createOrUpdateCategory(payload: CreateCategoryPayload): Future[Done]
  def updateCategory(payload: UpdateCategoryPayload): Future[Done]
  def deleteCategory(payload: DeleteCategoryPayload): Future[Done]
  def getCategory(id: OrgCategoryId, source: Option[String]): Future[OrgCategory]
  def getCategories(ids: Set[OrgCategoryId], source: Option[String]): Future[Seq[OrgCategory]]
  def findCategories(query: OrgCategoryFindQuery): Future[FindResult]

}
