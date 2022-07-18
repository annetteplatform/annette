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

package biz.lobachev.annette.service_catalog.impl

import akka.Done
import akka.util.Timeout
import biz.lobachev.annette.core.model.category._
import biz.lobachev.annette.core.model.indexing.FindResult
import biz.lobachev.annette.service_catalog.api.ServiceCatalogService
import biz.lobachev.annette.service_catalog.api.scope._
import biz.lobachev.annette.service_catalog.api.scope_principal._
import biz.lobachev.annette.service_catalog.api.item._
import biz.lobachev.annette.service_catalog.api.service_principal._
import biz.lobachev.annette.service_catalog.api.user._
import biz.lobachev.annette.service_catalog.impl.category.CategoryEntityService
import biz.lobachev.annette.service_catalog.impl.scope.ScopeEntityService
import biz.lobachev.annette.service_catalog.impl.scope_principal.ScopePrincipalEntityService
import biz.lobachev.annette.service_catalog.impl.item.ServiceItemEntityService
import biz.lobachev.annette.service_catalog.impl.service_principal.ServicePrincipalEntityService
import biz.lobachev.annette.service_catalog.impl.user.UserEntityService
import com.typesafe.config.Config

import java.util.concurrent.TimeUnit
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Try

class ServiceCatalogServiceImpl(
  categoryEntityService: CategoryEntityService,
  scopeEntityService: ScopeEntityService,
  scopePrincipalEntityService: ScopePrincipalEntityService,
  serviceEntityService: ServiceItemEntityService,
  servicePrincipalEntityService: ServicePrincipalEntityService,
  userService: UserEntityService,
  config: Config
) extends ServiceCatalogService {

  implicit val timeout =
    Try(config.getDuration("annette.timeout"))
      .map(d => Timeout(FiniteDuration(d.toNanos, TimeUnit.NANOSECONDS)))
      .getOrElse(Timeout(60.seconds))

  override def createCategory(payload: CreateCategoryPayload): Future[Done] =
    categoryEntityService.createCategory(payload)

  override def updateCategory(payload: UpdateCategoryPayload): Future[Done] =
    categoryEntityService.updateCategory(payload)

  override def deleteCategory(payload: DeleteCategoryPayload): Future[Done] =
    categoryEntityService.deleteCategory(payload)

  override def getCategoryById(id: CategoryId, fromReadSide: Boolean): Future[Category] =
    categoryEntityService.getCategoryById(id, fromReadSide)

  override def getCategoriesById(ids: Set[CategoryId], fromReadSide: Boolean): Future[Seq[Category]] =
    categoryEntityService.getCategoriesById(ids, fromReadSide)

  override def findCategories(payload: CategoryFindQuery): Future[FindResult] =
    categoryEntityService.findCategories(payload)

  override def createScope(payload: CreateScopePayload): Future[Done] =
    scopeEntityService.createScope(payload)

  override def updateScope(payload: UpdateScopePayload): Future[Done] =
    scopeEntityService.updateScope(payload)

  override def activateScope(payload: ActivateScopePayload): Future[Done] =
    scopeEntityService.activateScope(payload)

  override def deactivateScope(payload: DeactivateScopePayload): Future[Done] =
    scopeEntityService.deactivateScope(payload)

  override def deleteScope(payload: DeleteScopePayload): Future[Done] =
    scopeEntityService.deleteScope(payload)

  override def getScopeById(id: ScopeId, fromReadSide: Boolean): Future[Scope] =
    scopeEntityService.getScopeById(id, fromReadSide)

  override def getScopesById(ids: Set[ScopeId], fromReadSide: Boolean): Future[Seq[Scope]] =
    scopeEntityService.getScopesById(ids, fromReadSide)

  override def findScopes(payload: FindScopeQuery): Future[FindResult] =
    scopeEntityService.findScopes(payload)

  override def assignScopePrincipal(payload: AssignScopePrincipalPayload): Future[Done] =
    scopePrincipalEntityService.assignScopePrincipal(payload)

  override def unassignScopePrincipal(payload: UnassignScopePrincipalPayload): Future[Done] =
    scopePrincipalEntityService.unassignScopePrincipal(payload)

  override def findScopePrincipals(payload: FindScopePrincipalQuery): Future[FindResult] =
    scopePrincipalEntityService.findScopePrincipals(payload)

  override def createGroup(payload: CreateGroupPayload): Future[Done] =
    serviceEntityService.createGroup(payload)

  override def updateGroup(payload: UpdateGroupPayload): Future[Done] =
    serviceEntityService.updateGroup(payload)

  override def createService(payload: CreateServicePayload): Future[Done] =
    serviceEntityService.createService(payload)

  override def updateService(payload: UpdateServicePayload): Future[Done] =
    serviceEntityService.updateService(payload)

  override def activateServiceItem(payload: ActivateServiceItemPayload): Future[Done] =
    serviceEntityService.activateServiceItem(payload)

  override def deactivateServiceItem(payload: DeactivateServiceItemPayload): Future[Done] =
    serviceEntityService.deactivateServiceItem(payload)

  override def deleteServiceItem(payload: DeleteServiceItemPayload): Future[Done] =
    serviceEntityService.deleteServiceItem(payload)

  override def getServiceItemById(id: ServiceItemId, fromReadSide: Boolean): Future[ServiceItem] =
    serviceEntityService.getServiceItemById(id, fromReadSide)

  override def getServiceItemsById(ids: Set[ServiceItemId], fromReadSide: Boolean): Future[Seq[ServiceItem]] =
    serviceEntityService.getServiceItemsById(ids, fromReadSide)

  override def findServiceItems(payload: FindServiceItemsQuery): Future[FindResult] =
    serviceEntityService.findServiceItems(payload)

  override def assignServicePrincipal(payload: AssignServicePrincipalPayload): Future[Done] =
    servicePrincipalEntityService.assignServicePrincipal(payload)

  override def unassignServicePrincipal(payload: UnassignServicePrincipalPayload): Future[Done] =
    servicePrincipalEntityService.unassignServicePrincipal(payload)

  override def findServicePrincipals(payload: FindServicePrincipalQuery): Future[FindResult] =
    servicePrincipalEntityService.findServicePrincipals(payload)

  override def findScopesByCategory(payload: ScopeByCategoryFindQuery): Future[Seq[ScopeByCategoryFindResult]] =
    userService.findScopesByCategory(payload)

  override def getScopeServices(payload: ScopeServicesQuery): Future[ScopeServicesResult] =
    userService.getScopeServices(payload)

  override def findUserServices(payload: FindUserServicesQuery): Future[UserServicesResult] =
    userService.findUserServices(payload)
}
