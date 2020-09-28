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

package biz.lobachev.annette.gateway.core.authorization

import biz.lobachev.annette.authorization.api.AuthorizationService
import biz.lobachev.annette.authorization.api.assignment.{CheckPermissions, FindPermissions}
import biz.lobachev.annette.core.model.{Permission, PermissionId}
import biz.lobachev.annette.gateway.core.authentication.AuthenticatedRequest
//import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

// TODO: add logging of unauthorized action
class AuthorizationServiceAuthorizer(
  authorizationService: AuthorizationService,
  implicit val executionContext: ExecutionContext
) extends Authorizer {

//  final private val log: Logger = LoggerFactory.getLogger(this.getClass)

  def findPermissions[A](
    permissionIds: PermissionId*
  )(implicit request: AuthenticatedRequest[A]): Future[Set[PrincipalPermission]] =
    for {
      permissionAssignments <- authorizationService.findPermissions(
                                 FindPermissions(request.subject.principals.toSet, permissionIds.toSet)
                               )
    } yield permissionAssignments.map(assignment => PrincipalPermission(assignment.principal, assignment.permission))

  def checkAll[A](permissions: Permission*)(implicit request: AuthenticatedRequest[A]): Future[Boolean] =
    authorizationService.checkAllPermission(
      CheckPermissions(request.subject.principals.toSet, permissions.toSet)
    )

  def checkAny[A](permissions: Permission*)(implicit request: AuthenticatedRequest[A]): Future[Boolean] =
    authorizationService.checkAnyPermission(
      CheckPermissions(request.subject.principals.toSet, permissions.toSet)
    )

}
