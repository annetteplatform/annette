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

package biz.lobachev.annette.application.impl.translation.dao

import akka.Done
import biz.lobachev.annette.application.api.translation._
import biz.lobachev.annette.application.impl.translation.TranslationEntity
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import com.datastax.driver.core.{BoundStatement, PreparedStatement, Row}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import org.slf4j.LoggerFactory

import java.time.OffsetDateTime
import scala.collection.immutable.{Seq, _}
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

private[impl] class TranslationCassandraDbDao(session: CassandraSession)(implicit ec: ExecutionContext) {

  val log = LoggerFactory.getLogger(this.getClass)

  private var createTranslationStatement: PreparedStatement = _
  private var updateTranslationStatement: PreparedStatement = _
  private var deleteTranslationStatement: PreparedStatement = _

  def createTables(): Future[Unit] =
    for {
      _ <- session.executeCreateTable("""
                                        |CREATE TABLE IF NOT EXISTS translations (
                                        |      id                text ,
                                        |      name       text ,
                                        |      updated_at        text,
                                        |      updated_by_type   text,
                                        |      updated_by_id     text,
                                        |      PRIMARY KEY (id)
                                        |)
                                        |""".stripMargin)
    } yield ()

  def prepareStatements(): Future[Done] =
    for {
      createTranslationStmt <- session.prepare(
                                 """
                                   | INSERT INTO translations (
                                   |   id                ,
                                   |   name              ,
                                   |   updated_at        ,
                                   |   updated_by_type   ,
                                   |   updated_by_id
                                   | )
                                   | VALUES (
                                   |   :id                ,
                                   |   :name       ,
                                   |   :updated_at        ,
                                   |   :updated_by_type   ,
                                   |   :updated_by_id
                                   | )
                                   |""".stripMargin
                               )

      updateTranslationStmt <- session.prepare(
                                 """
                                   | UPDATE translations SET
                                   |    name            =  :name           ,
                                   |    updated_at      =  :updated_at     ,
                                   |    updated_by_type =  :updated_by_type,
                                   |    updated_by_id   =  :updated_by_id
                                   | WHERE id = :id
                                   |""".stripMargin
                               )

      deleteTranslationStmt <- session.prepare(
                                 """
                                   | DELETE FROM translations
                                   | WHERE id = :id
                                   |""".stripMargin
                               )

    } yield {
      createTranslationStatement = createTranslationStmt
      updateTranslationStatement = updateTranslationStmt
      deleteTranslationStatement = deleteTranslationStmt
      Done
    }

  def createTranslation(event: TranslationEntity.TranslationCreated): Seq[BoundStatement] =
    Seq(
      createTranslationStatement
        .bind()
        .setString("id", event.id)
        .setString("name", event.name)
        .setString("updated_at", event.createdAt.toString)
        .setString("updated_by_type", event.createdBy.principalType)
        .setString("updated_by_id", event.createdBy.principalId)
    )

  def updateTranslation(event: TranslationEntity.TranslationUpdated): Seq[BoundStatement] =
    Seq(
      updateTranslationStatement
        .bind()
        .setString("id", event.id)
        .setString("name", event.name)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def deleteTranslation(event: TranslationEntity.TranslationDeleted): Seq[BoundStatement] =
    Seq(
      deleteTranslationStatement
        .bind()
        .setString("id", event.id)
    )

  def getTranslationById(id: TranslationId): Future[Option[Translation]] =
    for {
      stmt   <- session.prepare("SELECT * FROM translations WHERE id=:id")
      result <- session.selectOne(stmt.bind().setString("id", id)).map(_.map(convertTranslation))
    } yield result

  def getTranslationsById(ids: Set[TranslationId]): Future[Seq[Translation]] =
    for {
      stmt   <- session.prepare("SELECT * FROM translations WHERE id IN ?")
      result <- session.selectAll(stmt.bind(ids.toList.asJava)).map(_.map(convertTranslation))
    } yield result

  def convertTranslation(row: Row): Translation =
    Translation(
      id = row.getString("id"),
      name = row.getString("name"),
      updatedAt = OffsetDateTime.parse(row.getString("updated_at")),
      updatedBy = AnnettePrincipal(
        principalType = row.getString("updated_by_type"),
        principalId = row.getString("updated_by_id")
      )
    )

}
