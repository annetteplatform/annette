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

package biz.lobachev.annette.authorization.impl

import java.util.concurrent.TimeUnit
import akka.util.Timeout
import akka.{Done, NotUsed}
import biz.lobachev.annette.authorization.api.AuthorizationServiceApi
import biz.lobachev.annette.authorization.api.assignment._
import biz.lobachev.annette.authorization.api.role._
import biz.lobachev.annette.authorization.impl.assignment.AssignmentEntityService
import biz.lobachev.annette.authorization.impl.role.RoleEntityService
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.indexing.FindResult
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.typesafe.config.Config

import scala.concurrent.duration._
import scala.util.Try

class AuthorizationServiceApiImpl(
  roleEntityService: RoleEntityService,
  assignmentEntityService: AssignmentEntityService,
  config: Config
) extends AuthorizationServiceApi {
  implicit val timeout =
    Try(config.getDuration("annette.timeout"))
      .map(d => Timeout(FiniteDuration(d.toNanos, TimeUnit.NANOSECONDS)))
      .getOrElse(Timeout(60.seconds))

  override def createRole: ServiceCall[CreateRolePayload, Done] =
    ServiceCall { payload =>
      roleEntityService.createRole(payload)
    }

  override def updateRole: ServiceCall[UpdateRolePayload, Done] =
    ServiceCall { payload =>
      roleEntityService.updateRole(payload)
    }

  override def deleteRole: ServiceCall[DeleteRolePayload, Done] =
    ServiceCall { payload =>
      roleEntityService.deleteRole(payload)
    }

  override def assignPrincipal: ServiceCall[AssignPrincipalPayload, Done] =
    ServiceCall { payload =>
      roleEntityService.assignPrincipal(payload)
    }

  override def unassignPrincipal: ServiceCall[UnassignPrincipalPayload, Done] =
    ServiceCall { payload =>
      roleEntityService.unassignPrincipal(payload)
    }

  override def getRole(id: AuthRoleId, fromReadSide: Boolean): ServiceCall[NotUsed, AuthRole] =
    ServiceCall { _ =>
      roleEntityService.getRole(id, fromReadSide)
    }

  override def getRolePrincipals(id: AuthRoleId, fromReadSide: Boolean): ServiceCall[NotUsed, Set[AnnettePrincipal]] =
    ServiceCall { _ =>
      roleEntityService.getRolePrincipals(id, fromReadSide)
    }

  override def getRoles(fromReadSide: Boolean): ServiceCall[Set[AuthRoleId], Seq[AuthRole]] =
    ServiceCall { payload =>
      roleEntityService.getRoles(payload, fromReadSide)
    }

  override def findRoles: ServiceCall[AuthRoleFindQuery, FindResult] =
    ServiceCall { payload =>
      roleEntityService.findRoles(payload)
    }

  override def assignPermission: ServiceCall[AssignPermissionPayload, Done] =
    ServiceCall { payload =>
      assignmentEntityService.assignPermission(payload)
    }

  override def unassignPermission: ServiceCall[UnassignPermissionPayload, Done] =
    ServiceCall { payload =>
      assignmentEntityService.unassignPermission(payload)
    }

  override def findPermissions: ServiceCall[FindPermissions, Set[PermissionAssignment]] =
    ServiceCall { payload =>
      assignmentEntityService.findPermissions(payload)
    }

  override def checkAnyPermission: ServiceCall[CheckPermissions, Boolean] =
    ServiceCall { payload =>
      assignmentEntityService.checkAnyPermission(payload)
    }

  override def checkAllPermission: ServiceCall[CheckPermissions, Boolean] =
    ServiceCall { payload =>
      assignmentEntityService.checkAllPermission(payload)
    }

  override def findAssignments: ServiceCall[FindAssignmentsQuery, AssignmentFindResult] =
    ServiceCall { payload =>
      assignmentEntityService.findAssignments(payload)
    }

}
