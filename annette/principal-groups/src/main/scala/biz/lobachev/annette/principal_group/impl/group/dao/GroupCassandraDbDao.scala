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

package biz.lobachev.annette.principal_group.impl.group.dao

import akka.Done
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.principal_group.api.group.{PrincipalGroup, PrincipalGroupId}
import biz.lobachev.annette.principal_group.impl.group.PrincipalGroupEntity.{
  PrincipalAssigned,
  PrincipalGroupCategoryUpdated,
  PrincipalGroupCreated,
  PrincipalGroupDeleted,
  PrincipalGroupDescriptionUpdated,
  PrincipalGroupNameUpdated,
  PrincipalUnassigned
}
import com.datastax.driver.core.{BoundStatement, PreparedStatement, Row}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession

import java.time.OffsetDateTime
import scala.collection.immutable._
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

private[impl] class GroupCassandraDbDao(session: CassandraSession)(implicit ec: ExecutionContext) {

  private var insertPrincipalGroupStatement: PreparedStatement            = null
  private var updatePrincipalGroupNameStatement: PreparedStatement        = null
  private var updatePrincipalGroupDescriptionStatement: PreparedStatement = null
  private var updatePrincipalGroupCategoryStatement: PreparedStatement    = null
  private var updatePrincipalGroupTimestampStatement: PreparedStatement   = null
  private var deletePrincipalGroupStatement: PreparedStatement            = null
  private var deletePrincipalGroupAssignmentsStatement: PreparedStatement = null
  private var assignPrincipalStatement: PreparedStatement                 = null
  private var unassignPrincipalStatement: PreparedStatement               = null

  def createTables(): Future[Done] =
    for {
      _ <- session.executeCreateTable(
             """
               |CREATE TABLE IF NOT EXISTS groups (
               |          id text PRIMARY KEY,
               |          name text,
               |          description text,
               |          category_id text,
               |          updated_at text,
               |          updated_by_type text,
               |          updated_by_id text,
               |)
               |""".stripMargin
           )
      _ <- session.executeCreateTable(
             """
               |CREATE TABLE IF NOT EXISTS assignments (
               |          id text,
               |          principal_type text,
               |          principal_id text,
               |          PRIMARY KEY( id, principal_type, principal_id)
               |)
               |""".stripMargin
           )

    } yield Done

  def prepareStatements(): Future[Done] =
    for {
      insertPrincipalGroupStmt            <- session.prepare(
                                               """
                                      | INSERT INTO groups (id, name, description, category_id,
                                      |     updated_at, updated_by_type, updated_by_id
                                      |     )
                                      |   VALUES (:id, :name, :description, :category_id,
                                      |     :updated_at, :updated_by_type, :updated_by_id
                                      |     )
                                      |""".stripMargin
                                             )
      updatePrincipalGroupNameStmt        <- session.prepare(
                                               """
                                          | UPDATE groups SET
                                          |   name = :name,
                                          |   updated_at = :updated_at,
                                          |   updated_by_type = :updated_by_type,
                                          |   updated_by_id = :updated_by_id
                                          | WHERE id = :id
                                          |""".stripMargin
                                             )
      updatePrincipalGroupDescriptionStmt <- session.prepare(
                                               """
                                                 | UPDATE groups SET
                                                 |   description = :description,
                                                 |   updated_at = :updated_at,
                                                 |   updated_by_type = :updated_by_type,
                                                 |   updated_by_id = :updated_by_id
                                                 | WHERE id = :id
                                                 |""".stripMargin
                                             )

      updatePrincipalGroupCategoryStmt    <- session.prepare(
                                               """
                                              | UPDATE groups SET
                                              |   category_id = :category_id,
                                              |   updated_at = :updated_at,
                                              |   updated_by_type = :updated_by_type,
                                              |   updated_by_id = :updated_by_id
                                              | WHERE id = :id
                                              |""".stripMargin
                                             )

      deletePrincipalGroupStmt            <- session.prepare(
                                               """
                                      | DELETE FROM groups
                                      |  WHERE id = :id
                                      |""".stripMargin
                                             )
      deletePrincipalGroupAssignmentsStmt <- session.prepare(
                                               """
                                                 | DELETE FROM assignments
                                                 |  WHERE id = :id
                                                 |""".stripMargin
                                             )

      assignPrincipalStmt                 <- session.prepare(
                                               """
                                 | INSERT INTO assignments (id, principal_type, principal_id)
                                 |   VALUES (:id, :principal_type, :principal_id)
                                 |""".stripMargin
                                             )
      unassignPrincipalStmt               <- session.prepare(
                                               """
                                   | DELETE FROM assignments
                                   |  WHERE id = :id AND
                                   |        principal_type = :principal_type AND
                                   |        principal_id = : principal_id
                                   |""".stripMargin
                                             )

      updatePrincipalGroupTimestampStmt   <- session.prepare(
                                               """
                                               | UPDATE groups SET
                                               |   updated_at = :updated_at,
                                               |   updated_by_type = :updated_by_type,
                                               |   updated_by_id = :updated_by_id
                                               | WHERE id = :id
                                               |""".stripMargin
                                             )
    } yield {
      insertPrincipalGroupStatement = insertPrincipalGroupStmt
      updatePrincipalGroupNameStatement = updatePrincipalGroupNameStmt
      updatePrincipalGroupDescriptionStatement = updatePrincipalGroupDescriptionStmt
      updatePrincipalGroupCategoryStatement = updatePrincipalGroupCategoryStmt
      deletePrincipalGroupStatement = deletePrincipalGroupStmt
      deletePrincipalGroupAssignmentsStatement = deletePrincipalGroupAssignmentsStmt
      assignPrincipalStatement = assignPrincipalStmt
      unassignPrincipalStatement = unassignPrincipalStmt
      updatePrincipalGroupTimestampStatement = updatePrincipalGroupTimestampStmt
      Done
    }

  def createPrincipalGroup(event: PrincipalGroupCreated): Future[List[BoundStatement]] =
    build(
      insertPrincipalGroupStatement
        .bind()
        .setString("id", event.id)
        .setString("name", event.name)
        .setString("description", event.description)
        .setString("category_id", event.categoryId)
        .setString("updated_at", event.createdAt.toString)
        .setString("updated_by_type", event.createdBy.principalType)
        .setString("updated_by_id", event.createdBy.principalId)
    )

  def updatePrincipalGroupName(event: PrincipalGroupNameUpdated): Future[List[BoundStatement]]               =
    build(
      updatePrincipalGroupNameStatement
        .bind()
        .setString("id", event.id)
        .setString("name", event.name)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )
  def updatePrincipalGroupDescription(event: PrincipalGroupDescriptionUpdated): Future[List[BoundStatement]] =
    build(
      updatePrincipalGroupDescriptionStatement
        .bind()
        .setString("id", event.id)
        .setString("description", event.description)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def updatePrincipalGroupCategory(event: PrincipalGroupCategoryUpdated): Future[List[BoundStatement]] =
    build(
      updatePrincipalGroupCategoryStatement
        .bind()
        .setString("id", event.id)
        .setString("category_id", event.categoryId)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def deletePrincipalGroup(event: PrincipalGroupDeleted): Future[List[BoundStatement]] =
    build(
      deletePrincipalGroupStatement
        .bind()
        .setString("id", event.id),
      deletePrincipalGroupAssignmentsStatement
        .bind()
        .setString("id", event.id)
    )

  def assignPrincipal(event: PrincipalAssigned): Future[List[BoundStatement]] =
    build(
      assignPrincipalStatement
        .bind()
        .setString("id", event.id)
        .setString("principal_type", event.principal.principalType)
        .setString("principal_id", event.principal.principalId),
      updatePrincipalGroupTimestampStatement
        .bind()
        .setString("id", event.id)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def unassignPrincipal(event: PrincipalUnassigned): Future[List[BoundStatement]] =
    build(
      unassignPrincipalStatement
        .bind()
        .setString("id", event.id)
        .setString("principal_type", event.principal.principalType)
        .setString("principal_id", event.principal.principalId),
      updatePrincipalGroupTimestampStatement
        .bind()
        .setString("id", event.id)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def getPrincipalGroupById(id: PrincipalGroupId): Future[Option[PrincipalGroup]] =
    for {
      stmt   <- session.prepare("SELECT * FROM groups WHERE id = ?")
      result <- session.selectOne(stmt.bind(id)).map(_.map(convertPrincipalGroup))
    } yield result

  def getPrincipalGroupsById(ids: Set[PrincipalGroupId]): Future[Seq[PrincipalGroup]] =
    for {
      stmt   <- session.prepare("SELECT * FROM groups WHERE id IN ?")
      result <- session.selectAll(stmt.bind(ids.toList.asJava)).map(_.map(convertPrincipalGroup))
    } yield result

  private def convertPrincipalGroup(row: Row): PrincipalGroup =
    PrincipalGroup(
      id = row.getString("id"),
      name = row.getString("name"),
      description = row.getString("description"),
      categoryId = row.getString("category_id"),
      updatedAt = OffsetDateTime.parse(row.getString("updated_at")),
      updatedBy = AnnettePrincipal(
        principalType = row.getString("updated_by_type"),
        principalId = row.getString("updated_by_id")
      )
    )

  def getAssignments(id: PrincipalGroupId): Future[Set[AnnettePrincipal]] =
    for {
      stmt   <- session.prepare("SELECT * FROM assignments WHERE id = ?")
      result <- session.selectAll(stmt.bind(id)).map(_.map(convertPrincipals))
    } yield result.toSet

  private def convertPrincipals(row: Row): AnnettePrincipal =
    AnnettePrincipal(
      principalType = row.getString("principal_type"),
      principalId = row.getString("principal_id")
    )

  private def build(statements: BoundStatement*): Future[List[BoundStatement]] =
    Future.successful(statements.toList)

}
