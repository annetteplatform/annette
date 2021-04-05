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

import java.time.OffsetDateTime
import akka.Done
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import biz.lobachev.annette.authorization.api.role._
import biz.lobachev.annette.authorization.impl.role.RoleEntity
import biz.lobachev.annette.core.model.auth.{AnnettePrincipal, Permission}
import com.datastax.driver.core.{BoundStatement, PreparedStatement, Row}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession

import scala.concurrent.{ExecutionContext, Future}

private[impl] class RoleCassandraDbDao(
  session: CassandraSession
)(implicit
  ec: ExecutionContext,
  materializer: Materializer
) extends RoleDbDao {

  private var createRoleStatement: PreparedStatement            = _
  private var updateRoleNameStatement: PreparedStatement        = _
  private var updateRoleDescriptionStatement: PreparedStatement = _
  private var updateRoleUpdatedAtByStatement: PreparedStatement = _
  private var deleteRoleStatement: PreparedStatement            = _
  private var addPermissionStatement: PreparedStatement         = _
  private var removePermissionStatement: PreparedStatement      = _
  private var deleteRolePermissionsStatement: PreparedStatement = _
  private var assignPrincipalStatement: PreparedStatement       = _
  private var unassignPrincipalStatement: PreparedStatement     = _
  private var deleteRolePrincipalsStatement: PreparedStatement  = _

  def createTables(): Future[Done] =
    for {
      _ <- session.executeCreateTable("""
                                        |CREATE TABLE IF NOT EXISTS roles (
                                        |          id               text PRIMARY KEY,
                                        |          name             text,
                                        |          description      text,
                                        |          updated_at       text,
                                        |          updated_by_type  text,
                                        |          updated_by_id    text
                                        |)
                                        |""".stripMargin)
      _ <- session.executeCreateTable("""
                                        |CREATE TABLE IF NOT EXISTS role_permissions (
                                        |          role_id          text,
                                        |          permission_id    text,
                                        |          arg1             text,
                                        |          arg2             text,
                                        |          arg3             text,
                                        |          PRIMARY KEY (role_id, permission_id, arg1, arg2, arg3)
                                        |)
                                        |""".stripMargin)
      _ <- session.executeCreateTable("""
                                        |CREATE TABLE IF NOT EXISTS role_principals (
                                        |          role_id           text,
                                        |          principal_type    text,
                                        |          principal_id      text,
                                        |          PRIMARY KEY (role_id, principal_type, principal_id)
                                        |)
                                        |""".stripMargin)
    } yield Done

  def prepareStatements(): Future[Done] =
    for {
      createRoleStmt            <- session.prepare(
                                     """
                            | INSERT INTO roles ( id, name, description,
                            |     updated_at, updated_by_type, updated_by_id
                            | )
                            |  VALUES ( :id, :name, :description,
                            |     :updated_at, :updated_by_type, :updated_by_id
                            |   )
                            |""".stripMargin
                                   )
      updateRoleNameStmt        <- session.prepare(
                                     """
                                | UPDATE roles SET
                                |     name = :name
                                |   WHERE id = :id
                                |""".stripMargin
                                   )
      updateRoleDescriptionStmt <- session.prepare(
                                     """
                                       | UPDATE roles SET
                                       |     description = :description
                                       |   WHERE id = :id
                                       |""".stripMargin
                                   )
      updateRoleUpdatedAtByStmt <- session.prepare(
                                     """
                                       | UPDATE roles SET
                                       |   updated_at = :updated_at,
                                       |   updated_by_type = :updated_by_type,
                                       |   updated_by_id = :updated_by_id
                                       |  WHERE id = :id
                                       |""".stripMargin
                                   )
      deleteRoleStmt            <- session.prepare(
                                     """
                            | DELETE FROM roles
                            |  WHERE id = :id
                            |""".stripMargin
                                   )
      addPermissionStmt         <- session.prepare(
                                     """
                               | INSERT INTO role_permissions ( role_id, permission_id, arg1, arg2, arg3 )
                               |  VALUES ( :role_id, :permission_id, :arg1, :arg2, :arg3 )
                               |""".stripMargin
                                   )
      removePermissionStmt      <- session.prepare(
                                     """
                                  | DELETE FROM role_permissions
                                  |  WHERE role_id = :role_id AND
                                  |        permission_id = :permission_id AND
                                  |        arg1 = :arg1 AND
                                  |        arg2 = :arg2 AND
                                  |        arg3 = :arg3
                                  |""".stripMargin
                                   )
      deleteRolePermissionsStmt <- session.prepare(
                                     """
                                       | DELETE FROM role_permissions
                                       |  WHERE role_id = :role_id
                                       |""".stripMargin
                                   )
      assignPrincipalStmt       <- session.prepare(
                                     """
                                 | INSERT INTO role_principals ( role_id, principal_type, principal_id )
                                 |  VALUES ( :role_id, :principal_type, :principal_id )
                                 |""".stripMargin
                                   )
      unassignPrincipalStmt     <- session.prepare(
                                     """
                                   | DELETE FROM role_principals
                                   |  WHERE role_id = :role_id AND
                                   |        principal_type = :principal_type AND
                                   |        principal_id = :principal_id
                                   |""".stripMargin
                                   )
      deleteRolePrincipalsStmt  <- session.prepare(
                                     """
                                      | DELETE FROM role_principals
                                      |  WHERE role_id = :role_id
                                      |""".stripMargin
                                   )
    } yield {
      createRoleStatement = createRoleStmt
      updateRoleNameStatement = updateRoleNameStmt
      updateRoleDescriptionStatement = updateRoleDescriptionStmt
      updateRoleUpdatedAtByStatement = updateRoleUpdatedAtByStmt
      deleteRoleStatement = deleteRoleStmt
      addPermissionStatement = addPermissionStmt
      removePermissionStatement = removePermissionStmt
      deleteRolePermissionsStatement = deleteRolePermissionsStmt
      assignPrincipalStatement = assignPrincipalStmt
      unassignPrincipalStatement = unassignPrincipalStmt
      deleteRolePrincipalsStatement = deleteRolePrincipalsStmt
      Done
    }

  def createRole(event: RoleEntity.RoleCreated): List[BoundStatement]                =
    List(
      createRoleStatement
        .bind()
        .setString("id", event.id)
        .setString("name", event.name)
        .setString("description", event.description)
        .setString("updated_at", event.createdAt.toString)
        .setString("updated_by_type", event.createdBy.principalType)
        .setString("updated_by_id", event.createdBy.principalId)
    ) ++ event.permissions.map { permission =>
      addPermissionStatement
        .bind()
        .setString("role_id", event.id)
        .setString("permission_id", permission.id)
        .setString("arg1", permission.arg1)
        .setString("arg2", permission.arg2)
        .setString("arg3", permission.arg3)
    }
  def updateRole(event: RoleEntity.RoleUpdated): List[BoundStatement] = {
    val nameUpdate        = event.name.map { name =>
      updateRoleNameStatement
        .bind()
        .setString("id", event.id)
        .setString("name", name)
    }.toList
    val descriptionUpdate = event.description.map { description =>
      updateRoleDescriptionStatement
        .bind()
        .setString("id", event.id)
        .setString("description", description)
    }.toList
    val updatedAtByUpdate = List(
      updateRoleUpdatedAtByStatement
        .bind()
        .setString("id", event.id)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

    val addedPermissions   = event.addedPermissions.map { permission =>
      addPermissionStatement
        .bind()
        .setString("role_id", event.id)
        .setString("permission_id", permission.id)
        .setString("arg1", permission.arg1)
        .setString("arg2", permission.arg2)
        .setString("arg3", permission.arg3)
    }
    val removedPermissions = event.removedPermissions.map { permission =>
      removePermissionStatement
        .bind()
        .setString("role_id", event.id)
        .setString("permission_id", permission.id)
        .setString("arg1", permission.arg1)
        .setString("arg2", permission.arg2)
        .setString("arg3", permission.arg3)
    }

    nameUpdate ++ descriptionUpdate ++ updatedAtByUpdate ++ addedPermissions ++ removedPermissions
  }
  def deleteRole(event: RoleEntity.RoleDeleted): List[BoundStatement]                =
    List(
      deleteRoleStatement
        .bind()
        .setString("id", event.id),
      deleteRolePermissionsStatement
        .bind()
        .setString("role_id", event.id),
      deleteRolePermissionsStatement
        .bind()
        .setString("role_id", event.id),
      deleteRolePrincipalsStatement
        .bind()
        .setString("role_id", event.id)
    )
  def assignPrincipal(event: RoleEntity.PrincipalAssigned): List[BoundStatement]     =
    List(
      assignPrincipalStatement
        .bind()
        .setString("role_id", event.roleId)
        .setString("principal_type", event.principal.principalType)
        .setString("principal_id", event.principal.principalId),
      updateRoleUpdatedAtByStatement
        .bind()
        .setString("id", event.roleId)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )
  def unassignPrincipal(event: RoleEntity.PrincipalUnassigned): List[BoundStatement] =
    List(
      unassignPrincipalStatement
        .bind()
        .setString("role_id", event.roleId)
        .setString("principal_type", event.principal.principalType)
        .setString("principal_id", event.principal.principalId),
      updateRoleUpdatedAtByStatement
        .bind()
        .setString("id", event.roleId)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def getRoleById(id: AuthRoleId): Future[Option[AuthRole]] =
    for {
      roleStmt       <- session.prepare("SELECT * FROM roles WHERE id = :id")
      maybeRole      <- session.selectOne(roleStmt.bind().setString("id", id)).map(_.map(convertRole))
      permissionStmt <- session.prepare("SELECT * FROM role_permissions WHERE role_id = :role_id")
      permissions    <- session.selectAll(permissionStmt.bind().setString("role_id", id)).map(_.map(convertPermission))
    } yield maybeRole.map(_.copy(permissions = permissions.toSet))

  def getRolePrincipals(id: AuthRoleId): Future[Option[Set[AnnettePrincipal]]] =
    for {
      roleStmt       <- session.prepare("SELECT count(*) as cnt FROM roles WHERE id = :id")
      maybeCnt       <- session.selectOne(roleStmt.bind().setString("id", id))
      principalsStmt <- session.prepare("SELECT * FROM role_principals WHERE role_id = :role_id")
      principals     <- session.selectAll(principalsStmt.bind().setString("role_id", id)).map(_.map(convertPrincipal))
    } yield maybeCnt.map(row => if (row.getLong("cnt") > 0) Some(principals.toSet) else None).getOrElse(None)

  def getRoleById(ids: Set[AuthRoleId]): Future[Map[AuthRoleId, AuthRole]] =
    Source(ids)
      .mapAsync(1) { id =>
        getRoleById(id)
      }
      .runWith(Sink.seq)
      .map(_.flatten.map(role => role.id -> role).toMap)

  private def convertRole(row: Row): AuthRole                              =
    AuthRole(
      id = row.getString("id"),
      name = row.getString("name"),
      description = row.getString("description"),
      permissions = Set.empty,
      updatedAt = OffsetDateTime.parse(row.getString("updated_at")),
      updatedBy = AnnettePrincipal(
        principalType = row.getString("updated_by_type"),
        principalId = row.getString("updated_by_id")
      )
    )

  private def convertPermission(row: Row): Permission =
    Permission(
      id = row.getString("permission_id"),
      arg1 = row.getString("arg1"),
      arg2 = row.getString("arg2"),
      arg3 = row.getString("arg3")
    )

  private def convertPrincipal(row: Row): AnnettePrincipal =
    AnnettePrincipal(
      principalType = row.getString("principal_type"),
      principalId = row.getString("principal_id")
    )
}
