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

package biz.lobachev.annette.service_catalog.service.user

import biz.lobachev.annette.service_catalog.api.scope.FindScopeQuery
import biz.lobachev.annette.service_catalog.api.scope_principal.FindScopePrincipalQuery
import biz.lobachev.annette.service_catalog.api.item.FindScopeItemsQuery
import biz.lobachev.annette.service_catalog.api.service_principal.FindServicePrincipalQuery
import biz.lobachev.annette.service_catalog.api.user._
import biz.lobachev.annette.service_catalog.service.group.GroupEntityService
import biz.lobachev.annette.service_catalog.service.scope.ScopeEntityService
import biz.lobachev.annette.service_catalog.service.scope_principal.ScopePrincipalEntityService
import biz.lobachev.annette.service_catalog.service.service.ServiceEntityService
import biz.lobachev.annette.service_catalog.service.service_principal.{
  ServicePrincipalEntity,
  ServicePrincipalEntityService
}

import scala.concurrent.{ExecutionContext, Future}

class UserEntityService(
  scopeEntityService: ScopeEntityService,
  scopePrincipalEntityService: ScopePrincipalEntityService,
  groupEntityService: GroupEntityService,
  serviceEntityService: ServiceEntityService,
  servicePrincipalEntityService: ServicePrincipalEntityService
)(implicit ec: ExecutionContext) {

  def findScopesByCategory(query: ScopeByCategoryFindQuery): Future[Seq[ScopeByCategoryFindResult]] =
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

  def getScopeServices(query: ScopeServicesQuery): Future[ScopeServicesResult] =
    for {
      scope             <- scopeEntityService.getScopeById(query.scopeId, true)
      groups            <- groupEntityService.getGroupsById(scope.groups.toSet, true).map(_.filter(_.active))
//      _                  = println(groups)
      serviceIds         = groups.flatMap(_.services).toSet
//      _                  = println(serviceIds)
      servicePrincipals <- servicePrincipalEntityService.findServicePrincipals(
                             FindServicePrincipalQuery(
                               size = 1000,
                               services = Some(serviceIds),
                               principalCodes = Some(query.principalCodes)
                             )
                           )
//      _                  = println(servicePrincipals)
      allowedServiceIds  = servicePrincipals.hits.map(h => ServicePrincipalEntity.fromCompositeId(h.id)._1).toSet
//      _                  = println(allowedServiceIds)
      services          <- serviceEntityService.getServicesById(allowedServiceIds, true).map(_.filter(_.active))
    } yield {
      val groupMap     = groups.map(g => g.id -> g).toMap
      val serviceIds   = services.map(_.id).toSet
      val userGroups   = scope.groups.flatMap { groupId =>
        groupMap
          .get(groupId)
          .filter(_.services.toSet.intersect(serviceIds).nonEmpty)
          .map { group =>
            UserGroup(
              id = group.id,
              icon = group.icon,
              label = group.label.get(query.languageId).getOrElse(""),
              labelDescription = group.labelDescription
                .get(query.languageId)
                .getOrElse(""),
              services = group.services.filter(serviceId => serviceIds.contains(serviceId))
            )
          }
      }
      val userServices = services.map(service =>
        UserService(
          id = service.id,
          icon = service.icon,
          label = service.label.get(query.languageId).getOrElse(""),
          labelDescription = service.labelDescription.get(query.languageId).getOrElse(""),
          link = service.link
        )
      )
      ScopeServicesResult(
        groups = userGroups,
        services = userServices
      )
    }

  def findUserServices(query: FindUserServicesQuery): Future[UserServicesResult] =
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
                            FindScopeItemsQuery(
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
      services = foundServices.hits.flatMap { hit =>
        serviceMap
          .get(hit.id)
          .map(s =>
            UserService(
              id = s.id,
              icon = s.icon,
              label = s.label.get(query.languageId).getOrElse(""),
              labelDescription = s.labelDescription.get(query.languageId).getOrElse(""),
              link = s.link,
              score = Some(hit.score)
            )
          )
      }
    )

}
