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

import akka.util.Timeout
import akka.{Done, NotUsed}
import biz.lobachev.annette.core.model.category._
import biz.lobachev.annette.core.model.indexing.FindResult
import biz.lobachev.annette.service_catalog.api._
import biz.lobachev.annette.service_catalog.api.finder.{
  FindUserServicesQuery,
  ScopeByCategoryFindQuery,
  ScopeByCategoryFindResult,
  ScopeServices,
  ScopeServicesQuery,
  UserServicesHitResult,
  UserServicesResult
}
import biz.lobachev.annette.service_catalog.api.group._
import biz.lobachev.annette.service_catalog.api.scope._
import biz.lobachev.annette.service_catalog.api.scope_principal._
import biz.lobachev.annette.service_catalog.api.service._
import biz.lobachev.annette.service_catalog.api.service_principal._
import biz.lobachev.annette.service_catalog.impl.category._
import biz.lobachev.annette.service_catalog.impl.group._
import biz.lobachev.annette.service_catalog.impl.scope._
import biz.lobachev.annette.service_catalog.impl.scope_principal._
import biz.lobachev.annette.service_catalog.impl.service._
import biz.lobachev.annette.service_catalog.impl.service_principal._
import com.lightbend.lagom.scaladsl.api.ServiceCall
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

class ServiceCatalogServiceImpl(
  categoryEntityService: CategoryEntityService,
  scopeEntityService: ScopeEntityService,
  scopePrincipalEntityService: ScopePrincipalEntityService,
  groupEntityService: GroupEntityService,
  serviceEntityService: ServiceEntityService,
  servicePrincipalEntityService: ServicePrincipalEntityService
)(implicit ec: ExecutionContext)
    extends ServiceCatalogServiceApi {

  implicit val timeout = Timeout(50.seconds)

  val log = LoggerFactory.getLogger(this.getClass)

  override def createCategory: ServiceCall[CreateCategoryPayload, Done] =
    ServiceCall { payload =>
      categoryEntityService.createCategory(payload)
    }

  override def updateCategory: ServiceCall[UpdateCategoryPayload, Done] =
    ServiceCall { payload =>
      categoryEntityService.updateCategory(payload)
    }

  override def deleteCategory: ServiceCall[DeleteCategoryPayload, Done] =
    ServiceCall { payload =>
      categoryEntityService.deleteCategory(payload)
    }

  override def getCategoryById(id: CategoryId, fromReadSide: Boolean = true): ServiceCall[NotUsed, Category] =
    ServiceCall { _ =>
      categoryEntityService.getCategoryById(id, fromReadSide)
    }

  override def getCategoriesById(fromReadSide: Boolean = true): ServiceCall[Set[CategoryId], Seq[Category]] =
    ServiceCall { ids =>
      categoryEntityService.getCategoriesById(ids, fromReadSide)
    }

  override def findCategories: ServiceCall[CategoryFindQuery, FindResult] =
    ServiceCall { query =>
      categoryEntityService.findCategories(query)
    }

  override def createScope: ServiceCall[CreateScopePayload, Done] =
    ServiceCall { payload =>
      scopeEntityService.createScope(payload)
    }

  override def updateScope: ServiceCall[UpdateScopePayload, Done] =
    ServiceCall { payload =>
      scopeEntityService.updateScope(payload)
    }

  override def activateScope: ServiceCall[ActivateScopePayload, Done] =
    ServiceCall { payload =>
      scopeEntityService.activateScope(payload)
    }

  override def deactivateScope: ServiceCall[DeactivateScopePayload, Done] =
    ServiceCall { payload =>
      scopeEntityService.deactivateScope(payload)
    }

  override def deleteScope: ServiceCall[DeleteScopePayload, Done] =
    ServiceCall { payload =>
      scopeEntityService.deleteScope(payload)
    }

  override def getScopeById(id: ScopeId, fromReadSide: Boolean = true): ServiceCall[NotUsed, Scope] =
    ServiceCall { _ =>
      scopeEntityService.getScopeById(id, fromReadSide)
    }

  override def getScopesById(fromReadSide: Boolean = true): ServiceCall[Set[ScopeId], Seq[Scope]] =
    ServiceCall { ids =>
      scopeEntityService.getScopesById(ids, fromReadSide)
    }

  override def findScopes: ServiceCall[FindScopeQuery, FindResult] =
    ServiceCall { query =>
      scopeEntityService.findScopes(query)
    }

  override def assignScopePrincipal: ServiceCall[AssignScopePrincipalPayload, Done] =
    ServiceCall { payload =>
      scopePrincipalEntityService.assignScopePrincipal(payload)
    }

  override def unassignScopePrincipal: ServiceCall[UnassignScopePrincipalPayload, Done] =
    ServiceCall { payload =>
      scopePrincipalEntityService.unassignScopePrincipal(payload)
    }

  override def findScopePrincipals: ServiceCall[FindScopePrincipalQuery, FindResult] =
    ServiceCall { query =>
      scopePrincipalEntityService.findScopePrincipals(query)
    }

  override def createGroup: ServiceCall[CreateGroupPayload, Done] =
    ServiceCall { payload =>
      groupEntityService.createGroup(payload)
    }

  override def updateGroup: ServiceCall[UpdateGroupPayload, Done] =
    ServiceCall { payload =>
      groupEntityService.updateGroup(payload)
    }

  override def activateGroup: ServiceCall[ActivateGroupPayload, Done] =
    ServiceCall { payload =>
      groupEntityService.activateGroup(payload)
    }

  override def deactivateGroup: ServiceCall[DeactivateGroupPayload, Done] =
    ServiceCall { payload =>
      groupEntityService.deactivateGroup(payload)
    }

  override def deleteGroup: ServiceCall[DeleteGroupPayload, Done] =
    ServiceCall { payload =>
      groupEntityService.deleteGroup(payload)
    }

  override def getGroupById(id: GroupId, fromReadSide: Boolean = true): ServiceCall[NotUsed, Group] =
    ServiceCall { _ =>
      groupEntityService.getGroupById(id, fromReadSide)
    }

  override def getGroupsById(fromReadSide: Boolean = true): ServiceCall[Set[GroupId], Seq[Group]] =
    ServiceCall { ids =>
      groupEntityService.getGroupsById(ids, fromReadSide)
    }

  override def findGroups: ServiceCall[FindGroupQuery, FindResult] =
    ServiceCall { query =>
      groupEntityService.findGroups(query)
    }

  override def createService: ServiceCall[CreateServicePayload, Done] =
    ServiceCall { payload =>
      serviceEntityService.createService(payload)
    }

  override def updateService: ServiceCall[UpdateServicePayload, Done] =
    ServiceCall { payload =>
      serviceEntityService.updateService(payload)
    }

  override def activateService: ServiceCall[ActivateServicePayload, Done] =
    ServiceCall { payload =>
      serviceEntityService.activateService(payload)
    }

  override def deactivateService: ServiceCall[DeactivateServicePayload, Done] =
    ServiceCall { payload =>
      serviceEntityService.deactivateService(payload)
    }

  override def deleteService: ServiceCall[DeleteServicePayload, Done] =
    ServiceCall { payload =>
      serviceEntityService.deleteService(payload)
    }

  override def getServiceById(id: ServiceId, fromReadSide: Boolean = true): ServiceCall[NotUsed, Service] =
    ServiceCall { _ =>
      serviceEntityService.getServiceById(id, fromReadSide)
    }

  override def getServicesById(fromReadSide: Boolean = true): ServiceCall[Set[ServiceId], Seq[Service]] =
    ServiceCall { ids =>
      serviceEntityService.getServicesById(ids, fromReadSide)
    }

  override def findServices: ServiceCall[FindServiceQuery, FindResult] =
    ServiceCall { query =>
      serviceEntityService.findServices(query)
    }

  override def assignServicePrincipal: ServiceCall[AssignServicePrincipalPayload, Done] =
    ServiceCall { payload =>
      servicePrincipalEntityService.assignServicePrincipal(payload)
    }

  override def unassignServicePrincipal: ServiceCall[UnassignServicePrincipalPayload, Done] =
    ServiceCall { payload =>
      servicePrincipalEntityService.unassignServicePrincipal(payload)
    }

  override def findServicePrincipals: ServiceCall[FindServicePrincipalQuery, FindResult] =
    ServiceCall { query =>
      servicePrincipalEntityService.findServicePrincipals(query)
    }

  override def findScopesByCategory: ServiceCall[ScopeByCategoryFindQuery, Seq[ScopeByCategoryFindResult]] =
    ServiceCall { query =>
      for {
        scopes <- scopeEntityService.findScopes(
                    FindScopeQuery(
                      size = 100,
                      categories = Some(query.categories),
                      active = Some(true)
                    )
                  )
        result <- if (scopes.hits.nonEmpty)
                    scopePrincipalEntityService
                      .findScopePrincipals(
                        FindScopePrincipalQuery(
                          size = 100,
                          scopes = Some(scopes.hits.map(_.id).toSet),
                          principalCodes = Some(query.principalCodes)
                        )
                      )
                      .map(_.hits.map(_.id))
                  else Future.successful(Seq.empty)
      } yield result.map { compositeId =>
        val (scopeId, principal) = ServicePrincipalEntity.fromCompositeId(compositeId)
        ScopeByCategoryFindResult(scopeId, principal)
      }
    }

  override def getScopeServices: ServiceCall[ScopeServicesQuery, ScopeServices] =
    ServiceCall { query =>
      for {
        scope             <- scopeEntityService.getScopeById(query.scopeId, true)
        groups            <- groupEntityService.getGroupsById(scope.groups.toSet, true).map(_.filter(_.active))
        _                  = println(groups)
        serviceIds         = groups.flatMap(_.services).toSet
        _                  = println(serviceIds)
        servicePrincipals <- servicePrincipalEntityService.findServicePrincipals(
                               FindServicePrincipalQuery(
                                 size = 1000,
                                 services = Some(serviceIds),
                                 principalCodes = Some(query.principalCodes)
                               )
                             )
        _                  = println(servicePrincipals)
        allowedServiceIds  = servicePrincipals.hits.map(h => ServicePrincipalEntity.fromCompositeId(h.id)._1).toSet
        _                  = println(allowedServiceIds)
        services          <- serviceEntityService.getServicesById(allowedServiceIds, true).map(_.filter(_.active))
      } yield {
        val groupMap       = groups.map(g => g.id -> g).toMap
        val serviceIds     = services.map(_.id).toSet
        val nonEmptyGroups = scope.groups.flatMap { groupId =>
          groupMap
            .get(groupId)
            .filter(_.services.toSet.intersect(serviceIds).nonEmpty)
            .map { group =>
              group.copy(
                label = query.languageId.map(lang => group.label.filter(l => l._1 == lang)).getOrElse(group.label),
                labelDescription = query.languageId
                  .map(lang => group.labelDescription.filter(l => l._1 == lang))
                  .getOrElse(group.labelDescription),
                services = group.services.filter(serviceId => serviceIds.contains(serviceId))
              )
            }
        }
        val services2      = query.languageId
          .map(lang =>
            services.map { service =>
              service.copy(
                label = service.label.filter(l => l._1 == lang),
                labelDescription = service.labelDescription.filter(l => l._1 == lang)
              )
            }
          )
          .getOrElse(services)
        ScopeServices(nonEmptyGroups, services2)
      }
    }

  override def findUserServices: ServiceCall[FindUserServicesQuery, UserServicesResult] =
    ServiceCall { query =>
      for {
        assignedServices <- servicePrincipalEntityService.findServicePrincipals(
                              FindServicePrincipalQuery(
                                size = 1000,
                                principalCodes = Some(query.principalCodes)
                              )
                            )
        serviceIds        = assignedServices.hits
                              .map(compositeId => ServicePrincipalEntity.fromCompositeId(compositeId.id)._1)
                              .toSet
        foundServices    <- serviceEntityService.findServices(
                              FindServiceQuery(
                                offset = query.offset,
                                size = query.size,
                                filter = Some(query.filter),
                                services = Some(serviceIds),
                                active = Some(true)
                              )
                            )
        serviceMap       <- serviceEntityService
                              .getServicesById(foundServices.hits.map(_.id).toSet, true)
                              .map(_.map(s => s.id -> s).toMap)
      } yield UserServicesResult(
        total = foundServices.total,
        hits = foundServices.hits.flatMap { hit =>
          serviceMap
            .get(hit.id)
            .map(s =>
              UserServicesHitResult(
                id = s.id,
                icon = s.icon,
                label = s.label.get(query.languageId).getOrElse(""),
                labelDescription = s.labelDescription.get(query.languageId).getOrElse(""),
                link = s.link,
                score = hit.score
              )
            )
        }
      )
    }
}
