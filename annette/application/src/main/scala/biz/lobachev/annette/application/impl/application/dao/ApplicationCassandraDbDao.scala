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

package biz.lobachev.annette.application.impl.application.dao

import java.time.OffsetDateTime

import akka.Done
import biz.lobachev.annette.application.api.application._
import biz.lobachev.annette.application.api.translation.{Caption, TextCaption, TranslationCaption}
import biz.lobachev.annette.application.impl.application.ApplicationEntity
import biz.lobachev.annette.core.model.AnnettePrincipal
import com.datastax.driver.core.{BoundStatement, PreparedStatement, Row}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import org.slf4j.LoggerFactory

import scala.collection.immutable.{Seq, _}
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

private[impl] class ApplicationCassandraDbDao(session: CassandraSession)(implicit ec: ExecutionContext)
    extends ApplicationDbDao {

  val log = LoggerFactory.getLogger(this.getClass)

  private var createApplicationStatement: PreparedStatement             = _
  private var updateApplicationNameStatement: PreparedStatement         = _
  private var updateApplicationCaptionStatement: PreparedStatement      = _
  private var updateApplicationTranslationsStatement: PreparedStatement = _
  private var updateApplicationServerUrlStatement: PreparedStatement    = _
  private var deleteApplicationStatement: PreparedStatement             = _

  override def createTables(): Future[Done] =
    for {
      _ <- session.executeCreateTable("""
                                        |CREATE TABLE IF NOT EXISTS applications (
                                        |          id                      text PRIMARY KEY,
                                        |          name                    text,
                                        |          caption_text            text,
                                        |          caption_translation_id  text,
                                        |          translations            list<text>,
                                        |          server_url              text,
                                        |          updated_at              text,
                                        |          updated_by_type         text,
                                        |          updated_by_id           text
                                        |)
                                        |""".stripMargin)
    } yield Done

  override def prepareStatements(): Future[Done] =
    for {
      createApplicationStmt             <- session.prepare(
                                             """
                                   |INSERT  INTO applications (
                                   |      id                    ,
                                   |      name                  ,
                                   |      caption_text          ,
                                   |      caption_translation_id,
                                   |      translations          ,
                                   |      server_url            ,
                                   |      updated_at            ,
                                   |      updated_by_type       ,
                                   |      updated_by_id         
                                   |     )
                                   |   VALUES (
                                   |      :id                    ,
                                   |      :name                  ,
                                   |      :caption_text          ,
                                   |      :caption_translation_id,
                                   |      :translations          ,
                                   |      :server_url            ,
                                   |      :updated_at            ,
                                   |      :updated_by_type       ,
                                   |      :updated_by_id          
                                   |     )  
                                   |""".stripMargin
                                           )
      updateApplicationNameStmt         <- session.prepare(
                                             """
                                       |UPDATE applications SET
                                       |   name = :name,
                                       |   updated_at = :updated_at,
                                       |   updated_by_type = :updated_by_type,
                                       |   updated_by_id = :updated_by_id   
                                       | WHERE id = :id
                                       |""".stripMargin
                                           )
      updateApplicationCaptionStmt      <- session.prepare(
                                             """
                                          |UPDATE applications SET
                                          |   caption_text = :caption_text          ,
                                          |   caption_translation_id = :caption_translation_id,
                                          |   updated_at = :updated_at,
                                          |   updated_by_type = :updated_by_type,
                                          |   updated_by_id = :updated_by_id
                                          | WHERE id = :id
                                          |""".stripMargin
                                           )
      updateApplicationTranslationsStmt <- session.prepare(
                                             """
                                               |UPDATE applications SET
                                               |   translations = :translations,
                                               |   updated_at = :updated_at,
                                               |   updated_by_type = :updated_by_type,
                                               |   updated_by_id = :updated_by_id
                                               | WHERE id = :id
                                               |""".stripMargin
                                           )
      updateApplicationServerUrlStmt    <- session.prepare(
                                             """
                                            |UPDATE applications SET
                                            |   server_url = :server_url,
                                            |   updated_at = :updated_at,
                                            |   updated_by_type = :updated_by_type,
                                            |   updated_by_id = :updated_by_id             
                                            | WHERE id = :id
                                            |""".stripMargin
                                           )
      deleteApplicationStmt             <- session.prepare(
                                             """
                                   |DELETE FROM applications
                                   | WHERE id = :id
                                   |""".stripMargin
                                           )
    } yield {
      createApplicationStatement = createApplicationStmt
      updateApplicationNameStatement = updateApplicationNameStmt
      updateApplicationCaptionStatement = updateApplicationCaptionStmt
      updateApplicationTranslationsStatement = updateApplicationTranslationsStmt
      updateApplicationServerUrlStatement = updateApplicationServerUrlStmt
      deleteApplicationStatement = deleteApplicationStmt
      Done
    }

  override def createApplication(event: ApplicationEntity.ApplicationCreated): Seq[BoundStatement] = {
    val (captionText, captionTranslationId) = event.caption match {
      case TextCaption(text)                 => (text, null)
      case TranslationCaption(translationId) => (null, translationId)
    }
    Seq(
      createApplicationStatement
        .bind()
        .setString("id", event.id)
        .setString("name", event.name)
        .setString("caption_text", captionText)
        .setString("caption_translation_id", captionTranslationId)
        .setList[String]("translations", event.translations.toSeq.asJava)
        .setString("server_url", event.serverUrl)
        .setString("updated_at", event.createdAt.toString)
        .setString("updated_by_type", event.createdBy.principalType)
        .setString("updated_by_id", event.createdBy.principalId)
    )
  }

  override def updateApplicationName(event: ApplicationEntity.ApplicationNameUpdated): Seq[BoundStatement] =
    Seq(
      updateApplicationNameStatement
        .bind()
        .setString("id", event.id)
        .setString("name", event.name)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  override def updateApplicationCaption(event: ApplicationEntity.ApplicationCaptionUpdated): Seq[BoundStatement] = {
    val (captionText, captionTranslationId) = event.caption match {
      case TextCaption(text)                 => (text, null)
      case TranslationCaption(translationId) => (null, translationId)
    }
    Seq(
      updateApplicationCaptionStatement
        .bind()
        .setString("id", event.id)
        .setString("caption_text", captionText)
        .setString("caption_translation_id", captionTranslationId)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )
  }

  override def updateApplicationTranslations(
    event: ApplicationEntity.ApplicationTranslationsUpdated
  ): Seq[BoundStatement] =
    Seq(
      updateApplicationTranslationsStatement
        .bind()
        .setString("id", event.id)
        .setList[String]("translations", event.translations.toSeq.asJava)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  override def updateApplicationServerUrl(event: ApplicationEntity.ApplicationServerUrlUpdated): Seq[BoundStatement] =
    Seq(
      updateApplicationServerUrlStatement
        .bind()
        .setString("id", event.id)
        .setString("server_url", event.serverUrl)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  override def deleteApplication(event: ApplicationEntity.ApplicationDeleted): Seq[BoundStatement] =
    Seq(
      deleteApplicationStatement
        .bind()
        .setString("id", event.id)
    )

  override def getApplicationById(id: ApplicationId): Future[Option[Application]] =
    for {
      stmt        <- session.prepare("SELECT * FROM applications WHERE id = :id")
      maybeEntity <- session.selectOne(stmt.bind().setString("id", id)).map(_.map(convertApplication))
    } yield maybeEntity

  override def getApplicationsById(ids: Set[ApplicationId]): Future[Map[ApplicationId, Application]] =
    for {
      stmt   <- session.prepare("SELECT * FROM applications WHERE id IN ?")
      result <- session.selectAll(stmt.bind(ids.toList.asJava)).map(_.map(convertApplication))
    } yield result.map(a => a.id -> a).toMap

  def convertApplication(row: Row): Application = {
    val caption: Caption = Option(row.getString("caption_text"))
      .map(t => TextCaption(t))
      .getOrElse(TranslationCaption(row.getString("caption_translation_id")))
    Application(
      id = row.getString("id"),
      name = row.getString("name"),
      caption = caption,
      translations = row.getList[String]("translations", classOf[String]).asScala.toSet,
      serverUrl = row.getString("server_url"),
      updatedAt = OffsetDateTime.parse(row.getString("updated_at")),
      updatedBy = AnnettePrincipal(
        principalType = row.getString("updated_by_type"),
        principalId = row.getString("updated_by_id")
      )
    )
  }
}
