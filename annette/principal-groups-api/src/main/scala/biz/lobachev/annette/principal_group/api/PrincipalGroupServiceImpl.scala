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

import akka.Done
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.elastic.FindResult
import biz.lobachev.annette.principal_group.api.group._
import biz.lobachev.annette.core.model.category._
import io.scalaland.chimney.dsl._

import scala.concurrent.{ExecutionContext, Future}

class PrincipalGroupServiceImpl(api: PrincipalGroupServiceApi, implicit val ec: ExecutionContext)
    extends PrincipalGroupService {

  override def createPrincipalGroup(payload: CreatePrincipalGroupPayload): Future[Done] =
    api.createPrincipalGroup.invoke(payload)

  override def updatePrincipalGroupName(payload: UpdatePrincipalGroupNamePayload): Future[Done] =
    api.updatePrincipalGroupName.invoke(payload)

  override def updatePrincipalGroupDescription(payload: UpdatePrincipalGroupDescriptionPayload): Future[Done] =
    api.updatePrincipalGroupDescription.invoke(payload)

  override def updatePrincipalGroupCategory(payload: UpdatePrincipalGroupCategoryPayload): Future[Done] =
    api.updatePrincipalGroupCategory.invoke(payload)

  override def deletePrincipalGroup(payload: DeletePrincipalGroupPayload): Future[Done] =
    api.deletePrincipalGroup.invoke(payload)

  override def assignPrincipal(payload: AssignPrincipalPayload): Future[Done] =
    api.assignPrincipal.invoke(payload)

  override def unassignPrincipal(payload: UnassignPrincipalPayload): Future[Done] =
    api.unassignPrincipal.invoke(payload)

  override def getPrincipalGroupById(id: PrincipalGroupId, fromReadSide: Boolean): Future[PrincipalGroup] =
    api.getPrincipalGroupById(id, fromReadSide).invoke()

  override def getPrincipalGroupsById(
    ids: Set[PrincipalGroupId],
    fromReadSide: Boolean
  ): Future[Map[PrincipalGroupId, PrincipalGroup]] =
    api.getPrincipalGroupsById(fromReadSide).invoke(ids)

  override def findPrincipalGroups(query: PrincipalGroupFindQuery): Future[FindResult] =
    api.findPrincipalGroups.invoke(query)

  override def getAssignments(id: PrincipalGroupId): Future[Set[AnnettePrincipal]] =
    api.getAssignments(id).invoke()

  // org category methods

  def createCategory(payload: CreateCategoryPayload): Future[Done] =
    api.createCategory.invoke(payload)

  def updateCategory(payload: UpdateCategoryPayload): Future[Done] =
    api.updateCategory.invoke(payload)

  def createOrUpdateCategory(payload: CreateCategoryPayload): Future[Done] =
    createCategory(payload).recoverWith {
      case CategoryAlreadyExist(_) =>
        val updatePayload = payload
          .into[UpdateCategoryPayload]
          .withFieldComputed(_.updatedBy, _.createdBy)
          .transform
        updateCategory(updatePayload)
      case th                      => Future.failed(th)
    }

  def deleteCategory(payload: DeleteCategoryPayload): Future[Done] =
    api.deleteCategory.invoke(payload)

  def getCategoryById(id: CategoryId, fromReadSide: Boolean): Future[Category] =
    api.getCategoryById(id, fromReadSide).invoke()

  def getCategoriesById(ids: Set[CategoryId], fromReadSide: Boolean): Future[Seq[Category]] =
    api.getCategoriesById(fromReadSide).invoke(ids)

  def findCategories(query: CategoryFindQuery): Future[FindResult] =
    api.findCategories.invoke(query)

}
