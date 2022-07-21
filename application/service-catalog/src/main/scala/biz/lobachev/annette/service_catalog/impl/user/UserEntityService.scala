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

package biz.lobachev.annette.service_catalog.impl.user

import biz.lobachev.annette.service_catalog.api.scope.FindScopeQuery
import biz.lobachev.annette.service_catalog.api.scope_principal.FindScopePrincipalQuery
import biz.lobachev.annette.service_catalog.api.item.{FindServiceItemsQuery, Group, Service, ServiceItem, ServiceItemId}
import biz.lobachev.annette.service_catalog.api.service_principal.FindServicePrincipalQuery
import biz.lobachev.annette.service_catalog.api.user._
import biz.lobachev.annette.service_catalog.impl.scope.ScopeEntityService
import biz.lobachev.annette.service_catalog.impl.scope_principal.ScopePrincipalEntityService
import biz.lobachev.annette.service_catalog.impl.item.ServiceItemEntityService
import biz.lobachev.annette.service_catalog.impl.service_principal.{
  ServicePrincipalEntity,
  ServicePrincipalEntityService
}

import scala.concurrent.{ExecutionContext, Future}

class UserEntityService(
  scopeEntityService: ScopeEntityService,
  scopePrincipalEntityService: ScopePrincipalEntityService,
  serviceEntityService: ServiceItemEntityService,
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

  def getServiceItems(ids: Set[ServiceItemId], processed: Set[ServiceItemId]): Future[Seq[ServiceItem]] =
    for {
      items         <- serviceEntityService.getServiceItemsById(ids, true).map(_.filter(_.active))
      childrenIds    =
        items.filter(_.isInstanceOf[Group]).flatMap(_.asInstanceOf[Group].children).toSet -- ids -- processed
      childrenItems <- if (childrenIds.nonEmpty)
                         getServiceItems(childrenIds, ids ++ processed).map(_.filter(_.active))
                       else Future.successful(Seq.empty[ServiceItem])
    } yield items ++ childrenItems

  def compactTree(
    children: Seq[ServiceItemId],
    serviceMap: Map[ServiceItemId, Service],
    groupMap: Map[ServiceItemId, Group],
    processedGroups: Set[ServiceItemId] = Set.empty
  ): (Seq[ServiceItemId], Map[ServiceItemId, ServiceItem]) = {
    val res = children.flatMap {
      case serviceId if serviceMap.contains(serviceId) =>
        Some(serviceId -> Map(serviceId -> serviceMap(serviceId)))
      case groupId
          if groupMap.contains(groupId) &&
            !processedGroups.contains(groupId) =>
        val (newChildren, newItems) = compactTree(
          children = groupMap(groupId).children,
          serviceMap = serviceMap,
          groupMap = groupMap,
          processedGroups = processedGroups + groupId
        )
        if (newChildren.nonEmpty)
          Some(groupId -> (newItems + (groupId -> groupMap(groupId).copy(children = newChildren))))
        else None
      case _                                           => None
    }
    (res.map(_._1), res.flatMap(_._2).toMap)
  }

  def getScopeServices(query: ScopeServicesQuery): Future[ScopeServicesResult] =
    for {
      scope             <- scopeEntityService.getScopeById(query.scopeId, true)
      items             <- getServiceItems(scope.children.toSet, Set.empty)
//      _                  = println(items)
      serviceIds         = items.flatMap {
                             case s: Service => Some(s.id)
                             case _          => None
                           }.toSet
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
    } yield {
      val serviceMap          = items.flatMap {
        case s: Service if allowedServiceIds.contains(s.id) => Some(s.id -> s)
        case _                                              => None
      }.toMap
      val groupMap            = items.flatMap {
        case s: Group => Some(s.id -> s)
        case _        => None
      }.toMap
      val (root, resultItems) = compactTree(scope.children, serviceMap, groupMap)
      ScopeServicesResult(
        root = root,
        serviceItems = resultItems.values.map {
          case s: Service =>
            UserService(
              id = s.id,
              icon = s.icon,
              label = s.label.get(query.languageId).getOrElse(""),
              labelDescription = s.labelDescription.get(query.languageId).getOrElse(""),
              link = s.link,
              score = None
            )
          case g: Group   =>
            UserGroup(
              id = g.id,
              icon = g.icon,
              label = g.label.get(query.languageId).getOrElse(""),
              labelDescription = g.labelDescription.get(query.languageId).getOrElse(""),
              children = g.children,
              score = None
            )
        }.toSeq
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
      // TODO: check if serviceIds is empty
      foundServices    <- serviceEntityService.findServiceItems(
                            FindServiceItemsQuery(
                              offset = query.offset,
                              size = query.size,
                              filter = Some(query.filter),
                              ids = Some(serviceIds),
                              active = Some(true),
                              types = Some(Set("service"))
                            )
                          )
      serviceMap       <- serviceEntityService
                            .getServiceItemsById(foundServices.hits.map(_.id).toSet, true)
                            .map(_.map(s => s.id -> s).toMap)
    } yield UserServicesResult(
      total = foundServices.total,
      services = foundServices.hits.flatMap { hit =>
        serviceMap
          .get(hit.id)
          .flatMap {
            case s: Service =>
              Some(
                UserService(
                  id = s.id,
                  icon = s.icon,
                  label = s.label.get(query.languageId).getOrElse(""),
                  labelDescription = s.labelDescription.get(query.languageId).getOrElse(""),
                  link = s.link,
                  score = Some(hit.score)
                )
              )
            case _          => None
          }
      }
    )

}
