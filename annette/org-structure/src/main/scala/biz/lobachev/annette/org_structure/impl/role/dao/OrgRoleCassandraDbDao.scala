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

package biz.lobachev.annette.org_structure.impl.role.dao

import java.time.OffsetDateTime

import akka.Done
import biz.lobachev.annette.core.model.AnnettePrincipal
import biz.lobachev.annette.org_structure.api.role.{OrgRole, OrgRoleId}
import biz.lobachev.annette.org_structure.impl.role.OrgRoleEntity.{OrgRoleCreated, OrgRoleDeleted, OrgRoleUpdated}
import com.datastax.driver.core.{BoundStatement, PreparedStatement, Row}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

private[impl] class OrgRoleCassandraDbDao(session: CassandraSession)(implicit
  ec: ExecutionContext
) extends OrgRoleDbDao {

  private var insertOrgRoleStatement: PreparedStatement = null
  private var updateOrgRoleStatement: PreparedStatement = null
  private var deleteOrgRoleStatement: PreparedStatement = null

  def createTables(): Future[Done] =
    for {
      _ <- session.executeCreateTable("""
                                        |CREATE TABLE IF NOT EXISTS org_roles (
                                        |          id text PRIMARY KEY,
                                        |          name text,
                                        |          description text,
                                        |          updated_at text,
                                        |          updated_by_type text,
                                        |          updated_by_id text,
                                        |)
                                        |""".stripMargin)

    } yield Done

  def prepareStatements(): Future[Done] =
    for {
      insertOrgRoleStmt <- session.prepare(
                             """
                               | INSERT  INTO org_roles (id, name, description,
                               |     updated_at, updated_by_type, updated_by_id
                               |    )
                               |   VALUES (:id, :name, :description,
                               |     :updated_at, :updated_by_type, :updated_by_id
                               |    )
                               |""".stripMargin
                           )
      updateOrgRoleStmt <- session.prepare(
                             """
                               | UPDATE org_roles SET
                               |   name = :name,
                               |   description = :description,
                               |   updated_at = :updated_at,
                               |   updated_by_type = :updated_by_type,
                               |   updated_by_id = :updated_by_id
                               | WHERE id = :id
                               |""".stripMargin
                           )
      deleteOrgRoleStmt <- session.prepare(
                             """
                               | DELETE FROM org_roles 
                               |   WHERE id = :id
                               |""".stripMargin
                           )
    } yield {
      insertOrgRoleStatement = insertOrgRoleStmt
      updateOrgRoleStatement = updateOrgRoleStmt
      deleteOrgRoleStatement = deleteOrgRoleStmt
      Done
    }

  def createOrgRole(event: OrgRoleCreated): List[BoundStatement] =
    List(
      insertOrgRoleStatement
        .bind()
        .setString("id", event.id)
        .setString("name", event.name)
        .setString("description", event.description)
        .setString("updated_at", event.createdAt.toString)
        .setString("updated_by_type", event.createdBy.principalType)
        .setString("updated_by_id", event.createdBy.principalId)
    )

  def updateOrgRole(event: OrgRoleUpdated): List[BoundStatement] =
    List(
      updateOrgRoleStatement
        .bind()
        .setString("id", event.id)
        .setString("name", event.name)
        .setString("description", event.description)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def deleteOrgRole(event: OrgRoleDeleted): List[BoundStatement] =
    List(
      deleteOrgRoleStatement
        .bind()
        .setString("id", event.id)
    )

  def getOrgRoleById(id: OrgRoleId): Future[Option[OrgRole]] =
    for {
      stmt   <- session.prepare("SELECT * FROM org_roles WHERE id = ?")
      result <- session.selectOne(stmt.bind(id)).map(_.map(convertOrgRole))
    } yield result

  def getOrgRolesById(ids: Set[OrgRoleId]): Future[Set[OrgRole]] =
    for {
      stmt   <- session.prepare("SELECT * FROM org_roles WHERE id IN ?")
      result <- session.selectAll(stmt.bind(ids.toList.asJava)).map(_.map(convertOrgRole))
    } yield result.toSet

  private def convertOrgRole(row: Row): OrgRole =
    OrgRole(
      id = row.getString("id"),
      name = row.getString("name"),
      description = row.getString("description"),
      updatedAt = OffsetDateTime.parse(row.getString("updated_at")),
      updatedBy = AnnettePrincipal(
        principalType = row.getString("updated_by_type"),
        principalId = row.getString("updated_by_id")
      )
    )

}
