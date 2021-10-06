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

import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import biz.lobachev.annette.core.model.indexing.FindResult
import biz.lobachev.annette.microservice_core.indexing.dao.AbstractIndexDao
import biz.lobachev.annette.org_structure.api.hierarchy
import biz.lobachev.annette.org_structure.api.hierarchy.{CompositeOrgItemId, ItemTypes, OrgItemFindQuery, OrgItemKey}
import biz.lobachev.annette.org_structure.api.role.OrgRoleId
import biz.lobachev.annette.org_structure.impl.hierarchy.entity.HierarchyEntity
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.requests.searches.sort.FieldSort
import org.slf4j.LoggerFactory

import java.time.OffsetDateTime
import scala.collection.immutable.Seq
import scala.concurrent.{ExecutionContext, Future}

class HierarchyIndexDao(client: ElasticClient)(implicit
  override val ec: ExecutionContext,
  materializer: Materializer
) extends AbstractIndexDao(client) {

  override val log = LoggerFactory.getLogger(this.getClass)

  override def indexConfigPath = "indexing.items-index"

  def createOrganization(event: HierarchyEntity.OrganizationCreated) =
    createIndexDoc(
      event.orgId,
      "id"         -> event.orgId,
      "orgId"      -> event.orgId,
      "parentId"   -> hierarchy.ROOT,
      "rootPath"   -> Seq(event.orgId),
      "name"       -> event.name,
      "type"       -> ItemTypes.Unit.toString,
      "children"   -> Seq.empty,
      "categoryId" -> event.categoryId,
      "level"      -> 0,
      "source"     -> event.source,
      "externalId" -> event.externalId,
      "updatedAt"  -> event.createdAt
    )

  def deleteOrganization(event: HierarchyEntity.OrganizationDeleted) =
    deleteIndexDoc(event.orgId)

  def createUnit(event: HierarchyEntity.UnitCreated) =
    createIndexDoc(
      event.unitId,
      "id"         -> event.unitId,
      "orgId"      -> OrgItemKey.extractOrgId(event.unitId),
      "parentId"   -> event.parentId,
      "rootPath"   -> event.rootPath,
      "name"       -> event.name,
      "type"       -> ItemTypes.Unit.toString,
      "children"   -> Seq.empty,
      "categoryId" -> event.categoryId,
      "level"      -> (event.rootPath.length - 1),
      "source"     -> event.source,
      "externalId" -> event.externalId,
      "updatedAt"  -> event.createdAt
    )

  def deleteUnit(event: HierarchyEntity.UnitDeleted) =
    deleteIndexDoc(event.unitId)

  def assignCategory(event: HierarchyEntity.CategoryAssigned) =
    updateIndexDoc(
      event.itemId,
      "categoryId" -> event.categoryId
    )

  def assignChief(event: HierarchyEntity.ChiefAssigned) =
    updateIndexDoc(
      event.unitId,
      "chief" -> event.chiefId
    )

  def unassignChief(event: HierarchyEntity.ChiefUnassigned) =
    updateIndexDoc(
      event.unitId,
      "chief" -> null
    )

  def createPosition(event: HierarchyEntity.PositionCreated) =
    createIndexDoc(
      event.positionId,
      "id"         -> event.positionId,
      "orgId"      -> OrgItemKey.extractOrgId(event.positionId),
      "parentId"   -> event.parentId,
      "rootPath"   -> event.rootPath,
      "name"       -> event.name,
      "type"       -> ItemTypes.Position.toString,
      "categoryId" -> event.categoryId,
      "limit"      -> event.limit,
      "level"      -> (event.rootPath.length - 1),
      "source"     -> event.source,
      "externalId" -> event.externalId,
      "updatedAt"  -> event.createdAt
    )

  def deletePosition(event: HierarchyEntity.PositionDeleted) =
    deleteIndexDoc(event.positionId)

  def updateName(event: HierarchyEntity.NameUpdated) =
    updateIndexDoc(
      event.itemId,
      "name"      -> event.name,
      "updatedAt" -> event.updatedAt
    )

  def updateSource(event: HierarchyEntity.SourceUpdated) =
    updateIndexDoc(
      event.itemId,
      "source"    -> event.source,
      "updatedAt" -> event.updatedAt
    )

  def updateExternalId(event: HierarchyEntity.ExternalIdUpdated) =
    updateIndexDoc(
      event.itemId,
      "externalId" -> event.externalId,
      "updatedAt"  -> event.updatedAt
    )

  def changePositionLimit(event: HierarchyEntity.PositionLimitChanged) =
    updateIndexDoc(
      event.positionId,
      "limit"     -> event.limit,
      "updatedAt" -> event.updatedAt
    )

  def updatePersons(
    positionId: CompositeOrgItemId,
    persons: Set[CompositeOrgItemId],
    updatedAt: OffsetDateTime
  ) =
    updateIndexDoc(
      positionId,
      "persons"   -> persons,
      "updatedAt" -> updatedAt
    )

  def updateRoles(
    positionId: CompositeOrgItemId,
    roles: Set[OrgRoleId],
    updatedAt: OffsetDateTime
  ) =
    updateIndexDoc(
      positionId,
      "orgRoles"  -> roles,
      "updatedAt" -> updatedAt
    )

  def updateChildren(
    itemId: CompositeOrgItemId,
    children: Seq[CompositeOrgItemId],
    updatedAt: OffsetDateTime
  ) =
    updateIndexDoc(
      itemId,
      "children"  -> children,
      "updatedAt" -> updatedAt
    )

  def updateRootPaths(
    rootPaths: Map[CompositeOrgItemId, Seq[CompositeOrgItemId]],
    updatedAt: OffsetDateTime
  ) =
    Source(rootPaths.toSeq)
      .mapAsync(1) {
        case (itemId, rootPath) =>
          updateIndexDoc(
            itemId,
            "rootPath"  -> rootPath,
            "updatedAt" -> updatedAt
          )
      }
      .runWith(Sink.ignore)
      .map(_ => ())

  // *************************** Search API ***************************

  def findOrgItem(query: OrgItemFindQuery): Future[FindResult] = {

    val filterQuery        = buildFilterQuery(query.filter, Seq("name" -> 3.0))
    val fieldQuery         = query.name.map(matchQuery(alias2FieldName("name"), _)).toSeq
    val orgUnitsQuery      =
      query.orgUnits.map(units => termsSetQuery(alias2FieldName("rootPath"), units, script("1"))).toSeq
    val personsQuery       =
      query.persons.map(persons => termsSetQuery(alias2FieldName("persons"), persons, script("1"))).toSeq
    val orgRolesQuery      =
      query.orgRoles.map(roles => termsSetQuery(alias2FieldName("orgRoles"), roles, script("1"))).toSeq
    val fromLevelQuery     = query.fromLevel.map(level => rangeQuery(alias2FieldName("level")).gte(level.toLong)).toSeq
    val toLevelQuery       = query.toLevel.map(level => rangeQuery(alias2FieldName("level")).lte(level.toLong)).toSeq
    val itemTypesQuery     =
      query.itemTypes
        .map(itemTypes => termsSetQuery(alias2FieldName("type"), itemTypes.map(_.toString), script("1")))
        .toSeq
    val organizationsQuery =
      query.organizations.map(orgs => termsSetQuery(alias2FieldName("orgId"), orgs, script("1"))).toSeq
    val parentsQuery       =
      query.parents.map(parents => termsSetQuery(alias2FieldName("parentId"), parents, script("1"))).toSeq
    val chiefsQuery        = query.chiefs.map(chiefs => termsSetQuery(alias2FieldName("chief"), chiefs, script("1"))).toSeq
    val categoryQuery      =
      query.categories.map(categories => termsSetQuery(alias2FieldName("categoryId"), categories, script("1"))).toSeq
    val sourceQuery        = query.sources.map(sources => termsSetQuery(alias2FieldName("source"), sources, script("1"))).toSeq
    val externalIdQuery    =
      query.externalIds.map(externalIds => termsSetQuery(alias2FieldName("externalId"), externalIds, script("1"))).toSeq

    val sortBy: Seq[FieldSort] = buildSortBySeq(query.sortBy)

    val searchRequest = search(indexName)
      .bool(
        must(
          filterQuery ++ fieldQuery ++ fieldQuery ++ orgUnitsQuery ++
            personsQuery ++ orgRolesQuery ++ fromLevelQuery ++ toLevelQuery ++
            itemTypesQuery ++ organizationsQuery ++ parentsQuery ++ chiefsQuery ++ categoryQuery ++
            sourceQuery ++ externalIdQuery
        )
      )
      .from(query.offset)
      .size(query.size)
      .sortBy(sortBy)
      .sourceInclude(alias2FieldName("updatedAt"))
      .trackTotalHits(true)

    findEntity(searchRequest)

  }

}
