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
import biz.lobachev.annette.microservice_core.db.{CassandraQuillDao, CassandraTableBuilder}
import biz.lobachev.annette.service_catalog.api.service.{Service, ServiceId, ServiceLink}
import biz.lobachev.annette.service_catalog.impl.common.{CaptionEncoder, IconEncoder}
import biz.lobachev.annette.service_catalog.impl.service.ServiceEntity.{
  ServiceActivated,
  ServiceCreated,
  ServiceDeactivated,
  ServiceDeleted,
  ServiceUpdated
}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import io.scalaland.chimney.dsl._
import play.api.libs.json.Json

import java.util.Date
import scala.collection.immutable._
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

private[impl] class ServiceDbDao(override val session: CassandraSession)(implicit
  val ec: ExecutionContext,
  val materializer: Materializer
) extends CassandraQuillDao
    with CaptionEncoder
    with IconEncoder {

  import ctx._

  implicit val serviceLinkEncoder = genericJsonEncoder[ServiceLink]
  implicit val serviceLinkDecoder = genericJsonDecoder[ServiceLink]

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
               .column("label", Map(Text, Text))
               .column("label_description", Map(Text, Text))
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
      .withFieldComputed(_.updatedAt, _.createdAt)
      .withFieldComputed(_.updatedBy, _.createdBy)
      .transform
    for {
      _ <- ctx.run(serviceSchema.insert(lift(service)))
    } yield Done
  }

  def updateService(event: ServiceUpdated): Future[Done] = {
    val updates    = Seq(
      event.name.map(v => "name" -> v),
      event.description.map(v => "description" -> v),
      event.icon.map(v => "icon" -> Json.toJson(v).toString()),
      event.label.map(v => "label" -> v.asJava),
      event.labelDescription.map(v => "label_description" -> v.asJava),
      event.link.map(v => "link" -> Json.toJson(v).toString()),
      Some("updated_by" -> event.updatedBy.code),
      Some("updated_at" -> new Date(event.updatedAt.toInstant.toEpochMilli))
    ).flatten
    val updatesCql = updates.map { case f -> _ => s"$f = ?" }.mkString(", ")
    val update     = s"UPDATE services SET $updatesCql WHERE id = ?;"
    val params     = updates.map { case _ -> v => v } :+ event.id

    println()
    println()
    println(update)
    println()
    println()

    for {
      _ <- session.executeWrite(update, params: _*)
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
                 _.active    -> lift(false),
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
