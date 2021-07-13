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

package biz.lobachev.annette.application.impl.translation_json.dao

import akka.Done
import biz.lobachev.annette.application.api.translation._
import biz.lobachev.annette.application.impl.translation_json.TranslationJsonEntity
import biz.lobachev.annette.core.model.LanguageId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import com.datastax.driver.core.{BoundStatement, PreparedStatement, Row}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsObject, Json}

import java.time.OffsetDateTime
import scala.collection.immutable.{Seq, _}
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

private[impl] class TranslationJsonCassandraDbDao(session: CassandraSession)(implicit ec: ExecutionContext) {

  val log = LoggerFactory.getLogger(this.getClass)

  private var updateTranslationJsonStatement: PreparedStatement = _
  private var deleteTranslationJsonStatement: PreparedStatement = _

  def createTables(): Future[Unit] =
    for {
      _ <- session.executeCreateTable("""
                                        |CREATE TABLE IF NOT EXISTS translation_jsons (
                                        |      translation_id    text ,
                                        |      language_id       text ,
                                        |      json              text ,
                                        |      updated_at        text,
                                        |      updated_by_type   text,
                                        |      updated_by_id     text,
                                        |      PRIMARY KEY ( translation_id, language_id)
                                        |)
                                        |""".stripMargin)
    } yield ()

  def prepareStatements(): Future[Done] =
    for {
      updateTranslationJsonStmt <- session.prepare(
                                     """
                                       | INSERT INTO translation_jsons (
                                       |   translation_id    ,
                                       |   language_id       ,
                                       |   json              ,
                                       |   updated_at        ,
                                       |   updated_by_type   ,
                                       |   updated_by_id
                                       | )
                                       | VALUES (
                                       |   :translation_id    ,
                                       |   :language_id       ,
                                       |   :json              ,
                                       |   :updated_at        ,
                                       |   :updated_by_type   ,
                                       |   :updated_by_id
                                       | )
                                       |""".stripMargin
                                   )

      deleteTranslationJsonStmt <- session.prepare(
                                     """
                                       | DELETE FROM translation_jsons
                                       | WHERE translation_id = :translation_id AND
                                       |       language_id = :language_id
                                       |""".stripMargin
                                   )

    } yield {
      updateTranslationJsonStatement = updateTranslationJsonStmt
      deleteTranslationJsonStatement = deleteTranslationJsonStmt
      Done
    }

  def deleteTranslationJson(event: TranslationJsonEntity.TranslationJsonDeleted): Seq[BoundStatement] =
    Seq(
      deleteTranslationJsonStatement
        .bind()
        .setString("translation_id", event.translationId)
        .setString("language_id", event.languageId)
    )

  def updateTranslationJson(event: TranslationJsonEntity.TranslationJsonUpdated): Seq[BoundStatement] =
    Seq(
      updateTranslationJsonStatement
        .bind()
        .setString("translation_id", event.translationId)
        .setString("language_id", event.languageId)
        .setString("json", event.json.toString())
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def getTranslationLanguages(translationId: TranslationId): Future[Seq[LanguageId]] =
    for {
      stmt   <- session.prepare("SELECT language_id FROM translation_jsons WHERE translation_id = :translation_id ")
      result <- session
                  .selectAll(
                    stmt
                      .bind()
                      .setString("translation_id", translationId)
                  )
                  .map(_.map(_.getString("language_id")))
    } yield result

  def getTranslationJsonById(id: TranslationId, languageId: LanguageId): Future[Option[TranslationJson]] =
    for {
      stmt        <- session.prepare(
                       "SELECT * FROM translation_jsons WHERE translation_id = :translation_id AND language_id = :language_id"
                     )
      maybeEntity <- session
                       .selectOne(
                         stmt
                           .bind()
                           .setString("translation_id", id)
                           .setString("language_id", languageId)
                       )
                       .map(_.map(convertTranslationJson))
    } yield maybeEntity

  def getTranslationJsons(
    ids: Set[TranslationId],
    languageId: LanguageId
  ): Future[Seq[TranslationJson]] =
    for {
      stmt   <-
        session.prepare("SELECT * FROM translation_jsons WHERE translation_id IN :ids AND language_id = :language_id")
      result <- session
                  .selectAll(
                    stmt
                      .bind()
                      .setList("ids", ids.toList.asJava)
                      .setString("language_id", languageId)
                  )
                  .map(_.map(convertTranslationJson))
    } yield result

  def convertTranslationJson(row: Row): TranslationJson =
    TranslationJson(
      translationId = row.getString("translation_id"),
      languageId = row.getString("language_id"),
      json = Json.parse(row.getString("json")).asInstanceOf[JsObject],
      updatedAt = OffsetDateTime.parse(row.getString("updated_at")),
      updatedBy = AnnettePrincipal(
        principalType = row.getString("updated_by_type"),
        principalId = row.getString("updated_by_id")
      )
    )

}
