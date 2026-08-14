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

import org.apache.pekko.Done
import biz.lobachev.annette.application.api.application._
import biz.lobachev.annette.application.impl.application.ApplicationEntity
import biz.lobachev.annette.microservice_core.pekko.db.{CassandraQuillDao, CassandraTableBuilder}
import com.typesafe.config.Config
import io.getquill.CassandraContextConfig
import biz.lobachev.annette.core.utils.ChimneyCommons._
import io.scalaland.chimney.dsl._

import scala.concurrent.{ExecutionContext, Future}

private[impl] class ApplicationDbDao(
  config: Config
)(implicit ec: ExecutionContext)
    extends CassandraQuillDao {

  override protected def cassandraContextConfig: CassandraContextConfig =
    if (config.hasPath("cassandra-quill"))
      CassandraContextConfig(config.getConfig("cassandra-quill"))
    else
      CassandraContextConfig(config.getConfig("cassandra.default"))

  import ctx._

  private val applicationSchema = quote(querySchema[Application]("applications"))

  private implicit val insertApplicationMeta = insertMeta[Application]()
  private implicit val updateApplicationMeta = updateMeta[Application](_.id)
  touch(insertApplicationMeta)
  touch(updateApplicationMeta)

  def createTables(): Future[Done] = {
    import CassandraTableBuilder.types._
    Future {
      ctx.session.execute(
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
      Done
    }
  }

  def createApplication(event: ApplicationEntity.ApplicationCreated): Future[Done] = {
    val application = event
      .into[Application]
      .withFieldComputed(_.updatedAt, _.createdAt)
      .withFieldComputed(_.updatedBy, _.createdBy)
      .transform
    for {
      _ <- ctx.run(applicationSchema.insert(lift(application)))
    } yield Done
  }

  def updateApplicationName(event: ApplicationEntity.ApplicationNameUpdated): Future[Done] =
    for {
      _ <- ctx.run(
             applicationSchema
               .filter(_.id == lift(event.id))
               .update(
                 _.name      -> lift(event.name),
                 _.updatedAt -> lift(event.updatedAt),
                 _.updatedBy -> lift(event.updatedBy)
               )
           )
    } yield Done

  def updateApplicationIcon(event: ApplicationEntity.ApplicationIconUpdated): Future[Done] =
    for {
      _ <- ctx.run(
             applicationSchema
               .filter(_.id == lift(event.id))
               .update(
                 _.icon      -> lift(event.icon),
                 _.updatedAt -> lift(event.updatedAt),
                 _.updatedBy -> lift(event.updatedBy)
               )
           )
    } yield Done

  def updateApplicationLabel(event: ApplicationEntity.ApplicationLabelUpdated): Future[Done] =
    for {
      _ <- ctx.run(
             applicationSchema
               .filter(_.id == lift(event.id))
               .update(
                 _.label     -> lift(event.label),
                 _.updatedAt -> lift(event.updatedAt),
                 _.updatedBy -> lift(event.updatedBy)
               )
           )
    } yield Done

  def updateApplicationLabelDescription(event: ApplicationEntity.ApplicationLabelDescriptionUpdated): Future[Done] =
    for {
      _ <- ctx.run(
             applicationSchema
               .filter(_.id == lift(event.id))
               .update(
                 _.labelDescription -> lift(event.labelDescription),
                 _.updatedAt        -> lift(event.updatedAt),
                 _.updatedBy        -> lift(event.updatedBy)
               )
           )
    } yield Done

  def updateApplicationTranslations(
    event: ApplicationEntity.ApplicationTranslationsUpdated
  ): Future[Done] =
    for {
      _ <- ctx.run(
             applicationSchema
               .filter(_.id == lift(event.id))
               .update(
                 _.translations -> lift(event.translations),
                 _.updatedAt    -> lift(event.updatedAt),
                 _.updatedBy    -> lift(event.updatedBy)
               )
           )
    } yield Done

  def updateApplicationBackendUrl(event: ApplicationEntity.ApplicationBackendUrlUpdated): Future[Done] =
    for {
      _ <- ctx.run(
             applicationSchema
               .filter(_.id == lift(event.id))
               .update(
                 _.backendUrl -> lift(event.backendUrl),
                 _.updatedAt  -> lift(event.updatedAt),
                 _.updatedBy  -> lift(event.updatedBy)
               )
           )
    } yield Done

  def updateApplicationFrontendUrl(event: ApplicationEntity.ApplicationFrontendUrlUpdated): Future[Done] =
    for {
      _ <- ctx.run(
             applicationSchema
               .filter(_.id == lift(event.id))
               .update(
                 _.frontendUrl -> lift(event.frontendUrl),
                 _.updatedAt   -> lift(event.updatedAt),
                 _.updatedBy   -> lift(event.updatedBy)
               )
           )
    } yield Done

  def deleteApplication(event: ApplicationEntity.ApplicationDeleted): Future[Done] =
    for {
      _ <- ctx.run(applicationSchema.filter(_.id == lift(event.id)).delete)
    } yield Done

  def getApplication(id: ApplicationId): Future[Option[Application]] =
    ctx
      .run(applicationSchema.filter(_.id == lift(id)))
      .map(_.headOption)

  def getApplications(ids: Set[ApplicationId]): Future[Seq[Application]] =
    ctx.run(applicationSchema.filter(b => liftQuery(ids).contains(b.id)))

  def getAllApplications(): Future[Seq[Application]] =
    ctx.run(applicationSchema)
}
