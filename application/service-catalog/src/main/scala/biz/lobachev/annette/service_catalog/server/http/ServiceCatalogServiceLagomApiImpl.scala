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

package biz.lobachev.annette.service_catalog.server.http

import akka.util.Timeout
import akka.{Done, NotUsed}
import biz.lobachev.annette.core.model.category._
import biz.lobachev.annette.core.model.indexing.FindResult
import biz.lobachev.annette.service_catalog.api.ServiceCatalogService
import biz.lobachev.annette.service_catalog.api.group._
import biz.lobachev.annette.service_catalog.api.scope._
import biz.lobachev.annette.service_catalog.api.scope_principal._
import biz.lobachev.annette.service_catalog.api.item._
import biz.lobachev.annette.service_catalog.api.service_principal._
import biz.lobachev.annette.service_catalog.api.user._
import biz.lobachev.annette.service_catalog.client.http.ServiceCatalogServiceLagomApi
import com.lightbend.lagom.scaladsl.api.ServiceCall
import org.slf4j.LoggerFactory

import scala.concurrent.duration._

class ServiceCatalogServiceLagomApiImpl(
  serviceCatalogService: ServiceCatalogService
) extends ServiceCatalogServiceLagomApi {

  implicit val timeout = Timeout(50.seconds)

  val log = LoggerFactory.getLogger(this.getClass)

  override def createCategory: ServiceCall[CreateCategoryPayload, Done] =
    ServiceCall { payload =>
      serviceCatalogService.createCategory(payload)
    }

  override def updateCategory: ServiceCall[UpdateCategoryPayload, Done] =
    ServiceCall { payload =>
      serviceCatalogService.updateCategory(payload)
    }

  override def deleteCategory: ServiceCall[DeleteCategoryPayload, Done] =
    ServiceCall { payload =>
      serviceCatalogService.deleteCategory(payload)
    }

  override def getCategoryById(id: CategoryId, fromReadSide: Boolean = true): ServiceCall[NotUsed, Category] =
    ServiceCall { _ =>
      serviceCatalogService.getCategoryById(id, fromReadSide)
    }

  override def getCategoriesById(fromReadSide: Boolean = true): ServiceCall[Set[CategoryId], Seq[Category]] =
    ServiceCall { ids =>
      serviceCatalogService.getCategoriesById(ids, fromReadSide)
    }

  override def findCategories: ServiceCall[CategoryFindQuery, FindResult] =
    ServiceCall { query =>
      serviceCatalogService.findCategories(query)
    }

  override def createScope: ServiceCall[CreateScopePayload, Done] =
    ServiceCall { payload =>
      serviceCatalogService.createScope(payload)
    }

  override def updateScope: ServiceCall[UpdateScopePayload, Done] =
    ServiceCall { payload =>
      serviceCatalogService.updateScope(payload)
    }

  override def activateScope: ServiceCall[ActivateScopePayload, Done] =
    ServiceCall { payload =>
      serviceCatalogService.activateScope(payload)
    }

  override def deactivateScope: ServiceCall[DeactivateScopePayload, Done] =
    ServiceCall { payload =>
      serviceCatalogService.deactivateScope(payload)
    }

  override def deleteScope: ServiceCall[DeleteScopePayload, Done] =
    ServiceCall { payload =>
      serviceCatalogService.deleteScope(payload)
    }

  override def getScopeById(id: ScopeId, fromReadSide: Boolean = true): ServiceCall[NotUsed, Scope] =
    ServiceCall { _ =>
      serviceCatalogService.getScopeById(id, fromReadSide)
    }

  override def getScopesById(fromReadSide: Boolean = true): ServiceCall[Set[ScopeId], Seq[Scope]] =
    ServiceCall { ids =>
      serviceCatalogService.getScopesById(ids, fromReadSide)
    }

  override def findScopes: ServiceCall[FindScopeQuery, FindResult] =
    ServiceCall { query =>
      serviceCatalogService.findScopes(query)
    }

  override def assignScopePrincipal: ServiceCall[AssignScopePrincipalPayload, Done] =
    ServiceCall { payload =>
      serviceCatalogService.assignScopePrincipal(payload)
    }

  override def unassignScopePrincipal: ServiceCall[UnassignScopePrincipalPayload, Done] =
    ServiceCall { payload =>
      serviceCatalogService.unassignScopePrincipal(payload)
    }

  override def findScopePrincipals: ServiceCall[FindScopePrincipalQuery, FindResult] =
    ServiceCall { query =>
      serviceCatalogService.findScopePrincipals(query)
    }

  override def createGroup: ServiceCall[CreateGroupPayload, Done] =
    ServiceCall { payload =>
      serviceCatalogService.createGroup(payload)
    }

  override def updateGroup: ServiceCall[UpdateGroupPayload, Done] =
    ServiceCall { payload =>
      serviceCatalogService.updateGroup(payload)
    }

  override def activateGroup: ServiceCall[ActivateGroupPayload, Done] =
    ServiceCall { payload =>
      serviceCatalogService.activateGroup(payload)
    }

  override def deactivateGroup: ServiceCall[DeactivateGroupPayload, Done] =
    ServiceCall { payload =>
      serviceCatalogService.deactivateGroup(payload)
    }

  override def deleteGroup: ServiceCall[DeleteGroupPayload, Done] =
    ServiceCall { payload =>
      serviceCatalogService.deleteGroup(payload)
    }

  override def getGroupById(id: GroupId, fromReadSide: Boolean = true): ServiceCall[NotUsed, Group] =
    ServiceCall { _ =>
      serviceCatalogService.getGroupById(id, fromReadSide)
    }

  override def getGroupsById(fromReadSide: Boolean = true): ServiceCall[Set[GroupId], Seq[Group]] =
    ServiceCall { ids =>
      serviceCatalogService.getGroupsById(ids, fromReadSide)
    }

  override def findGroups: ServiceCall[FindGroupQuery, FindResult] =
    ServiceCall { query =>
      serviceCatalogService.findGroups(query)
    }

  override def createService: ServiceCall[CreateServicePayload, Done] =
    ServiceCall { payload =>
      serviceCatalogService.createService(payload)
    }

  override def updateService: ServiceCall[UpdateServicePayload, Done] =
    ServiceCall { payload =>
      serviceCatalogService.updateService(payload)
    }

  override def activateService: ServiceCall[ActivateScopeItemPayload, Done] =
    ServiceCall { payload =>
      serviceCatalogService.activateService(payload)
    }

  override def deactivateService: ServiceCall[DeactivateScopeItemPayload, Done] =
    ServiceCall { payload =>
      serviceCatalogService.deactivateService(payload)
    }

  override def deleteService: ServiceCall[DeleteScopeItemPayload, Done] =
    ServiceCall { payload =>
      serviceCatalogService.deleteService(payload)
    }

  override def getServiceById(id: ScopeItemId, fromReadSide: Boolean = true): ServiceCall[NotUsed, ServiceItem] =
    ServiceCall { _ =>
      serviceCatalogService.getServiceById(id, fromReadSide)
    }

  override def getServicesById(fromReadSide: Boolean = true): ServiceCall[Set[ScopeItemId], Seq[ServiceItem]] =
    ServiceCall { ids =>
      serviceCatalogService.getServicesById(ids, fromReadSide)
    }

  override def findServices: ServiceCall[FindScopeItemsQuery, FindResult] =
    ServiceCall { query =>
      serviceCatalogService.findServices(query)
    }

  override def assignServicePrincipal: ServiceCall[AssignServicePrincipalPayload, Done] =
    ServiceCall { payload =>
      serviceCatalogService.assignServicePrincipal(payload)
    }

  override def unassignServicePrincipal: ServiceCall[UnassignServicePrincipalPayload, Done] =
    ServiceCall { payload =>
      serviceCatalogService.unassignServicePrincipal(payload)
    }

  override def findServicePrincipals: ServiceCall[FindServicePrincipalQuery, FindResult] =
    ServiceCall { query =>
      serviceCatalogService.findServicePrincipals(query)
    }

  override def findScopesByCategory: ServiceCall[ScopeByCategoryFindQuery, Seq[ScopeByCategoryFindResult]] =
    ServiceCall { query =>
      serviceCatalogService.findScopesByCategory(query)
    }

  override def getScopeServices: ServiceCall[ScopeServicesQuery, ScopeServicesResult] =
    ServiceCall { query =>
      serviceCatalogService.getScopeServices(query)
    }

  override def findUserServices: ServiceCall[FindUserServicesQuery, UserServicesResult] =
    ServiceCall { query =>
      serviceCatalogService.findUserServices(query)
    }
}
