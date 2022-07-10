package biz.lobachev.annette.service_catalog.impl

import akka.util.Timeout
import akka.{Done, NotUsed}
import biz.lobachev.annette.core.model.category._
import biz.lobachev.annette.core.model.indexing.FindResult
import biz.lobachev.annette.service_catalog.api._
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

import scala.concurrent.duration._

class ServiceCatalogServiceImpl(
  categoryEntityService: CategoryEntityService,
  scopeEntityService: ScopeEntityService,
  scopePrincipalEntityService: ScopePrincipalEntityService,
  groupEntityService: GroupEntityService,
  serviceEntityService: ServiceEntityService,
  servicePrincipalEntityService: ServicePrincipalEntityService
) extends ServiceCatalogServiceApi {

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

  override def findScopes: ServiceCall[ScopeFindQuery, FindResult] =
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

  override def findScopePrincipals: ServiceCall[ScopePrincipalFindQuery, FindResult] =
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

  override def findGroups: ServiceCall[GroupFindQuery, FindResult] =
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

  override def findServices: ServiceCall[ServiceFindQuery, FindResult] =
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

  override def findServicePrincipals: ServiceCall[ServicePrincipalFindQuery, FindResult] =
    ServiceCall { query =>
      servicePrincipalEntityService.findServicePrincipals(query)
    }

}
