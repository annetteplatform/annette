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

package biz.lobachev.annette.org_structure.impl

import java.util.concurrent.TimeUnit
import akka.util.Timeout
import akka.{Done, NotUsed}
import biz.lobachev.annette.core.model.elastic.FindResult
import biz.lobachev.annette.core.model.PersonId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.org_structure.api.OrgStructureServiceApi
import biz.lobachev.annette.org_structure.api.category.{
  CreateCategoryPayload,
  DeleteCategoryPayload,
  OrgCategory,
  OrgCategoryFindQuery,
  OrgCategoryId,
  UpdateCategoryPayload
}
import biz.lobachev.annette.org_structure.api.hierarchy._
import biz.lobachev.annette.org_structure.api.role._
import biz.lobachev.annette.org_structure.impl.category.CategoryEntityService
import biz.lobachev.annette.org_structure.impl.hierarchy.HierarchyEntityService
import biz.lobachev.annette.org_structure.impl.role.OrgRoleEntityService
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.Try

class OrgStructureServiceApiImpl(
  hierarchyEntityService: HierarchyEntityService,
  orgRoleEntityService: OrgRoleEntityService,
  categoryEntityService: CategoryEntityService,
  config: Config,
  implicit val ec: ExecutionContext
) extends OrgStructureServiceApi {
  implicit val timeout =
    Try(config.getDuration("annette.timeout"))
      .map(d => Timeout(FiniteDuration(d.toNanos, TimeUnit.NANOSECONDS)))
      .getOrElse(Timeout(60.seconds))

  // ****************************** Hierarchy methods ******************************

  override def createOrganization: ServiceCall[CreateOrganizationPayload, Done] =
    ServiceCall { payload =>
      for {
        category <- categoryEntityService.getCategoryById(payload.categoryId)
        result   <- if (category.forOrganization) hierarchyEntityService.createOrganization(payload)
                    else Future.failed(IncorrectCategory())
      } yield result
    }

  override def deleteOrganization: ServiceCall[DeleteOrganizationPayload, Done] = { payload =>
    hierarchyEntityService.deleteOrganization(payload)
  }

  override def getOrganizationById(orgId: OrgItemId): ServiceCall[NotUsed, Organization] = { _ =>
    hierarchyEntityService.getOrganizationById(orgId)
  }

  override def getOrganizationTree(orgId: OrgItemId, itemId: OrgItemId): ServiceCall[NotUsed, OrganizationTree] = { _ =>
    hierarchyEntityService.getOrganizationTree(orgId, itemId)
  }

  override def createUnit: ServiceCall[CreateUnitPayload, Done] =
    ServiceCall { payload =>
      for {
        category <- categoryEntityService.getCategoryById(payload.categoryId)
        result   <- if (category.forUnit) hierarchyEntityService.createUnit(payload)
                    else Future.failed(IncorrectCategory())
      } yield result
    }

  override def deleteUnit: ServiceCall[DeleteUnitPayload, Done] =
    ServiceCall { payload =>
      hierarchyEntityService.deleteUnit(payload)
    }

  override def assignChief: ServiceCall[AssignChiefPayload, Done] =
    ServiceCall { payload =>
      hierarchyEntityService.assignChief(payload)
    }

  override def unassignChief: ServiceCall[UnassignChiefPayload, Done] =
    ServiceCall { payload =>
      hierarchyEntityService.unassignChief(payload)
    }

  override def createPosition: ServiceCall[CreatePositionPayload, Done] =
    ServiceCall { payload =>
      for {
        category <- categoryEntityService.getCategoryById(payload.categoryId)
        result   <- if (category.forPosition) hierarchyEntityService.createPosition(payload)
                    else Future.failed(IncorrectCategory())
      } yield result
    }

  override def deletePosition: ServiceCall[DeletePositionPayload, Done] =
    ServiceCall { payload =>
      hierarchyEntityService.deletePosition(payload)
    }

  override def updateName: ServiceCall[UpdateNamePayload, Done] =
    ServiceCall { payload =>
      hierarchyEntityService.updateName(payload)
    }

  override def updateShortName: ServiceCall[UpdateShortNamePayload, Done] =
    ServiceCall { payload =>
      hierarchyEntityService.updateShortName(payload)
    }

  override def assignCategory: ServiceCall[AssignCategoryPayload, Done] =
    ServiceCall { payload =>
      for {
        category <- categoryEntityService.getCategoryById(payload.categoryId)
        result   <- hierarchyEntityService.assignCategory(payload, category)
      } yield result
    }

  def changePositionLimit: ServiceCall[ChangePositionLimitPayload, Done] =
    ServiceCall { payload =>
      hierarchyEntityService.changePositionLimit(payload)
    }

  override def assignPerson: ServiceCall[AssignPersonPayload, Done] =
    ServiceCall { payload =>
      hierarchyEntityService.assignPerson(payload)
    }

  override def unassignPerson: ServiceCall[UnassignPersonPayload, Done] =
    ServiceCall { payload =>
      hierarchyEntityService.unassignPerson(payload)
    }

  override def assignOrgRole: ServiceCall[AssignOrgRolePayload, Done] =
    ServiceCall { payload =>
      hierarchyEntityService.assignOrgRole(payload)
    }

  override def unassignOrgRole: ServiceCall[UnassignOrgRolePayload, Done] =
    ServiceCall { payload =>
      hierarchyEntityService.unassignOrgRole(payload)
    }

  override def moveItem: ServiceCall[MoveItemPayload, Done] =
    ServiceCall { payload =>
      hierarchyEntityService.moveItem(payload)
    }

  override def changeItemOrder: ServiceCall[ChangeItemOrderPayload, Done] =
    ServiceCall { payload =>
      hierarchyEntityService.changeItemOrder(payload)
    }

  // ****************************** OrgItem methods ******************************

  override def getOrgItemById(orgId: OrgItemId, id: OrgItemId): ServiceCall[NotUsed, OrgItem] =
    ServiceCall { _ =>
      hierarchyEntityService.getOrgItemById(orgId, id)
    }

  override def getOrgItemByIdFromReadSide(id: OrgItemId): ServiceCall[NotUsed, OrgItem] =
    ServiceCall { _ =>
      hierarchyEntityService.getOrgItemByIdFromReadSide(id)
    }

  override def getOrgItemsById(orgId: OrgItemId): ServiceCall[Set[OrgItemId], Map[OrgItemId, OrgItem]] =
    ServiceCall { ids =>
      hierarchyEntityService.getOrgItemsById(orgId, ids)
    }

  override def getOrgItemsByIdFromReadSide: ServiceCall[Set[OrgItemId], Map[OrgItemId, OrgItem]] =
    ServiceCall { ids =>
      hierarchyEntityService.getOrgItemsByIdFromReadSide(ids)
    }

  override def findOrgItems: ServiceCall[OrgItemFindQuery, FindResult] =
    ServiceCall { payload =>
      hierarchyEntityService.findOrgItems(payload)
    }

  override def getPersonPrincipals(personId: PersonId): ServiceCall[NotUsed, Set[AnnettePrincipal]] =
    ServiceCall { _ =>
      hierarchyEntityService.getPersonPrincipals(personId)
    }

  override def getPersonPositions(personId: PersonId): ServiceCall[NotUsed, Set[PersonPosition]] =
    ServiceCall { _ =>
      hierarchyEntityService.getPersonPositions(personId)
    }

  // ****************************** OrgRoles methods ******************************

  override def createOrgRole: ServiceCall[CreateOrgRolePayload, Done] =
    ServiceCall { payload =>
      orgRoleEntityService.createOrgRole(payload)
    }

  override def updateOrgRole: ServiceCall[UpdateOrgRolePayload, Done] =
    ServiceCall { payload =>
      orgRoleEntityService.updateOrgRole(payload)
    }

  override def deleteOrgRole: ServiceCall[DeleteOrgRolePayload, Done] =
    ServiceCall { payload =>
      orgRoleEntityService.deleteOrgRole(payload)
    }

  override def getOrgRoleById(id: OrgRoleId, fromReadSide: Boolean): ServiceCall[NotUsed, OrgRole] =
    ServiceCall { _ =>
      orgRoleEntityService.getOrgRoleById(id, fromReadSide)
    }

  override def getOrgRolesById(fromReadSide: Boolean): ServiceCall[Set[OrgRoleId], Map[OrgRoleId, OrgRole]] =
    ServiceCall { ids =>
      orgRoleEntityService.getOrgRolesById(ids, fromReadSide)
    }

  override def findOrgRoles: ServiceCall[OrgRoleFindQuery, FindResult] =
    ServiceCall { query =>
      orgRoleEntityService.findOrgRoles(query)
    }

  // ****************************** Category methods ******************************

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

  override def getCategoryById(id: OrgCategoryId, fromReadSide: Boolean): ServiceCall[NotUsed, OrgCategory] =
    ServiceCall { _ =>
      categoryEntityService.getCategoryById(id, fromReadSide)
    }

  override def getCategoriesById(
    fromReadSide: Boolean
  ): ServiceCall[Set[OrgCategoryId], Map[OrgCategoryId, OrgCategory]] =
    ServiceCall { ids =>
      categoryEntityService.getCategoriesById(ids, fromReadSide)
    }

  override def findCategories: ServiceCall[OrgCategoryFindQuery, FindResult] =
    ServiceCall { query =>
      categoryEntityService.findCategories(query)
    }

}
