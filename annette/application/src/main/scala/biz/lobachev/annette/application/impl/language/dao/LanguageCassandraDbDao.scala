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

package biz.lobachev.annette.application.impl.language.dao

import java.time.OffsetDateTime
import akka.Done
import biz.lobachev.annette.application.api.language._
import biz.lobachev.annette.application.impl.language.LanguageEntity
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import com.datastax.driver.core.{BoundStatement, PreparedStatement, Row}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession

import scala.collection.immutable.{Seq, _}
import scala.concurrent.{ExecutionContext, Future}

private[impl] class LanguageCassandraDbDao(session: CassandraSession)(implicit ec: ExecutionContext)
    extends LanguageDbDao {

  private var createLanguageStatement: PreparedStatement = _
  private var updateLanguageStatement: PreparedStatement = _
  private var deleteLanguageStatement: PreparedStatement = _

  def createTables(): Future[Unit] =
    for {
      _ <- session.executeCreateTable(
             """
               |CREATE TABLE  IF NOT EXISTS languages (
               |          id                text PRIMARY KEY,
               |          name              text,
               |          updated_at        text,
               |          updated_by_type   text,
               |          updated_by_id     text
               |)
               |""".stripMargin
           )
    } yield ()

  def prepareStatements(): Future[Done] =
    for {
      createLanguageStmt <- session.prepare(
                              """
                                | INSERT INTO languages (
                                |   id                ,
                                |   name              ,
                                |   updated_at        ,
                                |   updated_by_type   ,
                                |   updated_by_id
                                | )
                                | VALUES (
                                |   :id                ,
                                |   :name              ,
                                |   :updated_at        ,
                                |   :updated_by_type   ,
                                |   :updated_by_id
                                | )
                                |""".stripMargin
                            )
      updateLanguageStmt <- session.prepare(
                              """
                                | UPDATE languages SET
                                |    name            =  :name           ,
                                |    updated_at      =  :updated_at     ,
                                |    updated_by_type =  :updated_by_type,
                                |    updated_by_id   =  :updated_by_id
                                | WHERE id = :id
                                |""".stripMargin
                            )
      deleteLanguageStmt <- session.prepare(
                              """
                                | DELETE FROM languages
                                | WHERE id = :id
                                |""".stripMargin
                            )
    } yield {
      createLanguageStatement = createLanguageStmt
      updateLanguageStatement = updateLanguageStmt
      deleteLanguageStatement = deleteLanguageStmt
      Done
    }

  def createLanguage(event: LanguageEntity.LanguageCreated): Seq[BoundStatement] =
    Seq(
      createLanguageStatement
        .bind()
        .setString("id", event.id)
        .setString("name", event.name)
        .setString("updated_at", event.createdAt.toString)
        .setString("updated_by_type", event.createdBy.principalType)
        .setString("updated_by_id", event.createdBy.principalId)
    )

  def updateLanguage(event: LanguageEntity.LanguageUpdated): Seq[BoundStatement] =
    Seq(
      updateLanguageStatement
        .bind()
        .setString("id", event.id)
        .setString("name", event.name)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def deleteLanguage(event: LanguageEntity.LanguageDeleted): Seq[BoundStatement] =
    Seq(
      deleteLanguageStatement
        .bind()
        .setString("id", event.id)
    )

  def getLanguageById(id: LanguageId): Future[Option[Language]] =
    for {
      stmt        <- session.prepare("SELECT * FROM languages WHERE id = :id")
      maybeEntity <- session.selectOne(stmt.bind().setString("id", id)).map(_.map(convertLanguage))
    } yield maybeEntity

  def getLanguages: Future[Map[LanguageId, Language]] =
    for {
      stmt   <- session.prepare("SELECT * FROM languages")
      result <- session.selectAll(stmt.bind).map(_.map(convertLanguage))
    } yield result.map(a => a.id -> a).toMap

  def convertLanguage(row: Row): Language             =
    Language(
      id = row.getString("id"),
      name = row.getString("name"),
      updatedAt = OffsetDateTime.parse(row.getString("updated_at")),
      updatedBy = AnnettePrincipal(
        principalType = row.getString("updated_by_type"),
        principalId = row.getString("updated_by_id")
      )
    )

}
