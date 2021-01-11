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

import java.time.OffsetDateTime
import akka.Done
import biz.lobachev.annette.application.api.language.LanguageId
import biz.lobachev.annette.application.api.translation._
import biz.lobachev.annette.application.impl.translation.TranslationEntity
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import com.datastax.driver.core.{BoundStatement, PreparedStatement, Row}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsObject, Json}

import scala.collection.immutable.{Seq, _}
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

private[impl] class TranslationCassandraDbDao(session: CassandraSession)(implicit ec: ExecutionContext)
    extends TranslationDbDao {

  val log = LoggerFactory.getLogger(this.getClass)

  private var deleteTranslationStatement: PreparedStatement     = _
  private var changeTranslationJsonStatement: PreparedStatement = _

  def createTables(): Future[Unit] =
    for {
      _ <- session.executeCreateTable("""
                                        |CREATE TABLE IF NOT EXISTS translation_jsons (
                                        |      id                text ,
                                        |      language_id       text ,
                                        |      json              text ,
                                        |      updated_at        text,
                                        |      updated_by_type   text,
                                        |      updated_by_id     text,
                                        |      PRIMARY KEY ( id, language_id)
                                        |)
                                        |""".stripMargin)
    } yield ()

  def prepareStatements(): Future[Done] =
    for {
      changeTranslationJsonStmt <- session.prepare(
                                     """
                                       | INSERT INTO translation_jsons (
                                       |   id                ,
                                       |   language_id       ,
                                       |   json              ,
                                       |   updated_at        ,
                                       |   updated_by_type   ,
                                       |   updated_by_id
                                       | )
                                       | VALUES (
                                       |   :id                ,
                                       |   :language_id       ,
                                       |   :json              ,
                                       |   :updated_at        ,
                                       |   :updated_by_type   ,
                                       |   :updated_by_id
                                       | )
                                       |""".stripMargin
                                   )

      deleteTranslationStmt     <- session.prepare(
                                     """
                                   | DELETE FROM translation_jsons
                                   | WHERE id = :id
                                   |""".stripMargin
                                   )

    } yield {
      deleteTranslationStatement = deleteTranslationStmt
      changeTranslationJsonStatement = changeTranslationJsonStmt
      Done
    }

  def deleteTranslation(event: TranslationEntity.TranslationDeleted): Seq[BoundStatement] = {
    println(event)
    Seq(
      deleteTranslationStatement
        .bind()
        .setString("id", event.id)
    )
  }

  def changeTranslationJson(
    id: TranslationId,
    languageId: LanguageId,
    json: String,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime
  ): Seq[BoundStatement] =
    Seq(
      changeTranslationJsonStatement
        .bind()
        .setString("id", id)
        .setString("language_id", languageId)
        .setString("json", json)
        .setString("updated_at", updatedAt.toString)
        .setString("updated_by_type", updatedBy.principalType)
        .setString("updated_by_id", updatedBy.principalId)
    )

  def getTranslationJsonById(id: TranslationId, languageId: LanguageId): Future[Option[TranslationJson]] =
    for {
      stmt        <- session.prepare("SELECT * FROM translation_jsons WHERE id = :id AND language_id = :language_id")
      maybeEntity <- session
                       .selectOne(
                         stmt
                           .bind()
                           .setString("id", id)
                           .setString("language_id", languageId)
                       )
                       .map(_.map(convertTranslationJson))
    } yield maybeEntity

  def getTranslationJsonsById(
    ids: Set[TranslationId],
    languageId: LanguageId
  ): Future[Map[TranslationId, TranslationJson]]        =
    for {
      stmt   <- session.prepare("SELECT * FROM translation_jsons WHERE id IN :ids AND language_id = :language_id")
      result <- session
                  .selectAll(
                    stmt
                      .bind()
                      .setList("ids", ids.toList.asJava)
                      .setString("language_id", languageId)
                  )
                  .map(_.map(convertTranslationJson))
    } yield result.map(a => a.id -> a).toMap

  def convertTranslationJson(row: Row): TranslationJson =
    TranslationJson(
      id = row.getString("id"),
      languageId = row.getString("language_id"),
      json = Json.parse(row.getString("json")).asInstanceOf[JsObject],
      updatedAt = OffsetDateTime.parse(row.getString("updated_at")),
      updatedBy = AnnettePrincipal(
        principalType = row.getString("updated_by_type"),
        principalId = row.getString("updated_by_id")
      )
    )

}
