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

import akka.Done
import biz.lobachev.annette.application.api.application._
import biz.lobachev.annette.application.impl.application.ApplicationEntity
import biz.lobachev.annette.core.model.translation.Caption
import biz.lobachev.annette.microservice_core.db.{CassandraQuillDao, CassandraTableBuilder}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import io.scalaland.chimney.dsl._

import scala.concurrent.{ExecutionContext, Future}

private[impl] class ApplicationDbDao(
  override val session: CassandraSession
)(implicit ec: ExecutionContext)
    extends CassandraQuillDao {

  import ctx._

  private val applicationSchema = quote(querySchema[Application]("applications"))

  private implicit val captionEncoder        = genericJsonEncoder[Caption]
  private implicit val captionDecoder        = genericJsonDecoder[Caption]
  private implicit val insertApplicationMeta = insertMeta[Application]()
  private implicit val updateApplicationMeta = updateMeta[Application](_.id)
  println(captionEncoder.toString)
  println(captionDecoder.toString)
  println(insertApplicationMeta.toString)
  println(updateApplicationMeta.toString)

  def createTables(): Future[Done] = {
    import CassandraTableBuilder.types._
    for {
      _ <- session.executeCreateTable(
             CassandraTableBuilder("applications")
               .column("id", Text, true)
               .column("name", Text)
               .column("caption", Text)
               .column("translations", Set(Text))
               .column("server_url", Text)
               .column("updated_at", Timestamp)
               .column("updated_by", Text)
               .build
           )
    } yield Done
  }

  def createApplication(event: ApplicationEntity.ApplicationCreated): Future[Done] = {
    val application = event
      .into[Application]
      .withFieldComputed(_.updatedAt, _.createdAt)
      .withFieldComputed(_.updatedBy, _.createdBy)
      .transform
    ctx.run(applicationSchema.insert(lift(application)))

  }

  def updateApplicationName(event: ApplicationEntity.ApplicationNameUpdated): Future[Done] =
    ctx.run(
      applicationSchema
        .filter(_.id == lift(event.id))
        .update(
          _.name      -> lift(event.name),
          _.updatedAt -> lift(event.updatedAt),
          _.updatedBy -> lift(event.updatedBy)
        )
    )

  def updateApplicationCaption(event: ApplicationEntity.ApplicationCaptionUpdated): Future[Done] =
    ctx.run(
      applicationSchema
        .filter(_.id == lift(event.id))
        .update(
          _.caption   -> lift(event.caption),
          _.updatedAt -> lift(event.updatedAt),
          _.updatedBy -> lift(event.updatedBy)
        )
    )

  def updateApplicationTranslations(
    event: ApplicationEntity.ApplicationTranslationsUpdated
  ): Future[Done] =
    ctx.run(
      applicationSchema
        .filter(_.id == lift(event.id))
        .update(
          _.translations -> lift(event.translations),
          _.updatedAt    -> lift(event.updatedAt),
          _.updatedBy    -> lift(event.updatedBy)
        )
    )

  def updateApplicationServerUrl(event: ApplicationEntity.ApplicationServerUrlUpdated): Future[Done] =
    ctx.run(
      applicationSchema
        .filter(_.id == lift(event.id))
        .update(
          _.serverUrl -> lift(event.serverUrl),
          _.updatedAt -> lift(event.updatedAt),
          _.updatedBy -> lift(event.updatedBy)
        )
    )

  def deleteApplication(event: ApplicationEntity.ApplicationDeleted): Future[Done] =
    ctx.run(applicationSchema.filter(_.id == lift(event.id)).delete)

  def getApplicationById(id: ApplicationId): Future[Option[Application]] =
    ctx
      .run(applicationSchema.filter(_.id == lift(id)))
      .map(_.headOption)

  def getApplicationsById(ids: Set[ApplicationId]): Future[Seq[Application]] =
    ctx.run(applicationSchema.filter(b => liftQuery(ids).contains(b.id)))

}
