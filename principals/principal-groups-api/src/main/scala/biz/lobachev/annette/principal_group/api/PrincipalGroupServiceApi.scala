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

package biz.lobachev.annette.principal_group.api

import akka.{Done, NotUsed}
import biz.lobachev.annette.core.exception.AnnetteTransportExceptionSerializer
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.indexing.FindResult
import biz.lobachev.annette.core.model.category.{
  Category,
  CategoryFindQuery,
  CategoryId,
  CreateCategoryPayload,
  DeleteCategoryPayload,
  UpdateCategoryPayload
}
import biz.lobachev.annette.principal_group.api.group._
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}

trait PrincipalGroupServiceApi extends Service {

  def createPrincipalGroup: ServiceCall[CreatePrincipalGroupPayload, Done]
  def updatePrincipalGroup: ServiceCall[UpdatePrincipalGroupPayload, Done]
  def updatePrincipalGroupName: ServiceCall[UpdatePrincipalGroupNamePayload, Done]
  def updatePrincipalGroupDescription: ServiceCall[UpdatePrincipalGroupDescriptionPayload, Done]
  def updatePrincipalGroupCategory: ServiceCall[UpdatePrincipalGroupCategoryPayload, Done]
  def deletePrincipalGroup: ServiceCall[DeletePrincipalGroupPayload, Done]
  def assignPrincipal: ServiceCall[AssignPrincipalPayload, Done]
  def unassignPrincipal: ServiceCall[UnassignPrincipalPayload, Done]
  def getPrincipalGroup(id: PrincipalGroupId, source: Option[String] = None): ServiceCall[NotUsed, PrincipalGroup]
  def getPrincipalGroups(
    source: Option[String] = None
  ): ServiceCall[Set[PrincipalGroupId], Seq[PrincipalGroup]]
  def findPrincipalGroups: ServiceCall[PrincipalGroupFindQuery, FindResult]
  def getAssignments(id: PrincipalGroupId): ServiceCall[NotUsed, Set[AnnettePrincipal]]
  def getPrincipalAssignments: ServiceCall[Set[AnnettePrincipal], Set[PrincipalGroupId]]

  // org item category

  def createCategory: ServiceCall[CreateCategoryPayload, Done]
  def updateCategory: ServiceCall[UpdateCategoryPayload, Done]
  def deleteCategory: ServiceCall[DeleteCategoryPayload, Done]
  def getCategory(id: CategoryId, source: Option[String]): ServiceCall[NotUsed, Category]
  def getCategories(source: Option[String]): ServiceCall[Set[CategoryId], Seq[Category]]
  def findCategories: ServiceCall[CategoryFindQuery, FindResult]

  final override def descriptor = {
    import Service._
    // @formatter:off
    named("principal-groups")
      .withCalls(
        pathCall("/api/principal-groups/v1/createPrincipalGroup",                    createPrincipalGroup),
        pathCall("/api/principal-groups/v1/updatePrincipalGroup",                    updatePrincipalGroup),
        pathCall("/api/principal-groups/v1/updatePrincipalGroupName",                updatePrincipalGroupName),
        pathCall("/api/principal-groups/v1/updatePrincipalGroupDescription",         updatePrincipalGroupDescription),
        pathCall("/api/principal-groups/v1/updatePrincipalGroupCategory",            updatePrincipalGroupCategory),
        pathCall("/api/principal-groups/v1/deletePrincipalGroup",                    deletePrincipalGroup),
        pathCall("/api/principal-groups/v1/assignPrincipal",                         assignPrincipal),
        pathCall("/api/principal-groups/v1/unassignPrincipal",                       unassignPrincipal),
        pathCall("/api/principal-groups/v1/getPrincipalGroup/:id?source", getPrincipalGroup _),
        pathCall("/api/principal-groups/v1/getPrincipalGroups?source",    getPrincipalGroups _),
        pathCall("/api/principal-groups/v1/findPrincipalGroups",                     findPrincipalGroups),
        pathCall("/api/principal-groups/v1/getAssignments/:id",                      getAssignments _),
        pathCall("/api/principal-groups/v1/getPrincipalAssignments",                 getPrincipalAssignments),

        pathCall("/api/principal-groups/v1/createCategory",                 createCategory),
        pathCall("/api/principal-groups/v1/updateCategory",                 updateCategory),
        pathCall("/api/principal-groups/v1/deleteCategory",                 deleteCategory),
        pathCall("/api/principal-groups/v1/getCategory/:id?readSide",  getCategory _),
        pathCall("/api/principal-groups/v1/getCategories?readSide",    getCategories _) ,
        pathCall("/api/principal-groups/v1/findCategories",                 findCategories),
      )
      .withExceptionSerializer(new AnnetteTransportExceptionSerializer())
      .withAutoAcl(true)
    // @formatter:on
  }
}
