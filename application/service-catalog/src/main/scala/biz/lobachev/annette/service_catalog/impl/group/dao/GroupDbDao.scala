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

package biz.lobachev.annette.service_catalog.impl.group.dao

import akka.Done
import akka.stream.Materializer
import biz.lobachev.annette.microservice_core.db.{CassandraQuillDao, CassandraTableBuilder}
import biz.lobachev.annette.service_catalog.api.group.{Group, GroupId}
import biz.lobachev.annette.service_catalog.impl.common.IconEncoder
import biz.lobachev.annette.service_catalog.impl.group.GroupEntity.{
  GroupActivated,
  GroupCreated,
  GroupDeactivated,
  GroupDeleted,
  GroupUpdated
}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import io.scalaland.chimney.dsl._
import play.api.libs.json.Json

import java.util.Date
import scala.collection.immutable._
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

private[impl] class GroupDbDao(override val session: CassandraSession)(implicit
  val ec: ExecutionContext,
  val materializer: Materializer
) extends CassandraQuillDao
    with IconEncoder {

  import ctx._

  private val groupSchema = quote(querySchema[GroupRecord]("groups"))

  private implicit val insertGroupMeta = insertMeta[GroupRecord]()
  private implicit val updateGroupMeta = updateMeta[GroupRecord](_.id)
  println(insertGroupMeta.toString)
  println(updateGroupMeta.toString)

  def createTables(): Future[Done] = {
    import CassandraTableBuilder.types._
    for {

      _ <- session.executeCreateTable(
             CassandraTableBuilder("groups")
               .column("id", Text, true)
               .column("name", Text)
               .column("description", Text)
               .column("icon", Text)
               .column("label", Map(Text, Text))
               .column("label_description", Map(Text, Text))
               .column("services", List(Text))
               .column("active", Boolean)
               .column("updated_at", Timestamp)
               .column("updated_by", Text)
               .build
           )
    } yield Done
  }

  def createGroup(event: GroupCreated): Future[Done] = {
    val group = event
      .into[GroupRecord]
      .withFieldConst(_.active, true)
      .withFieldComputed(_.updatedAt, _.createdAt)
      .withFieldComputed(_.updatedBy, _.createdBy)
      .transform
    for {
      _ <- ctx.run(groupSchema.insert(lift(group)))
    } yield Done
  }

  def updateGroup(event: GroupUpdated): Future[Done] = {
    val updates    = Seq(
      event.name.map(v => "name" -> v),
      event.description.map(v => "description" -> v),
      event.icon.map(v => "icon" -> Json.toJson(v).toString()),
      event.label.map(v => "label" -> v.asJava),
      event.labelDescription.map(v => "label_description" -> v.asJava),
      event.services.map(v => "services" -> v.asJava),
      Some("updated_by" -> event.updatedBy.code),
      Some("updated_at" -> new Date(event.updatedAt.toInstant.toEpochMilli))
    ).flatten
    val updatesCql = updates.map { case f -> _ => s"$f = ?" }.mkString(", ")
    val update     = s"UPDATE groups SET $updatesCql WHERE id = ?;"
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

  def activateGroup(event: GroupActivated): Future[Done] =
    for {
      _ <- ctx.run(
             groupSchema
               .filter(_.id == lift(event.id))
               .update(
                 _.active    -> lift(true),
                 _.updatedBy -> lift(event.updatedBy),
                 _.updatedAt -> lift(event.updatedAt)
               )
           )
    } yield Done

  def deactivateGroup(event: GroupDeactivated): Future[Done] =
    for {
      _ <- ctx.run(
             groupSchema
               .filter(_.id == lift(event.id))
               .update(
                 _.active    -> lift(false),
                 _.updatedBy -> lift(event.updatedBy),
                 _.updatedAt -> lift(event.updatedAt)
               )
           )
    } yield Done

  def deleteGroup(event: GroupDeleted): Future[Done] =
    for {
      _ <- ctx.run(groupSchema.filter(_.id == lift(event.id)).delete)
    } yield Done

  def getGroupById(id: GroupId): Future[Option[Group]] =
    for {
      maybeGroup <- ctx
                      .run(groupSchema.filter(_.id == lift(id)))
                      .map(_.headOption.map(_.toGroup))

    } yield maybeGroup

  def getGroupsById(ids: Set[GroupId]): Future[Seq[Group]] =
    for {
      groups <- ctx.run(groupSchema.filter(b => liftQuery(ids).contains(b.id))).map(_.map(_.toGroup))
    } yield groups

}