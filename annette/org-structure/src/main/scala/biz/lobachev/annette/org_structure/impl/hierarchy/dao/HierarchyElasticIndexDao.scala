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
import biz.lobachev.annette.attributes.api.query.AttributeElastic
import biz.lobachev.annette.core.model.elastic.FindResult
import biz.lobachev.annette.microservice_core.elastic.{AbstractElasticIndexDao, ElasticSettings}
import biz.lobachev.annette.org_structure.api.hierarchy
import biz.lobachev.annette.org_structure.api.hierarchy.{CompositeOrgItemId, ItemTypes, OrgItemFindQuery, OrgItemKey}
import biz.lobachev.annette.org_structure.api.role.OrgRoleId
import biz.lobachev.annette.org_structure.impl.hierarchy.entity.HierarchyEntity
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.requests.common.RefreshPolicy
import com.sksamuel.elastic4s.requests.indexes.CreateIndexRequest
import com.sksamuel.elastic4s.requests.searches.sort.FieldSort
import org.slf4j.LoggerFactory

import java.time.OffsetDateTime
import scala.collection.immutable.Seq
import scala.concurrent.{ExecutionContext, Future}

class HierarchyElasticIndexDao(elasticSettings: ElasticSettings, elasticClient: ElasticClient)(implicit
  override val ec: ExecutionContext,
  materializer: Materializer
) extends AbstractElasticIndexDao(elasticSettings, elasticClient)
    with AttributeElastic {

  override val log = LoggerFactory.getLogger(this.getClass)

  override def indexSuffix = "org-structure-items"

  def createIndexRequest: CreateIndexRequest =
    createIndex(indexName)
      .mapping(
        properties(
          keywordField("id"),
          keywordField("orgId"),
          keywordField("parentId"),
          keywordField("rootPath"),
          textField("name").fielddata(true),
          keywordField("type"),
          keywordField("children"),
          keywordField("chief"),
          keywordField("categoryId"),
          intField("limit"),
          keywordField("persons"),
          keywordField("orgRoles"),
          intField("level"),
          keywordField("source"),
          keywordField("externalId"),
          dateField("updatedAt")
        )
      )

  def createOrganization(event: HierarchyEntity.OrganizationCreated): Future[Unit] =
    elasticClient.execute {
      indexInto(indexName)
        .id(event.orgId)
        .fields(
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
        .refresh(RefreshPolicy.Immediate)
    }.map(processResponse("createOrganization", event.orgId)(_))

  def deleteOrganization(event: HierarchyEntity.OrganizationDeleted): Future[Unit] =
    elasticClient
      .execute(deleteById(indexName, event.orgId))
      .map(processResponse("deleteOrganization", event.orgId)(_))

  def createUnit(event: HierarchyEntity.UnitCreated): Future[Unit] =
    elasticClient.execute {
      indexInto(indexName)
        .id(event.unitId)
        .fields(
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
        .refresh(RefreshPolicy.Immediate)
    }.map(processResponse("createUnit", event.unitId)(_))

  def deleteUnit(event: HierarchyEntity.UnitDeleted): Future[Unit] =
    elasticClient
      .execute(deleteById(indexName, event.unitId))
      .map(processResponse("deleteUnit", event.unitId)(_))

  def assignCategory(event: HierarchyEntity.CategoryAssigned): Future[Unit] =
    elasticClient.execute {
      updateById(indexName, event.itemId)
        .doc("categoryId" -> event.categoryId)
        .refresh(RefreshPolicy.Immediate)
    }.map(processResponse("assignCategory", event.itemId)(_))

  def assignChief(event: HierarchyEntity.ChiefAssigned): Future[Unit] =
    elasticClient.execute {
      updateById(indexName, event.unitId)
        .doc("chief" -> event.chiefId)
        .refresh(RefreshPolicy.Immediate)
    }.map(processResponse("assignChief", event.unitId)(_))

  def unassignChief(event: HierarchyEntity.ChiefUnassigned): Future[Unit] =
    elasticClient.execute {
      updateById(indexName, event.unitId)
        .doc("chief" -> null)
        .refresh(RefreshPolicy.Immediate)
    }.map(processResponse("unassignChief", event.unitId)(_))

  def createPosition(event: HierarchyEntity.PositionCreated): Future[Unit] =
    elasticClient.execute {
      indexInto(indexName)
        .id(event.positionId)
        .fields(
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
        .refresh(RefreshPolicy.Immediate)
    }.map(processResponse("createPosition", event.positionId)(_))

  def deletePosition(event: HierarchyEntity.PositionDeleted): Future[Unit] =
    elasticClient
      .execute(deleteById(indexName, event.positionId))
      .map(processResponse("deletePosition", event.positionId)(_))

  def updateName(event: HierarchyEntity.NameUpdated): Future[Unit] =
    elasticClient.execute {
      updateById(indexName, event.itemId)
        .doc(
          "name"      -> event.name,
          "updatedAt" -> event.updatedAt
        )
        .refresh(RefreshPolicy.Immediate)
    }.map(processResponse("updateName", event.itemId)(_))

  def updateSource(event: HierarchyEntity.SourceUpdated) =
    elasticClient.execute {
      updateById(indexName, event.itemId)
        .doc(
          "source"    -> event.source,
          "updatedAt" -> event.updatedAt
        )
        .refresh(RefreshPolicy.Immediate)
    }.map(processResponse("updateSource", event.itemId)(_))

  def updateExternalId(event: HierarchyEntity.ExternalIdUpdated) =
    elasticClient.execute {
      updateById(indexName, event.itemId)
        .doc(
          "externalId" -> event.externalId,
          "updatedAt"  -> event.updatedAt
        )
        .refresh(RefreshPolicy.Immediate)
    }.map(processResponse("updateExternalId", event.itemId)(_))

  def changePositionLimit(event: HierarchyEntity.PositionLimitChanged): Future[Unit] =
    elasticClient.execute {
      updateById(indexName, event.positionId)
        .doc("limit" -> event.limit, "updatedAt" -> event.updatedAt)
        .refresh(RefreshPolicy.Immediate)
    }.map(processResponse("changePositionLimit", event.positionId)(_))

  def updatePersons(
    positionId: CompositeOrgItemId,
    persons: Set[CompositeOrgItemId],
    updatedAt: OffsetDateTime
  ): Future[Unit] =
    elasticClient.execute {
      updateById(indexName, positionId)
        .doc("persons" -> persons, "updatedAt" -> updatedAt)
        .refresh(RefreshPolicy.Immediate)
    }.map(processResponse("updatePersons", positionId)(_))

  def updateRoles(
    positionId: CompositeOrgItemId,
    roles: Set[OrgRoleId],
    updatedAt: OffsetDateTime
  ): Future[Unit] =
    elasticClient.execute {
      updateById(indexName, positionId)
        .doc("orgRoles" -> roles, "updatedAt" -> updatedAt)
        .refresh(RefreshPolicy.Immediate)
    }.map(processResponse("updateRoles", positionId)(_))

  def updateChildren(
    itemId: CompositeOrgItemId,
    children: Seq[CompositeOrgItemId],
    updatedAt: OffsetDateTime
  ): Future[Unit] =
    elasticClient.execute {
      updateById(indexName, itemId)
        .doc("children" -> children, "updatedAt" -> updatedAt)
        .refresh(RefreshPolicy.Immediate)
    }.map(processResponse("updateChildren", itemId)(_))

  def updateRootPaths(
    rootPaths: Map[CompositeOrgItemId, Seq[CompositeOrgItemId]],
    updatedAt: OffsetDateTime
  ): Future[Unit] =
    Source(rootPaths.toSeq)
      .mapAsync(1) {
        case (itemId, rootPath) =>
          elasticClient.execute {
            updateById(indexName, itemId)
              .doc("rootPath" -> rootPath, "updatedAt" -> updatedAt)
              .refresh(RefreshPolicy.Immediate)
          }.map(processResponse("updateRootPaths", itemId)(_))
      }
      .runWith(Sink.ignore)
      .map(_ => ())

  // *************************** Search API ***************************

  def findOrgItem(query: OrgItemFindQuery): Future[FindResult] = {

    val filterQuery        = buildFilterQuery(query.filter, Seq("name" -> 3.0))
    val fieldQuery         = query.name.map(matchQuery("name", _)).toSeq
    val orgUnitsQuery      = query.orgUnits.map(units => termsSetQuery("rootPath", units, script("1"))).toSeq
    val personsQuery       = query.persons.map(persons => termsSetQuery("persons", persons, script("1"))).toSeq
    val orgRolesQuery      = query.orgRoles.map(roles => termsSetQuery("orgRoles", roles, script("1"))).toSeq
    val fromLevelQuery     = query.fromLevel.map(level => rangeQuery("level").gte(level.toLong)).toSeq
    val toLevelQuery       = query.toLevel.map(level => rangeQuery("level").lte(level.toLong)).toSeq
    val itemTypesQuery     =
      query.itemTypes.map(itemTypes => termsSetQuery("type", itemTypes.map(_.toString), script("1"))).toSeq
    val organizationsQuery = query.organizations.map(orgs => termsSetQuery("orgId", orgs, script("1"))).toSeq
    val parentsQuery       = query.parents.map(parents => termsSetQuery("parentId", parents, script("1"))).toSeq
    val chiefsQuery        = query.chiefs.map(chiefs => termsSetQuery("chief", chiefs, script("1"))).toSeq
    val categoryQuery      = query.categories.map(categories => termsSetQuery("categoryId", categories, script("1"))).toSeq
    val sourceQuery        = query.sources.map(sources => termsSetQuery("source", sources, script("1"))).toSeq
    val externalIdQuery    =
      query.externalIds.map(externalIds => termsSetQuery("externalId", externalIds, script("1"))).toSeq
    val attributeQuery     = buildAttributeQuery(query.attributes)

    val sortBy: Seq[FieldSort] = buildSortBySeq(query.sortBy)

    val searchRequest = search(indexName)
      .bool(
        must(
          filterQuery ++ fieldQuery ++ fieldQuery ++ orgUnitsQuery ++
            personsQuery ++ orgRolesQuery ++ fromLevelQuery ++ toLevelQuery ++
            itemTypesQuery ++ organizationsQuery ++ parentsQuery ++ chiefsQuery ++ categoryQuery ++
            sourceQuery ++ externalIdQuery ++ attributeQuery
        )
      )
      .from(query.offset)
      .size(query.size)
      .sortBy(sortBy)
      .sourceInclude("updatedAt")
      .trackTotalHits(true)

    findEntity(searchRequest)

  }

}
