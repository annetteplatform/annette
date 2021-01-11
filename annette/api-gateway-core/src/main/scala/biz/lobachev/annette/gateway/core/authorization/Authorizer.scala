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

import biz.lobachev.annette.core.model.PermissionId
import biz.lobachev.annette.core.model.auth.Permission
import biz.lobachev.annette.gateway.core.authentication.AuthenticatedRequest

import scala.concurrent.{ExecutionContext, Future}

trait Authorizer {

  implicit val executionContext: ExecutionContext

  def findPermissions[A](
    permissionIds: PermissionId*
  )(implicit request: AuthenticatedRequest[A]): Future[Set[PrincipalPermission]]

  def checkAll[A](permissions: Permission*)(implicit request: AuthenticatedRequest[A]): Future[Boolean]

  def checkAny[A](permissions: Permission*)(implicit request: AuthenticatedRequest[A]): Future[Boolean]

  def performFindPermission[A, B](
    permissionIds: PermissionId*
  )(action: Set[PrincipalPermission] => Future[B])(implicit request: AuthenticatedRequest[A]): Future[B] =
    for {
      permissions <- findPermissions(permissionIds: _*)
      result      <- action(permissions)
    } yield result

  def performCheckAll[A, B](
    permissions: Permission*
  )(action: => Future[B])(implicit request: AuthenticatedRequest[A]): Future[B] =
    for {
      isAuthorized <- checkAll(permissions: _*)
      result       <- if (isAuthorized) action
                      else Future.failed(AuthorizationFailedException())
    } yield result

  def performCheckAny[A, B](
    permissions: Permission*
  )(action: => Future[B])(implicit request: AuthenticatedRequest[A]): Future[B] =
    for {
      isAuthorized <- checkAny(permissions: _*)
      result       <- if (isAuthorized) action
                      else Future.failed(AuthorizationFailedException())
    } yield result

  def performCheck[A](isAuthorizedFuture: Future[Boolean])(action: => Future[A]): Future[A] =
    for {
      isAuthorized <- isAuthorizedFuture
      result       <- if (isAuthorized) action
                      else Future.failed(AuthorizationFailedException())
    } yield result
}
