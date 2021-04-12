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

package biz.lobachev.annette.principal_group.impl

import akka.{Done, NotUsed}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.elastic.FindResult
import biz.lobachev.annette.principal_group.api.PrincipalGroupServiceApi
import biz.lobachev.annette.principal_group.api.category._
import biz.lobachev.annette.principal_group.api.group.{
  AssignPrincipalPayload,
  CreatePrincipalGroupPayload,
  DeletePrincipalGroupPayload,
  PrincipalGroup,
  PrincipalGroupFindQuery,
  PrincipalGroupId,
  UnassignPrincipalPayload,
  UpdatePrincipalGroupCategoryPayload,
  UpdatePrincipalGroupDescriptionPayload,
  UpdatePrincipalGroupNamePayload
}
import biz.lobachev.annette.principal_group.impl.category.CategoryEntityService
import biz.lobachev.annette.principal_group.impl.group.PrincipalGroupEntityService
import com.lightbend.lagom.scaladsl.api.ServiceCall

class PrincipalGroupServiceApiImpl(
  groupEntityService: PrincipalGroupEntityService,
  categoryEntityService: CategoryEntityService
) extends PrincipalGroupServiceApi {

  override def createPrincipalGroup: ServiceCall[CreatePrincipalGroupPayload, Done] =
    ServiceCall { payload =>
      groupEntityService.createPrincipalGroup(payload)
    }

  override def updatePrincipalGroupName: ServiceCall[UpdatePrincipalGroupNamePayload, Done] =
    ServiceCall { payload =>
      groupEntityService.updatePrincipalGroupName(payload)
    }

  override def updatePrincipalGroupDescription: ServiceCall[UpdatePrincipalGroupDescriptionPayload, Done] =
    ServiceCall { payload =>
      groupEntityService.updatePrincipalGroupDescription(payload)
    }

  override def updatePrincipalGroupCategory: ServiceCall[UpdatePrincipalGroupCategoryPayload, Done] =
    ServiceCall { payload =>
      groupEntityService.updatePrincipalGroupCategory(payload)
    }

  override def deletePrincipalGroup: ServiceCall[DeletePrincipalGroupPayload, Done] =
    ServiceCall { payload =>
      groupEntityService.deletePrincipalGroup(payload)
    }

  override def assignPrincipal: ServiceCall[AssignPrincipalPayload, Done] =
    ServiceCall { payload =>
      groupEntityService.assignPrincipal(payload)
    }

  override def unassignPrincipal: ServiceCall[UnassignPrincipalPayload, Done] =
    ServiceCall { payload =>
      groupEntityService.unassignPrincipal(payload)
    }

  override def getPrincipalGroupById(
    id: PrincipalGroupId,
    fromReadSide: Boolean
  ): ServiceCall[NotUsed, PrincipalGroup] =
    ServiceCall { _ =>
      groupEntityService.getPrincipalGroupById(id, fromReadSide)
    }

  override def getPrincipalGroupsById(
    fromReadSide: Boolean
  ): ServiceCall[Set[PrincipalGroupId], Map[PrincipalGroupId, PrincipalGroup]] =
    ServiceCall { ids =>
      groupEntityService.getPrincipalGroupsById(ids, fromReadSide)
    }

  override def findPrincipalGroups: ServiceCall[PrincipalGroupFindQuery, FindResult] =
    ServiceCall { query =>
      groupEntityService.findPrincipalGroups(query)
    }

  override def getAssignments(id: PrincipalGroupId): ServiceCall[NotUsed, Set[AnnettePrincipal]] =
    ServiceCall { _ =>
      groupEntityService.getAssignments(id)
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

  override def getCategoryById(id: CategoryId, fromReadSide: Boolean): ServiceCall[NotUsed, Category] =
    ServiceCall { _ =>
      categoryEntityService.getCategoryById(id, fromReadSide)
    }

  override def getCategoriesById(
    fromReadSide: Boolean
  ): ServiceCall[Set[CategoryId], Map[CategoryId, Category]] =
    ServiceCall { ids =>
      categoryEntityService.getCategoriesById(ids, fromReadSide)
    }

  override def findCategories: ServiceCall[CategoryFindQuery, FindResult] =
    ServiceCall { query =>
      categoryEntityService.findCategories(query)
    }

}
