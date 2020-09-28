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

package biz.lobachev.annette.gateway.api.authorization

import biz.lobachev.annette.authorization.api.AuthorizationService
import biz.lobachev.annette.authorization.api.assignment.FindAssignmentsQuery
import biz.lobachev.annette.authorization.api.role._
import biz.lobachev.annette.gateway.api.authorization.Permissions._
import biz.lobachev.annette.gateway.core.authentication.AuthenticatedAction
import biz.lobachev.annette.gateway.core.authorization.Authorizer
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, ControllerComponents}

import scala.concurrent.ExecutionContext

@Singleton
class AuthorizationController @Inject() (
  authenticated: AuthenticatedAction,
  authorizer: Authorizer,
  authorizationService: AuthorizationService,
  cc: ControllerComponents,
  implicit val ec: ExecutionContext
) extends AbstractController(cc) {

  // private val log = LoggerFactory.getLogger(this.getClass)

  def createRole: Action[CreateRolePayload] =
    authenticated.async(parse.json[CreateRolePayload]) { implicit request =>
      authorizer.performCheckAll(MAINTAIN_AUTHORIZATION_ROLE) {
        val payload = request.body
        for {
          _    <- authorizationService.createRole(payload)
          role <- authorizationService.getRoleById(payload.id, false)
        } yield Ok(Json.toJson(role))
      }
    }

  def updateRole =
    authenticated.async(parse.json[UpdateRolePayload]) { implicit request =>
      authorizer.performCheckAll(MAINTAIN_AUTHORIZATION_ROLE) {
        val payload = request.body
        for {
          _    <- authorizationService.updateRole(payload)
          role <- authorizationService.getRoleById(payload.id, false)
        } yield Ok(Json.toJson(role))
      }
    }

  def deleteRole =
    authenticated.async(parse.json[DeleteRolePayload]) { implicit request =>
      authorizer.performCheckAll(MAINTAIN_AUTHORIZATION_ROLE) {
        val payload = request.body
        for {
          _ <- authorizationService.deleteRole(payload)
        } yield Ok("")
      }
    }

  def findRoles =
    authenticated.async(parse.json[AuthRoleFindQuery]) { implicit request =>
      authorizer.performCheckAny(VIEW_AUTHORIZATION_ROLE, MAINTAIN_AUTHORIZATION_ROLE) {
        val payload = request.body
        for {
          roles <- authorizationService.findRoles(payload)
        } yield Ok(Json.toJson(roles))
      }
    }

  def getRoleById(id: AuthRoleId, fromReadSide: Boolean) =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(VIEW_AUTHORIZATION_ROLE, MAINTAIN_AUTHORIZATION_ROLE) {
        println(fromReadSide)
        for {
          role <- authorizationService.getRoleById(id, fromReadSide)
        } yield Ok(Json.toJson(role))
      }
    }

  def getRolesById(fromReadSide: Boolean) =
    authenticated.async(parse.json[Set[AuthRoleId]]) { implicit request =>
      authorizer.performCheckAny(VIEW_AUTHORIZATION_ROLE, MAINTAIN_AUTHORIZATION_ROLE) {
        val payload = request.body
        for {
          roles <- authorizationService.getRolesById(payload, fromReadSide)
        } yield Ok(Json.toJson(roles))
      }
    }

  def assignPrincipal =
    authenticated.async(parse.json[AssignPrincipalPayload]) { implicit request =>
      authorizer.performCheckAll(MAINTAIN_ROLE_PRINCIPALS) {
        val payload = request.body
        for {
          _ <- authorizationService.assignPrincipal(payload)
        } yield Ok("")
      }
    }

  def unassignPrincipal =
    authenticated.async(parse.json[UnassignPrincipalPayload]) { implicit request =>
      authorizer.performCheckAll(MAINTAIN_ROLE_PRINCIPALS) {
        val payload = request.body
        for {
          _ <- authorizationService.unassignPrincipal(payload)
        } yield Ok("")
      }
    }

  def getRolePrincipals(id: AuthRoleId, fromReadSide: Boolean) =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(VIEW_ROLE_PRINCIPALS, MAINTAIN_ROLE_PRINCIPALS) {
        for {
          role <- authorizationService.getRolePrincipals(id, fromReadSide)
        } yield Ok(Json.toJson(role))
      }
    }

  def findAssignments =
    authenticated.async(parse.json[FindAssignmentsQuery]) { implicit request =>
      authorizer.performCheckAll(VIEW_ASSIGNMENTS) {
        val payload = request.body
        for {
          result <- authorizationService.findAssignments(payload)
        } yield Ok(Json.toJson(result))
      }
    }
}
