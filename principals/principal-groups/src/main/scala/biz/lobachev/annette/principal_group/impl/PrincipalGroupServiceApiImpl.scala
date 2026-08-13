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

package biz.lobachev.annette.principal_group.impl

import java.time.OffsetDateTime
import com.google.protobuf.empty.Empty
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.category.{
  Category,
  CategoryFindQuery,
  CreateCategoryPayload,
  DeleteCategoryPayload,
  UpdateCategoryPayload
}
import biz.lobachev.annette.core.model.indexing.SortBy
import biz.lobachev.annette.principal_group.api.{grpc => g}
import biz.lobachev.annette.principal_group.api.grpc.PrincipalGroupService
import biz.lobachev.annette.principal_group.api.group._
import biz.lobachev.annette.principal_group.impl.category.CategoryEntityService
import biz.lobachev.annette.principal_group.impl.group.PrincipalGroupEntityService

import scala.concurrent.{ExecutionContext, Future}

class PrincipalGroupServiceApiImpl(
  groupEntityService: PrincipalGroupEntityService,
  categoryEntityService: CategoryEntityService
)(implicit ec: ExecutionContext) extends PrincipalGroupService {

  private def toDomain(p: g.AnnettePrincipal): AnnettePrincipal =
    AnnettePrincipal(p.code)

  private def fromDomain(p: AnnettePrincipal): g.AnnettePrincipal =
    g.AnnettePrincipal(p.code)

  private def unwrapPrincipal(p: Option[g.AnnettePrincipal]): AnnettePrincipal =
    p.map(toDomain).getOrElse(throw new IllegalArgumentException("missing principal"))

  private def formatOdt(odt: OffsetDateTime): String = odt.toString

  private def fromDomain(p: PrincipalGroup): g.PrincipalGroup =
    g.PrincipalGroup(
      id = p.id,
      name = p.name,
      description = p.description,
      categoryId = p.categoryId,
      updatedAt = p.updatedAt.toString,
      updatedBy = Some(fromDomain(p.updatedBy))
    )

  private def fromDomain(c: Category): g.Category =
    g.Category(
      id = c.id,
      name = c.name,
      updatedAt = c.updatedAt.toString,
      updatedBy = Some(fromDomain(c.updatedBy))
    )

  // === Principal group CRUD ===

  override def createPrincipalGroup(in: g.CreatePrincipalGroupPayload): Future[Empty] =
    groupEntityService
      .createPrincipalGroup(
        CreatePrincipalGroupPayload(
          id = in.id,
          name = in.name,
          description = in.description,
          categoryId = in.categoryId,
          createdBy = unwrapPrincipal(in.createdBy)
        )
      )
      .map(_ => Empty())

  override def updatePrincipalGroup(in: g.UpdatePrincipalGroupPayload): Future[Empty] =
    groupEntityService
      .updatePrincipalGroup(
        UpdatePrincipalGroupPayload(
          id = in.id,
          name = in.name,
          description = in.description,
          categoryId = in.categoryId,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def updatePrincipalGroupName(in: g.UpdatePrincipalGroupNamePayload): Future[Empty] =
    groupEntityService
      .updatePrincipalGroupName(
        UpdatePrincipalGroupNamePayload(
          id = in.id,
          name = in.name,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def updatePrincipalGroupDescription(in: g.UpdatePrincipalGroupDescriptionPayload): Future[Empty] =
    groupEntityService
      .updatePrincipalGroupDescription(
        UpdatePrincipalGroupDescriptionPayload(
          id = in.id,
          description = in.description,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def updatePrincipalGroupCategory(in: g.UpdatePrincipalGroupCategoryPayload): Future[Empty] =
    groupEntityService
      .updatePrincipalGroupCategory(
        UpdatePrincipalGroupCategoryPayload(
          id = in.id,
          categoryId = in.categoryId,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def deletePrincipalGroup(in: g.DeletePrincipalGroupPayload): Future[Empty] =
    groupEntityService
      .deletePrincipalGroup(
        DeletePrincipalGroupPayload(id = in.id, updatedBy = unwrapPrincipal(in.updatedBy))
      )
      .map(_ => Empty())

  override def assignPrincipal(in: g.AssignPrincipalPayload): Future[Empty] =
    groupEntityService
      .assignPrincipal(
        AssignPrincipalPayload(
          id = in.id,
          principal = unwrapPrincipal(in.principal),
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def unassignPrincipal(in: g.UnassignPrincipalPayload): Future[Empty] =
    groupEntityService
      .unassignPrincipal(
        UnassignPrincipalPayload(
          id = in.id,
          principal = unwrapPrincipal(in.principal),
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def getPrincipalGroup(in: g.GetPrincipalGroupRequest): Future[g.PrincipalGroup] =
    groupEntityService.getPrincipalGroup(in.id, in.source).map(fromDomain)

  override def getPrincipalGroups(in: g.GetPrincipalGroupsRequest): Future[g.GetPrincipalGroupsResponse] =
    groupEntityService
      .getPrincipalGroups(in.ids.toSet, in.source)
      .map(groups => g.GetPrincipalGroupsResponse(groups = groups.map(fromDomain)))

  override def findPrincipalGroups(in: g.PrincipalGroupFindQuery): Future[g.FindResult] =
    groupEntityService
      .findPrincipalGroups(
        PrincipalGroupFindQuery(
          offset = in.offset,
          size = in.size,
          filter = in.filter,
          categories = if (in.categories.isEmpty) None else Some(in.categories.toSet),
          sortBy = Some(in.sortBy.map(s => SortBy(field = s.field, descending = s.descending)).toSeq).filter(_.nonEmpty)
        )
      )
      .map(fromDomain)

  override def getAssignments(in: g.GetAssignmentsRequest): Future[g.GetAssignmentsResponse] =
    groupEntityService
      .getAssignments(in.id)
      .map(principals => g.GetAssignmentsResponse(principals = principals.map(fromDomain).toSeq))

  override def getPrincipalAssignments(in: g.GetPrincipalAssignmentsRequest): Future[g.GetPrincipalAssignmentsResponse] =
    groupEntityService
      .getPrincipalAssignments(in.principals.map(toDomain).toSet)
      .map(ids => g.GetPrincipalAssignmentsResponse(groupIds = ids.toSeq))

  // === Category CRUD ===

  override def createCategory(in: g.CreateCategoryPayload): Future[Empty] =
    categoryEntityService
      .createCategory(
        CreateCategoryPayload(id = in.id, name = in.name, createdBy = unwrapPrincipal(in.createdBy))
      )
      .map(_ => Empty())

  override def updateCategory(in: g.UpdateCategoryPayload): Future[Empty] =
    categoryEntityService
      .updateCategory(
        UpdateCategoryPayload(id = in.id, name = in.name, updatedBy = unwrapPrincipal(in.updatedBy))
      )
      .map(_ => Empty())

  override def deleteCategory(in: g.DeleteCategoryPayload): Future[Empty] =
    categoryEntityService
      .deleteCategory(DeleteCategoryPayload(id = in.id, deletedBy = unwrapPrincipal(in.deletedBy)))
      .map(_ => Empty())

  override def getCategory(in: g.GetCategoryRequest): Future[g.Category] =
    categoryEntityService.getCategory(in.id, in.source).map(fromDomain)

  override def getCategories(in: g.GetCategoriesRequest): Future[g.GetCategoriesResponse] =
    categoryEntityService
      .getCategories(in.ids.toSet, in.source)
      .map(categories => g.GetCategoriesResponse(categories = categories.map(fromDomain)))

  override def findCategories(in: g.CategoryFindQuery): Future[g.FindResult] =
    categoryEntityService
      .findCategories(
        CategoryFindQuery(
          offset = in.offset,
          size = in.size,
          filter = in.filter,
          name = in.name,
          sortBy = Some(in.sortBy.map(s => SortBy(field = s.field, descending = s.descending)).toSeq).filter(_.nonEmpty)
        )
      )
      .map(fromDomain)

  // === domain → proto (response converters) ===

  private def fromDomain(f: biz.lobachev.annette.core.model.indexing.FindResult): g.FindResult =
    g.FindResult(
      total = f.total,
      hits = f.hits.map(h => g.HitResult(id = h.id, score = h.score, updatedAt = formatOdt(h.updatedAt)))
    )
}
