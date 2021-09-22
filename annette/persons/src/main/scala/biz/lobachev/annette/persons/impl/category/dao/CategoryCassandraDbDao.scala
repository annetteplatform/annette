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

package biz.lobachev.annette.persons.impl.category.dao

import akka.Done
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.category.{Category, CategoryId}
import biz.lobachev.annette.persons.impl.category
import com.datastax.driver.core.{BoundStatement, PreparedStatement, Row}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession

import java.time.OffsetDateTime
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

class CategoryCassandraDbDao(session: CassandraSession, tableName: String)(implicit
  ec: ExecutionContext
) {

  private var insertCategoryStatement: PreparedStatement = null
  private var updateCategoryStatement: PreparedStatement = null
  private var deleteCategoryStatement: PreparedStatement = null

  def createTables(): Future[Done] =
    for {
      _ <- session.executeCreateTable(s"""
                                         |CREATE TABLE IF NOT EXISTS ${tableName} (
                                         |          id text PRIMARY KEY,
                                         |          name text,
                                         |          updated_at text,
                                         |          updated_by_type text,
                                         |          updated_by_id text,
                                         |)
                                         |""".stripMargin)

    } yield Done

  def prepareStatements(): Future[Done] =
    for {
      insertCategoryStmt <- session.prepare(
                              s"""
                                 | INSERT  INTO ${tableName} (id, name,
                                 |     updated_at, updated_by_type, updated_by_id
                                 |    )
                                 |   VALUES (:id, :name,
                                 |     :updated_at, :updated_by_type, :updated_by_id
                                 |    )
                                 |""".stripMargin
                            )
      updateCategoryStmt <- session.prepare(
                              s"""
                                 | UPDATE ${tableName} SET
                                 |   name = :name,
                                 |   updated_at = :updated_at,
                                 |   updated_by_type = :updated_by_type,
                                 |   updated_by_id = :updated_by_id
                                 | WHERE id = :id
                                 |""".stripMargin
                            )
      deleteCategoryStmt <- session.prepare(
                              s"""
                                 | DELETE FROM ${tableName}
                                 |   WHERE id = :id
                                 |""".stripMargin
                            )
    } yield {
      insertCategoryStatement = insertCategoryStmt
      updateCategoryStatement = updateCategoryStmt
      deleteCategoryStatement = deleteCategoryStmt
      Done
    }

  def createCategory(event: category.CategoryEntity.CategoryCreated): List[BoundStatement] =
    List(
      insertCategoryStatement
        .bind()
        .setString("id", event.id)
        .setString("name", event.name)
        .setString("updated_at", event.createdAt.toString)
        .setString("updated_by_type", event.createdBy.principalType)
        .setString("updated_by_id", event.createdBy.principalId)
    )

  def updateCategory(event: category.CategoryEntity.CategoryUpdated): List[BoundStatement] =
    List(
      updateCategoryStatement
        .bind()
        .setString("id", event.id)
        .setString("name", event.name)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def deleteCategory(event: category.CategoryEntity.CategoryDeleted): List[BoundStatement] =
    List(
      deleteCategoryStatement
        .bind()
        .setString("id", event.id)
    )

  def getCategoryById(id: CategoryId): Future[Option[Category]] =
    for {
      stmt   <- session.prepare(s"SELECT * FROM ${tableName} WHERE id = ?")
      result <- session.selectOne(stmt.bind(id)).map(_.map(convertCategory))
    } yield result

  def getCategoriesById(ids: Set[CategoryId]): Future[Seq[Category]] =
    for {
      stmt   <- session.prepare(s"SELECT * FROM ${tableName} WHERE id IN ?")
      result <- session
                  .selectAll(stmt.bind(ids.toList.asJava))
                  .map(
                    _.map(convertCategory)
                  )
    } yield result

  private def convertCategory(row: Row): Category =
    Category(
      id = row.getString("id"),
      name = row.getString("name"),
      updatedAt = OffsetDateTime.parse(row.getString("updated_at")),
      updatedBy = AnnettePrincipal(
        principalType = row.getString("updated_by_type"),
        principalId = row.getString("updated_by_id")
      )
    )

}
