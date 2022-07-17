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

package biz.lobachev.annette.service_catalog.client.http

import akka.Done
import biz.lobachev.annette.core.model.category._
import biz.lobachev.annette.core.model.indexing.FindResult
import biz.lobachev.annette.service_catalog.api.ServiceCatalogService
import biz.lobachev.annette.service_catalog.api.group._
import biz.lobachev.annette.service_catalog.api.scope._
import biz.lobachev.annette.service_catalog.api.scope_principal.{
  AssignScopePrincipalPayload,
  FindScopePrincipalQuery,
  UnassignScopePrincipalPayload
}
import biz.lobachev.annette.service_catalog.api.item._
import biz.lobachev.annette.service_catalog.api.service_principal.{
  AssignServicePrincipalPayload,
  FindServicePrincipalQuery,
  UnassignServicePrincipalPayload
}
import biz.lobachev.annette.service_catalog.api.user._

import scala.concurrent.Future

class ServiceCatalogServiceLagomImpl(api: ServiceCatalogServiceLagomApi) extends ServiceCatalogService {
  override def createCategory(payload: CreateCategoryPayload): Future[Done] =
    api.createCategory.invoke(payload)

  override def updateCategory(payload: UpdateCategoryPayload): Future[Done] =
    api.updateCategory.invoke(payload)

  override def deleteCategory(payload: DeleteCategoryPayload): Future[Done] =
    api.deleteCategory.invoke(payload)

  override def getCategoryById(id: CategoryId, fromReadSide: Boolean): Future[Category] =
    api.getCategoryById(id, fromReadSide).invoke()

  override def getCategoriesById(ids: Set[CategoryId], fromReadSide: Boolean): Future[Seq[Category]] =
    api.getCategoriesById(fromReadSide).invoke(ids)

  override def findCategories(payload: CategoryFindQuery): Future[FindResult] =
    api.findCategories.invoke(payload)

  override def createScope(payload: CreateScopePayload): Future[Done] =
    api.createScope.invoke(payload)

  override def updateScope(payload: UpdateScopePayload): Future[Done] =
    api.updateScope.invoke(payload)

  override def activateScope(payload: ActivateScopePayload): Future[Done] =
    api.activateScope.invoke(payload)

  override def deactivateScope(payload: DeactivateScopePayload): Future[Done] =
    api.deactivateScope.invoke(payload)

  override def deleteScope(payload: DeleteScopePayload): Future[Done] =
    api.deleteScope.invoke(payload)

  override def getScopeById(id: ScopeId, fromReadSide: Boolean): Future[Scope] =
    api.getScopeById(id, fromReadSide).invoke()

  override def getScopesById(ids: Set[ScopeId], fromReadSide: Boolean): Future[Seq[Scope]] =
    api.getScopesById(fromReadSide).invoke(ids)

  override def findScopes(payload: FindScopeQuery): Future[FindResult] =
    api.findScopes.invoke(payload)

  override def assignScopePrincipal(payload: AssignScopePrincipalPayload): Future[Done] =
    api.assignScopePrincipal.invoke(payload)

  override def unassignScopePrincipal(payload: UnassignScopePrincipalPayload): Future[Done] =
    api.unassignScopePrincipal.invoke(payload)

  override def findScopePrincipals(payload: FindScopePrincipalQuery): Future[FindResult] =
    api.findScopePrincipals.invoke(payload)

  override def createGroup(payload: CreateGroupPayload): Future[Done] =
    api.createGroup.invoke(payload)

  override def updateGroup(payload: UpdateGroupPayload): Future[Done] =
    api.updateGroup.invoke(payload)

  override def activateGroup(payload: ActivateGroupPayload): Future[Done] =
    api.activateGroup.invoke(payload)

  override def deactivateGroup(payload: DeactivateGroupPayload): Future[Done] =
    api.deactivateGroup.invoke(payload)

  override def deleteGroup(payload: DeleteGroupPayload): Future[Done] =
    api.deleteGroup.invoke(payload)

  override def getGroupById(id: GroupId, fromReadSide: Boolean): Future[Group] =
    api.getGroupById(id, fromReadSide).invoke()

  override def getGroupsById(ids: Set[GroupId], fromReadSide: Boolean): Future[Seq[Group]] =
    api.getGroupsById(fromReadSide).invoke(ids)

  override def findGroups(payload: FindGroupQuery): Future[FindResult] =
    api.findGroups.invoke(payload)

  override def createService(payload: CreateServicePayload): Future[Done] =
    api.createService.invoke(payload)

  override def updateService(payload: UpdateServicePayload): Future[Done] =
    api.updateService.invoke(payload)

  override def activateService(payload: ActivateScopeItemPayload): Future[Done] =
    api.activateService.invoke(payload)

  override def deactivateService(payload: DeactivateScopeItemPayload): Future[Done] =
    api.deactivateService.invoke(payload)

  override def deleteService(payload: DeleteScopeItemPayload): Future[Done] =
    api.deleteService.invoke(payload)

  override def getServiceById(id: ScopeItemId, fromReadSide: Boolean): Future[ServiceItem] =
    api.getServiceById(id, fromReadSide).invoke()

  override def getServicesById(ids: Set[ScopeItemId], fromReadSide: Boolean): Future[Seq[ServiceItem]] =
    api.getServicesById(fromReadSide).invoke(ids)

  override def findServices(payload: FindScopeItemsQuery): Future[FindResult] =
    api.findServices.invoke(payload)

  override def assignServicePrincipal(payload: AssignServicePrincipalPayload): Future[Done] =
    api.assignServicePrincipal.invoke(payload)

  override def unassignServicePrincipal(payload: UnassignServicePrincipalPayload): Future[Done] =
    api.unassignServicePrincipal.invoke(payload)

  override def findServicePrincipals(payload: FindServicePrincipalQuery): Future[FindResult] =
    api.findServicePrincipals.invoke(payload)

  override def findScopesByCategory(payload: ScopeByCategoryFindQuery): Future[Seq[ScopeByCategoryFindResult]] =
    api.findScopesByCategory.invoke(payload)

  override def getScopeServices(payload: ScopeServicesQuery): Future[ScopeServicesResult] =
    api.getScopeServices.invoke(payload)

  override def findUserServices(payload: FindUserServicesQuery): Future[UserServicesResult] =
    api.findUserServices.invoke(payload)
}
