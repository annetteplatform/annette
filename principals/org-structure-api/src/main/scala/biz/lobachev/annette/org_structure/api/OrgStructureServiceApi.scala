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
import biz.lobachev.annette.core.attribute.{AttributeMetadata, AttributeValues, UpdateAttributesPayload}
import biz.lobachev.annette.core.exception.AnnetteTransportExceptionSerializer
import biz.lobachev.annette.core.model.indexing.FindResult
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

import scala.collection.immutable.Map

trait OrgStructureServiceApi extends Service {

  // hierarchy methods
  def createOrganization: ServiceCall[CreateOrganizationPayload, Done]
  def createUnit: ServiceCall[CreateUnitPayload, Done]
  def createPosition: ServiceCall[CreatePositionPayload, Done]

  def updateName: ServiceCall[UpdateNamePayload, Done]
  def assignCategory: ServiceCall[AssignCategoryPayload, Done]
  def updateSource: ServiceCall[UpdateSourcePayload, Done]
  def updateExternalId: ServiceCall[UpdateExternalIdPayload, Done]
  def moveItem: ServiceCall[MoveItemPayload, Done]

  def assignChief: ServiceCall[AssignChiefPayload, Done]
  def unassignChief: ServiceCall[UnassignChiefPayload, Done]

  def changePositionLimit: ServiceCall[ChangePositionLimitPayload, Done]
  def assignPerson: ServiceCall[AssignPersonPayload, Done]
  def unassignPerson: ServiceCall[UnassignPersonPayload, Done]
  def assignOrgRole: ServiceCall[AssignOrgRolePayload, Done]
  def unassignOrgRole: ServiceCall[UnassignOrgRolePayload, Done]

  def deleteOrgItem: ServiceCall[DeleteOrgItemPayload, Done]

  def getOrganization(orgId: CompositeOrgItemId): ServiceCall[NotUsed, Organization]
  def getOrganizationTree(itemId: CompositeOrgItemId): ServiceCall[NotUsed, OrganizationTree]

  def getOrgItem(
    itemId: CompositeOrgItemId,
    source: Option[String],
    attributes: Option[String] = None
  ): ServiceCall[NotUsed, OrgItem]
  def getOrgItems(
    source: Option[String],
    attributes: Option[String] = None
  ): ServiceCall[Set[CompositeOrgItemId], Seq[OrgItem]]

  def getItemIdsByExternalId: ServiceCall[Set[String], Map[String, CompositeOrgItemId]]

  def getPersonPrincipals(personId: PersonId): ServiceCall[NotUsed, Set[AnnettePrincipal]]
  def getPersonPositions(personId: PersonId): ServiceCall[NotUsed, Set[PersonPosition]]

  def findOrgItems: ServiceCall[OrgItemFindQuery, FindResult]

  // OrgItem attribute methods

  def getOrgItemMetadata: ServiceCall[NotUsed, Map[String, AttributeMetadata]]
  def updateOrgItemAttributes: ServiceCall[UpdateAttributesPayload, Done]
  def getOrgItemAttributes(
    id: CompositeOrgItemId,
    source: Option[String] = None,
    attributes: Option[String] = None
  ): ServiceCall[NotUsed, AttributeValues]
  def getOrgItemsAttributes(
    source: Option[String] = None,
    attributes: Option[String] = None
  ): ServiceCall[Set[CompositeOrgItemId], Map[String, AttributeValues]]

  // OrgRole methods

  def createOrgRole: ServiceCall[CreateOrgRolePayload, Done]
  def updateOrgRole: ServiceCall[UpdateOrgRolePayload, Done]
  def deleteOrgRole: ServiceCall[DeleteOrgRolePayload, Done]
  def getOrgRole(id: OrgRoleId, source: Option[String]): ServiceCall[NotUsed, OrgRole]
  def getOrgRoles(source: Option[String]): ServiceCall[Set[OrgRoleId], Seq[OrgRole]]
  def findOrgRoles: ServiceCall[OrgRoleFindQuery, FindResult]

  // OrgItem Category methods

  def createCategory: ServiceCall[CreateCategoryPayload, Done]
  def updateCategory: ServiceCall[UpdateCategoryPayload, Done]
  def deleteCategory: ServiceCall[DeleteCategoryPayload, Done]
  def getCategory(id: OrgCategoryId, source: Option[String]): ServiceCall[NotUsed, OrgCategory]
  def getCategories(source: Option[String]): ServiceCall[Set[OrgCategoryId], Seq[OrgCategory]]
  def findCategories: ServiceCall[OrgCategoryFindQuery, FindResult]

  final override def descriptor = {
    import Service._
    // @formatter:off
    named("org-structure")
      .withCalls(
        pathCall("/api/org-structure/v1/createOrganization",   createOrganization),
        pathCall("/api/org-structure/v1/createUnit",           createUnit),
        pathCall("/api/org-structure/v1/createPosition",       createPosition),
        pathCall("/api/org-structure/v1/updateName",           updateName ),
        pathCall("/api/org-structure/v1/assignCategory",       assignCategory),
        pathCall("/api/org-structure/v1/updateSource",         updateSource ),
        pathCall("/api/org-structure/v1/updateExternalId",     updateExternalId ),
        pathCall("/api/org-structure/v1/moveItem",             moveItem),
        pathCall("/api/org-structure/v1/assignChief",          assignChief),
        pathCall("/api/org-structure/v1/unassignChief",        unassignChief),
        pathCall("/api/org-structure/v1/changePositionLimit",  changePositionLimit),
        pathCall("/api/org-structure/v1/assignPerson",         assignPerson),
        pathCall("/api/org-structure/v1/unassignPerson",       unassignPerson),
        pathCall("/api/org-structure/v1/assignOrgRole",        assignOrgRole),
        pathCall("/api/org-structure/v1/unassignOrgRole",      unassignOrgRole),
        pathCall("/api/org-structure/v1/deleteOrgItem",        deleteOrgItem),
        pathCall("/api/org-structure/v1/getOrganization/:orgId",            getOrganization _),
        pathCall("/api/org-structure/v1/getOrganizationTree/:itemId",           getOrganizationTree _),
        pathCall("/api/org-structure/v1/getOrgItem/:itemId?source&attributes",  getOrgItem _),
        pathCall("/api/org-structure/v1/getOrgItems?source&attributes",         getOrgItems _ ),
        pathCall("/api/org-structure/v1/getItemIdsByExternalId",         getItemIdsByExternalId  ),
        pathCall("/api/org-structure/v1/getPersonPrincipals/:personId",  getPersonPrincipals _),
        pathCall("/api/org-structure/v1/getPersonPositions/:personId",   getPersonPositions _),
        pathCall("/api/org-structure/v1/findOrgItems",                   findOrgItems ),

        pathCall("/api/org-structure/v1/getOrgItemMetadata",             getOrgItemMetadata),
        pathCall("/api/org-structure/v1/updateOrgItemAttributes",        updateOrgItemAttributes),
        pathCall("/api/org-structure/v1/getOrgItemAttributes/:id?source&attributes", getOrgItemAttributes _),
        pathCall("/api/org-structure/v1/getOrgItemsAttributes?source&attributes",    getOrgItemsAttributes _),

        pathCall("/api/org-structure/v1/createOrgRole",                createOrgRole),
        pathCall("/api/org-structure/v1/updateOrgRole",                updateOrgRole),
        pathCall("/api/org-structure/v1/deleteOrgRole",                deleteOrgRole),
        pathCall("/api/org-structure/v1/getOrgRole/:id?source", getOrgRole _),
        pathCall("/api/org-structure/v1/getOrgRoles?source",    getOrgRoles _) ,
        pathCall("/api/org-structure/v1/findOrgRoles",                 findOrgRoles),

        pathCall("/api/org-structure/v1/createCategory",                 createCategory),
        pathCall("/api/org-structure/v1/updateCategory",                 updateCategory),
        pathCall("/api/org-structure/v1/deleteCategory",                 deleteCategory),
        pathCall("/api/org-structure/v1/getCategory/:id?source",  getCategory _),
        pathCall("/api/org-structure/v1/getCategories?source",    getCategories _) ,
        pathCall("/api/org-structure/v1/findCategories",                 findCategories),

      )
      .withExceptionSerializer(new AnnetteTransportExceptionSerializer())
      .withAutoAcl(true)
    // @formatter:on
  }
}
