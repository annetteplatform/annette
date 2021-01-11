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

package biz.lobachev.annette.authorization.impl.assignment.dao

import akka.Done
import biz.lobachev.annette.authorization.api.assignment._
import biz.lobachev.annette.authorization.impl.assignment.AssignmentEntity
import biz.lobachev.annette.core.model.auth.{AnnettePrincipal, Permission}
import com.datastax.driver.core.{BoundStatement, PreparedStatement, Row}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession

import scala.collection.immutable._
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

private[impl] class AssignmentCassandraDbDao(session: CassandraSession)(implicit ec: ExecutionContext)
    extends AssignmentDbDao {

  private var onPermissionAssignedStatement: PreparedStatement   = _
  private var onPermissionUnassignedStatement: PreparedStatement = _

  def createTables(): Future[Done] =
    for {
      _ <- session.executeCreateTable(
             """
               |CREATE TABLE IF NOT EXISTS permission_assignments (
               |          principal      text,
               |          permission_id  text,
               |          arg1           text,
               |          arg2           text,
               |          arg3           text,
               |          source         text,
               |          PRIMARY KEY (principal, permission_id, arg1, arg2, arg3, source )
               |)
               |""".stripMargin
           )
    } yield Done

  def prepareStatements(): Future[Done] =
    for {
      onPermissionAssignedStmt   <- session.prepare(
                                      """
                                      | INSERT  INTO permission_assignments (
                                      |     principal, permission_id, arg1, arg2, arg3, source
                                      |   )
                                      |   VALUES (
                                      |     :principal, :permission_id, :arg1, :arg2, :arg3, :source
                                      |   )
                                      |""".stripMargin
                                    )
      onPermissionUnassignedStmt <- session.prepare(
                                      """
                                        | DELETE FROM permission_assignments
                                        | WHERE principal = :principal AND
                                        |       permission_id = :permission_id AND
                                        |       arg1 = :arg1 AND
                                        |       arg2 = :arg2 AND
                                        |       arg3 = :arg3 AND
                                        |       source = :source
                                        |""".stripMargin
                                    )
    } yield {
      onPermissionAssignedStatement = onPermissionAssignedStmt
      onPermissionUnassignedStatement = onPermissionUnassignedStmt
      Done
    }

  def assignPermission(event: AssignmentEntity.PermissionAssigned): BoundStatement     =
    onPermissionAssignedStatement
      .bind()
      .setString("principal", event.principal.code)
      .setString("permission_id", event.permission.id)
      .setString("arg1", event.permission.arg1)
      .setString("arg2", event.permission.arg2)
      .setString("arg3", event.permission.arg3)
      .setString("source", event.source.code)
  def unassignPermission(event: AssignmentEntity.PermissionUnassigned): BoundStatement =
    onPermissionUnassignedStatement
      .bind()
      .setString("principal", event.principal.code)
      .setString("permission_id", event.permission.id)
      .setString("arg1", event.permission.arg1)
      .setString("arg2", event.permission.arg2)
      .setString("arg3", event.permission.arg3)
      .setString("source", event.source.code)

  def checkAnyPermission(payload: CheckPermissions): Future[Boolean] =
    payload.permissions.foldLeft(Future.successful(false)) { (accFuture, permission) =>
      accFuture.flatMap { acc =>
        if (acc) Future.successful(true)
        else checkPermission(payload.principals, permission)
      }
    }

  def checkAllPermission(payload: CheckPermissions): Future[Boolean] =
    payload.permissions.foldLeft(Future.successful(true)) { (accFuture, permission) =>
      accFuture.flatMap { acc =>
        if (acc) checkPermission(payload.principals, permission)
        else Future.successful(false)
      }
    }

  private def checkPermission(principals: Set[AnnettePrincipal], permission: Permission) =
    for {
      stmt   <- session.prepare(
                  """
                  | SELECT count(*) as cnt FROM permission_assignments
                  | WHERE principal IN :principals AND
                  |       permission_id = :permission_id AND
                  |       arg1 = :arg1 AND
                  |       arg2 = :arg2 AND
                  |       arg3 = :arg3
                  |""".stripMargin
                )
      result <- session
                  .selectOne(
                    stmt
                      .bind()
                      .setList[String]("principals", principals.map(_.code).toList.asJava)
                      .setString("permission_id", permission.id)
                      .setString("arg1", permission.arg1)
                      .setString("arg2", permission.arg2)
                      .setString("arg3", permission.arg3)
                  )
    } yield result.map(_.getLong("cnt") > 0).getOrElse(false)

  def findPermissions(payload: FindPermissions): Future[Set[PermissionAssignment]] =
    for {
      stmt   <- session.prepare(
                  "SELECT * FROM permission_assignments " +
                    "WHERE principal IN :principals AND permission_id IN :permission_ids"
                )
      result <- session
                  .selectAll(
                    stmt
                      .bind()
                      .setList[String]("principals", payload.principals.map(_.code).toList.asJava)
                      .setList[String]("permission_ids", payload.permissionIds.toList.asJava)
                  )
                  .map(_.map(convertToPermissionAssignment).toSet)
    } yield result

  private def convertToPermissionAssignment(row: Row): PermissionAssignment = {
    val principal = AnnettePrincipal.fromCode(row.getString("principal")).getOrElse(AnnettePrincipal("", ""))
    val source    = AuthSource.fromCode(row.getString("source")).getOrElse(AuthSource("", ""))
    PermissionAssignment(
      principal = principal,
      permission = Permission(
        id = row.getString("permission_id"),
        arg1 = row.getString("arg1"),
        arg2 = row.getString("arg2"),
        arg3 = row.getString("arg3")
      ),
      source = source
    )
  }

}
