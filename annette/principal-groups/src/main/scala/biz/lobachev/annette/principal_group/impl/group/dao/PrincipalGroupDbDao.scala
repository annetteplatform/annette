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

package biz.lobachev.annette.principal_group.impl.group.dao

import akka.Done
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.microservice_core.db.{CassandraQuillDao, CassandraTableBuilder}
import biz.lobachev.annette.principal_group.api.group.{PrincipalGroup, PrincipalGroupId}
import biz.lobachev.annette.principal_group.impl.group.PrincipalGroupEntity.{
  PrincipalAssigned,
  PrincipalGroupCategoryUpdated,
  PrincipalGroupCreated,
  PrincipalGroupDeleted,
  PrincipalGroupDescriptionUpdated,
  PrincipalGroupNameUpdated,
  PrincipalUnassigned
}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import io.scalaland.chimney.dsl._

import scala.collection.immutable._
import scala.concurrent.{ExecutionContext, Future}

private[impl] class PrincipalGroupDbDao(
  override val session: CassandraSession
)(implicit
  ec: ExecutionContext
) extends CassandraQuillDao {

  import ctx._

  private val groupSchema      = quote(querySchema[PrincipalGroup]("groups"))
  private val assignmentSchema = quote(querySchema[AssignmentRecord]("assignments"))

  private implicit val insertGroupMeta = insertMeta[PrincipalGroup]()
  private implicit val updateGroupMeta = updateMeta[PrincipalGroup](_.id)
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
               .column("category_id", Text)
               .column("updated_at", Timestamp)
               .column("updated_by", Text)
               .build
           )
      _ <- session.executeCreateTable(
             CassandraTableBuilder("assignments")
               .column("group_id", Text)
               .column("principal", Text)
               .withPrimaryKey("group_id", "principal")
               .build
           )

    } yield Done
  }

  def createPrincipalGroup(event: PrincipalGroupCreated) = {
    val entity = event
      .into[PrincipalGroup]
      .withFieldComputed(_.updatedAt, _.createdAt)
      .withFieldComputed(_.updatedBy, _.createdBy)
      .transform
    ctx.run(groupSchema.insert(lift(entity)))
  }

  def updatePrincipalGroupName(event: PrincipalGroupNameUpdated) =
    ctx.run(
      groupSchema
        .filter(_.id == lift(event.id))
        .update(
          _.name      -> lift(event.name),
          _.updatedAt -> lift(event.updatedAt),
          _.updatedBy -> lift(event.updatedBy)
        )
    )

  def updatePrincipalGroupDescription(event: PrincipalGroupDescriptionUpdated) =
    ctx.run(
      groupSchema
        .filter(_.id == lift(event.id))
        .update(
          _.description -> lift(event.description),
          _.updatedAt   -> lift(event.updatedAt),
          _.updatedBy   -> lift(event.updatedBy)
        )
    )

  def updatePrincipalGroupCategory(event: PrincipalGroupCategoryUpdated) =
    ctx.run(
      groupSchema
        .filter(_.id == lift(event.id))
        .update(
          _.categoryId -> lift(event.categoryId),
          _.updatedAt  -> lift(event.updatedAt),
          _.updatedBy  -> lift(event.updatedBy)
        )
    )

  def deletePrincipalGroup(event: PrincipalGroupDeleted) =
    for {
      _ <- ctx.run(groupSchema.filter(_.id == lift(event.id)).delete)
      _ <- ctx.run(assignmentSchema.filter(_.groupId == lift(event.id)).delete)
    } yield Done

  def assignPrincipal(event: PrincipalAssigned) =
    for {
      _ <- ctx.run(assignmentSchema.insert(lift(AssignmentRecord(event.id, event.principal))))
      _ <- ctx.run(
             groupSchema
               .filter(_.id == lift(event.id))
               .update(
                 _.updatedAt -> lift(event.updatedAt),
                 _.updatedBy -> lift(event.updatedBy)
               )
           )
    } yield Done

  def unassignPrincipal(event: PrincipalUnassigned) =
    for {
      _ <- ctx.run(
             assignmentSchema
               .filter(r =>
                 r.groupId == lift(event.id) &&
                   r.principal == lift(event.principal)
               )
               .delete
           )
      _ <- ctx.run(
             groupSchema
               .filter(_.id == lift(event.id))
               .update(
                 _.updatedAt -> lift(event.updatedAt),
                 _.updatedBy -> lift(event.updatedBy)
               )
           )
    } yield Done

  def getPrincipalGroupById(id: PrincipalGroupId): Future[Option[PrincipalGroup]] =
    ctx
      .run(groupSchema.filter(_.id == lift(id)))
      .map(_.headOption)

  def getPrincipalGroupsById(ids: Set[PrincipalGroupId]): Future[Seq[PrincipalGroup]] =
    ctx.run(groupSchema.filter(b => liftQuery(ids).contains(b.id)))

  def getAssignments(id: PrincipalGroupId): Future[Set[AnnettePrincipal]] =
    ctx
      .run(assignmentSchema.filter(_.groupId == lift(id)).map(_.principal))
      .map(_.toSet)
}
