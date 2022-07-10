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
import biz.lobachev.annette.core.model.translation.{TextCaption, TranslationCaption}
import biz.lobachev.annette.microservice_core.db.{CassandraQuillDao, CassandraTableBuilder}
import biz.lobachev.annette.service_catalog.api.group.{Group, GroupId}
import biz.lobachev.annette.service_catalog.impl.group.GroupEntity.{
  GroupActivated,
  GroupCreated,
  GroupDeactivated,
  GroupDeleted,
  GroupUpdated
}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import io.scalaland.chimney.dsl._

import scala.collection.immutable._
import scala.concurrent.{ExecutionContext, Future}

private[impl] class GroupDbDao(override val session: CassandraSession)(implicit
  val ec: ExecutionContext,
  val materializer: Materializer
) extends CassandraQuillDao {

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
               .column("caption_text", Text)
               .column("caption_translation", Text)
               .column("caption_description_text", Text)
               .column("caption_description_translation", Text)
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
      _ <- ctx.run(groupSchema.insert(lift(group)))
    } yield Done
  }

  def updateGroup(event: GroupUpdated): Future[Done] = {
    // TODO:
    val updates: Seq[GroupRecord => (Any, Any)] = Seq(
      event.name.map(v => (r: GroupRecord) => r.name -> lift(v)),
      event.description.map(v => (r: GroupRecord) => r.description -> lift(v)),
      event.icon.map(v => (r: GroupRecord) => r.icon -> lift(v)),
      event.caption.map {
        case TextCaption(v) => (r: GroupRecord) => r.captionText -> lift(v)
        case _              => (r: GroupRecord) => r.captionText -> lift("")
      },
      event.caption.map {
        case TranslationCaption(v) => (r: GroupRecord) => r.captionTranslation -> lift(v)
        case _                     => (r: GroupRecord) => r.captionTranslation -> lift("")
      },
      event.captionDescription.map {
        case TextCaption(v) => (r: GroupRecord) => r.captionDescriptionText -> lift(v)
        case _              => (r: GroupRecord) => r.captionDescriptionText -> lift("")
      },
      event.captionDescription.map {
        case TranslationCaption(v) => (r: GroupRecord) => r.captionDescriptionTranslation -> lift(v)
        case _                     => (r: GroupRecord) => r.captionDescriptionTranslation -> lift("")
      },
      event.services.map(v => (r: GroupRecord) => r.services -> lift(v.toList)),
      Some((r: GroupRecord) => r.updatedBy -> lift(event.updatedBy)),
      Some((r: GroupRecord) => r.updatedAt -> lift(event.updatedAt))
    ).flatten
    for {
      _ <- ctx.run(groupSchema.filter(_.id == lift(event.id)).update(updates.head, updates.tail: _*))
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
                 _.active    -> lift(true),
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
