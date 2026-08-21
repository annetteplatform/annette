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

import java.time.OffsetDateTime
import biz.lobachev.annette.authorization.api.assignment._
import biz.lobachev.annette.authorization.api.role._
import biz.lobachev.annette.authorization.api.{grpc => g}
import biz.lobachev.annette.core.exception.AnnetteGrpcExceptionMapping
import biz.lobachev.annette.core.model.auth.{AnnettePrincipal, Permission}
import biz.lobachev.annette.core.model.indexing.{FindResult, HitResult}
import biz.lobachev.annette.core.utils.ChimneyCommons._
import io.scalaland.chimney.dsl._
import org.apache.pekko.Done

import scala.concurrent.{ExecutionContext, Future}

class AuthorizationServiceGrpcImpl(client: g.AuthorizationServiceClient)(implicit val ec: ExecutionContext)
    extends AuthorizationService {

  private def unwrapPrincipal(p: Option[g.AnnettePrincipal]): AnnettePrincipal =
    p.map(pr => AnnettePrincipal(pr.code)).getOrElse(throw new IllegalArgumentException("missing principal"))

  private def toDomain(p: g.AnnettePrincipal): AnnettePrincipal = AnnettePrincipal(p.code)

  private def fromDomain(p: AnnettePrincipal): g.AnnettePrincipal = g.AnnettePrincipal(p.code)

  private def toDomain(p: g.Permission): Permission =
    Permission(id = p.id, arg1 = p.arg1, arg2 = p.arg2, arg3 = p.arg3)

  private def fromDomain(p: Permission): g.Permission =
    g.Permission(id = p.id, arg1 = p.arg1, arg2 = p.arg2, arg3 = p.arg3)

  private def toDomain(s: g.AuthSource): AuthSource = AuthSource(sourceType = s.sourceType, sourceId = s.sourceId)

  private def fromDomain(s: AuthSource): g.AuthSource = g.AuthSource(sourceType = s.sourceType, sourceId = s.sourceId)

  private def findResultFromProto(f: g.FindResult): FindResult =
    FindResult(
      total = f.total,
      hits = f.hits.map(h => HitResult(id = h.id, score = h.score, updatedAt = OffsetDateTime.parse(h.updatedAt)))
    )

  private def call[T](f: Future[T]): Future[T] = AnnetteGrpcExceptionMapping.recoverAnnette(f)

  override def createRole(payload: CreateRolePayload): Future[Done] =
    call(
      client.createRole(
        g.CreateRolePayload(
          id = payload.id,
          name = payload.name,
          description = payload.description,
          permissions = payload.permissions.map(fromDomain).toSeq,
          createdBy = Some(fromDomain(payload.createdBy))
        )
      )
    ).map(_ => Done)

  override def updateRole(payload: UpdateRolePayload): Future[Done] =
    call(
      client.updateRole(
        g.UpdateRolePayload(
          id = payload.id,
          name = payload.name,
          description = payload.description,
          permissions = payload.permissions.map(fromDomain).toSeq,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def createOrUpdateRole(payload: CreateRolePayload): Future[Done] =
    createRole(payload).recoverWith {
      case RoleAlreadyExist(_) =>
        val updatePayload = payload
          .into[UpdateRolePayload]
          .withFieldComputed(_.updatedBy, _.createdBy)
          .transform
        updateRole(updatePayload)
    }

  override def deleteRole(payload: DeleteRolePayload): Future[Done] =
    call(
      client.deleteRole(
        g.DeleteRolePayload(id = payload.id, deletedBy = Some(fromDomain(payload.deletedBy)))
      )
    ).map(_ => Done)

  override def getRole(id: AuthRoleId, source: Option[String]): Future[AuthRole] =
    call(client.getRole(g.GetRoleRequest(id = id, source = source))).map { r =>
      AuthRole(
        id = r.id,
        name = r.name,
        description = r.description,
        permissions = r.permissions.map(toDomain).toSet,
        updatedAt = OffsetDateTime.parse(r.updatedAt),
        updatedBy = unwrapPrincipal(r.updatedBy)
      )
    }

  override def getRoles(ids: Set[AuthRoleId], source: Option[String]): Future[Seq[AuthRole]] =
    call(client.getRoles(g.GetRolesRequest(ids = ids.toSeq, source = source))).map(
      _.roles.map { r =>
        AuthRole(
          id = r.id,
          name = r.name,
          description = r.description,
          permissions = r.permissions.map(toDomain).toSet,
          updatedAt = OffsetDateTime.parse(r.updatedAt),
          updatedBy = unwrapPrincipal(r.updatedBy)
        )
      }
    )

  override def findRoles(payload: AuthRoleFindQuery): Future[FindResult] =
    call(
      client.findRoles(
        g.AuthRoleFindQuery(
          offset = payload.offset,
          size = payload.size,
          filter = payload.filter,
          sortBy = payload.sortBy.map(_.map(s => g.SortBy(field = s.field, descending = s.descending))).getOrElse(Seq.empty)
        )
      )
    ).map(findResultFromProto)

  override def assignPrincipal(payload: AssignPrincipalPayload): Future[Done] =
    call(
      client.assignPrincipal(
        g.AssignPrincipalPayload(
          roleId = payload.roleId,
          principal = Some(fromDomain(payload.principal)),
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def unassignPrincipal(payload: UnassignPrincipalPayload): Future[Done] =
    call(
      client.unassignPrincipal(
        g.UnassignPrincipalPayload(
          roleId = payload.roleId,
          principal = Some(fromDomain(payload.principal)),
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(_ => Done)

  override def getRolePrincipals(id: AuthRoleId, source: Option[String]): Future[Set[AnnettePrincipal]] =
    call(client.getRolePrincipals(g.GetRolePrincipalsRequest(id = id, source = source)))
      .map(_.principals.map(toDomain).toSet)

  override def assignPermission(payload: AssignPermissionPayload): Future[Done] =
    call(
      client.assignPermission(
        g.AssignPermissionPayload(
          principal = Some(fromDomain(payload.principal)),
          permission = Some(fromDomain(payload.permission)),
          source = Some(fromDomain(payload.source)),
          updatedBy = Some(fromDomain(payload.updatedBy)),
          updatedAt = payload.updatedAt.map(_.toString)
        )
      )
    ).map(_ => Done)

  override def unassignPermission(payload: UnassignPermissionPayload): Future[Done] =
    call(
      client.unassignPermission(
        g.UnassignPermissionPayload(
          principal = Some(fromDomain(payload.principal)),
          permission = Some(fromDomain(payload.permission)),
          source = Some(fromDomain(payload.source)),
          updatedBy = Some(fromDomain(payload.updatedBy)),
          updatedAt = payload.updatedAt.map(_.toString)
        )
      )
    ).map(_ => Done)

  override def findPermissions(payload: FindPermissions): Future[Set[PermissionAssignment]] =
    call(
      client.findPermissions(
        g.FindPermissionsRequest(
          principals = payload.principals.map(fromDomain).toSeq,
          permissionIds = payload.permissionIds.toSeq
        )
      )
    ).map(
      _.assignments.map { a =>
        PermissionAssignment(
          principal = toDomain(a.principal.getOrElse(throw new IllegalArgumentException("missing principal"))),
          permission = toDomain(a.permission.getOrElse(throw new IllegalArgumentException("missing permission"))),
          source = toDomain(a.source.getOrElse(throw new IllegalArgumentException("missing source"))),
          updatedBy = a.updatedBy.map(toDomain),
          updatedAt = a.updatedAt.map(OffsetDateTime.parse)
        )
      }.toSet
    )

  override def checkAllPermission(payload: CheckPermissions): Future[Boolean] =
    call(
      client.checkAllPermission(
        g.CheckPermissionsRequest(
          principals = payload.principals.map(fromDomain).toSeq,
          permissions = payload.permissions.map(fromDomain).toSeq
        )
      )
    ).map(_.result)

  override def checkAnyPermission(payload: CheckPermissions): Future[Boolean] =
    call(
      client.checkAnyPermission(
        g.CheckPermissionsRequest(
          principals = payload.principals.map(fromDomain).toSeq,
          permissions = payload.permissions.map(fromDomain).toSeq
        )
      )
    ).map(_.result)

  override def findAssignments(payload: FindAssignmentsQuery): Future[AssignmentFindResult] =
    call(
      client.findAssignments(
        g.FindAssignmentsQuery(
          offset = payload.offset,
          size = payload.size,
          permission = Some(fromDomain(payload.permission)),
          principalType = payload.principalType,
          principalId = payload.principalId,
          source = Some(fromDomain(payload.source)),
          sortBy =
            payload.sortBy.map(_.map(s => g.SortBy(field = s.field, descending = s.descending))).getOrElse(Seq.empty)
        )
      )
    ).map { r =>
      AssignmentFindResult(
        total = r.total,
        hits = r.hits.map { h =>
          val a = h.assignment.getOrElse(throw new IllegalArgumentException("missing assignment"))
          AssignmentHitResult(
            id = h.id,
            score = h.score,
            assignment = PermissionAssignment(
              principal = toDomain(a.principal.getOrElse(throw new IllegalArgumentException("missing principal"))),
              permission = toDomain(a.permission.getOrElse(throw new IllegalArgumentException("missing permission"))),
              source = toDomain(a.source.getOrElse(throw new IllegalArgumentException("missing source"))),
              updatedBy = a.updatedBy.map(toDomain),
              updatedAt = a.updatedAt.map(OffsetDateTime.parse)
            )
          )
        }
      )
    }
}
