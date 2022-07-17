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

import akka.{Done, NotUsed}
import biz.lobachev.annette.core.exception.AnnetteTransportExceptionSerializer
import biz.lobachev.annette.core.model.category._
import biz.lobachev.annette.core.model.indexing.FindResult
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
import com.lightbend.lagom.scaladsl.api.Service._
import com.lightbend.lagom.scaladsl.api.{Service => LagomService, ServiceCall}

trait ServiceCatalogServiceLagomApi extends LagomService {

  def createCategory: ServiceCall[CreateCategoryPayload, Done]
  def updateCategory: ServiceCall[UpdateCategoryPayload, Done]
  def deleteCategory: ServiceCall[DeleteCategoryPayload, Done]
  def getCategoryById(id: CategoryId, fromReadSide: Boolean): ServiceCall[NotUsed, Category]
  def getCategoriesById(
    fromReadSide: Boolean
  ): ServiceCall[Set[CategoryId], Seq[Category]]
  def findCategories: ServiceCall[CategoryFindQuery, FindResult]

  def createScope: ServiceCall[CreateScopePayload, Done]
  def updateScope: ServiceCall[UpdateScopePayload, Done]
  def activateScope: ServiceCall[ActivateScopePayload, Done]
  def deactivateScope: ServiceCall[DeactivateScopePayload, Done]
  def deleteScope: ServiceCall[DeleteScopePayload, Done]
  def getScopeById(id: ScopeId, fromReadSide: Boolean = true): ServiceCall[NotUsed, Scope]
  def getScopesById(fromReadSide: Boolean = true): ServiceCall[Set[ScopeId], Seq[Scope]]
  def findScopes: ServiceCall[FindScopeQuery, FindResult]

  def assignScopePrincipal: ServiceCall[AssignScopePrincipalPayload, Done]
  def unassignScopePrincipal: ServiceCall[UnassignScopePrincipalPayload, Done]
  def findScopePrincipals: ServiceCall[FindScopePrincipalQuery, FindResult]

  def createGroup: ServiceCall[CreateGroupPayload, Done]
  def updateGroup: ServiceCall[UpdateGroupPayload, Done]
  def createService: ServiceCall[CreateServicePayload, Done]
  def updateService: ServiceCall[UpdateServicePayload, Done]

  def activateServiceItem: ServiceCall[ActivateServiceItemPayload, Done]
  def deactivateServiceItem: ServiceCall[DeactivateServiceItemPayload, Done]
  def deleteServiceItem: ServiceCall[DeleteServiceItemPayload, Done]
  def getServiceItemById(id: ServiceItemId, fromReadSide: Boolean = true): ServiceCall[NotUsed, ServiceItem]
  def getServiceItemsById(fromReadSide: Boolean = true): ServiceCall[Set[ServiceItemId], Seq[ServiceItem]]
  def findServiceItems: ServiceCall[FindServiceItemsQuery, FindResult]

  def assignServicePrincipal: ServiceCall[AssignServicePrincipalPayload, Done]
  def unassignServicePrincipal: ServiceCall[UnassignServicePrincipalPayload, Done]
  def findServicePrincipals: ServiceCall[FindServicePrincipalQuery, FindResult]

  def findScopesByCategory: ServiceCall[ScopeByCategoryFindQuery, Seq[ScopeByCategoryFindResult]]
  def getScopeServices: ServiceCall[ScopeServicesQuery, ScopeServicesResult]
  def findUserServices: ServiceCall[FindUserServicesQuery, UserServicesResult]

  final override def descriptor =
    named("serviceCatalog")
      .withCalls(
        pathCall("/api/serviceCatalog/v1/createCategory", createCategory),
        pathCall("/api/serviceCatalog/v1/updateCategory", updateCategory),
        pathCall("/api/serviceCatalog/v1/deleteCategory", deleteCategory),
        pathCall("/api/serviceCatalog/v1/getCategoryById/:id/:fromReadSide", getCategoryById _),
        pathCall("/api/serviceCatalog/v1/getCategoriesById/:fromReadSide", getCategoriesById _),
        pathCall("/api/serviceCatalog/v1/findCategories", findCategories),
        pathCall("/api/serviceCatalog/v1/createScope", createScope),
        pathCall("/api/serviceCatalog/v1/updateScope", updateScope),
        pathCall("/api/serviceCatalog/v1/activateScope", activateScope),
        pathCall("/api/serviceCatalog/v1/deactivateScope", deactivateScope),
        pathCall("/api/serviceCatalog/v1/deleteScope", deleteScope),
        pathCall("/api/serviceCatalog/v1/getScopeById/:id/:fromReadSide", getScopeById _),
        pathCall("/api/serviceCatalog/v1/getScopesById/:fromReadSide", getScopesById _),
        pathCall("/api/serviceCatalog/v1/findScopes", findScopes),
        pathCall("/api/serviceCatalog/v1/assignScopePrincipal", assignScopePrincipal),
        pathCall("/api/serviceCatalog/v1/unassignScopePrincipal", unassignScopePrincipal),
        pathCall("/api/serviceCatalog/v1/findScopePrincipals", findScopePrincipals),
        pathCall("/api/serviceCatalog/v1/createGroup", createGroup),
        pathCall("/api/serviceCatalog/v1/updateGroup", updateGroup),
        pathCall("/api/serviceCatalog/v1/createService", createService),
        pathCall("/api/serviceCatalog/v1/updateService", updateService),
        pathCall("/api/serviceCatalog/v1/activateServiceItem", activateServiceItem),
        pathCall("/api/serviceCatalog/v1/deactivateServiceItem", deactivateServiceItem),
        pathCall("/api/serviceCatalog/v1/deleteServiceItem", deleteServiceItem),
        pathCall("/api/serviceCatalog/v1/getServiceItemById/:id/:fromReadSide", getServiceItemById _),
        pathCall("/api/serviceCatalog/v1/getServiceItemsById/:fromReadSide", getServiceItemsById _),
        pathCall("/api/serviceCatalog/v1/findServiceItems", findServiceItems),
        pathCall("/api/serviceCatalog/v1/assignServicePrincipal", assignServicePrincipal),
        pathCall("/api/serviceCatalog/v1/unassignServicePrincipal", unassignServicePrincipal),
        pathCall("/api/serviceCatalog/v1/findServicePrincipals", findServicePrincipals),
        pathCall("/api/serviceCatalog/v1/findScopesByCategory", findScopesByCategory),
        pathCall("/api/serviceCatalog/v1/getScopeServices", getScopeServices),
        pathCall("/api/serviceCatalog/v1/findUserServices", findUserServices)
      )
      .withExceptionSerializer(new AnnetteTransportExceptionSerializer())
      .withAutoAcl(true)
}
