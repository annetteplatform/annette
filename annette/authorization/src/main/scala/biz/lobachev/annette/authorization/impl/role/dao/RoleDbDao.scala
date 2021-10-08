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

package biz.lobachev.annette.authorization.impl.role.dao

import akka.Done
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import biz.lobachev.annette.authorization.api.role.{AuthRole, AuthRoleId}
import biz.lobachev.annette.authorization.impl.role.RoleEntity
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.microservice_core.db.{CassandraQuillDao, CassandraTableBuilder}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import io.scalaland.chimney.dsl._

import scala.concurrent.{ExecutionContext, Future}

private[impl] class RoleDbDao(
  override val session: CassandraSession
)(implicit ec: ExecutionContext, materializer: Materializer)
    extends CassandraQuillDao {

  import ctx._

  private val roleSchema           = quote(querySchema[AuthRoleRecord]("roles"))
  private val rolePermissionSchema = quote(querySchema[RolePermissionRecord]("role_permissions"))
  private val rolePrincipalSchema  = quote(querySchema[RolePrincipalRecord]("role_principals"))

  private implicit val insertRoleEntityMeta           = insertMeta[AuthRoleRecord]()
  private implicit val updateRoleEntityMeta           = updateMeta[AuthRoleRecord](_.id)
  private implicit val insertRolePermissionEntityMeta = insertMeta[RolePermissionRecord]()
  private implicit val insertRolePrincipalEntityMeta  = insertMeta[RolePrincipalRecord]()
  println(insertRoleEntityMeta.toString)
  println(updateRoleEntityMeta.toString)
  println(insertRolePermissionEntityMeta.toString)
  println(insertRolePrincipalEntityMeta.toString)

  def createTables(): Future[Done] = {
    import CassandraTableBuilder.types._
    for {
      _ <- session.executeCreateTable(
             CassandraTableBuilder("roles")
               .column("id", Text, true)
               .column("name", Text)
               .column("description", Text)
               .column("updated_at", Timestamp)
               .column("updated_by", Text)
               .build
           )
      _ <- session.executeCreateTable(
             CassandraTableBuilder("role_permissions")
               .column("role_id", Text)
               .column("permission_id", Text)
               .column("arg1", Text)
               .column("arg2", Text)
               .column("arg3", Text)
               .withPrimaryKey("role_id", "permission_id", "arg1", "arg2", "arg3")
               .build
           )

      _ <- session.executeCreateTable(
             CassandraTableBuilder("role_principals")
               .column("role_id", Text)
               .column("principal_type", Text)
               .column("principal_id", Text)
               .withPrimaryKey("role_id", "principal_type", "principal_id")
               .build
           )
    } yield Done
  }

  def createRole(event: RoleEntity.RoleCreated) = {
    val roleRecord = event
      .into[AuthRoleRecord]
      .withFieldComputed(_.updatedAt, _.createdAt)
      .withFieldComputed(_.updatedBy, _.createdBy)
      .transform
    for {
      _ <- ctx.run(roleSchema.insert(lift(roleRecord)))

      _ <- Source(event.permissions)
             .mapAsync(1) { permission =>
               val rolePermissionRecord = RolePermissionRecord(
                 roleId = event.id,
                 permissionId = permission.id,
                 arg1 = permission.arg1,
                 arg2 = permission.arg2,
                 arg3 = permission.arg3
               )
               ctx.run(rolePermissionSchema.insert(lift(rolePermissionRecord)))
             }
             .runWith(Sink.ignore)
    } yield Done
  }

  def updateRole(event: RoleEntity.RoleUpdated) =
    for {
      _ <- event.name.map { name =>
             ctx.run(
               roleSchema
                 .filter(_.id == lift(event.id))
                 .update(
                   _.name -> lift(name)
                 )
             )
           }.getOrElse(Future.successful(Done))

      _ <- event.description.map { description =>
             ctx.run(
               roleSchema
                 .filter(_.id == lift(event.id))
                 .update(
                   _.description -> lift(description)
                 )
             )
           }.getOrElse(Future.successful(Done))

      _ <- ctx.run(
             roleSchema
               .filter(_.id == lift(event.id))
               .update(
                 _.updatedAt -> lift(event.updatedAt),
                 _.updatedBy -> lift(event.updatedBy)
               )
           )

      _ <- Source(event.addedPermissions)
             .mapAsync(1) { permission =>
               val rolePermissionRecord = RolePermissionRecord(
                 roleId = event.id,
                 permissionId = permission.id,
                 arg1 = permission.arg1,
                 arg2 = permission.arg2,
                 arg3 = permission.arg3
               )
               ctx.run(rolePermissionSchema.insert(lift(rolePermissionRecord)))
             }
             .runWith(Sink.ignore)

      _ <- Source(event.removedPermissions)
             .mapAsync(1) { permission =>
               ctx.run(
                 rolePermissionSchema
                   .filter(e =>
                     e.roleId == lift(event.id) &&
                       e.permissionId == lift(permission.id) &&
                       e.arg1 == lift(permission.arg1) &&
                       e.arg2 == lift(permission.arg2) &&
                       e.arg3 == lift(permission.arg3)
                   )
                   .delete
               )
             }
             .runWith(Sink.ignore)
    } yield Done

  def deleteRole(event: RoleEntity.RoleDeleted) =
    for {
      _ <- ctx.run(roleSchema.filter(_.id == lift(event.id)).delete)
      _ <- ctx.run(rolePermissionSchema.filter(e => e.roleId == lift(event.id)).delete)
      _ <- ctx.run(rolePrincipalSchema.filter(e => e.roleId == lift(event.id)).delete)
    } yield Done

  def assignPrincipal(event: RoleEntity.PrincipalAssigned) = {
    val rolePrincipalRecord = RolePrincipalRecord(
      event.roleId,
      event.principal.principalType,
      event.principal.principalId
    )
    for {
      _ <- ctx.run(rolePrincipalSchema.insert(lift(rolePrincipalRecord)))
      _ <- ctx.run(
             roleSchema
               .filter(_.id == lift(event.roleId))
               .update(
                 _.updatedAt -> lift(event.updatedAt),
                 _.updatedBy -> lift(event.updatedBy)
               )
           )
    } yield Done
  }

  def unassignPrincipal(event: RoleEntity.PrincipalUnassigned) =
    for {
      _ <- ctx.run(
             rolePrincipalSchema
               .filter(e =>
                 e.roleId == lift(event.roleId) &&
                   e.principalType == lift(event.principal.principalType) &&
                   e.principalId == lift(event.principal.principalId)
               )
               .delete
           )
      _ <- ctx.run(
             roleSchema
               .filter(_.id == lift(event.roleId))
               .update(
                 _.updatedAt -> lift(event.updatedAt),
                 _.updatedBy -> lift(event.updatedBy)
               )
           )
    } yield Done

  def getRoleById(id: AuthRoleId): Future[Option[AuthRole]] =
    for {
      maybeRoleRecord <- ctx
                           .run(roleSchema.filter(_.id == lift(id)))
                           .map(_.headOption)
      permissions     <- ctx
                           .run(rolePermissionSchema.filter(_.roleId == lift(id)))
    } yield maybeRoleRecord.map(_.toAuthRole.copy(permissions = permissions.map(_.toPermission).toSet))

  def getRolePrincipals(id: AuthRoleId): Future[Option[Set[AnnettePrincipal]]] =
    for {
      maybeRole  <- ctx.run(roleSchema.filter(_.id == lift(id)).map(_.id)).map(_.headOption)
      principals <- ctx.run(rolePrincipalSchema.filter(_.roleId == lift(id)))
    } yield maybeRole.map(_ => principals.map(_.toPrincipal).toSet)

  def getRolesById(ids: Set[AuthRoleId]): Future[Seq[AuthRole]] =
    for {
      roles       <- ctx
                       .run(roleSchema.filter(b => liftQuery(ids).contains(b.id)))
                       .map(_.map(_.toAuthRole))
      permissions <- ctx
                       .run(rolePermissionSchema.filter(b => liftQuery(ids).contains(b.roleId)))
                       .map(_.groupMap(_.roleId)(_.toPermission))
    } yield roles
      .map(role => role.copy(permissions = permissions.get(role.id).map(_.toSet).getOrElse(Set.empty)))

}
