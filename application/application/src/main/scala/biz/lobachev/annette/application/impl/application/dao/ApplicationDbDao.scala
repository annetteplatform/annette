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
import biz.lobachev.annette.microservice_core.db.{CassandraQuillDao, CassandraTableBuilder}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import io.scalaland.chimney.dsl._

import scala.concurrent.{ExecutionContext, Future}

class ApplicationDbDao(
  override val session: CassandraSession
)(implicit ec: ExecutionContext)
    extends CassandraQuillDao {

  import ctx._

  private val applicationSchema = quote(querySchema[Application]("applications"))

  private implicit val insertApplicationMeta = insertMeta[Application]()
  private implicit val updateApplicationMeta = updateMeta[Application](_.id)
  touch(insertApplicationMeta)
  touch(updateApplicationMeta)

  def createTables(): Future[Done] = {
    import CassandraTableBuilder.types._
    for {
      _ <- session.executeCreateTable(
             CassandraTableBuilder("applications")
               .column("id", Text, true)
               .column("name", Text)
               .column("icon", Text)
               .column("label", Map(Text, Text))
               .column("label_description", Map(Text, Text))
               .column("translations", Set(Text))
               .column("frontend_url", Text)
               .column("backend_url", Text)
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

  def updateApplicationIcon(event: ApplicationEntity.ApplicationIconUpdated): Future[Done] =
    ctx.run(
      applicationSchema
        .filter(_.id == lift(event.id))
        .update(
          _.icon      -> lift(event.icon),
          _.updatedAt -> lift(event.updatedAt),
          _.updatedBy -> lift(event.updatedBy)
        )
    )

  def updateApplicationLabel(event: ApplicationEntity.ApplicationLabelUpdated): Future[Done] =
    ctx.run(
      applicationSchema
        .filter(_.id == lift(event.id))
        .update(
          _.label     -> lift(event.label),
          _.updatedAt -> lift(event.updatedAt),
          _.updatedBy -> lift(event.updatedBy)
        )
    )

  def updateApplicationLabelDescription(event: ApplicationEntity.ApplicationLabelDescriptionUpdated): Future[Done] =
    ctx.run(
      applicationSchema
        .filter(_.id == lift(event.id))
        .update(
          _.labelDescription -> lift(event.labelDescription),
          _.updatedAt        -> lift(event.updatedAt),
          _.updatedBy        -> lift(event.updatedBy)
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

  def updateApplicationBackendUrl(event: ApplicationEntity.ApplicationBackendUrlUpdated): Future[Done] =
    ctx.run(
      applicationSchema
        .filter(_.id == lift(event.id))
        .update(
          _.backendUrl -> lift(event.backendUrl),
          _.updatedAt  -> lift(event.updatedAt),
          _.updatedBy  -> lift(event.updatedBy)
        )
    )

  def updateApplicationFrontendUrl(event: ApplicationEntity.ApplicationFrontendUrlUpdated): Future[Done] =
    ctx.run(
      applicationSchema
        .filter(_.id == lift(event.id))
        .update(
          _.frontendUrl -> lift(event.frontendUrl),
          _.updatedAt   -> lift(event.updatedAt),
          _.updatedBy   -> lift(event.updatedBy)
        )
    )

  def deleteApplication(event: ApplicationEntity.ApplicationDeleted): Future[Done] =
    ctx.run(applicationSchema.filter(_.id == lift(event.id)).delete)

  def getApplication(id: ApplicationId): Future[Option[Application]] =
    ctx
      .run(applicationSchema.filter(_.id == lift(id)))
      .map(_.headOption)

  def getApplications(ids: Set[ApplicationId]): Future[Seq[Application]] =
    ctx.run(applicationSchema.filter(b => liftQuery(ids).contains(b.id)))

  def getAllApplications(): Future[Seq[Application]] =
    ctx.run(applicationSchema)
}
