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

package biz.lobachev.annette.authorization.api

import akka.{Done, NotUsed}
import biz.lobachev.annette.authorization.api.assignment._
import biz.lobachev.annette.authorization.api.role._
import biz.lobachev.annette.core.exception.AnnetteTransportExceptionSerializer
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.indexing.FindResult
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}

trait AuthorizationServiceApi extends Service {

  def createRole: ServiceCall[CreateRolePayload, Done]
  def updateRole: ServiceCall[UpdateRolePayload, Done]
  def deleteRole: ServiceCall[DeleteRolePayload, Done]
  def getRoleById(id: AuthRoleId, fromReadSide: Boolean = true): ServiceCall[NotUsed, AuthRole]
  def getRolesById(fromReadSide: Boolean = true): ServiceCall[Set[AuthRoleId], Seq[AuthRole]]
  def findRoles: ServiceCall[AuthRoleFindQuery, FindResult]

  def assignPrincipal: ServiceCall[AssignPrincipalPayload, Done]
  def unassignPrincipal: ServiceCall[UnassignPrincipalPayload, Done]
  def getRolePrincipals(id: AuthRoleId, fromReadSide: Boolean = true): ServiceCall[NotUsed, Set[AnnettePrincipal]]

  def assignPermission: ServiceCall[AssignPermissionPayload, Done]
  def unassignPermission: ServiceCall[UnassignPermissionPayload, Done]
  def findPermissions: ServiceCall[FindPermissions, Set[PermissionAssignment]]
  def checkAllPermission: ServiceCall[CheckPermissions, Boolean]
  def checkAnyPermission: ServiceCall[CheckPermissions, Boolean]
  def findAssignments: ServiceCall[FindAssignmentsQuery, AssignmentFindResult]

  final override def descriptor = {
    import Service._
    // @formatter:off
    named("authorization")
      .withCalls(
        pathCall("/api/authorization/v1/createRole",                   createRole ),
        pathCall("/api/authorization/v1/updateRole",                   updateRole ),
        pathCall("/api/authorization/v1/deleteRole",                   deleteRole ),
        pathCall("/api/authorization/v1/getRoleById/:id/:fromReadSide",getRoleById _),
        pathCall("/api/authorization/v1/getRolesById/:fromReadSide",   getRolesById _),
        pathCall("/api/authorization/v1/findRoles",                    findRoles ),
        pathCall("/api/authorization/v1/assignPrincipal",              assignPrincipal ),
        pathCall("/api/authorization/v1/unassignPrincipal",            unassignPrincipal ),
        pathCall("/api/authorization/v1/getRolePrincipals/:id/:fromReadSide", getRolePrincipals _),

        pathCall("/api/authorization/v1/assignPermission",   assignPermission ),
        pathCall("/api/authorization/v1/unassignPermission", unassignPermission ),
        pathCall("/api/authorization/v1/findAssignments",    findAssignments ),
        pathCall("/api/authorization/v1/findPermissions",    findPermissions ),
        pathCall("/api/authorization/v1/checkAllPermission", checkAllPermission ),
        pathCall("/api/authorization/v1/checkAnyPermission", checkAnyPermission ),
      )
      .withExceptionSerializer(new AnnetteTransportExceptionSerializer())
      .withAutoAcl(true)
    // @formatter:on
  }
}
