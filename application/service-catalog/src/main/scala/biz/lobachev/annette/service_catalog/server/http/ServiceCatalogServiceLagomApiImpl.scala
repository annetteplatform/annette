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

  override def getCategory(id: CategoryId, fromReadSide: Boolean = true): ServiceCall[NotUsed, Category] =
    ServiceCall { _ =>
      serviceCatalogService.getCategory(id, fromReadSide)
    }

  override def getCategories(fromReadSide: Boolean = true): ServiceCall[Set[CategoryId], Seq[Category]] =
    ServiceCall { ids =>
      serviceCatalogService.getCategories(ids, fromReadSide)
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

  override def getScope(id: ScopeId, fromReadSide: Boolean = true): ServiceCall[NotUsed, Scope] =
    ServiceCall { _ =>
      serviceCatalogService.getScope(id, fromReadSide)
    }

  override def getScopes(fromReadSide: Boolean = true): ServiceCall[Set[ScopeId], Seq[Scope]] =
    ServiceCall { ids =>
      serviceCatalogService.getScopes(ids, fromReadSide)
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

  override def createService: ServiceCall[CreateServicePayload, Done] =
    ServiceCall { payload =>
      serviceCatalogService.createService(payload)
    }

  override def updateService: ServiceCall[UpdateServicePayload, Done] =
    ServiceCall { payload =>
      serviceCatalogService.updateService(payload)
    }

  override def activateServiceItem: ServiceCall[ActivateServiceItemPayload, Done] =
    ServiceCall { payload =>
      serviceCatalogService.activateServiceItem(payload)
    }

  override def deactivateServiceItem: ServiceCall[DeactivateServiceItemPayload, Done] =
    ServiceCall { payload =>
      serviceCatalogService.deactivateServiceItem(payload)
    }

  override def deleteServiceItem: ServiceCall[DeleteServiceItemPayload, Done] =
    ServiceCall { payload =>
      serviceCatalogService.deleteServiceItem(payload)
    }

  override def getServiceItem(id: ServiceItemId, fromReadSide: Boolean = true): ServiceCall[NotUsed, ServiceItem] =
    ServiceCall { _ =>
      serviceCatalogService.getServiceItem(id, fromReadSide)
    }

  override def getServiceItems(fromReadSide: Boolean = true): ServiceCall[Set[ServiceItemId], Seq[ServiceItem]] =
    ServiceCall { ids =>
      serviceCatalogService.getServiceItems(ids, fromReadSide)
    }

  override def findServiceItems: ServiceCall[FindServiceItemsQuery, FindResult] =
    ServiceCall { query =>
      serviceCatalogService.findServiceItems(query)
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
