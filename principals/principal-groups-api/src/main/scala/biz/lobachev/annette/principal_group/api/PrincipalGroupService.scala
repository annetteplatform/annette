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
import biz.lobachev.annette.core.model.indexing.FindResult
import biz.lobachev.annette.core.model.category._
import biz.lobachev.annette.principal_group.api.group._

import scala.concurrent.Future

trait PrincipalGroupService {

  def createPrincipalGroup(payload: CreatePrincipalGroupPayload): Future[Done]
  def updatePrincipalGroup(payload: UpdatePrincipalGroupPayload): Future[Done]
  def updatePrincipalGroupName(payload: UpdatePrincipalGroupNamePayload): Future[Done]
  def updatePrincipalGroupDescription(payload: UpdatePrincipalGroupDescriptionPayload): Future[Done]
  def updatePrincipalGroupCategory(payload: UpdatePrincipalGroupCategoryPayload): Future[Done]
  def deletePrincipalGroup(payload: DeletePrincipalGroupPayload): Future[Done]
  def assignPrincipal(payload: AssignPrincipalPayload): Future[Done]
  def unassignPrincipal(payload: UnassignPrincipalPayload): Future[Done]
  def getPrincipalGroup(id: PrincipalGroupId, source: Option[String]): Future[PrincipalGroup]
  def getPrincipalGroups(
    ids: Set[PrincipalGroupId],
    source: Option[String]
  ): Future[Seq[PrincipalGroup]]
  def findPrincipalGroups(query: PrincipalGroupFindQuery): Future[FindResult]
  def getAssignments(id: PrincipalGroupId): Future[Set[AnnettePrincipal]]
  def getPrincipalAssignments(principals: Set[AnnettePrincipal]): Future[Set[PrincipalGroupId]]

  // category methods

  def createCategory(payload: CreateCategoryPayload): Future[Done]
  def createOrUpdateCategory(payload: CreateCategoryPayload): Future[Done]
  def updateCategory(payload: UpdateCategoryPayload): Future[Done]
  def deleteCategory(payload: DeleteCategoryPayload): Future[Done]
  def getCategory(id: CategoryId, source: Option[String]): Future[Category]
  def getCategories(ids: Set[CategoryId], source: Option[String]): Future[Seq[Category]]
  def findCategories(query: CategoryFindQuery): Future[FindResult]
}
