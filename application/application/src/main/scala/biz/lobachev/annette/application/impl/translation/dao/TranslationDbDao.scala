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
import biz.lobachev.annette.microservice_core.db.{CassandraQuillDao, CassandraTableBuilder}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import biz.lobachev.annette.core.utils.ChimneyCommons._
import io.scalaland.chimney.dsl._

import scala.concurrent.{ExecutionContext, Future}

private[application] class TranslationDbDao(
  override val session: CassandraSession
)(implicit ec: ExecutionContext)
    extends CassandraQuillDao {

  import ctx._

  private val schema = quote(querySchema[Translation]("translations"))

  private implicit val insertEntityMeta = insertMeta[Translation]()
  private implicit val updateEntityMeta = updateMeta[Translation](_.id)
  touch(insertEntityMeta)
  touch(updateEntityMeta)

  def createTables() = {
    import CassandraTableBuilder.types._
    for {
      _ <- session.executeCreateTable(
             CassandraTableBuilder("translations")
               .column("id", Text, true)
               .column("name", Text)
               .column("updated_at", Timestamp)
               .column("updated_by", Text)
               .build
           )
    } yield Done
  }

  def createTranslation(event: TranslationEntity.TranslationCreated) = {
    val entity = event
      .into[Translation]
      .withFieldComputed(_.updatedAt, _.createdAt)
      .withFieldComputed(_.updatedBy, _.createdBy)
      .transform
    ctx.run(schema.insert(lift(entity)))
  }

  def updateTranslation(event: TranslationEntity.TranslationUpdated) = {
    val entity = event.transformInto[Translation]
    ctx.run(schema.filter(_.id == lift(entity.id)).update(lift(entity)))
  }

  def deleteTranslation(event: TranslationEntity.TranslationDeleted) =
    ctx.run(schema.filter(_.id == lift(event.id)).delete)

  def getTranslation(id: TranslationId): Future[Option[Translation]] =
    ctx
      .run(schema.filter(_.id == lift(id)))
      .map(_.headOption)

  def getTranslations(ids: Set[TranslationId]): Future[Seq[Translation]] =
    ctx.run(schema.filter(b => liftQuery(ids).contains(b.id)))
}
