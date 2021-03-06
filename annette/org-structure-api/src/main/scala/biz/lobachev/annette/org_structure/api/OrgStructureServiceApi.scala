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

import akka.{Done, NotUsed}
import biz.lobachev.annette.core.exception.AnnetteTransportExceptionSerializer
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
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}

trait OrgStructureServiceApi extends Service {

  // hierarchy methods

  def createOrganization: ServiceCall[CreateOrganizationPayload, Done]
  def deleteOrganization: ServiceCall[DeleteOrganizationPayload, Done]
  def getOrganizationById(orgId: OrgItemId): ServiceCall[NotUsed, Organization]
  def getOrganizationTree(orgId: OrgItemId, itemId: OrgItemId): ServiceCall[NotUsed, OrganizationTree]

  def createUnit: ServiceCall[CreateUnitPayload, Done]
  def deleteUnit: ServiceCall[DeleteUnitPayload, Done]
  def assignChief: ServiceCall[AssignChiefPayload, Done]
  def unassignChief: ServiceCall[UnassignChiefPayload, Done]

  def createPosition: ServiceCall[CreatePositionPayload, Done]
  def deletePosition: ServiceCall[DeletePositionPayload, Done]
  def updateName: ServiceCall[UpdateNamePayload, Done]
  def updateShortName: ServiceCall[UpdateShortNamePayload, Done]
  def assignCategory: ServiceCall[AssignCategoryPayload, Done]
  def changePositionLimit: ServiceCall[ChangePositionLimitPayload, Done]
  def assignPerson: ServiceCall[AssignPersonPayload, Done]
  def unassignPerson: ServiceCall[UnassignPersonPayload, Done]
  def assignOrgRole: ServiceCall[AssignOrgRolePayload, Done]
  def unassignOrgRole: ServiceCall[UnassignOrgRolePayload, Done]

  def getOrgItemById(orgId: OrgItemId, id: OrgItemId): ServiceCall[NotUsed, OrgItem]
  def getOrgItemsById(orgId: OrgItemId): ServiceCall[Set[OrgItemId], Map[OrgItemId, OrgItem]]
  def getOrgItemByIdFromReadSide(id: OrgItemId): ServiceCall[NotUsed, OrgItem]
  def getOrgItemsByIdFromReadSide: ServiceCall[Set[OrgItemId], Map[OrgItemId, OrgItem]]
  def findOrgItems: ServiceCall[OrgItemFindQuery, FindResult]

  def moveItem: ServiceCall[MoveItemPayload, Done]
  def changeItemOrder: ServiceCall[ChangeItemOrderPayload, Done]

  def getPersonPrincipals(personId: PersonId): ServiceCall[NotUsed, Set[AnnettePrincipal]]
  def getPersonPositions(personId: PersonId): ServiceCall[NotUsed, Set[PersonPosition]]

  // org role methods

  def createOrgRole: ServiceCall[CreateOrgRolePayload, Done]
  def updateOrgRole: ServiceCall[UpdateOrgRolePayload, Done]
  def deleteOrgRole: ServiceCall[DeleteOrgRolePayload, Done]
  def getOrgRoleById(id: OrgRoleId, fromReadSide: Boolean): ServiceCall[NotUsed, OrgRole]
  def getOrgRolesById(fromReadSide: Boolean): ServiceCall[Set[OrgRoleId], Map[OrgRoleId, OrgRole]]
  def findOrgRoles: ServiceCall[OrgRoleFindQuery, FindResult]

  // org item category

  def createCategory: ServiceCall[CreateCategoryPayload, Done]
  def updateCategory: ServiceCall[UpdateCategoryPayload, Done]
  def deleteCategory: ServiceCall[DeleteCategoryPayload, Done]
  def getCategoryById(id: OrgCategoryId, fromReadSide: Boolean): ServiceCall[NotUsed, OrgCategory]
  def getCategoriesById(
    fromReadSide: Boolean
  ): ServiceCall[Set[OrgCategoryId], Map[OrgCategoryId, OrgCategory]]
  def findCategories: ServiceCall[OrgCategoryFindQuery, FindResult]

  final override def descriptor = {
    import Service._
    // @formatter:off
    named("org-structure")
      .withCalls(
        pathCall("/api/org-structure/v1/createOrganization",                 createOrganization),
        pathCall("/api/org-structure/v1/deleteOrganization",                 deleteOrganization),
        pathCall("/api/org-structure/v1/getOrganizationById/:orgId",         getOrganizationById _),
        pathCall("/api/org-structure/v1/getOrganizationTree/:orgId/:itemId", getOrganizationTree _),
        pathCall("/api/org-structure/v1/createUnit",                         createUnit),
        pathCall("/api/org-structure/v1/deleteUnit",                         deleteUnit),
        pathCall("/api/org-structure/v1/assignChief",                        assignChief),
        pathCall("/api/org-structure/v1/unassignChief",                      unassignChief),
        pathCall("/api/org-structure/v1/createPosition",                     createPosition),
        pathCall("/api/org-structure/v1/deletePosition",                     deletePosition),
        pathCall("/api/org-structure/v1/changePositionLimit",                changePositionLimit),
        pathCall("/api/org-structure/v1/assignPerson",                       assignPerson),
        pathCall("/api/org-structure/v1/unassignPerson",                     unassignPerson),
        pathCall("/api/org-structure/v1/assignOrgRole",                      assignOrgRole),
        pathCall("/api/org-structure/v1/unassignOrgRole",                    unassignOrgRole),
        pathCall("/api/org-structure/v1/moveItem",                           moveItem),
        pathCall("/api/org-structure/v1/changeItemOrder",                    changeItemOrder),

        pathCall("/api/org-structure/v1/updateName",                     updateName ),
        pathCall("/api/org-structure/v1/updateShortName",                updateShortName ),
        pathCall("/api/org-structure/v1/assignCategory",                 assignCategory),
        pathCall("/api/org-structure/v1/getOrgItemById/:orgId/:id",      getOrgItemById _),
        pathCall("/api/org-structure/v1/getOrgItemByIdFromReadSide/:id", getOrgItemByIdFromReadSide _),
        pathCall("/api/org-structure/v1/getOrgItemsById/:orgId",         getOrgItemsById _ ),
        pathCall("/api/org-structure/v1/getOrgItemsByIdFromReadSide",    getOrgItemsByIdFromReadSide  ),
        pathCall("/api/org-structure/v1/getPersonPrincipals/:personId",  getPersonPrincipals _),
        pathCall("/api/org-structure/v1/getPersonPositions/:personId",   getPersonPositions _),
        pathCall("/api/org-structure/v1/findOrgItems",                   findOrgItems ),

        pathCall("/api/org-structure/v1/createOrgRole",                createOrgRole),
        pathCall("/api/org-structure/v1/updateOrgRole",                updateOrgRole),
        pathCall("/api/org-structure/v1/deleteOrgRole",                deleteOrgRole),
        pathCall("/api/org-structure/v1/getOrgRoleById/:id/:readSide", getOrgRoleById _),
        pathCall("/api/org-structure/v1/getOrgRolesById/:readSide",    getOrgRolesById _) ,
        pathCall("/api/org-structure/v1/findOrgRoles",                 findOrgRoles),

        pathCall("/api/org-structure/v1/createCategory",                 createCategory),
        pathCall("/api/org-structure/v1/updateCategory",                 updateCategory),
        pathCall("/api/org-structure/v1/deleteCategory",                 deleteCategory),
        pathCall("/api/org-structure/v1/getCategoryById/:id/:readSide",  getCategoryById _),
        pathCall("/api/org-structure/v1/getCategoriesById/:readSide",    getCategoriesById _) ,
        pathCall("/api/org-structure/v1/findCategories",                 findCategories),

      )
      .withExceptionSerializer(new AnnetteTransportExceptionSerializer())
      .withAutoAcl(true)
    // @formatter:on
  }
}
