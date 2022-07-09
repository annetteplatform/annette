package biz.lobachev.annette.service_catalog.api

import akka.{Done, NotUsed}
import biz.lobachev.annette.core.exception.AnnetteTransportExceptionSerializer
import biz.lobachev.annette.core.model.category._
import biz.lobachev.annette.core.model.indexing.FindResult
import biz.lobachev.annette.service_catalog.api.group._
import biz.lobachev.annette.service_catalog.api.scope._
import biz.lobachev.annette.service_catalog.api.scope_principal._
import biz.lobachev.annette.service_catalog.api.service._
import biz.lobachev.annette.service_catalog.api.service_principal._
import com.lightbend.lagom.scaladsl.api.{ServiceCall, Service => LagomService}

trait ServiceCatalogServiceApi extends LagomService {

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
  def getScopesById(fromReadSide: Boolean = true): ServiceCall[Seq[ScopeId], Seq[Scope]]
  def findScopes: ServiceCall[ScopeFindQuery, FindResult]

  def assignScopePrincipal: ServiceCall[AssignScopePrincipalPayload, Done]
  def unassignScopePrincipal: ServiceCall[UnassignScopePrincipalPayload, Done]
  def findScopePrincipals: ServiceCall[ScopePrincipalFindQuery, FindResult]

  def createGroup: ServiceCall[CreateGroupPayload, Done]
  def updateGroup: ServiceCall[UpdateGroupPayload, Done]
  def activateGroup: ServiceCall[ActivateGroupPayload, Done]
  def deactivateGroup: ServiceCall[DeactivateGroupPayload, Done]
  def deleteGroup: ServiceCall[DeleteGroupPayload, Done]
  def getGroupById(id: GroupId, fromReadSide: Boolean = true): ServiceCall[NotUsed, Group]
  def getGroupsById(fromReadSide: Boolean = true): ServiceCall[Seq[GroupId], Seq[Group]]
  def findGroups: ServiceCall[GroupFindQuery, FindResult]

  def createService: ServiceCall[CreateServicePayload, Done]
  def updateService: ServiceCall[UpdateServicePayload, Done]
  def activateService: ServiceCall[ActivateServicePayload, Done]
  def deactivateService: ServiceCall[DeactivateServicePayload, Done]
  def deleteService: ServiceCall[DeleteServicePayload, Done]
  def getServiceById(id: ServiceId, fromReadSide: Boolean = true): ServiceCall[NotUsed, Service]
  def getServicesById(fromReadSide: Boolean = true): ServiceCall[Seq[ServiceId], Seq[Service]]
  def findServices: ServiceCall[ServiceFindQuery, FindResult]

  def assignServicePrincipal: ServiceCall[AssignServicePrincipalPayload, Done]
  def unassignServicePrincipal: ServiceCall[UnassignServicePrincipalPayload, Done]
  def findServicePrincipals: ServiceCall[ServicePrincipalFindQuery, FindResult]

  final override def descriptor = {
    import LagomService._
    named("serviceCatalog")
      .withCalls(
        pathCall("/api/serviceCatalog/v1/createCategory", createCategory),
        pathCall("/api/serviceCatalog/v1/updateCategory", updateCategory),
        pathCall("/api/serviceCatalog/v1/deleteCategory", deleteCategory),
        pathCall("/api/serviceCatalog/v1/getCategoryById/:id/:fromReadSide", getCategoryById _),
        pathCall("/api/serviceCatalog/v1/getCategoriesByIds/:fromReadSide", getCategoriesById _),
        pathCall("/api/serviceCatalog/v1/findCategories", findCategories),
        pathCall("/api/serviceCatalog/v1/createScope", createScope),
        pathCall("/api/serviceCatalog/v1/updateScope", updateScope),
        pathCall("/api/serviceCatalog/v1/activateScope", activateScope),
        pathCall("/api/serviceCatalog/v1/deactivateScope", deactivateScope),
        pathCall("/api/serviceCatalog/v1/deleteScope", deleteScope),
        pathCall("/api/serviceCatalog/v1/getScopeById/:id/:fromReadSide", getScopeById _),
        pathCall("/api/serviceCatalog/v1/getScopesByIds/:fromReadSide", getScopesById _),
        pathCall("/api/serviceCatalog/v1/findScopes", findScopes),
        pathCall("/api/serviceCatalog/v1/assignScopePrincipal", assignScopePrincipal),
        pathCall("/api/serviceCatalog/v1/unassignScopePrincipal", unassignScopePrincipal),
        pathCall("/api/serviceCatalog/v1/findScopePrincipals", findServicePrincipals),
        pathCall("/api/serviceCatalog/v1/createGroup", createGroup),
        pathCall("/api/serviceCatalog/v1/updateGroup", updateGroup),
        pathCall("/api/serviceCatalog/v1/activateGroup", activateGroup),
        pathCall("/api/serviceCatalog/v1/deactivateGroup", deactivateGroup),
        pathCall("/api/serviceCatalog/v1/deleteGroup", deleteGroup),
        pathCall("/api/serviceCatalog/v1/getGroupById/:id/:fromReadSide", getGroupById _),
        pathCall("/api/serviceCatalog/v1/getGroupsByIds/:fromReadSide", getGroupsById _),
        pathCall("/api/serviceCatalog/v1/findGroups", findGroups),
        pathCall("/api/serviceCatalog/v1/createService", createService),
        pathCall("/api/serviceCatalog/v1/updateService", updateService),
        pathCall("/api/serviceCatalog/v1/activateService", activateService),
        pathCall("/api/serviceCatalog/v1/deactivateService", deactivateService),
        pathCall("/api/serviceCatalog/v1/deleteService", deleteService),
        pathCall("/api/serviceCatalog/v1/getServiceById/:id/:fromReadSide", getServiceById _),
        pathCall("/api/serviceCatalog/v1/getServicesByIds/:fromReadSide", getServicesById _),
        pathCall("/api/serviceCatalog/v1/findServices", findServices),
        pathCall("/api/serviceCatalog/v1/assignServicePrincipal", assignServicePrincipal),
        pathCall("/api/serviceCatalog/v1/unassignServicePrincipal", unassignServicePrincipal),
        pathCall("/api/serviceCatalog/v1/findServicePrincipals", findServicePrincipals)
      )
      .withExceptionSerializer(new AnnetteTransportExceptionSerializer())
      .withAutoAcl(true)
  }
}
