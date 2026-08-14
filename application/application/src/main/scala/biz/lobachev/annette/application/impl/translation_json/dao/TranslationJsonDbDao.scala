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

import org.apache.pekko.Done
import biz.lobachev.annette.application.api.translation._
import biz.lobachev.annette.application.impl.translation_json.TranslationJsonEntity
import biz.lobachev.annette.application.impl.translation_json.model.TranslationJsonInt
import biz.lobachev.annette.core.model.LanguageId
import biz.lobachev.annette.microservice_core.pekko.db.{CassandraQuillDao, CassandraTableBuilder}
import com.typesafe.config.Config
import io.getquill.CassandraContextConfig
import biz.lobachev.annette.core.utils.ChimneyCommons._
import io.scalaland.chimney.dsl._
import play.api.libs.json.JsObject

import scala.concurrent.{ExecutionContext, Future}

private[impl] class TranslationJsonDbDao(
  config: Config
)(implicit ec: ExecutionContext)
    extends CassandraQuillDao {

  override protected def cassandraContextConfig: CassandraContextConfig =
    if (config.hasPath("cassandra-quill"))
      CassandraContextConfig(config.getConfig("cassandra-quill"))
    else
      CassandraContextConfig(config.getConfig("cassandra.default"))

  import ctx._

  private val schema = quote(querySchema[TranslationJsonInt]("translation_jsons"))

  private implicit val jsEncoder        = genericJsonEncoder[JsObject]
  private implicit val jsDecoder        = genericJsonDecoder[JsObject]
  private implicit val insertEntityMeta = insertMeta[TranslationJson]()
  private implicit val updateEntityMeta = updateMeta[TranslationJson](_.translationId, _.languageId)
  touch(jsEncoder)
  touch(jsDecoder)
  touch(insertEntityMeta)
  touch(updateEntityMeta)

  def createTables(): Future[Done] = {
    import CassandraTableBuilder.types._
    Future {
      ctx.session.execute(
        CassandraTableBuilder("translation_jsons")
          .column("translation_id", Text)
          .column("language_id", Text)
          .column("json", Text)
          .column("updated_at", Timestamp)
          .column("updated_by", Text)
          .withPrimaryKey("translation_id", "language_id")
          .build
      )
      Done
    }
  }

  def updateTranslationJson(event: TranslationJsonEntity.TranslationJsonUpdated): Future[Done] = {
    val entity = event.transformInto[TranslationJsonInt]
    for {
      _ <- ctx.run(schema.insert(lift(entity)))
    } yield Done
  }

  def deleteTranslationJson(event: TranslationJsonEntity.TranslationJsonDeleted): Future[Done] =
    for {
      _ <- ctx.run(
             schema
               .filter(e =>
                 e.translationId == lift(event.translationId) &&
                   e.languageId == lift(event.languageId)
               )
               .delete
           )
    } yield Done

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

  def getTranslationJson(id: TranslationId, languageId: LanguageId): Future[Option[TranslationJson]] =
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
