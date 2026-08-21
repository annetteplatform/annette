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

import java.time.OffsetDateTime
import biz.lobachev.annette.core.exception.AnnetteGrpcExceptionMapping
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.category._
import biz.lobachev.annette.core.model.indexing.{FindResult, HitResult, SortBy}
import biz.lobachev.annette.core.model.text.Icon
import biz.lobachev.annette.service_catalog.api.item._
import biz.lobachev.annette.service_catalog.api.scope._
import biz.lobachev.annette.service_catalog.api.scope_principal._
import biz.lobachev.annette.service_catalog.api.service_principal._
import biz.lobachev.annette.service_catalog.api.user._
import biz.lobachev.annette.service_catalog.api.{grpc => g}
import org.apache.pekko.Done
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

class ServiceCatalogServiceGrpcImpl(client: g.ServiceCatalogServiceClient)(implicit val ec: ExecutionContext)
    extends ServiceCatalogService {

  private def unwrapPrincipal(p: Option[g.AnnettePrincipal]): AnnettePrincipal =
    p.map(pr => AnnettePrincipal(pr.code)).getOrElse(throw new IllegalArgumentException("missing principal"))

  private def fromDomain(p: AnnettePrincipal): g.AnnettePrincipal = g.AnnettePrincipal(p.code)

  private def sortByToProto(sortBy: Option[Seq[SortBy]]): Seq[g.SortBy] =
    sortBy.map(_.map(s => g.SortBy(field = s.field, descending = s.descending))).getOrElse(Seq.empty)

  private def toDomain(f: g.FindResult): FindResult =
    FindResult(
      total = f.total,
      hits = f.hits.map(h => HitResult(id = h.id, score = h.score, updatedAt = OffsetDateTime.parse(h.updatedAt)))
    )

  private def call[T](f: Future[T]): Future[T] = AnnetteGrpcExceptionMapping.recoverAnnette(f)

  // === Category converters ===

  private def fromProto(c: g.Category): Category =
    Category(
      id = c.id,
      name = c.name,
      updatedAt = OffsetDateTime.parse(c.updatedAt),
      updatedBy = unwrapPrincipal(c.updatedBy)
    )

  // === Scope converters ===

  private def fromProto(s: g.Scope): Scope =
    Scope(
      id = s.id,
      name = s.name,
      description = s.description,
      categoryId = s.categoryId,
      children = s.children,
      active = s.active,
      updatedBy = unwrapPrincipal(s.updatedBy),
      updatedAt = OffsetDateTime.parse(s.updatedAt)
    )

  private def iconToJson(icon: Icon): String = Json.stringify(Json.toJson(icon)(Icon.format))

  private def linkToJson(link: ServiceLink): String = Json.stringify(Json.toJson(link)(ServiceLink.format))

  private def mapToJson(m: Map[String, String]): String = Json.stringify(Json.toJson(m))

  // === Category CRUD ===

  override def createCategory(payload: CreateCategoryPayload): Future[Done] =
    call(
      client.createCategory(
        g.CreateCategoryPayload(id = payload.id, name = payload.name, createdBy = Some(fromDomain(payload.createdBy)))
      )
    ).map(_ => Done)

  override def updateCategory(payload: UpdateCategoryPayload): Future[Done] =
    call(
      client.updateCategory(
        g.UpdateCategoryPayload(id = payload.id, name = payload.name, updatedBy = Some(fromDomain(payload.updatedBy)))
      )
    ).map(_ => Done)

  override def deleteCategory(payload: DeleteCategoryPayload): Future[Done] =
    call(
      client.deleteCategory(g.DeleteCategoryPayload(id = payload.id, deletedBy = Some(fromDomain(payload.deletedBy))))
    ).map(_ => Done)

  override def getCategory(id: CategoryId, source: Option[String]): Future[Category] =
    call(client.getCategory(g.GetCategoryRequest(id = id, source = source))).map(fromProto)

  override def getCategories(ids: Set[CategoryId], source: Option[String]): Future[Seq[Category]] =
    call(client.getCategories(g.GetCategoriesRequest(ids = ids.toSeq, source = source)))
      .map(_.categories.map(fromProto))

  override def findCategories(payload: CategoryFindQuery): Future[FindResult] =
    call(
      client.findCategories(
        g.CategoryFindQuery(
          offset = payload.offset,
          size = payload.size,
          filter = payload.filter,
          name = payload.name,
          sortBy = sortByToProto(payload.sortBy)
        )
      )
    ).map(toDomain)

  // === Scope CRUD ===

  override def createScope(payload: CreateScopePayload): Future[Done] =
    call(
      client.createScope(
        g.CreateScopePayload(
          id = payload.id,
          name = payload.name,
          description = payload.description,
          categoryId = payload.categoryId,
          children = payload.children,
          createdBy = Some(fromDomain(payload.createdBy))
        )
      )
    ).map(_ => Done)

  override def updateScope(payload: UpdateScopePayload): Future[Done] =
    call(
      client.updateScope(
        g.UpdateScopePayload(
          id = payload.id,
          name = payload.name,
          description = payload.description,
          categoryId = payload.categoryId,
          children = payload.children.getOrElse(Seq.empty),
          childrenUpdated = payload.children.isDefined,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def activateScope(payload: ActivateScopePayload): Future[Done] =
    call(
      client.activateScope(g.ActivateScopePayload(id = payload.id, updatedBy = Some(fromDomain(payload.updatedBy))))
    ).map(_ => Done)

  override def deactivateScope(payload: DeactivateScopePayload): Future[Done] =
    call(
      client.deactivateScope(g.DeactivateScopePayload(id = payload.id, updatedBy = Some(fromDomain(payload.updatedBy))))
    ).map(_ => Done)

  override def deleteScope(payload: DeleteScopePayload): Future[Done] =
    call(client.deleteScope(g.DeleteScopePayload(id = payload.id, deletedBy = Some(fromDomain(payload.deletedBy)))))
      .map(_ => Done)

  override def getScope(id: ScopeId, source: Option[String]): Future[Scope] =
    call(client.getScope(g.GetScopeRequest(id = id, source = source))).map(fromProto)

  override def getScopes(ids: Set[ScopeId], source: Option[String]): Future[Seq[Scope]] =
    call(client.getScopes(g.GetScopesRequest(ids = ids.toSeq, source = source)))
      .map(_.scopes.map(fromProto))

  override def findScopes(payload: FindScopeQuery): Future[FindResult] =
    call(
      client.findScopes(
        g.FindScopeQuery(
          offset = payload.offset,
          size = payload.size,
          filter = payload.filter,
          categories = payload.categories.map(_.toSeq).getOrElse(Seq.empty),
          active = payload.active,
          sortBy = sortByToProto(payload.sortBy)
        )
      )
    ).map(toDomain)

  // === ScopePrincipal ===

  override def assignScopePrincipal(payload: AssignScopePrincipalPayload): Future[Done] =
    call(
      client.assignScopePrincipal(
        g.AssignScopePrincipalPayload(
          scopeId = payload.scopeId,
          principal = Some(fromDomain(payload.principal)),
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def unassignScopePrincipal(payload: UnassignScopePrincipalPayload): Future[Done] =
    call(
      client.unassignScopePrincipal(
        g.UnassignScopePrincipalPayload(
          scopeId = payload.scopeId,
          principal = Some(fromDomain(payload.principal)),
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def findScopePrincipals(payload: FindScopePrincipalQuery): Future[FindResult] =
    call(
      client.findScopePrincipals(
        g.FindScopePrincipalQuery(
          offset = payload.offset,
          size = payload.size,
          scopes = payload.scopes.map(_.toSeq).getOrElse(Seq.empty),
          principalCodes = payload.principalCodes.map(_.toSeq).getOrElse(Seq.empty),
          sortBy = sortByToProto(payload.sortBy)
        )
      )
    ).map(toDomain)

  // === ServiceItem (Group/Service) ===

  override def createGroup(payload: CreateGroupPayload): Future[Done] =
    call(
      client.createGroup(
        g.CreateGroupPayload(
          id = payload.id,
          name = payload.name,
          description = payload.description,
          icon = iconToJson(payload.icon),
          label = payload.label,
          labelDescription = payload.labelDescription,
          children = payload.children,
          createdBy = Some(fromDomain(payload.createdBy))
        )
      )
    ).map(_ => Done)

  override def updateGroup(payload: UpdateGroupPayload): Future[Done] =
    call(
      client.updateGroup(
        g.UpdateGroupPayload(
          id = payload.id,
          name = payload.name,
          description = payload.description,
          icon = payload.icon.map(iconToJson),
          labelJson = payload.label.map(mapToJson),
          labelDescriptionJson = payload.labelDescription.map(mapToJson),
          children = payload.children.getOrElse(Seq.empty),
          childrenUpdated = payload.children.isDefined,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def createService(payload: CreateServicePayload): Future[Done] =
    call(
      client.createService(
        g.CreateServicePayload(
          id = payload.id,
          name = payload.name,
          description = payload.description,
          icon = iconToJson(payload.icon),
          label = payload.label,
          labelDescription = payload.labelDescription,
          link = linkToJson(payload.link),
          createdBy = Some(fromDomain(payload.createdBy))
        )
      )
    ).map(_ => Done)

  override def updateService(payload: UpdateServicePayload): Future[Done] =
    call(
      client.updateService(
        g.UpdateServicePayload(
          id = payload.id,
          name = payload.name,
          description = payload.description,
          icon = payload.icon.map(iconToJson),
          labelJson = payload.label.map(mapToJson),
          labelDescriptionJson = payload.labelDescription.map(mapToJson),
          link = payload.link.map(linkToJson),
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def activateServiceItem(payload: ActivateServiceItemPayload): Future[Done] =
    call(
      client.activateServiceItem(
        g.ActivateServiceItemPayload(id = payload.id, updatedBy = Some(fromDomain(payload.updatedBy)))
      )
    ).map(_ => Done)

  override def deactivateServiceItem(payload: DeactivateServiceItemPayload): Future[Done] =
    call(
      client.deactivateServiceItem(
        g.DeactivateServiceItemPayload(id = payload.id, updatedBy = Some(fromDomain(payload.updatedBy)))
      )
    ).map(_ => Done)

  override def deleteServiceItem(payload: DeleteServiceItemPayload): Future[Done] =
    call(
      client.deleteServiceItem(
        g.DeleteServiceItemPayload(id = payload.id, deletedBy = Some(fromDomain(payload.deletedBy)))
      )
    ).map(_ => Done)

  override def getServiceItem(id: ServiceItemId, source: Option[String]): Future[ServiceItem] =
    call(client.getServiceItem(g.GetServiceItemRequest(id = id, source = source)))
      .map(r => Json.parse(r.itemJson).as[ServiceItem](ServiceItem.format))

  override def getServiceItems(ids: Set[ServiceItemId], source: Option[String]): Future[Seq[ServiceItem]] =
    call(client.getServiceItems(g.GetServiceItemsRequest(ids = ids.toSeq, source = source)))
      .map(_.itemsJson.map(json => Json.parse(json).as[ServiceItem](ServiceItem.format)))

  override def findServiceItems(payload: FindServiceItemsQuery): Future[FindResult] =
    call(
      client.findServiceItems(
        g.FindServiceItemsQuery(
          offset = payload.offset,
          size = payload.size,
          filter = payload.filter,
          types = payload.types.map(_.toSeq).getOrElse(Seq.empty),
          ids = payload.ids.map(_.toSeq).getOrElse(Seq.empty),
          active = payload.active,
          sortBy = sortByToProto(payload.sortBy)
        )
      )
    ).map(toDomain)

  // === ServicePrincipal ===

  override def assignServicePrincipal(payload: AssignServicePrincipalPayload): Future[Done] =
    call(
      client.assignServicePrincipal(
        g.AssignServicePrincipalPayload(
          serviceId = payload.serviceId,
          principal = Some(fromDomain(payload.principal)),
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def unassignServicePrincipal(payload: UnassignServicePrincipalPayload): Future[Done] =
    call(
      client.unassignServicePrincipal(
        g.UnassignServicePrincipalPayload(
          serviceId = payload.serviceId,
          principal = Some(fromDomain(payload.principal)),
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def findServicePrincipals(payload: FindServicePrincipalQuery): Future[FindResult] =
    call(
      client.findServicePrincipals(
        g.FindServicePrincipalQuery(
          offset = payload.offset,
          size = payload.size,
          services = payload.services.map(_.toSeq).getOrElse(Seq.empty),
          principalCodes = payload.principalCodes.map(_.toSeq).getOrElse(Seq.empty),
          sortBy = sortByToProto(payload.sortBy)
        )
      )
    ).map(toDomain)

  // === User queries ===

  override def findScopesByCategory(payload: ScopeByCategoryFindQuery): Future[Seq[ScopeByCategoryFindResult]] =
    call(
      client.findScopesByCategory(
        g.ScopeByCategoryFindQuery(
          categories = payload.categories.toSeq,
          principalCodes = payload.principalCodes.toSeq
        )
      )
    ).map(
      _.results.map(r => ScopeByCategoryFindResult(scopeId = r.scopeId, principal = unwrapPrincipal(r.principal)))
    )

  override def getScopeServices(payload: ScopeServicesQuery): Future[ScopeServicesResult] =
    call(
      client.getScopeServices(
        g.ScopeServicesQuery(
          scopeId = payload.scopeId,
          principalCodes = payload.principalCodes.toSeq,
          languageId = payload.languageId
        )
      )
    ).map { r =>
      ScopeServicesResult(
        root = r.root,
        serviceItems = r.serviceItemsJson.map(json => Json.parse(json).as[UserServiceItem](UserServiceItem.format))
      )
    }

  override def findUserServices(payload: FindUserServicesQuery): Future[UserServicesResult] =
    call(
      client.findUserServices(
        g.FindUserServicesQuery(
          offset = payload.offset,
          size = payload.size,
          filter = payload.filter,
          principalCodes = payload.principalCodes.toSeq,
          languageId = payload.languageId
        )
      )
    ).map { r =>
      UserServicesResult(
        total = r.total,
        services = r.servicesJson.map(json => Json.parse(json).as[UserService](UserService.format))
      )
    }
}
