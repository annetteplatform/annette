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

package biz.lobachev.annette.service_catalog.impl.item.dao

import akka.Done
import akka.stream.Materializer
import biz.lobachev.annette.microservice_core.db.{CassandraQuillDao, CassandraTableBuilder}
import biz.lobachev.annette.service_catalog.api.item.{ServiceItem, ServiceItemId, ServiceLink}
import biz.lobachev.annette.service_catalog.impl.item.ServiceItemEntity.{
  GroupCreated,
  GroupUpdated,
  ServiceCreated,
  ServiceItemActivated,
  ServiceItemDeactivated,
  ServiceItemDeleted,
  ServiceUpdated
}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import io.scalaland.chimney.dsl._
import play.api.libs.json.Json

import java.util.Date
import scala.collection.immutable._
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

private[service_catalog] class ServiceItemDbDao(override val session: CassandraSession)(implicit
  val ec: ExecutionContext,
  val materializer: Materializer
) extends CassandraQuillDao {

  import ctx._

  implicit val serviceLinkEncoder = genericJsonEncoder[ServiceLink]
  implicit val serviceLinkDecoder = genericJsonDecoder[ServiceLink]

  private val serviceSchema = quote(querySchema[ServiceItemRecord]("service_items"))

  private implicit val insertServiceMeta = insertMeta[ServiceItemRecord]()
  private implicit val updateServiceMeta = updateMeta[ServiceItemRecord](_.id)
  touch(insertServiceMeta)
  touch(updateServiceMeta)

  def createTables(): Future[Done] = {
    import CassandraTableBuilder.types._
    for {

      _ <- session.executeCreateTable(
             CassandraTableBuilder("service_items")
               .column("id", Text, true)
               .column("name", Text)
               .column("description", Text)
               .column("icon", Text)
               .column("label", Map(Text, Text))
               .column("label_description", Map(Text, Text))
               .column("item_type", Text)
               .column("link", Text)
               .column("children", List(Text))
               .column("active", Boolean)
               .column("updated_at", Timestamp)
               .column("updated_by", Text)
               .build
           )
    } yield Done
  }

  def createGroup(event: GroupCreated): Future[Done] = {
    val group = event
      .into[ServiceItemRecord]
      .withFieldConst(_.itemType, "group")
      .withFieldConst(_.active, true)
      .withFieldConst(_.link, None)
      .withFieldComputed(_.updatedAt, _.createdAt)
      .withFieldComputed(_.updatedBy, _.createdBy)
      .transform
    for {
      _ <- ctx.run(serviceSchema.insert(lift(group)))
    } yield Done
  }

  def updateGroup(event: GroupUpdated): Future[Done] = {
    val updates    = Seq(
      event.name.map(v => "name" -> v),
      event.description.map(v => "description" -> v),
      event.icon.map(v => "icon" -> Json.toJson(v).toString()),
      event.label.map(v => "label" -> v.asJava),
      event.labelDescription.map(v => "label_description" -> v.asJava),
      event.children.map(v => "children" -> v.asJava),
      Some("updated_by" -> event.updatedBy.code),
      Some("updated_at" -> new Date(event.updatedAt.toInstant.toEpochMilli))
    ).flatten
    val updatesCql = updates.map { case f -> _ => s"$f = ?" }.mkString(", ")
    val update     = s"UPDATE service_items SET $updatesCql WHERE id = ?;"
    val params     = updates.map { case _ -> v => v } :+ event.id
    for {
      _ <- session.executeWrite(update, params: _*)
    } yield Done
  }

  def createService(event: ServiceCreated): Future[Done] = {
    val service = event
      .into[ServiceItemRecord]
      .withFieldConst(_.itemType, "service")
      .withFieldConst(_.active, true)
      .withFieldConst(_.children, None)
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
    val update     = s"UPDATE service_items SET $updatesCql WHERE id = ?;"
    val params     = updates.map { case _ -> v => v } :+ event.id
    for {
      _ <- session.executeWrite(update, params: _*)
    } yield Done
  }

  def activateService(event: ServiceItemActivated): Future[Done] =
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

  def deactivateService(event: ServiceItemDeactivated): Future[Done] =
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

  def deleteService(event: ServiceItemDeleted): Future[Done] =
    for {
      _ <- ctx.run(serviceSchema.filter(_.id == lift(event.id)).delete)
    } yield Done

  def getServiceItem(id: ServiceItemId): Future[Option[ServiceItem]] =
    for {
      maybeService <- ctx
                        .run(serviceSchema.filter(_.id == lift(id)))
                        .map(_.headOption.map(_.toServiceItem))

    } yield maybeService

  def getServiceItems(ids: Set[ServiceItemId]): Future[Seq[ServiceItem]] =
    for {
      services <- ctx.run(serviceSchema.filter(b => liftQuery(ids).contains(b.id))).map(_.map(_.toServiceItem))
    } yield services

}
