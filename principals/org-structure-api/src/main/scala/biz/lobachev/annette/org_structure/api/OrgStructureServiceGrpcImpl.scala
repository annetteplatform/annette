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

import java.time.OffsetDateTime
import com.google.protobuf.empty.Empty
import biz.lobachev.annette.core.attribute.{AttributeMetadata, UpdateAttributesPayload}
import biz.lobachev.annette.core.exception.AnnetteGrpcExceptionMapping
import biz.lobachev.annette.core.model.PersonId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.indexing.{FindResult, HitResult, SortBy}
import biz.lobachev.annette.org_structure.api.category._
import biz.lobachev.annette.org_structure.api.hierarchy._
import biz.lobachev.annette.org_structure.api.role._
import biz.lobachev.annette.org_structure.api.{grpc => g}
import org.apache.pekko.Done
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

class OrgStructureServiceGrpcImpl(client: g.OrgStructureServiceClient)(implicit val ec: ExecutionContext)
    extends OrgStructureService {

  private def unwrapPrincipal(p: Option[g.AnnettePrincipal]): AnnettePrincipal =
    p.map(pr => AnnettePrincipal(pr.code)).getOrElse(throw new IllegalArgumentException("missing principal"))

  private def fromDomain(p: AnnettePrincipal): g.AnnettePrincipal = g.AnnettePrincipal(p.code)

  private def sortByToProto(sortBy: Option[Seq[SortBy]]): Seq[g.SortBy] =
    sortBy.map(_.map(s => g.SortBy(field = s.field, descending = s.descending))).getOrElse(Seq.empty)

  private def findResultFromProto(f: g.FindResult): FindResult =
    FindResult(
      total = f.total,
      hits = f.hits.map(h => HitResult(id = h.id, score = h.score, updatedAt = OffsetDateTime.parse(h.updatedAt)))
    )

  private def call[T](f: Future[T]): Future[T] = AnnetteGrpcExceptionMapping.recoverAnnette(f)

  private def attrsToProto(attributes: Option[Map[String, String]]): Map[String, String] =
    attributes.getOrElse(Map.empty)

  // === Hierarchy mutations ===

  override def createOrganization(payload: CreateOrganizationPayload): Future[Done] =
    call(
      client.createOrganization(
        g.CreateOrganizationPayload(
          orgId = payload.orgId,
          name = payload.name,
          categoryId = payload.categoryId,
          source = payload.source,
          externalId = payload.externalId,
          attributes = attrsToProto(payload.attributes),
          createdBy = Some(fromDomain(payload.createdBy))
        )
      )
    ).map(_ => Done)

  override def createUnit(payload: CreateUnitPayload): Future[Done] =
    call(
      client.createUnit(
        g.CreateUnitPayload(
          unitId = payload.unitId,
          parentId = payload.parentId,
          name = payload.name,
          categoryId = payload.categoryId,
          order = payload.order,
          source = payload.source,
          externalId = payload.externalId,
          attributes = attrsToProto(payload.attributes),
          createdBy = Some(fromDomain(payload.createdBy))
        )
      )
    ).map(_ => Done)

  override def createPosition(payload: CreatePositionPayload): Future[Done] =
    call(
      client.createPosition(
        g.CreatePositionPayload(
          positionId = payload.positionId,
          parentId = payload.parentId,
          name = payload.name,
          limit = payload.limit,
          categoryId = payload.categoryId,
          order = payload.order,
          source = payload.source,
          externalId = payload.externalId,
          attributes = attrsToProto(payload.attributes),
          createdBy = Some(fromDomain(payload.createdBy))
        )
      )
    ).map(_ => Done)

  override def updateName(payload: UpdateNamePayload): Future[Done] =
    call(
      client.updateName(
        g.UpdateNamePayload(itemId = payload.itemId, name = payload.name, updatedBy = Some(fromDomain(payload.updatedBy)))
      )
    ).map(_ => Done)

  override def assignCategory(payload: AssignCategoryPayload): Future[Done] =
    call(
      client.assignCategory(
        g.AssignCategoryPayload(
          itemId = payload.itemId,
          categoryId = payload.categoryId,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def updateSource(payload: UpdateSourcePayload): Future[Done] =
    call(
      client.updateSource(
        g.UpdateSourcePayload(itemId = payload.itemId, source = payload.source, updatedBy = Some(fromDomain(payload.updatedBy)))
      )
    ).map(_ => Done)

  override def updateExternalId(payload: UpdateExternalIdPayload): Future[Done] =
    call(
      client.updateExternalId(
        g.UpdateExternalIdPayload(
          itemId = payload.itemId,
          externalId = payload.externalId,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def moveItem(payload: MoveItemPayload): Future[Done] =
    call(
      client.moveItem(
        g.MoveItemPayload(
          itemId = payload.itemId,
          newParentId = payload.newParentId,
          order = payload.order,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def assignChief(payload: AssignChiefPayload): Future[Done] =
    call(
      client.assignChief(
        g.AssignChiefPayload(unitId = payload.unitId, chiefId = payload.chiefId, updatedBy = Some(fromDomain(payload.updatedBy)))
      )
    ).map(_ => Done)

  override def unassignChief(payload: UnassignChiefPayload): Future[Done] =
    call(
      client.unassignChief(
        g.UnassignChiefPayload(unitId = payload.unitId, updatedBy = Some(fromDomain(payload.updatedBy)))
      )
    ).map(_ => Done)

  override def changePositionLimit(payload: ChangePositionLimitPayload): Future[Done] =
    call(
      client.changePositionLimit(
        g.ChangePositionLimitPayload(
          positionId = payload.positionId,
          limit = payload.limit,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def assignPerson(payload: AssignPersonPayload): Future[Done] =
    call(
      client.assignPerson(
        g.AssignPersonPayload(
          positionId = payload.positionId,
          personId = payload.personId,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def unassignPerson(payload: UnassignPersonPayload): Future[Done] =
    call(
      client.unassignPerson(
        g.UnassignPersonPayload(
          positionId = payload.positionId,
          personId = payload.personId,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def assignOrgRole(payload: AssignOrgRolePayload): Future[Done] =
    call(
      client.assignOrgRole(
        g.AssignOrgRolePayload(
          positionId = payload.positionId,
          orgRoleId = payload.orgRoleId,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def unassignOrgRole(payload: UnassignOrgRolePayload): Future[Done] =
    call(
      client.unassignOrgRole(
        g.UnassignOrgRolePayload(
          positionId = payload.positionId,
          orgRoleId = payload.orgRoleId,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def deleteOrgItem(payload: DeleteOrgItemPayload): Future[Done] =
    call(
      client.deleteOrgItem(
        g.DeleteOrgItemPayload(itemId = payload.itemId, deletedBy = Some(fromDomain(payload.deletedBy)))
      )
    ).map(_ => Done)

  // === Hierarchy queries ===
  // The large hierarchy types (Organization, OrganizationTree, OrgItem) travel as JSON
  // strings per D1 (sealed-trait OrgItem hierarchies); parsed back to domain here.

  override def getOrganization(orgId: CompositeOrgItemId): Future[Organization] =
    call(client.getOrganization(g.GetOrganizationRequest(orgId = orgId)))
      .map(r => Json.parse(r.organizationJson).as[Organization])

  override def getOrganizationTree(itemId: CompositeOrgItemId): Future[OrganizationTree] =
    call(client.getOrganizationTree(g.GetOrganizationTreeRequest(itemId = itemId)))
      .map(r => Json.parse(r.organizationTreeJson).as[OrganizationTree])

  override def getOrgItem(
    itemId: CompositeOrgItemId,
    source: Option[String],
    attributes: Option[String]
  ): Future[OrgItem] =
    call(client.getOrgItem(g.GetOrgItemRequest(itemId = itemId, source = source, attributes = attributes)))
      .map(r => Json.parse(r.orgItemJson).as[OrgItem])

  override def getOrgItems(
    ids: Set[CompositeOrgItemId],
    source: Option[String],
    attributes: Option[String]
  ): Future[Seq[OrgItem]] =
    call(client.getOrgItems(g.GetOrgItemsRequest(ids = ids.toSeq, source = source, attributes = attributes)))
      .map(_.orgItemsJson.map(json => Json.parse(json).as[OrgItem]))

  override def getItemIdsByExternalId(externalIds: Set[String]): Future[Map[String, CompositeOrgItemId]] =
    call(client.getItemIdsByExternalId(g.GetItemIdsByExternalIdRequest(externalIds = externalIds.toSeq)))
      .map(_.itemIds)

  override def getPersonPrincipals(personId: PersonId): Future[Set[AnnettePrincipal]] =
    call(client.getPersonPrincipals(g.GetPersonPrincipalsRequest(personId = personId)))
      .map(_.principals.map(p => AnnettePrincipal(p.code)).toSet)

  override def getPersonPositions(personId: PersonId): Future[Set[PersonPosition]] =
    call(client.getPersonPositions(g.GetPersonPositionsRequest(personId = personId)))
      .map(
        _.positions.map(pp => PersonPosition(personId = pp.personId, positionId = pp.positionId)).toSet
      )

  override def findOrgItems(query: OrgItemFindQuery): Future[FindResult] =
    call(
      client.findOrgItems(
        g.OrgItemFindQuery(
          offset = query.offset,
          size = query.size,
          filter = query.filter,
          name = query.name,
          orgUnits = query.orgUnits.map(_.toSeq).getOrElse(Seq.empty),
          persons = query.persons.map(_.toSeq).getOrElse(Seq.empty),
          orgRoles = query.orgRoles.map(_.toSeq).getOrElse(Seq.empty),
          fromLevel = query.fromLevel,
          toLevel = query.toLevel,
          itemTypes = query.itemTypes.map(_.map(_.toString).toSeq).getOrElse(Seq.empty),
          organizations = query.organizations.map(_.toSeq).getOrElse(Seq.empty),
          parents = query.parents.map(_.toSeq).getOrElse(Seq.empty),
          chiefs = query.chiefs.map(_.toSeq).getOrElse(Seq.empty),
          categories = query.categories.map(_.toSeq).getOrElse(Seq.empty),
          sources = query.sources.map(_.toSeq).getOrElse(Seq.empty),
          externalIds = query.externalIds.map(_.toSeq).getOrElse(Seq.empty),
          sortBy = sortByToProto(query.sortBy)
        )
      )
    ).map(findResultFromProto)

  // === Attribute methods ===

  override def getOrgItemMetadata: Future[Map[String, AttributeMetadata]] =
    call(client.getOrgItemMetadata(Empty()))
      .map(_.metadataJson.map { case (k, v) => k -> Json.parse(v).as[AttributeMetadata] })

  override def updateOrgItemAttributes(payload: UpdateAttributesPayload): Future[Done] =
    call(
      client.updateOrgItemAttributes(
        g.UpdateAttributesPayload(
          id = payload.id,
          attributes = payload.attributes,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def getOrgItemAttributes(
    id: CompositeOrgItemId,
    source: Option[String],
    attributes: Option[String]
  ): Future[Map[String, String]] =
    call(client.getOrgItemAttributes(g.GetOrgItemAttributesRequest(id = id, source = source, attributes = attributes)))
      .map(_.values)

  override def getOrgItemsAttributes(
    ids: Set[CompositeOrgItemId],
    source: Option[String],
    attributes: Option[String]
  ): Future[Map[String, Map[String, String]]] =
    call(
      client.getOrgItemsAttributes(
        g.GetOrgItemsAttributesRequest(ids = ids.toSeq, source = source, attributes = attributes)
      )
    ).map(_.values.map { case (k, v) => k -> v.values })

  // === OrgRole methods ===

  private def toDomain(r: g.OrgRole): OrgRole =
    OrgRole(
      id = r.id,
      name = r.name,
      description = r.description,
      updatedAt = OffsetDateTime.parse(r.updatedAt),
      updatedBy = unwrapPrincipal(r.updatedBy)
    )

  override def createOrgRole(payload: CreateOrgRolePayload): Future[Done] =
    call(
      client.createOrgRole(
        g.CreateOrgRolePayload(
          id = payload.id,
          name = payload.name,
          description = payload.description,
          createdBy = Some(fromDomain(payload.createdBy))
        )
      )
    ).map(_ => Done)

  override def createOrUpdateOrgRole(payload: CreateOrgRolePayload): Future[Done] =
    createOrgRole(payload).recoverWith {
      case OrgRoleAlreadyExist(_) =>
        updateOrgRole(
          UpdateOrgRolePayload(
            id = payload.id,
            name = payload.name,
            description = payload.description,
            updatedBy = payload.createdBy
          )
        )
    }

  override def updateOrgRole(payload: UpdateOrgRolePayload): Future[Done] =
    call(
      client.updateOrgRole(
        g.UpdateOrgRolePayload(
          id = payload.id,
          name = payload.name,
          description = payload.description,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def deleteOrgRole(payload: DeleteOrgRolePayload): Future[Done] =
    call(
      client.deleteOrgRole(g.DeleteOrgRolePayload(id = payload.id, updatedBy = Some(fromDomain(payload.updatedBy))))
    ).map(_ => Done)

  override def getOrgRole(id: OrgRoleId, source: Option[String]): Future[OrgRole] =
    call(client.getOrgRole(g.GetOrgRoleRequest(id = id, source = source))).map(toDomain)

  override def getOrgRoles(ids: Set[OrgRoleId], source: Option[String]): Future[Seq[OrgRole]] =
    call(client.getOrgRoles(g.GetOrgRolesRequest(ids = ids.toSeq, source = source)))
      .map(_.orgRoles.map(toDomain))

  override def findOrgRoles(query: OrgRoleFindQuery): Future[FindResult] =
    call(
      client.findOrgRoles(
        g.OrgRoleFindQuery(
          offset = query.offset,
          size = query.size,
          filter = query.filter,
          name = query.name,
          description = query.description,
          sortBy = sortByToProto(query.sortBy)
        )
      )
    ).map(findResultFromProto)

  // === Category methods ===

  private def toDomain(c: g.OrgCategory): OrgCategory =
    OrgCategory(
      id = c.id,
      name = c.name,
      forOrganization = c.forOrganization,
      forUnit = c.forUnit,
      forPosition = c.forPosition,
      updatedAt = OffsetDateTime.parse(c.updatedAt),
      updatedBy = unwrapPrincipal(c.updatedBy)
    )

  override def createCategory(payload: CreateCategoryPayload): Future[Done] =
    call(
      client.createCategory(
        g.CreateCategoryPayload(
          id = payload.id,
          name = payload.name,
          forOrganization = payload.forOrganization,
          forUnit = payload.forUnit,
          forPosition = payload.forPosition,
          createdBy = Some(fromDomain(payload.createdBy))
        )
      )
    ).map(_ => Done)

  override def createOrUpdateCategory(payload: CreateCategoryPayload): Future[Done] =
    createCategory(payload).recoverWith {
      case OrgCategoryAlreadyExist(_) =>
        updateCategory(
          UpdateCategoryPayload(
            id = payload.id,
            name = payload.name,
            forOrganization = payload.forOrganization,
            forUnit = payload.forUnit,
            forPosition = payload.forPosition,
            updatedBy = payload.createdBy
          )
        )
    }

  override def updateCategory(payload: UpdateCategoryPayload): Future[Done] =
    call(
      client.updateCategory(
        g.UpdateCategoryPayload(
          id = payload.id,
          name = payload.name,
          forOrganization = payload.forOrganization,
          forUnit = payload.forUnit,
          forPosition = payload.forPosition,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def deleteCategory(payload: DeleteCategoryPayload): Future[Done] =
    call(
      client.deleteCategory(g.DeleteCategoryPayload(id = payload.id, updatedBy = Some(fromDomain(payload.updatedBy))))
    ).map(_ => Done)

  override def getCategory(id: OrgCategoryId, source: Option[String]): Future[OrgCategory] =
    call(client.getCategory(g.GetCategoryRequest(id = id, source = source))).map(toDomain)

  override def getCategories(ids: Set[OrgCategoryId], source: Option[String]): Future[Seq[OrgCategory]] =
    call(client.getCategories(g.GetCategoriesRequest(ids = ids.toSeq, source = source)))
      .map(_.categories.map(toDomain))

  override def findCategories(query: OrgCategoryFindQuery): Future[FindResult] =
    call(
      client.findCategories(
        g.OrgCategoryFindQuery(
          offset = query.offset,
          size = query.size,
          filter = query.filter,
          name = query.name,
          forOrganization = query.forOrganization,
          forUnit = query.forUnit,
          forPosition = query.forPosition,
          sortBy = sortByToProto(query.sortBy)
        )
      )
    ).map(findResultFromProto)
}
