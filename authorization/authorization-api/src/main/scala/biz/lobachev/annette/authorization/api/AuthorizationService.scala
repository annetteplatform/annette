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

import akka.Done
import biz.lobachev.annette.authorization.api.assignment._
import biz.lobachev.annette.authorization.api.role._
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.indexing.FindResult

import scala.concurrent.Future

trait AuthorizationService {

  def createRole(payload: CreateRolePayload): Future[Done]
  def updateRole(payload: UpdateRolePayload): Future[Done]
  def createOrUpdateRole(payload: CreateRolePayload): Future[Done]
  def deleteRole(payload: DeleteRolePayload): Future[Done]
  def getRoleById(id: AuthRoleId, fromReadSide: Boolean = true): Future[AuthRole]
  def getRolesById(ids: Set[AuthRoleId], fromReadSide: Boolean = true): Future[Seq[AuthRole]]
  def findRoles(payload: AuthRoleFindQuery): Future[FindResult]

  def assignPrincipal(payload: AssignPrincipalPayload): Future[Done]
  def unassignPrincipal(payload: UnassignPrincipalPayload): Future[Done]
  def getRolePrincipals(id: AuthRoleId, fromReadSide: Boolean = true): Future[Set[AnnettePrincipal]]

  def assignPermission(payload: AssignPermissionPayload): Future[Done]
  def unassignPermission(payload: UnassignPermissionPayload): Future[Done]
  def findPermissions(payload: FindPermissions): Future[Set[PermissionAssignment]]
  def checkAllPermission(payload: CheckPermissions): Future[Boolean]
  def checkAnyPermission(payload: CheckPermissions): Future[Boolean]
  def findAssignments(payload: FindAssignmentsQuery): Future[AssignmentFindResult]

}
