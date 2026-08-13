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

package biz.lobachev.annette.org_structure.impl

import com.google.protobuf.empty.Empty
import biz.lobachev.annette.core.attribute.UpdateAttributesPayload
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.indexing.SortBy
import biz.lobachev.annette.org_structure.api.{grpc => g}
import biz.lobachev.annette.org_structure.api.grpc.OrgStructureService
import biz.lobachev.annette.org_structure.api.category._
import biz.lobachev.annette.org_structure.api.hierarchy._
import biz.lobachev.annette.org_structure.api.role._
import biz.lobachev.annette.org_structure.impl.category.CategoryEntityService
import biz.lobachev.annette.org_structure.impl.hierarchy.HierarchyEntityService
import biz.lobachev.annette.org_structure.impl.role.OrgRoleEntityService
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

class OrgStructureServiceApiImpl(
  hierarchyEntityService: HierarchyEntityService,
  orgRoleEntityService: OrgRoleEntityService,
  categoryEntityService: CategoryEntityService
)(implicit ec: ExecutionContext) extends OrgStructureService {

  private def toDomain(p: Option[g.AnnettePrincipal]): AnnettePrincipal =
    p.map(p => AnnettePrincipal(p.code)).getOrElse(throw new IllegalArgumentException("missing principal"))

  private def fromDomain(p: AnnettePrincipal): g.AnnettePrincipal =
    g.AnnettePrincipal(p.code)

  private def fromDomain(c: OrgCategory): g.OrgCategory =
    g.OrgCategory(
      id = c.id,
      name = c.name,
      forOrganization = c.forOrganization,
      forUnit = c.forUnit,
      forPosition = c.forPosition,
      updatedAt = c.updatedAt.toString,
      updatedBy = Some(fromDomain(c.updatedBy))
    )

  private def fromDomain(r: OrgRole): g.OrgRole =
    g.OrgRole(
      id = r.id,
      name = r.name,
      description = r.description,
      updatedAt = r.updatedAt.toString,
      updatedBy = Some(fromDomain(r.updatedBy))
    )

  private def fromDomain(f: biz.lobachev.annette.core.model.indexing.FindResult): g.FindResult =
    g.FindResult(
      total = f.total,
      hits = f.hits.map(h => g.HitResult(id = h.id, score = h.score, updatedAt = h.updatedAt.toString))
    )

  // === Hierarchy mutations ===

  override def createOrganization(in: g.CreateOrganizationPayload): Future[Empty] =
    for {
      category <- categoryEntityService.getCategoryFromOrigin(in.categoryId)
      _   <- if (category.forOrganization)
                    hierarchyEntityService.createOrganization(
                      CreateOrganizationPayload(
                        orgId = in.orgId,
                        name = in.name,
                        categoryId = in.categoryId,
                        source = in.source,
                        externalId = in.externalId,
                        attributes = if (in.attributes.isEmpty) None else Some(in.attributes),
                        createdBy = toDomain(in.createdBy)
                      )
                    )
                  else Future.failed(IncorrectCategory())
    } yield Empty()

  override def createUnit(in: g.CreateUnitPayload): Future[Empty] =
    for {
      category <- categoryEntityService.getCategoryFromOrigin(in.categoryId)
      _   <- if (category.forUnit)
                    hierarchyEntityService.createUnit(
                      CreateUnitPayload(
                        unitId = in.unitId,
                        parentId = in.parentId,
                        name = in.name,
                        categoryId = in.categoryId,
                        order = in.order,
                        source = in.source,
                        externalId = in.externalId,
                        attributes = if (in.attributes.isEmpty) None else Some(in.attributes),
                        createdBy = toDomain(in.createdBy)
                      )
                    )
                  else Future.failed(IncorrectCategory())
    } yield Empty()

  override def createPosition(in: g.CreatePositionPayload): Future[Empty] =
    for {
      category <- categoryEntityService.getCategoryFromOrigin(in.categoryId)
      _   <- if (category.forPosition)
                    hierarchyEntityService.createPosition(
                      CreatePositionPayload(
                        positionId = in.positionId,
                        parentId = in.parentId,
                        name = in.name,
                        limit = in.limit,
                        categoryId = in.categoryId,
                        order = in.order,
                        source = in.source,
                        externalId = in.externalId,
                        attributes = if (in.attributes.isEmpty) None else Some(in.attributes),
                        createdBy = toDomain(in.createdBy)
                      )
                    )
                  else Future.failed(IncorrectCategory())
    } yield Empty()

  override def updateName(in: g.UpdateNamePayload): Future[Empty] =
    hierarchyEntityService
      .updateName(UpdateNamePayload(itemId = in.itemId, name = in.name, updatedBy = toDomain(in.updatedBy)))
      .map(_ => Empty())

  override def assignCategory(in: g.AssignCategoryPayload): Future[Empty] =
    for {
      category <- categoryEntityService.getCategoryFromOrigin(in.categoryId)
      _   <- hierarchyEntityService.assignCategory(
                    AssignCategoryPayload(itemId = in.itemId, categoryId = in.categoryId, updatedBy = toDomain(in.updatedBy)),
                    category
                  )
    } yield Empty()

  override def updateSource(in: g.UpdateSourcePayload): Future[Empty] =
    hierarchyEntityService
      .updateSource(UpdateSourcePayload(itemId = in.itemId, source = in.source, updatedBy = toDomain(in.updatedBy)))
      .map(_ => Empty())

  override def updateExternalId(in: g.UpdateExternalIdPayload): Future[Empty] =
    hierarchyEntityService
      .updateExternalId(
        UpdateExternalIdPayload(itemId = in.itemId, externalId = in.externalId, updatedBy = toDomain(in.updatedBy))
      )
      .map(_ => Empty())

  override def moveItem(in: g.MoveItemPayload): Future[Empty] =
    hierarchyEntityService
      .moveItem(
        MoveItemPayload(itemId = in.itemId, newParentId = in.newParentId, order = in.order, updatedBy = toDomain(in.updatedBy))
      )
      .map(_ => Empty())

  override def assignChief(in: g.AssignChiefPayload): Future[Empty] =
    hierarchyEntityService
      .assignChief(AssignChiefPayload(unitId = in.unitId, chiefId = in.chiefId, updatedBy = toDomain(in.updatedBy)))
      .map(_ => Empty())

  override def unassignChief(in: g.UnassignChiefPayload): Future[Empty] =
    hierarchyEntityService
      .unassignChief(UnassignChiefPayload(unitId = in.unitId, updatedBy = toDomain(in.updatedBy)))
      .map(_ => Empty())

  override def changePositionLimit(in: g.ChangePositionLimitPayload): Future[Empty] =
    hierarchyEntityService
      .changePositionLimit(
        ChangePositionLimitPayload(positionId = in.positionId, limit = in.limit, updatedBy = toDomain(in.updatedBy))
      )
      .map(_ => Empty())

  override def assignPerson(in: g.AssignPersonPayload): Future[Empty] =
    hierarchyEntityService
      .assignPerson(AssignPersonPayload(positionId = in.positionId, personId = in.personId, updatedBy = toDomain(in.updatedBy)))
      .map(_ => Empty())

  override def unassignPerson(in: g.UnassignPersonPayload): Future[Empty] =
    hierarchyEntityService
      .unassignPerson(
        UnassignPersonPayload(positionId = in.positionId, personId = in.personId, updatedBy = toDomain(in.updatedBy))
      )
      .map(_ => Empty())

  override def assignOrgRole(in: g.AssignOrgRolePayload): Future[Empty] =
    hierarchyEntityService
      .assignOrgRole(
        AssignOrgRolePayload(positionId = in.positionId, orgRoleId = in.orgRoleId, updatedBy = toDomain(in.updatedBy))
      )
      .map(_ => Empty())

  override def unassignOrgRole(in: g.UnassignOrgRolePayload): Future[Empty] =
    hierarchyEntityService
      .unassignOrgRole(
        UnassignOrgRolePayload(positionId = in.positionId, orgRoleId = in.orgRoleId, updatedBy = toDomain(in.updatedBy))
      )
      .map(_ => Empty())

  override def deleteOrgItem(in: g.DeleteOrgItemPayload): Future[Empty] =
    hierarchyEntityService
      .deleteOrgItem(DeleteOrgItemPayload(itemId = in.itemId, deletedBy = toDomain(in.deletedBy)))
      .map(_ => Empty())

  // === Hierarchy queries ===

  override def getOrganization(in: g.GetOrganizationRequest): Future[g.GetOrganizationResponse] =
    hierarchyEntityService.getOrganization(in.orgId).map { org =>
      g.GetOrganizationResponse(organizationJson = Json.stringify(Json.toJson(org)))
    }

  override def getOrganizationTree(in: g.GetOrganizationTreeRequest): Future[g.GetOrganizationTreeResponse] =
    hierarchyEntityService.getOrganizationTree(in.itemId).map { tree =>
      g.GetOrganizationTreeResponse(organizationTreeJson = Json.stringify(Json.toJson(tree)))
    }

  override def getOrgItem(in: g.GetOrgItemRequest): Future[g.GetOrgItemResponse] =
    hierarchyEntityService.getOrgItem(in.itemId, in.source, in.attributes).map { item =>
      g.GetOrgItemResponse(orgItemJson = Json.stringify(Json.toJson(item)))
    }

  override def getOrgItems(in: g.GetOrgItemsRequest): Future[g.GetOrgItemsResponse] =
    hierarchyEntityService
      .getOrgItems(in.ids.toSet, in.source, in.attributes)
      .map(items => g.GetOrgItemsResponse(orgItemsJson = items.map(item => Json.stringify(Json.toJson(item)))))

  override def getItemIdsByExternalId(in: g.GetItemIdsByExternalIdRequest): Future[g.GetItemIdsByExternalIdResponse] =
    hierarchyEntityService
      .getItemIdsByExternalId(in.externalIds.toSet)
      .map(m => g.GetItemIdsByExternalIdResponse(itemIds = m))

  override def getPersonPrincipals(in: g.GetPersonPrincipalsRequest): Future[g.GetPersonPrincipalsResponse] =
    hierarchyEntityService
      .getPersonPrincipals(in.personId)
      .map(principals => g.GetPersonPrincipalsResponse(principals = principals.map(fromDomain).toSeq))

  override def getPersonPositions(in: g.GetPersonPositionsRequest): Future[g.GetPersonPositionsResponse] =
    hierarchyEntityService
      .getPersonPositions(in.personId)
      .map(positions =>
        g.GetPersonPositionsResponse(positions =
          positions.map(pp => g.PersonPosition(personId = pp.personId, positionId = pp.positionId)).toSeq
        )
      )

  override def findOrgItems(in: g.OrgItemFindQuery): Future[g.FindResult] =
    hierarchyEntityService
      .findOrgItems(
        OrgItemFindQuery(
          offset = in.offset,
          size = in.size,
          filter = in.filter,
          name = in.name,
          orgUnits = if (in.orgUnits.isEmpty) None else Some(in.orgUnits.toSet),
          persons = if (in.persons.isEmpty) None else Some(in.persons.toSet),
          orgRoles = if (in.orgRoles.isEmpty) None else Some(in.orgRoles.toSet),
          fromLevel = in.fromLevel,
          toLevel = in.toLevel,
          itemTypes = if (in.itemTypes.isEmpty) None else Some(in.itemTypes.map(ItemTypes.withName).toSet),
          organizations = if (in.organizations.isEmpty) None else Some(in.organizations.toSet),
          parents = if (in.parents.isEmpty) None else Some(in.parents.toSet),
          chiefs = if (in.chiefs.isEmpty) None else Some(in.chiefs.toSet),
          categories = if (in.categories.isEmpty) None else Some(in.categories.toSet),
          sources = if (in.sources.isEmpty) None else Some(in.sources.toSet),
          externalIds = if (in.externalIds.isEmpty) None else Some(in.externalIds.toSet),
          sortBy = Some(in.sortBy.map(s => SortBy(field = s.field, descending = s.descending)).toSeq).filter(_.nonEmpty)
        )
      )
      .map(fromDomain)

  // === Attribute methods ===

  override def getOrgItemMetadata(in: Empty): Future[g.GetOrgItemMetadataResponse] =
    hierarchyEntityService.getEntityMetadata
      .map { metadata =>
        g.GetOrgItemMetadataResponse(
          metadataJson = metadata.map { case (k, v) => k -> Json.stringify(Json.toJson(v)) }
        )
      }

  override def updateOrgItemAttributes(in: g.UpdateAttributesPayload): Future[Empty] =
    hierarchyEntityService
      .updateOrgItemAttributes(
        UpdateAttributesPayload(id = in.id, attributes = in.attributes, updatedBy = toDomain(in.updatedBy))
      )
      .map(_ => Empty())

  override def getOrgItemAttributes(in: g.GetOrgItemAttributesRequest): Future[g.AttributeValuesResponse] =
    hierarchyEntityService
      .getOrgItemAttributes(in.id, in.source, in.attributes)
      .map(values => g.AttributeValuesResponse(values = values))

  override def getOrgItemsAttributes(in: g.GetOrgItemsAttributesRequest): Future[g.GetOrgItemsAttributesResponse] =
    hierarchyEntityService
      .getOrgItemsAttributes(in.ids.toSet, in.source, in.attributes)
      .map { valuesMap =>
        g.GetOrgItemsAttributesResponse(
          values = valuesMap.map { case (k, v) => k -> g.AttributeValuesResponse(values = v) }
        )
      }

  // === OrgRole methods ===

  override def createOrgRole(in: g.CreateOrgRolePayload): Future[Empty] =
    orgRoleEntityService
      .createOrgRole(
        CreateOrgRolePayload(id = in.id, name = in.name, description = in.description, createdBy = toDomain(in.createdBy))
      )
      .map(_ => Empty())

  override def updateOrgRole(in: g.UpdateOrgRolePayload): Future[Empty] =
    orgRoleEntityService
      .updateOrgRole(
        UpdateOrgRolePayload(id = in.id, name = in.name, description = in.description, updatedBy = toDomain(in.updatedBy))
      )
      .map(_ => Empty())

  override def deleteOrgRole(in: g.DeleteOrgRolePayload): Future[Empty] =
    orgRoleEntityService
      .deleteOrgRole(DeleteOrgRolePayload(id = in.id, updatedBy = toDomain(in.updatedBy)))
      .map(_ => Empty())

  override def getOrgRole(in: g.GetOrgRoleRequest): Future[g.OrgRole] =
    orgRoleEntityService.getOrgRole(in.id, in.source).map(fromDomain)

  override def getOrgRoles(in: g.GetOrgRolesRequest): Future[g.GetOrgRolesResponse] =
    orgRoleEntityService
      .getOrgRoles(in.ids.toSet, in.source)
      .map(roles => g.GetOrgRolesResponse(orgRoles = roles.map(fromDomain)))

  override def findOrgRoles(in: g.OrgRoleFindQuery): Future[g.FindResult] =
    orgRoleEntityService
      .findOrgRoles(
        OrgRoleFindQuery(
          offset = in.offset,
          size = in.size,
          filter = in.filter,
          name = in.name,
          description = in.description,
          sortBy = Some(in.sortBy.map(s => SortBy(field = s.field, descending = s.descending)).toSeq).filter(_.nonEmpty)
        )
      )
      .map(fromDomain)

  // === Category methods ===

  override def createCategory(in: g.CreateCategoryPayload): Future[Empty] =
    categoryEntityService
      .createCategory(
        CreateCategoryPayload(
          id = in.id,
          name = in.name,
          forOrganization = in.forOrganization,
          forUnit = in.forUnit,
          forPosition = in.forPosition,
          createdBy = toDomain(in.createdBy)
        )
      )
      .map(_ => Empty())

  override def updateCategory(in: g.UpdateCategoryPayload): Future[Empty] =
    categoryEntityService
      .updateCategory(
        UpdateCategoryPayload(
          id = in.id,
          name = in.name,
          forOrganization = in.forOrganization,
          forUnit = in.forUnit,
          forPosition = in.forPosition,
          updatedBy = toDomain(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def deleteCategory(in: g.DeleteCategoryPayload): Future[Empty] =
    categoryEntityService
      .deleteCategory(DeleteCategoryPayload(id = in.id, updatedBy = toDomain(in.updatedBy)))
      .map(_ => Empty())

  override def getCategory(in: g.GetCategoryRequest): Future[g.OrgCategory] =
    categoryEntityService.getCategory(in.id, in.source).map(fromDomain)

  override def getCategories(in: g.GetCategoriesRequest): Future[g.GetCategoriesResponse] =
    categoryEntityService
      .getCategories(in.ids.toSet, in.source)
      .map(categories => g.GetCategoriesResponse(categories = categories.map(fromDomain)))

  override def findCategories(in: g.OrgCategoryFindQuery): Future[g.FindResult] =
    categoryEntityService
      .findCategories(
        OrgCategoryFindQuery(
          offset = in.offset,
          size = in.size,
          filter = in.filter,
          name = in.name,
          forOrganization = in.forOrganization,
          forUnit = in.forUnit,
          forPosition = in.forPosition,
          sortBy = Some(in.sortBy.map(s => SortBy(field = s.field, descending = s.descending)).toSeq).filter(_.nonEmpty)
        )
      )
      .map(fromDomain)
}
