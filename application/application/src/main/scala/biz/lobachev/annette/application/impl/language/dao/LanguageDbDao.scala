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

import akka.Done
import biz.lobachev.annette.application.api.language._
import biz.lobachev.annette.application.impl.language.LanguageEntity
import biz.lobachev.annette.core.model.LanguageId
import biz.lobachev.annette.microservice_core.db.{CassandraQuillDao, CassandraTableBuilder}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import io.scalaland.chimney.dsl._

import scala.concurrent.{ExecutionContext, Future}

private[impl] class LanguageDbDao(
  override val session: CassandraSession
)(implicit ec: ExecutionContext)
    extends CassandraQuillDao {

  import ctx._

  private val schema = quote(querySchema[Language]("languages"))

  private implicit val insertEntityMeta = insertMeta[Language]()
  private implicit val updateEntityMeta = updateMeta[Language](_.id)
  println(insertEntityMeta.toString)
  println(updateEntityMeta.toString)

  def createTables() = {
    import CassandraTableBuilder.types._
    for {
      _ <- session.executeCreateTable(
             CassandraTableBuilder("languages")
               .column("id", Text, true)
               .column("name", Text)
               .column("updated_at", Timestamp)
               .column("updated_by", Text)
               .build
           )
    } yield Done
  }

  def createLanguage(event: LanguageEntity.LanguageCreated) = {
    val entity = event
      .into[Language]
      .withFieldComputed(_.updatedAt, _.createdAt)
      .withFieldComputed(_.updatedBy, _.createdBy)
      .transform
    ctx.run(schema.insert(lift(entity)))
  }
  def updateLanguage(event: LanguageEntity.LanguageUpdated) = {
    val entity = event.transformInto[Language]
    ctx.run(schema.filter(_.id == lift(entity.id)).update(lift(entity)))
  }

  def deleteLanguage(event: LanguageEntity.LanguageDeleted) =
    ctx.run(schema.filter(_.id == lift(event.id)).delete)

  def getLanguageById(id: LanguageId): Future[Option[Language]] =
    ctx
      .run(schema.filter(_.id == lift(id)))
      .map(_.headOption)

  def getLanguagesById(ids: Set[LanguageId]): Future[Seq[Language]] =
    ctx.run(schema.filter(b => liftQuery(ids).contains(b.id)))

  def getAllLanguages(): Future[Seq[Language]] =
    ctx.run(schema)

}
