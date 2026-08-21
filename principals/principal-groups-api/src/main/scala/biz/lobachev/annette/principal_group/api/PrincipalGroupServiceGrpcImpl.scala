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

package biz.lobachev.annette.principal_group.api

import java.time.OffsetDateTime
import biz.lobachev.annette.core.exception.AnnetteGrpcExceptionMapping
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.category._
import biz.lobachev.annette.core.model.indexing.{FindResult, HitResult, SortBy}
import biz.lobachev.annette.principal_group.api.group._
import biz.lobachev.annette.principal_group.api.{grpc => g}
import org.apache.pekko.Done

import scala.concurrent.{ExecutionContext, Future}

class PrincipalGroupServiceGrpcImpl(client: g.PrincipalGroupServiceClient)(implicit val ec: ExecutionContext)
    extends PrincipalGroupService {

  private def unwrapPrincipal(p: Option[g.AnnettePrincipal]): AnnettePrincipal =
    p.map(pr => AnnettePrincipal(pr.code)).getOrElse(throw new IllegalArgumentException("missing principal"))

  private def fromDomain(p: AnnettePrincipal): g.AnnettePrincipal = g.AnnettePrincipal(p.code)

  private def toDomain(p: g.AnnettePrincipal): AnnettePrincipal = AnnettePrincipal(p.code)

  private def sortByToProto(sortBy: Option[Seq[SortBy]]): Seq[g.SortBy] =
    sortBy.map(_.map(s => g.SortBy(field = s.field, descending = s.descending))).getOrElse(Seq.empty)

  private def findResultFromProto(f: g.FindResult): FindResult =
    FindResult(
      total = f.total,
      hits = f.hits.map(h => HitResult(id = h.id, score = h.score, updatedAt = OffsetDateTime.parse(h.updatedAt)))
    )

  private def toDomain(x: g.PrincipalGroup): PrincipalGroup =
    PrincipalGroup(
      id = x.id,
      name = x.name,
      description = x.description,
      categoryId = x.categoryId,
      updatedAt = OffsetDateTime.parse(x.updatedAt),
      updatedBy = unwrapPrincipal(x.updatedBy)
    )

  private def toDomain(c: g.Category): Category =
    Category(
      id = c.id,
      name = c.name,
      updatedAt = OffsetDateTime.parse(c.updatedAt),
      updatedBy = unwrapPrincipal(c.updatedBy)
    )

  private def call[T](f: Future[T]): Future[T] = AnnetteGrpcExceptionMapping.recoverAnnette(f)

  override def createPrincipalGroup(payload: CreatePrincipalGroupPayload): Future[Done] =
    call(
      client.createPrincipalGroup(
        g.CreatePrincipalGroupPayload(
          id = payload.id,
          name = payload.name,
          description = payload.description,
          categoryId = payload.categoryId,
          createdBy = Some(fromDomain(payload.createdBy))
        )
      )
    ).map(_ => Done)

  override def updatePrincipalGroup(payload: UpdatePrincipalGroupPayload): Future[Done] =
    call(
      client.updatePrincipalGroup(
        g.UpdatePrincipalGroupPayload(
          id = payload.id,
          name = payload.name,
          description = payload.description,
          categoryId = payload.categoryId,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def updatePrincipalGroupName(payload: UpdatePrincipalGroupNamePayload): Future[Done] =
    call(
      client.updatePrincipalGroupName(
        g.UpdatePrincipalGroupNamePayload(
          id = payload.id,
          name = payload.name,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def updatePrincipalGroupDescription(payload: UpdatePrincipalGroupDescriptionPayload): Future[Done] =
    call(
      client.updatePrincipalGroupDescription(
        g.UpdatePrincipalGroupDescriptionPayload(
          id = payload.id,
          description = payload.description,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def updatePrincipalGroupCategory(payload: UpdatePrincipalGroupCategoryPayload): Future[Done] =
    call(
      client.updatePrincipalGroupCategory(
        g.UpdatePrincipalGroupCategoryPayload(
          id = payload.id,
          categoryId = payload.categoryId,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def deletePrincipalGroup(payload: DeletePrincipalGroupPayload): Future[Done] =
    call(
      client.deletePrincipalGroup(
        g.DeletePrincipalGroupPayload(id = payload.id, updatedBy = Some(fromDomain(payload.updatedBy)))
      )
    ).map(_ => Done)

  override def assignPrincipal(payload: AssignPrincipalPayload): Future[Done] =
    call(
      client.assignPrincipal(
        g.AssignPrincipalPayload(
          id = payload.id,
          principal = Some(fromDomain(payload.principal)),
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def unassignPrincipal(payload: UnassignPrincipalPayload): Future[Done] =
    call(
      client.unassignPrincipal(
        g.UnassignPrincipalPayload(
          id = payload.id,
          principal = Some(fromDomain(payload.principal)),
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def getPrincipalGroup(id: PrincipalGroupId, source: Option[String]): Future[PrincipalGroup] =
    call(client.getPrincipalGroup(g.GetPrincipalGroupRequest(id = id, source = source))).map(toDomain)

  override def getPrincipalGroups(ids: Set[PrincipalGroupId], source: Option[String]): Future[Seq[PrincipalGroup]] =
    call(client.getPrincipalGroups(g.GetPrincipalGroupsRequest(ids = ids.toSeq, source = source)))
      .map(_.groups.map(toDomain))

  override def findPrincipalGroups(query: PrincipalGroupFindQuery): Future[FindResult] =
    call(
      client.findPrincipalGroups(
        g.PrincipalGroupFindQuery(
          offset = query.offset,
          size = query.size,
          filter = query.filter,
          categories = query.categories.map(_.toSeq).getOrElse(Seq.empty),
          sortBy = sortByToProto(query.sortBy)
        )
      )
    ).map(findResultFromProto)

  override def getAssignments(id: PrincipalGroupId): Future[Set[AnnettePrincipal]] =
    call(client.getAssignments(g.GetAssignmentsRequest(id = id)))
      .map(_.principals.map(toDomain).toSet)

  override def getPrincipalAssignments(principals: Set[AnnettePrincipal]): Future[Set[PrincipalGroupId]] =
    call(
      client.getPrincipalAssignments(
        g.GetPrincipalAssignmentsRequest(principals = principals.map(fromDomain).toSeq)
      )
    ).map(_.groupIds.toSet)

  // category methods

  override def createCategory(payload: CreateCategoryPayload): Future[Done] =
    call(
      client.createCategory(
        g.CreateCategoryPayload(id = payload.id, name = payload.name, createdBy = Some(fromDomain(payload.createdBy)))
      )
    ).map(_ => Done)

  override def createOrUpdateCategory(payload: CreateCategoryPayload): Future[Done] =
    createCategory(payload).recoverWith {
      case CategoryAlreadyExist(_) =>
        updateCategory(UpdateCategoryPayload(id = payload.id, name = payload.name, updatedBy = payload.createdBy))
    }

  override def updateCategory(payload: UpdateCategoryPayload): Future[Done] =
    call(
      client.updateCategory(
        g.UpdateCategoryPayload(id = payload.id, name = payload.name, updatedBy = Some(fromDomain(payload.updatedBy)))
      )
    ).map(_ => Done)

  override def deleteCategory(payload: DeleteCategoryPayload): Future[Done] =
    call(
      client.deleteCategory(
        g.DeleteCategoryPayload(id = payload.id, deletedBy = Some(fromDomain(payload.deletedBy)))
      )
    ).map(_ => Done)

  override def getCategory(id: CategoryId, source: Option[String]): Future[Category] =
    call(client.getCategory(g.GetCategoryRequest(id = id, source = source))).map(toDomain)

  override def getCategories(ids: Set[CategoryId], source: Option[String]): Future[Seq[Category]] =
    call(client.getCategories(g.GetCategoriesRequest(ids = ids.toSeq, source = source)))
      .map(_.categories.map(toDomain))

  override def findCategories(query: CategoryFindQuery): Future[FindResult] =
    call(
      client.findCategories(
        g.CategoryFindQuery(
          offset = query.offset,
          size = query.size,
          filter = query.filter,
          name = query.name,
          sortBy = sortByToProto(query.sortBy)
        )
      )
    ).map(findResultFromProto)
}
