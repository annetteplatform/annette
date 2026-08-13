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

import java.time.OffsetDateTime
import com.google.protobuf.empty.Empty
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.category.{
  Category,
  CategoryFindQuery,
  CreateCategoryPayload,
  DeleteCategoryPayload,
  UpdateCategoryPayload
}
import biz.lobachev.annette.core.model.indexing.{FindResult, SortBy}
import biz.lobachev.annette.core.model.text.{Icon, MultiLanguageText}
import biz.lobachev.annette.service_catalog.api.{grpc => g}
import biz.lobachev.annette.service_catalog.api.grpc.ServiceCatalogService
import biz.lobachev.annette.service_catalog.api.item.{
  FindServiceItemsQuery,
  ServiceLink,
  ServiceItem
}
import biz.lobachev.annette.service_catalog.api.scope.{
  ActivateScopePayload,
  CreateScopePayload,
  DeactivateScopePayload,
  DeleteScopePayload,
  FindScopeQuery,
  Scope,
  UpdateScopePayload
}
import biz.lobachev.annette.service_catalog.api.scope_principal.{
  AssignScopePrincipalPayload,
  FindScopePrincipalQuery,
  UnassignScopePrincipalPayload
}
import biz.lobachev.annette.service_catalog.api.service_principal.{
  AssignServicePrincipalPayload,
  FindServicePrincipalQuery,
  UnassignServicePrincipalPayload
}
import biz.lobachev.annette.service_catalog.api.user.{
  FindUserServicesQuery,
  ScopeByCategoryFindQuery,
  ScopeServicesQuery,
  UserServiceItem
}
import biz.lobachev.annette.service_catalog.impl.category.CategoryEntityService
import biz.lobachev.annette.service_catalog.impl.item.ServiceItemEntityService
import biz.lobachev.annette.service_catalog.impl.scope.ScopeEntityService
import biz.lobachev.annette.service_catalog.impl.scope_principal.ScopePrincipalEntityService
import biz.lobachev.annette.service_catalog.impl.service_principal.ServicePrincipalEntityService
import biz.lobachev.annette.service_catalog.impl.user.UserEntityService
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

class ServiceCatalogServiceApiImpl(
  categoryEntityService: CategoryEntityService,
  scopeEntityService: ScopeEntityService,
  scopePrincipalEntityService: ScopePrincipalEntityService,
  serviceItemEntityService: ServiceItemEntityService,
  servicePrincipalEntityService: ServicePrincipalEntityService,
  userService: UserEntityService
)(implicit ec: ExecutionContext) extends ServiceCatalogService {

  private def toDomain(p: g.AnnettePrincipal): AnnettePrincipal =
    AnnettePrincipal(p.code)

  private def fromDomain(p: AnnettePrincipal): g.AnnettePrincipal =
    g.AnnettePrincipal(p.code)

  private def unwrapPrincipal(p: Option[g.AnnettePrincipal]): AnnettePrincipal =
    p.map(toDomain).getOrElse(throw new IllegalArgumentException("missing principal"))

  private def formatOdt(odt: OffsetDateTime): String = odt.toString

  private def parseIcon(json: String): Icon =
    Json.parse(json).as[Icon]

  private def parseServiceLink(json: String): ServiceLink =
    Json.parse(json).as[ServiceLink]

  private def parseMultiLanguageText(m: Map[String, String]): MultiLanguageText = m

  // === Category converters ===

  private def fromDomain(c: Category): g.Category =
    g.Category(
      id = c.id,
      name = c.name,
      updatedAt = formatOdt(c.updatedAt),
      updatedBy = Some(fromDomain(c.updatedBy))
    )

  // === Scope converters ===

  private def fromDomain(s: Scope): g.Scope =
    g.Scope(
      id = s.id,
      name = s.name,
      description = s.description,
      categoryId = s.categoryId,
      children = s.children,
      active = s.active,
      updatedBy = Some(fromDomain(s.updatedBy)),
      updatedAt = formatOdt(s.updatedAt)
    )

  // === Category CRUD ===

  override def createCategory(in: g.CreateCategoryPayload): Future[Empty] =
    categoryEntityService
      .createCategory(
        CreateCategoryPayload(id = in.id, name = in.name, createdBy = unwrapPrincipal(in.createdBy))
      )
      .map(_ => Empty())

  override def updateCategory(in: g.UpdateCategoryPayload): Future[Empty] =
    categoryEntityService
      .updateCategory(
        UpdateCategoryPayload(id = in.id, name = in.name, updatedBy = unwrapPrincipal(in.updatedBy))
      )
      .map(_ => Empty())

  override def deleteCategory(in: g.DeleteCategoryPayload): Future[Empty] =
    categoryEntityService
      .deleteCategory(DeleteCategoryPayload(id = in.id, deletedBy = unwrapPrincipal(in.deletedBy)))
      .map(_ => Empty())

  override def getCategory(in: g.GetCategoryRequest): Future[g.Category] =
    categoryEntityService.getCategory(in.id, in.source).map(fromDomain)

  override def getCategories(in: g.GetCategoriesRequest): Future[g.GetCategoriesResponse] =
    categoryEntityService
      .getCategories(in.ids.toSet, in.source)
      .map(categories => g.GetCategoriesResponse(categories = categories.map(fromDomain)))

  override def findCategories(in: g.CategoryFindQuery): Future[g.FindResult] =
    categoryEntityService
      .findCategories(
        CategoryFindQuery(
          offset = in.offset,
          size = in.size,
          filter = in.filter,
          name = in.name,
          sortBy = toSortBy(in.sortBy)
        )
      )
      .map(fromDomain)

  // === Scope CRUD ===

  override def createScope(in: g.CreateScopePayload): Future[Empty] =
    scopeEntityService
      .createScope(
        CreateScopePayload(
          id = in.id,
          name = in.name,
          description = in.description,
          categoryId = in.categoryId,
          children = in.children,
          createdBy = unwrapPrincipal(in.createdBy)
        )
      )
      .map(_ => Empty())

  override def updateScope(in: g.UpdateScopePayload): Future[Empty] =
    scopeEntityService
      .updateScope(
        UpdateScopePayload(
          id = in.id,
          name = in.name,
          description = in.description,
          categoryId = in.categoryId,
          children = if (in.childrenUpdated) Some(in.children) else None,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def activateScope(in: g.ActivateScopePayload): Future[Empty] =
    scopeEntityService
      .activateScope(ActivateScopePayload(id = in.id, updatedBy = unwrapPrincipal(in.updatedBy)))
      .map(_ => Empty())

  override def deactivateScope(in: g.DeactivateScopePayload): Future[Empty] =
    scopeEntityService
      .deactivateScope(DeactivateScopePayload(id = in.id, updatedBy = unwrapPrincipal(in.updatedBy)))
      .map(_ => Empty())

  override def deleteScope(in: g.DeleteScopePayload): Future[Empty] =
    scopeEntityService
      .deleteScope(DeleteScopePayload(id = in.id, deletedBy = unwrapPrincipal(in.deletedBy)))
      .map(_ => Empty())

  override def getScope(in: g.GetScopeRequest): Future[g.Scope] =
    scopeEntityService.getScope(in.id, in.source).map(fromDomain)

  override def getScopes(in: g.GetScopesRequest): Future[g.GetScopesResponse] =
    scopeEntityService
      .getScopes(in.ids.toSet, in.source)
      .map(scopes => g.GetScopesResponse(scopes = scopes.map(fromDomain)))

  override def findScopes(in: g.FindScopeQuery): Future[g.FindResult] =
    scopeEntityService
      .findScopes(
        FindScopeQuery(
          offset = in.offset,
          size = in.size,
          filter = in.filter,
          categories = if (in.categories.isEmpty) None else Some(in.categories.toSet),
          active = in.active,
          sortBy = toSortBy(in.sortBy)
        )
      )
      .map(fromDomain)

  // === ScopePrincipal ===

  override def assignScopePrincipal(in: g.AssignScopePrincipalPayload): Future[Empty] =
    scopePrincipalEntityService
      .assignScopePrincipal(
        AssignScopePrincipalPayload(
          scopeId = in.scopeId,
          principal = unwrapPrincipal(in.principal),
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def unassignScopePrincipal(in: g.UnassignScopePrincipalPayload): Future[Empty] =
    scopePrincipalEntityService
      .unassignScopePrincipal(
        UnassignScopePrincipalPayload(
          scopeId = in.scopeId,
          principal = unwrapPrincipal(in.principal),
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def findScopePrincipals(in: g.FindScopePrincipalQuery): Future[g.FindResult] =
    scopePrincipalEntityService
      .findScopePrincipals(
        FindScopePrincipalQuery(
          offset = in.offset,
          size = in.size,
          scopes = if (in.scopes.isEmpty) None else Some(in.scopes.toSet),
          principalCodes = if (in.principalCodes.isEmpty) None else Some(in.principalCodes.toSet),
          sortBy = toSortBy(in.sortBy)
        )
      )
      .map(fromDomain)

  // === ServiceItem (Group/Service) ===

  override def createGroup(in: g.CreateGroupPayload): Future[Empty] =
    serviceItemEntityService
      .createGroup(
        biz.lobachev.annette.service_catalog.api.item.CreateGroupPayload(
          id = in.id,
          name = in.name,
          description = in.description,
          icon = parseIcon(in.icon),
          label = parseMultiLanguageText(in.label),
          labelDescription = parseMultiLanguageText(in.labelDescription),
          children = in.children,
          createdBy = unwrapPrincipal(in.createdBy)
        )
      )
      .map(_ => Empty())

  override def updateGroup(in: g.UpdateGroupPayload): Future[Empty] =
    serviceItemEntityService
      .updateGroup(
        biz.lobachev.annette.service_catalog.api.item.UpdateGroupPayload(
          id = in.id,
          name = in.name,
          description = in.description,
          icon = in.icon.map(parseIcon),
          label = in.labelJson.map(s => parseMultiLanguageText(Json.parse(s).as[Map[String, String]])),
          labelDescription = in.labelDescriptionJson.map(s => parseMultiLanguageText(Json.parse(s).as[Map[String, String]])),
          children = if (in.childrenUpdated) Some(in.children) else None,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def createService(in: g.CreateServicePayload): Future[Empty] =
    serviceItemEntityService
      .createService(
        biz.lobachev.annette.service_catalog.api.item.CreateServicePayload(
          id = in.id,
          name = in.name,
          description = in.description,
          icon = parseIcon(in.icon),
          label = parseMultiLanguageText(in.label),
          labelDescription = parseMultiLanguageText(in.labelDescription),
          link = parseServiceLink(in.link),
          createdBy = unwrapPrincipal(in.createdBy)
        )
      )
      .map(_ => Empty())

  override def updateService(in: g.UpdateServicePayload): Future[Empty] =
    serviceItemEntityService
      .updateService(
        biz.lobachev.annette.service_catalog.api.item.UpdateServicePayload(
          id = in.id,
          name = in.name,
          description = in.description,
          icon = in.icon.map(parseIcon),
          label = in.labelJson.map(s => parseMultiLanguageText(Json.parse(s).as[Map[String, String]])),
          labelDescription = in.labelDescriptionJson.map(s => parseMultiLanguageText(Json.parse(s).as[Map[String, String]])),
          link = in.link.map(parseServiceLink),
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def activateServiceItem(in: g.ActivateServiceItemPayload): Future[Empty] =
    serviceItemEntityService
      .activateServiceItem(
        biz.lobachev.annette.service_catalog.api.item.ActivateServiceItemPayload(
          id = in.id,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def deactivateServiceItem(in: g.DeactivateServiceItemPayload): Future[Empty] =
    serviceItemEntityService
      .deactivateServiceItem(
        biz.lobachev.annette.service_catalog.api.item.DeactivateServiceItemPayload(
          id = in.id,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def deleteServiceItem(in: g.DeleteServiceItemPayload): Future[Empty] =
    serviceItemEntityService
      .deleteServiceItem(
        biz.lobachev.annette.service_catalog.api.item.DeleteServiceItemPayload(
          id = in.id,
          deletedBy = unwrapPrincipal(in.deletedBy)
        )
      )
      .map(_ => Empty())

  override def getServiceItem(in: g.GetServiceItemRequest): Future[g.ServiceItemResponse] =
    serviceItemEntityService
      .getServiceItem(in.id, in.source)
      .map(item => g.ServiceItemResponse(itemJson = Json.stringify(Json.toJson(item)(ServiceItem.format))))

  override def getServiceItems(in: g.GetServiceItemsRequest): Future[g.GetServiceItemsResponse] =
    serviceItemEntityService
      .getServiceItems(in.ids.toSet, in.source)
      .map(items =>
        g.GetServiceItemsResponse(
          itemsJson = items.map(item => Json.stringify(Json.toJson(item)(ServiceItem.format))).toSeq
        )
      )

  override def findServiceItems(in: g.FindServiceItemsQuery): Future[g.FindResult] =
    serviceItemEntityService
      .findServiceItems(
        FindServiceItemsQuery(
          offset = in.offset,
          size = in.size,
          filter = in.filter,
          types = if (in.types.isEmpty) None else Some(in.types.toSet),
          ids = if (in.ids.isEmpty) None else Some(in.ids.toSet),
          active = in.active,
          sortBy = toSortBy(in.sortBy)
        )
      )
      .map(fromDomain)

  // === ServicePrincipal ===

  override def assignServicePrincipal(in: g.AssignServicePrincipalPayload): Future[Empty] =
    servicePrincipalEntityService
      .assignServicePrincipal(
        AssignServicePrincipalPayload(
          serviceId = in.serviceId,
          principal = unwrapPrincipal(in.principal),
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def unassignServicePrincipal(in: g.UnassignServicePrincipalPayload): Future[Empty] =
    servicePrincipalEntityService
      .unassignServicePrincipal(
        UnassignServicePrincipalPayload(
          serviceId = in.serviceId,
          principal = unwrapPrincipal(in.principal),
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def findServicePrincipals(in: g.FindServicePrincipalQuery): Future[g.FindResult] =
    servicePrincipalEntityService
      .findServicePrincipals(
        FindServicePrincipalQuery(
          offset = in.offset,
          size = in.size,
          services = if (in.services.isEmpty) None else Some(in.services.toSet),
          principalCodes = if (in.principalCodes.isEmpty) None else Some(in.principalCodes.toSet),
          sortBy = toSortBy(in.sortBy)
        )
      )
      .map(fromDomain)

  // === User queries ===

  override def findScopesByCategory(in: g.ScopeByCategoryFindQuery): Future[g.ScopeByCategoryFindResultResponse] =
    userService
      .findScopesByCategory(
        ScopeByCategoryFindQuery(
          categories = in.categories.toSet,
          principalCodes = in.principalCodes.toSet
        )
      )
      .map(results =>
        g.ScopeByCategoryFindResultResponse(
          results = results.map(r =>
            g.ScopeByCategoryFindResult(scopeId = r.scopeId, principal = Some(fromDomain(r.principal)))
          )
        )
      )

  override def getScopeServices(in: g.ScopeServicesQuery): Future[g.ScopeServicesResult] =
    userService
      .getScopeServices(
        ScopeServicesQuery(
          scopeId = in.scopeId,
          principalCodes = in.principalCodes.toSet,
          languageId = in.languageId
        )
      )
      .map { result =>
        g.ScopeServicesResult(
          root = result.root,
          serviceItemsJson = result.serviceItems.map(item => Json.stringify(Json.toJson(item)(UserServiceItem.format))).toSeq
        )
      }

  override def findUserServices(in: g.FindUserServicesQuery): Future[g.UserServicesResult] =
    userService
      .findUserServices(
        FindUserServicesQuery(
          offset = in.offset,
          size = in.size,
          filter = in.filter,
          principalCodes = in.principalCodes.toSet,
          languageId = in.languageId
        )
      )
      .map { result =>
        g.UserServicesResult(
          total = result.total,
          servicesJson = result.services.map(item => Json.stringify(Json.toJson(item.asInstanceOf[UserServiceItem])(UserServiceItem.format)))
        )
      }

  // === helpers ===

  private def toSortBy(sortBy: Seq[g.SortBy]): Option[Seq[SortBy]] =
    Some(sortBy.map(s => SortBy(field = s.field, descending = s.descending)).toSeq).filter(_.nonEmpty)

  private def fromDomain(f: FindResult): g.FindResult =
    g.FindResult(
      total = f.total,
      hits = f.hits.map(h => g.HitResult(id = h.id, score = h.score, updatedAt = formatOdt(h.updatedAt)))
    )
}
