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
import biz.lobachev.annette.core.model.indexing.FindResult
import biz.lobachev.annette.principal_group.api.PrincipalGroupServiceApi
import biz.lobachev.annette.core.model.category._
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
  UpdatePrincipalGroupNamePayload,
  UpdatePrincipalGroupPayload
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

  override def updatePrincipalGroup: ServiceCall[UpdatePrincipalGroupPayload, Done] =
    ServiceCall { payload =>
      groupEntityService.updatePrincipalGroup(payload)
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

  override def getPrincipalGroup(
    id: PrincipalGroupId,
    source: Option[String]
  ): ServiceCall[NotUsed, PrincipalGroup] =
    ServiceCall { _ =>
      groupEntityService.getPrincipalGroup(id, source)
    }

  override def getPrincipalGroups(
    source: Option[String]
  ): ServiceCall[Set[PrincipalGroupId], Seq[PrincipalGroup]] =
    ServiceCall { ids =>
      groupEntityService.getPrincipalGroups(ids, source)
    }

  override def findPrincipalGroups: ServiceCall[PrincipalGroupFindQuery, FindResult] =
    ServiceCall { query =>
      groupEntityService.findPrincipalGroups(query)
    }

  override def getAssignments(id: PrincipalGroupId): ServiceCall[NotUsed, Set[AnnettePrincipal]] =
    ServiceCall { _ =>
      groupEntityService.getAssignments(id)
    }

  override def getPrincipalAssignments: ServiceCall[Set[AnnettePrincipal], Set[PrincipalGroupId]] =
    ServiceCall { principals =>
      groupEntityService.getPrincipalAssignments(principals)
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

  override def getCategory(id: CategoryId, source: Option[String]): ServiceCall[NotUsed, Category] =
    ServiceCall { _ =>
      categoryEntityService.getCategory(id, source)
    }

  override def getCategories(
    source: Option[String]
  ): ServiceCall[Set[CategoryId], Seq[Category]] =
    ServiceCall { ids =>
      categoryEntityService.getCategories(ids, source)
    }

  override def findCategories: ServiceCall[CategoryFindQuery, FindResult] =
    ServiceCall { query =>
      categoryEntityService.findCategories(query)
    }

}
