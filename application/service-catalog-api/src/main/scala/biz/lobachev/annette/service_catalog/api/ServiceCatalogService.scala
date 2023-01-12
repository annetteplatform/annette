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

package biz.lobachev.annette.service_catalog.api

import akka.Done
import biz.lobachev.annette.core.model.category._
import biz.lobachev.annette.core.model.indexing.FindResult
import biz.lobachev.annette.service_catalog.api.user.{
  FindUserServicesQuery,
  ScopeByCategoryFindQuery,
  ScopeByCategoryFindResult,
  ScopeServicesQuery,
  ScopeServicesResult,
  UserServicesResult
}
import biz.lobachev.annette.service_catalog.api.scope._
import biz.lobachev.annette.service_catalog.api.scope_principal._
import biz.lobachev.annette.service_catalog.api.item._
import biz.lobachev.annette.service_catalog.api.service_principal._

import scala.concurrent.Future

trait ServiceCatalogService {

  def createCategory(payload: CreateCategoryPayload): Future[Done]
  def updateCategory(payload: UpdateCategoryPayload): Future[Done]
  def deleteCategory(payload: DeleteCategoryPayload): Future[Done]
  def getCategory(id: CategoryId, fromReadSide: Boolean): Future[Category]
  def getCategories(
    ids: Set[CategoryId],
    fromReadSide: Boolean
  ): Future[Seq[Category]]
  def findCategories(payload: CategoryFindQuery): Future[FindResult]

  def createScope(payload: CreateScopePayload): Future[Done]
  def updateScope(payload: UpdateScopePayload): Future[Done]
  def activateScope(payload: ActivateScopePayload): Future[Done]
  def deactivateScope(payload: DeactivateScopePayload): Future[Done]
  def deleteScope(payload: DeleteScopePayload): Future[Done]
  def getScope(id: ScopeId, fromReadSide: Boolean = true): Future[Scope]
  def getScopes(ids: Set[ScopeId], fromReadSide: Boolean = true): Future[Seq[Scope]]
  def findScopes(payload: FindScopeQuery): Future[FindResult]

  def assignScopePrincipal(payload: AssignScopePrincipalPayload): Future[Done]
  def unassignScopePrincipal(payload: UnassignScopePrincipalPayload): Future[Done]
  def findScopePrincipals(payload: FindScopePrincipalQuery): Future[FindResult]

  def createGroup(payload: CreateGroupPayload): Future[Done]
  def updateGroup(payload: UpdateGroupPayload): Future[Done]
  def createService(payload: CreateServicePayload): Future[Done]
  def updateService(payload: UpdateServicePayload): Future[Done]

  def activateServiceItem(payload: ActivateServiceItemPayload): Future[Done]
  def deactivateServiceItem(payload: DeactivateServiceItemPayload): Future[Done]
  def deleteServiceItem(payload: DeleteServiceItemPayload): Future[Done]
  def getServiceItem(id: ServiceItemId, fromReadSide: Boolean = true): Future[ServiceItem]
  def getServiceItems(ids: Set[ServiceItemId], fromReadSide: Boolean = true): Future[Seq[ServiceItem]]
  def findServiceItems(payload: FindServiceItemsQuery): Future[FindResult]

  def assignServicePrincipal(payload: AssignServicePrincipalPayload): Future[Done]
  def unassignServicePrincipal(payload: UnassignServicePrincipalPayload): Future[Done]
  def findServicePrincipals(payload: FindServicePrincipalQuery): Future[FindResult]

  def findScopesByCategory(payload: ScopeByCategoryFindQuery): Future[Seq[ScopeByCategoryFindResult]]
  def getScopeServices(payload: ScopeServicesQuery): Future[ScopeServicesResult]
  def findUserServices(payload: FindUserServicesQuery): Future[UserServicesResult]

}
