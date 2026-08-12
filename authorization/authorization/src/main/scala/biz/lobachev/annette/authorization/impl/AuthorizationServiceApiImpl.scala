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

import java.time.OffsetDateTime
import com.google.protobuf.empty.Empty
import biz.lobachev.annette.authorization.api.{grpc => g} // generated proto messages, aliased to avoid name collisions
import biz.lobachev.annette.authorization.api.assignment.{
  AssignPermissionPayload,
  AssignmentFindResult,
  AuthSource,
  CheckPermissions,
  FindAssignmentsQuery,
  FindPermissions,
  PermissionAssignment,
  UnassignPermissionPayload
}
import biz.lobachev.annette.authorization.api.grpc.AuthorizationService
import biz.lobachev.annette.authorization.api.role.{
  AssignPrincipalPayload,
  AuthRole,
  AuthRoleFindQuery,
  CreateRolePayload,
  DeleteRolePayload,
  UnassignPrincipalPayload,
  UpdateRolePayload
}
import biz.lobachev.annette.authorization.impl.assignment.AssignmentEntityService
import biz.lobachev.annette.authorization.impl.role.RoleEntityService
import biz.lobachev.annette.core.model.auth.{AnnettePrincipal, Permission}
import biz.lobachev.annette.core.model.indexing.{FindResult, SortBy}
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContext, Future}

/**
 * Implements the generated gRPC trait `AuthorizationService`. Converts proto messages to plain
 * Scala case classes, delegates to the underlying EntityService, converts results back.
 *
 * The proto package `biz.lobachev.annette.authorization.api.grpc` is aliased as `g` so its
 * message companions (which share simple names with the domain case classes — both have
 * `Permission`, `AuthRole`, etc.) can be referenced unambiguously.
 *
 * Per dev/migration/003-core-recipe.md §B and slice 004 step 3.
 */
class AuthorizationServiceApiImpl(
  roleEntityService: RoleEntityService,
  assignmentEntityService: AssignmentEntityService,
  @annotation.unused config: Config
)(implicit ec: ExecutionContext) extends AuthorizationService {

  // --- proto ↔ domain converters (mechanical translation; no business logic) ---
  // Generated proto fields are Options for message-typed fields (proto3 default); unwrap with .get
  // because all required fields are populated by the gateway client. For genuine optional fields
  // (e.g. updatedAt) the Option is preserved end-to-end.

  private def toDomain(p: g.Permission): Permission =
    Permission(id = p.id, arg1 = p.arg1, arg2 = p.arg2, arg3 = p.arg3)

  private def fromDomain(p: Permission): g.Permission =
    g.Permission(id = p.id, arg1 = p.arg1, arg2 = p.arg2, arg3 = p.arg3)

  private def toDomain(p: g.AnnettePrincipal): AnnettePrincipal =
    AnnettePrincipal(p.code)

  private def fromDomain(p: AnnettePrincipal): g.AnnettePrincipal =
    g.AnnettePrincipal(p.code)

  private def toDomain(s: g.AuthSource): AuthSource =
    AuthSource(sourceType = s.sourceType, sourceId = s.sourceId)

  private def fromDomain(s: AuthSource): g.AuthSource =
    g.AuthSource(sourceType = s.sourceType, sourceId = s.sourceId)

  private def formatOdt(odt: OffsetDateTime): String = odt.toString

  private def unwrapPrincipal(p: Option[g.AnnettePrincipal]): AnnettePrincipal =
    p.map(toDomain).getOrElse(throw new IllegalArgumentException("missing principal"))

  private def unwrapPermission(p: Option[g.Permission]): Permission =
    p.map(toDomain).getOrElse(throw new IllegalArgumentException("missing permission"))

  private def unwrapSource(s: Option[g.AuthSource]): AuthSource =
    s.map(toDomain).getOrElse(throw new IllegalArgumentException("missing source"))

  // === Role CRUD ===

  override def createRole(in: g.CreateRolePayload): Future[Empty] =
    roleEntityService
      .createRole(
        CreateRolePayload(
          id = in.id,
          name = in.name,
          description = in.description,
          permissions = in.permissions.map(toDomain).toSet,
          createdBy = unwrapPrincipal(in.createdBy)
        )
      )
      .map(_ => Empty())

  override def updateRole(in: g.UpdateRolePayload): Future[Empty] =
    roleEntityService
      .updateRole(
        UpdateRolePayload(
          id = in.id,
          name = in.name,
          description = in.description,
          permissions = in.permissions.map(toDomain).toSet,
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def deleteRole(in: g.DeleteRolePayload): Future[Empty] =
    roleEntityService
      .deleteRole(DeleteRolePayload(id = in.id, deletedBy = unwrapPrincipal(in.deletedBy)))
      .map(_ => Empty())

  override def getRole(in: g.GetRoleRequest): Future[g.AuthRole] =
    roleEntityService.getRole(in.id, in.source).map(fromDomain)

  override def getRoles(in: g.GetRolesRequest): Future[g.GetRolesResponse] =
    roleEntityService
      .getRoles(in.ids.toSet, in.source)
      .map(roles => g.GetRolesResponse(roles = roles.map(fromDomain)))

  override def findRoles(in: g.AuthRoleFindQuery): Future[g.FindResult] =
    roleEntityService
      .findRoles(
        AuthRoleFindQuery(
          offset = in.offset,
          size = in.size,
          filter = in.filter,
          sortBy = Some(in.sortBy.map(s => SortBy(field = s.field, descending = s.descending)).toSeq).filter(_.nonEmpty)
        )
      )
      .map(fromDomain)

  // === Role ↔ Principal ===

  override def assignPrincipal(in: g.AssignPrincipalPayload): Future[Empty] =
    roleEntityService
      .assignPrincipal(
        AssignPrincipalPayload(
          roleId = in.roleId,
          principal = unwrapPrincipal(in.principal),
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def unassignPrincipal(in: g.UnassignPrincipalPayload): Future[Empty] =
    roleEntityService
      .unassignPrincipal(
        UnassignPrincipalPayload(
          roleId = in.roleId,
          principal = unwrapPrincipal(in.principal),
          updatedBy = unwrapPrincipal(in.updatedBy)
        )
      )
      .map(_ => Empty())

  override def getRolePrincipals(in: g.GetRolePrincipalsRequest): Future[g.GetRolePrincipalsResponse] =
    roleEntityService
      .getRolePrincipals(in.id, in.source)
      .map(ps => g.GetRolePrincipalsResponse(principals = ps.toSeq.map(fromDomain)))

  // === Permission / Assignment ===

  override def assignPermission(in: g.AssignPermissionPayload): Future[Empty] =
    assignmentEntityService
      .assignPermission(
        AssignPermissionPayload(
          principal = unwrapPrincipal(in.principal),
          permission = unwrapPermission(in.permission),
          source = unwrapSource(in.source),
          updatedBy = unwrapPrincipal(in.updatedBy),
          updatedAt = in.updatedAt.map(OffsetDateTime.parse)
        )
      )
      .map(_ => Empty())

  override def unassignPermission(in: g.UnassignPermissionPayload): Future[Empty] =
    assignmentEntityService
      .unassignPermission(
        UnassignPermissionPayload(
          principal = unwrapPrincipal(in.principal),
          permission = unwrapPermission(in.permission),
          source = unwrapSource(in.source),
          updatedBy = unwrapPrincipal(in.updatedBy),
          updatedAt = in.updatedAt.map(OffsetDateTime.parse)
        )
      )
      .map(_ => Empty())

  override def findPermissions(in: g.FindPermissionsRequest): Future[g.FindPermissionsResponse] =
    assignmentEntityService
      .findPermissions(
        FindPermissions(
          principals = in.principals.map(toDomain).toSet,
          permissionIds = in.permissionIds.toSet
        )
      )
      .map(ps => g.FindPermissionsResponse(assignments = ps.toSeq.map(fromDomain)))

  override def checkAllPermission(in: g.CheckPermissionsRequest): Future[g.BooleanResponse] =
    assignmentEntityService
      .checkAllPermission(
        CheckPermissions(
          principals = in.principals.map(toDomain).toSet,
          permissions = in.permissions.map(toDomain).toSet
        )
      )
      .map(g.BooleanResponse(_))

  override def checkAnyPermission(in: g.CheckPermissionsRequest): Future[g.BooleanResponse] =
    assignmentEntityService
      .checkAnyPermission(
        CheckPermissions(
          principals = in.principals.map(toDomain).toSet,
          permissions = in.permissions.map(toDomain).toSet
        )
      )
      .map(g.BooleanResponse(_))

  override def findAssignments(in: g.FindAssignmentsQuery): Future[g.AssignmentFindResult] =
    assignmentEntityService
      .findAssignments(
        FindAssignmentsQuery(
          offset = in.offset,
          size = in.size,
          permission = unwrapPermission(in.permission),
          principalType = in.principalType,
          principalId = in.principalId,
          source = unwrapSource(in.source),
          sortBy = Some(in.sortBy.map(s => SortBy(field = s.field, descending = s.descending)).toSeq).filter(_.nonEmpty)
        )
      )
      .map(fromDomain)

  // === domain → proto (response converters) ===

  private def fromDomain(r: AuthRole): g.AuthRole =
    g.AuthRole(
      id = r.id,
      name = r.name,
      description = r.description,
      permissions = r.permissions.toSeq.map(fromDomain),
      updatedAt = formatOdt(r.updatedAt),
      updatedBy = Some(fromDomain(r.updatedBy))
    )

  private def fromDomain(f: FindResult): g.FindResult =
    g.FindResult(
      total = f.total,
      hits = f.hits.map(h => g.HitResult(id = h.id, score = h.score, updatedAt = formatOdt(h.updatedAt)))
    )

  private def fromDomain(p: PermissionAssignment): g.PermissionAssignment =
    g.PermissionAssignment(
      principal = Some(fromDomain(p.principal)),
      permission = Some(fromDomain(p.permission)),
      source = Some(fromDomain(p.source)),
      updatedBy = p.updatedBy.map(fromDomain),
      updatedAt = p.updatedAt.map(formatOdt)
    )

  private def fromDomain(a: AssignmentFindResult): g.AssignmentFindResult =
    g.AssignmentFindResult(
      total = a.total,
      hits = a.hits.map(h =>
        g.AssignmentHitResult(id = h.id, score = h.score, assignment = Some(fromDomain(h.assignment)))
      )
    )
}
