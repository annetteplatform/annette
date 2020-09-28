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

import biz.lobachev.annette.core.model.{AnnettePrincipal, Permission, PermissionId}
import biz.lobachev.annette.gateway.core.authentication.AuthenticatedRequest
import org.slf4j.{Logger, LoggerFactory}
import pureconfig._
import pureconfig.generic.auto._

import scala.concurrent.{ExecutionContext, Future}

// TODO: add logging of unauthorized action
class ConfigurationAuthorizer(implicit
  val executionContext: ExecutionContext
) extends Authorizer {

  final private val log: Logger = LoggerFactory.getLogger(this.getClass)

  import biz.lobachev.annette.gateway.core.authorization.ConfigurationAuthorizer._

  val assignments: Set[PrincipalPermission] =
    ConfigSource.default
      .at("annette.authorization.config-authorizer")
      .load[AuthorizerConfig]
      .fold(
        failure => {
          log.error("Config load error", failure.prettyPrint())
          Set.empty
        },
        config =>
          config.assignments.flatMap { assignment =>
            val permissions: Set[Permission] = config.roles
              .get(assignment.roleId)
              .getOrElse {
                log.error("Role {} not found in config", assignment.roleId)
                Set.empty
              }
            permissions.map { permission =>
              PrincipalPermission(
                AnnettePrincipal(assignment.principalType, assignment.principalId),
                permission
              )
            }
          }
      )

  def findPermissions[A](
    permissionIds: PermissionId*
  )(implicit request: AuthenticatedRequest[A]): Future[Set[PrincipalPermission]] =
    Future.successful {
      val permissionIdSet = permissionIds.toSet
      val principals      = request.subject.principals.toSet
      assignments.filter(assignment =>
        permissionIdSet.contains(assignment.permission.id) && principals.contains(assignment.principal)
      )
    }

  def checkAll[A](permissions: Permission*)(implicit request: AuthenticatedRequest[A]): Future[Boolean] =
    check(permissions.toSet, request, true)

  def checkAny[A](permissions: Permission*)(implicit request: AuthenticatedRequest[A]): Future[Boolean] =
    check(permissions.toSet, request, false)

  private def check[A](permissions: Set[Permission], request: AuthenticatedRequest[A], checkAll: Boolean) =
    Future.successful {
      val principals       = request.subject.principals.toSet
      val foundPermissions = assignments
        .filter(assignment => permissions.contains(assignment.permission) && principals.contains(assignment.principal))
        .map(_.permission)
      if (checkAll) foundPermissions.intersect(permissions).size == permissions.size
      else foundPermissions.intersect(permissions).size > 0
    }
}

object ConfigurationAuthorizer {

  case class AuthorizerConfig(
    roles: Map[String, Set[Permission]],
    assignments: Set[Assignment]
  )

  case class Assignment(
    roleId: String,
    principalType: String,
    principalId: String
  )

}
