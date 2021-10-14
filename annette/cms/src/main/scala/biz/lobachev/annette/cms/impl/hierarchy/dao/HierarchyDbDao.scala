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

package biz.lobachev.annette.cms.impl.hierarchy.dao

import akka.Done
import biz.lobachev.annette.cms.api.space.{SpaceId, WikiHierarchy}
import biz.lobachev.annette.cms.impl.hierarchy
import biz.lobachev.annette.cms.impl.hierarchy.HierarchyEntity
import biz.lobachev.annette.microservice_core.db.{CassandraQuillDao, CassandraTableBuilder}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import io.scalaland.chimney.dsl._

import scala.concurrent.{ExecutionContext, Future}

private[impl] class HierarchyDbDao(
  override val session: CassandraSession
)(implicit
  ec: ExecutionContext
) extends CassandraQuillDao {

  import ctx._

  private val hierarchySchema         = quote(querySchema[HierarchyRecord]("hierarchies"))
  private val hierarchyChildrenSchema = quote(querySchema[HierarchyChildrenRecord]("hierarchy_children"))

  private implicit val insertHierarchyMeta         = insertMeta[HierarchyRecord]()
  private implicit val updateHierarchyMeta         = updateMeta[HierarchyRecord](_.spaceId)
  private implicit val insertHierarchyChildrenMeta = insertMeta[HierarchyChildrenRecord]()
  private implicit val updateHierarchyChildreMeta  = updateMeta[HierarchyChildrenRecord](_.spaceId, _.postId)
  println(insertHierarchyMeta.toString)
  println(updateHierarchyMeta.toString)
  println(insertHierarchyChildrenMeta.toString)
  println(updateHierarchyChildreMeta.toString)

  def createTables(): Future[Done] = {
    import CassandraTableBuilder.types._
    for {
      _ <- session.executeCreateTable(
             CassandraTableBuilder("hierarchies")
               .column("space_id", Text, true)
               .column("root_post_id", Text)
               .column("updated_at", Timestamp)
               .column("updated_by", Text)
               .build
           )
      _ <- session.executeCreateTable(
             CassandraTableBuilder("hierarchy_children")
               .column("space_id", Text)
               .column("post_id", Text)
               .column("child_posts", List(Text))
               .withPrimaryKey("space_id", "post_id")
               .build
           )
    } yield Done
  }

  def createSpace(event: HierarchyEntity.SpaceCreated) = {
    val hierarchyRecord = event.transformInto[HierarchyRecord]
    ctx.run(hierarchySchema.insert(lift(hierarchyRecord)))
  }

  def addRootPost(event: HierarchyEntity.RootPostAdded) =
    ctx.run(
      hierarchySchema
        .filter(_.spaceId == lift(event.spaceId))
        .update(
          _.rootPostId -> lift(Option(event.postId)),
          _.updatedAt  -> lift(event.updatedAt),
          _.updatedBy  -> lift(event.updatedBy)
        )
    )

  def addPost(event: HierarchyEntity.PostAdded) = {
    val hierarchyChildrenRecord = HierarchyChildrenRecord(event.spaceId, event.parent, event.children.toList)
    for {
      _ <- ctx.run(hierarchyChildrenSchema.insert(lift(hierarchyChildrenRecord)))
      _ <- ctx.run(
             hierarchySchema
               .filter(_.spaceId == lift(event.spaceId))
               .update(
                 _.updatedAt -> lift(event.updatedAt),
                 _.updatedBy -> lift(event.updatedBy)
               )
           )
    } yield Done
  }

  def movePost(event: HierarchyEntity.PostMoved) =
    for {
      _ <- if (event.oldParent != event.newParent)
             if (event.oldChildren.isEmpty)
               ctx.run(
                 hierarchyChildrenSchema
                   .filter(r =>
                     r.spaceId == lift(event.spaceId) &&
                       r.postId == lift(event.postId)
                   )
                   .delete
               )
             else
               ctx.run(
                 hierarchyChildrenSchema.insert(
                   lift(HierarchyChildrenRecord(event.spaceId, event.oldParent, event.oldChildren.toList))
                 )
               )
           else Future.successful(Done)
      _ <- ctx.run(
             hierarchyChildrenSchema.insert(
               lift(HierarchyChildrenRecord(event.spaceId, event.newParent, event.newChildren.toList))
             )
           )

      _ <- ctx.run(
             hierarchySchema
               .filter(_.spaceId == lift(event.spaceId))
               .update(
                 _.updatedAt -> lift(event.updatedAt),
                 _.updatedBy -> lift(event.updatedBy)
               )
           )
    } yield Done

  def removeRootPost(event: HierarchyEntity.RootPostRemoved) = {
    val none: Option[String] = None
    ctx.run(
      hierarchySchema
        .filter(_.spaceId == lift(event.spaceId))
        .update(
          _.rootPostId -> lift(none),
          _.updatedAt  -> lift(event.updatedAt),
          _.updatedBy  -> lift(event.updatedBy)
        )
    )
  }

  def removePost(event: HierarchyEntity.PostRemoved) =
    for {
      _ <- if (event.children.isEmpty)
             ctx.run(
               hierarchyChildrenSchema
                 .filter(r =>
                   r.spaceId == lift(event.spaceId) &&
                     r.postId == lift(event.postId)
                 )
                 .delete
             )
           else
             ctx.run(
               hierarchyChildrenSchema.insert(
                 lift(HierarchyChildrenRecord(event.spaceId, event.postId, event.children.toList))
               )
             )
      _ <- ctx.run(
             hierarchySchema
               .filter(_.spaceId == lift(event.spaceId))
               .update(
                 _.updatedAt -> lift(event.updatedAt),
                 _.updatedBy -> lift(event.updatedBy)
               )
           )
    } yield Done

  def deleteSpace(event: hierarchy.HierarchyEntity.SpaceDeleted) =
    for {
      _ <- ctx.run(hierarchyChildrenSchema.filter(_.spaceId == lift(event.spaceId)).delete)
      _ <- ctx.run(hierarchySchema.filter(_.spaceId == lift(event.spaceId)).delete)
    } yield Done

  def getHierarchyById(id: SpaceId): Future[Option[WikiHierarchy]] =
    for {
      maybeEntity <- ctx
                       .run(hierarchySchema.filter(_.spaceId == lift(id)))
                       .map(_.headOption.map(_.toWikiHierarchy))
      children    <- ctx
                       .run(hierarchyChildrenSchema.filter(_.spaceId == lift(id)))
                       .map(_.map(r => r.postId -> r.childPosts).toMap)
    } yield maybeEntity.map(_.copy(childPosts = children))

}
