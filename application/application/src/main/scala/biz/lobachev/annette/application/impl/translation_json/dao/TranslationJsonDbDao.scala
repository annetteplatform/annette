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
import biz.lobachev.annette.application.impl.translation_json.model.TranslationJsonInt
import biz.lobachev.annette.core.model.LanguageId
import biz.lobachev.annette.microservice_core.db.{CassandraQuillDao, CassandraTableBuilder}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import play.api.libs.json.JsObject
import io.scalaland.chimney.dsl._

import scala.concurrent.{ExecutionContext, Future}

private[impl] class TranslationJsonDbDao(
  override val session: CassandraSession
)(implicit ec: ExecutionContext)
    extends CassandraQuillDao {

  import ctx._

  private val schema = quote(querySchema[TranslationJsonInt]("translation_jsons"))

  private implicit val jsEncoder        = genericJsonEncoder[JsObject]
  private implicit val jsDecoder        = genericJsonDecoder[JsObject]
  private implicit val insertEntityMeta = insertMeta[TranslationJson]()
  private implicit val updateEntityMeta = updateMeta[TranslationJson](_.translationId, _.languageId)
  println(jsEncoder.toString)
  println(jsDecoder.toString)
  println(insertEntityMeta.toString)
  println(updateEntityMeta.toString)

  def createTables() = {
    import CassandraTableBuilder.types._
    for {
      _ <- session.executeCreateTable(
             CassandraTableBuilder("translation_jsons")
               .column("translation_id", Text)
               .column("language_id", Text)
               .column("json", Text)
               .column("updated_at", Timestamp)
               .column("updated_by", Text)
               .withPrimaryKey("translation_id", "language_id")
               .build
           )
    } yield Done
  }

  def updateTranslationJson(event: TranslationJsonEntity.TranslationJsonUpdated) = {
    val entity = event.transformInto[TranslationJsonInt]
    ctx.run(schema.insert(lift(entity)))
  }

  def deleteTranslationJson(event: TranslationJsonEntity.TranslationJsonDeleted) =
    ctx.run(
      schema
        .filter(e =>
          e.translationId == lift(event.translationId) &&
            e.languageId == lift(event.languageId)
        )
        .delete
    )

  def getTranslationLanguages(translationId: TranslationId): Future[Seq[LanguageId]] =
    ctx
      .run(schema.filter(_.translationId == lift(translationId)).map(_.languageId))
      .map(_.sorted)

  def getTranslationLanguages(ids: Set[TranslationId]): Future[Map[TranslationId, Seq[LanguageId]]] =
    ctx
      .run(schema.filter(b => liftQuery(ids).contains(b.translationId)).map(r => r.translationId -> r.languageId))
      .map(
        _.groupMap(_._1)(_._2).map { case (k, v) => k -> v.sorted }
      )

  def getTranslationJsonById(id: TranslationId, languageId: LanguageId): Future[Option[TranslationJson]] =
    ctx
      .run(
        schema.filter(e =>
          e.translationId == lift(id) &&
            e.languageId == lift(languageId)
        )
      )
      .map(_.headOption.map(_.toTranslationJson))

  def getTranslationJsons(ids: Set[TranslationId], languageId: LanguageId): Future[Seq[TranslationJson]] =
    ctx
      .run(
        schema.filter(e =>
          liftQuery(ids).contains(e.translationId) &&
            e.languageId == lift(languageId)
        )
      )
      .map(_.map(_.toTranslationJson))
}
