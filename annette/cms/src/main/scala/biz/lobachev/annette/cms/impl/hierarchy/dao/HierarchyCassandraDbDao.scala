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
import biz.lobachev.annette.cms.api.post.PostId
import biz.lobachev.annette.cms.api.space.{SpaceId, WikiHierarchy}
import biz.lobachev.annette.cms.impl.hierarchy
import biz.lobachev.annette.cms.impl.hierarchy.HierarchyEntity
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import com.datastax.driver.core.{BoundStatement, PreparedStatement, Row}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import org.slf4j.LoggerFactory

import java.time.OffsetDateTime
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

private[impl] class HierarchyCassandraDbDao(
  session: CassandraSession
)(implicit
  ec: ExecutionContext
) {

  val log = LoggerFactory.getLogger(this.getClass)

  private var createSpaceStatement: PreparedStatement          = _
  private var updateRootPostStatement: PreparedStatement       = _
  private var updateSpaceTimestampStatement: PreparedStatement = _
  private var updateChildrenStatement: PreparedStatement       = _
  private var removeChildrenStatement: PreparedStatement       = _
  private var deleteSpaceStatement: PreparedStatement          = _
  private var deleteChildrenStatement: PreparedStatement       = _

  def createTables(): Future[Done] =
    for {
      _ <- session.executeCreateTable("""
                                        |CREATE TABLE IF NOT EXISTS hierarchy (
                                        |          space_id text PRIMARY KEY,
                                        |          root_post_id text,
                                        |          updated_at text,
                                        |          updated_by_type text,
                                        |          updated_by_id text,
                                        |)
                                        |""".stripMargin)
      _ <- session.executeCreateTable("""
                                        |CREATE TABLE IF NOT EXISTS hierarchy_children (
                                        |          space_id text,
                                        |          post_id text,
                                        |          child_posts list<text>,
                                        |          PRIMARY KEY (space_id, post_id)
                                        |)
                                        |""".stripMargin)
    } yield Done

  def prepareStatements(): Future[Done] =
    for {
      createSpaceStmt          <- session.prepare(
                                    """
                             | INSERT INTO hierarchy (space_id, root_post_id,
                             |     updated_at, updated_by_type, updated_by_id
                             |     )
                             |   VALUES (:space_id, :root_post_id,
                             |     :updated_at, :updated_by_type, :updated_by_id
                             |     )
                             |""".stripMargin
                                  )
      updateRootPostStmt       <- session.prepare(
                                    """
                                | UPDATE hierarchy SET
                                |   root_post_id = :root_post_id,
                                |   updated_at = :updated_at,
                                |   updated_by_type = :updated_by_type,
                                |   updated_by_id = :updated_by_id
                                |  WHERE space_id = :space_id
                                |""".stripMargin
                                  )
      updateSpaceTimestampStmt <- session.prepare(
                                    """
                                      | UPDATE hierarchy SET
                                      |   updated_at = :updated_at,
                                      |   updated_by_type = :updated_by_type,
                                      |   updated_by_id = :updated_by_id
                                      |  WHERE space_id = :space_id
                                      |""".stripMargin
                                  )
      updateChildrenStmt       <- session.prepare(
                                    """
                                | INSERT INTO hierarchy_children (space_id, post_id,  child_posts)
                                |   VALUES (:space_id, :post_id,  :child_posts)
                                |""".stripMargin
                                  )
      removeChildrenStmt       <- session.prepare(
                                    """
                                | DELETE FROM hierarchy_children
                                |   WHERE
                                |     space_id = :space_id AND
                                |     post_id = :post_id
                                |""".stripMargin
                                  )

      deleteSpaceStmt          <- session.prepare(
                                    """
                             | DELETE FROM hierarchy
                             |  WHERE space_id = :space_id
                             |""".stripMargin
                                  )
      deleteChildrenStmt       <- session.prepare(
                                    """
                                | DELETE FROM hierarchy_children
                                |  WHERE space_id = :space_id
                                |""".stripMargin
                                  )
    } yield {
      createSpaceStatement = createSpaceStmt
      updateRootPostStatement = updateRootPostStmt
      updateSpaceTimestampStatement = updateSpaceTimestampStmt
      updateChildrenStatement = updateChildrenStmt
      removeChildrenStatement = removeChildrenStmt
      deleteSpaceStatement = deleteSpaceStmt
      deleteChildrenStatement = deleteChildrenStmt
      Done
    }

  def createSpace(event: HierarchyEntity.SpaceCreated): Future[Seq[BoundStatement]]  =
    build(
      createSpaceStatement
        .bind()
        .setString("space_id", event.spaceId)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )
  def addRootPost(event: HierarchyEntity.RootPostAdded): Future[Seq[BoundStatement]] =
    build(
      updateRootPostStatement
        .bind()
        .setString("space_id", event.spaceId)
        .setString("root_post_id", event.postId)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def addPost(event: HierarchyEntity.PostAdded): Future[Seq[BoundStatement]] =
    build(
      updateChildrenStatement
        .bind()
        .setString("space_id", event.spaceId)
        .setString("post_id", event.parent)
        .setList[String]("child_posts", event.children.toList.asJava),
      updateSpaceTimestampStatement
        .bind()
        .setString("space_id", event.spaceId)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def movePost(event: HierarchyEntity.PostMoved): Future[Seq[BoundStatement]] = {
    val timestampSeq = Seq(
      updateSpaceTimestampStatement
        .bind()
        .setString("space_id", event.spaceId)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )
    val seq          =
      if (event.oldParent == event.newParent)
        Seq(
          updateChildrenStatement
            .bind()
            .setString("space_id", event.spaceId)
            .setString("post_id", event.newParent)
            .setList[String]("child_posts", event.newChildren.toList.asJava)
        )
      else if (event.oldChildren.isEmpty)
        Seq(
          removeChildrenStatement
            .bind()
            .setString("space_id", event.spaceId)
            .setString("post_id", event.oldParent),
          updateChildrenStatement
            .bind()
            .setString("space_id", event.spaceId)
            .setString("post_id", event.newParent)
            .setList[String]("child_posts", event.newChildren.toList.asJava)
        )
      else
        Seq(
          updateChildrenStatement
            .bind()
            .setString("space_id", event.spaceId)
            .setString("post_id", event.oldParent)
            .setList[String]("child_posts", event.oldChildren.toList.asJava),
          updateChildrenStatement
            .bind()
            .setString("space_id", event.spaceId)
            .setString("post_id", event.newParent)
            .setList[String]("child_posts", event.newChildren.toList.asJava)
        )

    Future.successful(seq ++ timestampSeq)

  }

  def removeRootPost(event: HierarchyEntity.RootPostRemoved): Future[Seq[BoundStatement]] =
    build(
      updateRootPostStatement
        .bind()
        .setString("space_id", event.spaceId)
        .setString("root_post_id", null)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def removePost(event: HierarchyEntity.PostRemoved): Future[Seq[BoundStatement]] =
    build(
      if (event.children.isEmpty)
        removeChildrenStatement
          .bind()
          .setString("space_id", event.spaceId)
          .setString("post_id", event.postId)
      else
        updateChildrenStatement
          .bind()
          .setString("space_id", event.spaceId)
          .setString("post_id", event.postId)
          .setList[String]("child_posts", event.children.toList.asJava),
      updateSpaceTimestampStatement
        .bind()
        .setString("space_id", event.spaceId)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def deleteSpace(event: hierarchy.HierarchyEntity.SpaceDeleted): Future[Seq[BoundStatement]] =
    build(
      deleteSpaceStatement
        .bind()
        .setString("space_id", event.spaceId),
      deleteChildrenStatement
        .bind()
        .setString("space_id", event.spaceId)
    )

  def getHierarchyById(id: SpaceId): Future[Option[WikiHierarchy]] =
    for {
      stmt         <- session.prepare("SELECT * FROM hierarchy WHERE space_id = :space_id")
      maybeEntity  <- session.selectOne(stmt.bind().setString("space_id", id)).map(_.map(convertHierarchy))
      childrenStmt <- session.prepare("SELECT * FROM hierarchy_children WHERE space_id = :space_id")
      children     <- session.selectAll(childrenStmt.bind().setString("space_id", id)).map(_.map(convertChildren).toMap)
    } yield maybeEntity.map(_.copy(childPosts = children))
//

  private def convertHierarchy(row: Row): WikiHierarchy =
    WikiHierarchy(
      spaceId = row.getString("space_id"),
      rootPostId = Option(row.getString("root_post_id")),
      updatedAt = OffsetDateTime.parse(row.getString("updated_at")),
      updatedBy = AnnettePrincipal(
        principalType = row.getString("updated_by_type"),
        principalId = row.getString("updated_by_id")
      )
    )

  private def convertChildren(row: Row): (PostId, Seq[PostId]) =
    row.getString("post_id") ->
      row.getList[String]("child_posts", classOf[String]).asScala.toSeq

  private def build(statements: BoundStatement*): Future[List[BoundStatement]] =
    Future.successful(statements.toList)

}
