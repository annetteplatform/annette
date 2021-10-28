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
import biz.lobachev.annette.authorization.api.assignment.{
  AssignPermissionPayload,
  AssignmentFindResult,
  CheckPermissions,
  FindAssignmentsQuery,
  FindPermissions,
  PermissionAssignment,
  UnassignPermissionPayload
}
import biz.lobachev.annette.authorization.api.role.{
  AssignPrincipalPayload,
  AuthRole,
  AuthRoleFindQuery,
  AuthRoleId,
  CreateRolePayload,
  DeleteRolePayload,
  RoleAlreadyExist,
  UnassignPrincipalPayload,
  UpdateRolePayload
}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.indexing.FindResult
import io.scalaland.chimney.dsl._

import scala.concurrent.{ExecutionContext, Future}

class AuthorizationServiceImpl(api: AuthorizationServiceApi, implicit val ec: ExecutionContext)
    extends AuthorizationService {
  override def createRole(payload: CreateRolePayload): Future[Done] = api.createRole.invoke(payload)

  override def updateRole(payload: UpdateRolePayload): Future[Done] = api.updateRole.invoke(payload)

  def createOrUpdateRole(payload: CreateRolePayload): Future[Done] =
    createRole(payload).recoverWith {
      case RoleAlreadyExist(_) =>
        val updatePayload = payload
          .into[UpdateRolePayload]
          .withFieldComputed(_.updatedBy, _.createdBy)
          .transform
        updateRole(updatePayload)
      case th                  => Future.failed(th)
    }

  override def deleteRole(payload: DeleteRolePayload): Future[Done] = api.deleteRole.invoke(payload)

  override def getRoleById(id: AuthRoleId, fromReadSide: Boolean): Future[AuthRole] =
    api.getRoleById(id, fromReadSide).invoke()

  override def getRolesById(ids: Set[AuthRoleId], fromReadSide: Boolean): Future[Seq[AuthRole]] =
    api.getRolesById(fromReadSide).invoke(ids)

  override def findRoles(payload: AuthRoleFindQuery): Future[FindResult] = api.findRoles.invoke(payload)

  override def assignPrincipal(payload: AssignPrincipalPayload): Future[Done] = api.assignPrincipal.invoke(payload)

  override def unassignPrincipal(payload: UnassignPrincipalPayload): Future[Done] =
    api.unassignPrincipal.invoke(payload)

  override def getRolePrincipals(id: AuthRoleId, fromReadSide: Boolean): Future[Set[AnnettePrincipal]] =
    api.getRolePrincipals(id, fromReadSide).invoke()

  override def assignPermission(payload: AssignPermissionPayload): Future[Done] = api.assignPermission.invoke(payload)

  override def unassignPermission(payload: UnassignPermissionPayload): Future[Done] =
    api.unassignPermission.invoke(payload)

  override def findPermissions(payload: FindPermissions): Future[Set[PermissionAssignment]] =
    api.findPermissions.invoke(payload)

  override def checkAllPermission(payload: CheckPermissions): Future[Boolean] = api.checkAllPermission.invoke(payload)

  override def checkAnyPermission(payload: CheckPermissions): Future[Boolean] = api.checkAnyPermission.invoke(payload)

  override def findAssignments(payload: FindAssignmentsQuery): Future[AssignmentFindResult] =
    api.findAssignments.invoke(payload)
}
