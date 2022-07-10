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

package biz.lobachev.annette.service_catalog.impl.service.dao

import akka.Done
import akka.stream.Materializer
import biz.lobachev.annette.core.model.translation.{TextCaption, TranslationCaption}
import biz.lobachev.annette.microservice_core.db.{CassandraQuillDao, CassandraTableBuilder}
import biz.lobachev.annette.service_catalog.api.service.{Service, ServiceId}
import biz.lobachev.annette.service_catalog.impl.service.ServiceEntity.{
  ServiceActivated,
  ServiceCreated,
  ServiceDeactivated,
  ServiceDeleted,
  ServiceUpdated
}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import io.scalaland.chimney.dsl._

import scala.collection.immutable._
import scala.concurrent.{ExecutionContext, Future}

private[impl] class ServiceDbDao(override val session: CassandraSession)(implicit
  val ec: ExecutionContext,
  val materializer: Materializer
) extends CassandraQuillDao {

  import ctx._

  private val serviceSchema = quote(querySchema[ServiceRecord]("services"))

  private implicit val insertServiceMeta = insertMeta[ServiceRecord]()
  private implicit val updateServiceMeta = updateMeta[ServiceRecord](_.id)
  println(insertServiceMeta.toString)
  println(updateServiceMeta.toString)

  def createTables(): Future[Done] = {
    import CassandraTableBuilder.types._
    for {

      _ <- session.executeCreateTable(
             CassandraTableBuilder("services")
               .column("id", Text, true)
               .column("name", Text)
               .column("description", Text)
               .column("icon", Text)
               .column("caption_text", Text)
               .column("caption_translation", Text)
               .column("caption_description_text", Text)
               .column("caption_description_translation", Text)
               .column("link", Text)
               .column("active", Boolean)
               .column("updated_at", Timestamp)
               .column("updated_by", Text)
               .build
           )
    } yield Done
  }

  def createService(event: ServiceCreated): Future[Done] = {
    val service = event
      .into[ServiceRecord]
      .withFieldConst(_.active, true)
      .withFieldComputed(
        _.captionText,
        _.caption match {
          case TextCaption(v) => v
          case _              => ""
        }
      )
      .withFieldComputed(
        _.captionTranslation,
        _.caption match {
          case TranslationCaption(v) => v
          case _                     => ""
        }
      )
      .withFieldComputed(
        _.captionDescriptionText,
        _.captionDescription match {
          case TextCaption(v) => v
          case _              => ""
        }
      )
      .withFieldComputed(
        _.captionDescriptionTranslation,
        _.captionDescription match {
          case TranslationCaption(v) => v
          case _                     => ""
        }
      )
      .withFieldComputed(_.updatedAt, _.createdAt)
      .withFieldComputed(_.updatedBy, _.createdBy)
      .transform
    for {
      _ <- ctx.run(serviceSchema.insert(lift(service)))
    } yield Done
  }

  def updateService(event: ServiceUpdated): Future[Done] = {
    // TODO:
    val updates: Seq[ServiceRecord => (Any, Any)] = Seq(
      event.name.map(v => (r: ServiceRecord) => r.name -> lift(v)),
      event.description.map(v => (r: ServiceRecord) => r.description -> lift(v)),
      event.icon.map(v => (r: ServiceRecord) => r.icon -> lift(v)),
      event.caption.map {
        case TextCaption(v) => (r: ServiceRecord) => r.captionText -> lift(v)
        case _              => (r: ServiceRecord) => r.captionText -> lift("")
      },
      event.caption.map {
        case TranslationCaption(v) => (r: ServiceRecord) => r.captionTranslation -> lift(v)
        case _                     => (r: ServiceRecord) => r.captionTranslation -> lift("")
      },
      event.captionDescription.map {
        case TextCaption(v) => (r: ServiceRecord) => r.captionDescriptionText -> lift(v)
        case _              => (r: ServiceRecord) => r.captionDescriptionText -> lift("")
      },
      event.captionDescription.map {
        case TranslationCaption(v) => (r: ServiceRecord) => r.captionDescriptionTranslation -> lift(v)
        case _                     => (r: ServiceRecord) => r.captionDescriptionTranslation -> lift("")
      },
      event.link.map(v => (r: ServiceRecord) => r.link -> lift(v)),
      Some((r: ServiceRecord) => r.updatedBy -> lift(event.updatedBy)),
      Some((r: ServiceRecord) => r.updatedAt -> lift(event.updatedAt))
    ).flatten
    for {
      _ <- ctx.run(serviceSchema.filter(_.id == lift(event.id)).update(updates.head, updates.tail: _*))
    } yield Done
  }

  def activateService(event: ServiceActivated): Future[Done] =
    for {
      _ <- ctx.run(
             serviceSchema
               .filter(_.id == lift(event.id))
               .update(
                 _.active    -> lift(true),
                 _.updatedBy -> lift(event.updatedBy),
                 _.updatedAt -> lift(event.updatedAt)
               )
           )
    } yield Done

  def deactivateService(event: ServiceDeactivated): Future[Done] =
    for {
      _ <- ctx.run(
             serviceSchema
               .filter(_.id == lift(event.id))
               .update(
                 _.active    -> lift(true),
                 _.updatedBy -> lift(event.updatedBy),
                 _.updatedAt -> lift(event.updatedAt)
               )
           )
    } yield Done

  def deleteService(event: ServiceDeleted): Future[Done] =
    for {
      _ <- ctx.run(serviceSchema.filter(_.id == lift(event.id)).delete)
    } yield Done

  def getServiceById(id: ServiceId): Future[Option[Service]] =
    for {
      maybeService <- ctx
                        .run(serviceSchema.filter(_.id == lift(id)))
                        .map(_.headOption.map(_.toService))

    } yield maybeService

  def getServicesById(ids: Set[ServiceId]): Future[Seq[Service]] =
    for {
      services <- ctx.run(serviceSchema.filter(b => liftQuery(ids).contains(b.id))).map(_.map(_.toService))
    } yield services

}
