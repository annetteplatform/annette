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

package biz.lobachev.annette.cms.impl.space.dao

import akka.Done
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import biz.lobachev.annette.cms.api.space._
import biz.lobachev.annette.cms.impl.space.SpaceEntity
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import com.datastax.driver.core.{BoundStatement, PreparedStatement, Row}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import org.slf4j.LoggerFactory

import java.time.OffsetDateTime
import scala.collection.immutable.{Seq, _}
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

private[impl] class SpaceCassandraDbDao(
  session: CassandraSession
)(implicit
  ec: ExecutionContext,
  materializer: Materializer
) {

  val log = LoggerFactory.getLogger(this.getClass)

  private var createSpaceStatement: PreparedStatement                  = _
  private var updateSpaceNameStatement: PreparedStatement              = _
  private var updateSpaceDescriptionStatement: PreparedStatement       = _
  private var updateSpaceCategoryStatement: PreparedStatement          = _
  private var updateSpaceTimestampStatement: PreparedStatement         = _
  private var assignSpaceTargetPrincipalStatement: PreparedStatement   = _
  private var unassignSpaceTargetPrincipalStatement: PreparedStatement = _
  private var activateSpaceStatement: PreparedStatement                = _
  private var deactivateSpaceStatement: PreparedStatement              = _
  private var deleteSpaceStatement: PreparedStatement                  = _
  private var deleteSpaceTargetsStatement: PreparedStatement           = _

  def createTables(): Future[Done] =
    for {
      _ <- session.executeCreateTable("""
                                        |CREATE TABLE IF NOT EXISTS spaces (
                                        |          id text PRIMARY KEY,
                                        |          name text,
                                        |          description text,
                                        |          space_type text,
                                        |          category_id text,
                                        |          active boolean,
                                        |          updated_at text,
                                        |          updated_by_type text,
                                        |          updated_by_id text,
                                        |)
                                        |""".stripMargin)
      _ <- session.executeCreateTable("""
                                        |CREATE TABLE IF NOT EXISTS space_targets (
                                        |          space_id text,
                                        |          principal text,
                                        |          principal_type text,
                                        |          principal_id text,
                                        |          PRIMARY KEY (space_id, principal)
                                        |)
                                        |""".stripMargin)
    } yield Done

  def prepareStatements(): Future[Done] =
    for {
      createSpaceStmt                  <- session.prepare(
                                            """
                             | INSERT INTO spaces (id, name, description, space_type, category_id, active,
                             |     updated_at, updated_by_type, updated_by_id
                             |     )
                             |   VALUES (:id, :name, :description, :space_type, :category_id, :active,
                             |     :updated_at, :updated_by_type, :updated_by_id
                             |     )
                             |""".stripMargin
                                          )
      updateSpaceNameStmt              <- session.prepare(
                                            """
                                 | UPDATE spaces SET
                                 |   name = :name,
                                 |   updated_at = :updated_at,
                                 |   updated_by_type = :updated_by_type,
                                 |   updated_by_id = :updated_by_id
                                 |  WHERE id = :id
                                 |""".stripMargin
                                          )
      updateSpaceDescriptionStmt       <- session.prepare(
                                            """
                                        | UPDATE spaces SET
                                        |   description = :description,
                                        |   updated_at = :updated_at,
                                        |   updated_by_type = :updated_by_type,
                                        |   updated_by_id = :updated_by_id
                                        |  WHERE id = :id
                                        |""".stripMargin
                                          )
      updateSpaceCategoryStmt          <- session.prepare(
                                            """
                                     | UPDATE spaces SET
                                     |   category_id = :category_id,
                                     |   updated_at = :updated_at,
                                     |   updated_by_type = :updated_by_type,
                                     |   updated_by_id = :updated_by_id
                                     |  WHERE id = :id
                                     |""".stripMargin
                                          )
      updateSpaceTimestampStmt         <- session.prepare(
                                            """
                                      | UPDATE spaces SET
                                      |   updated_at = :updated_at,
                                      |   updated_by_type = :updated_by_type,
                                      |   updated_by_id = :updated_by_id
                                      |  WHERE id = :id
                                      |""".stripMargin
                                          )
      assignSpaceTargetPrincipalStmt   <-
        session.prepare(
          """
            | INSERT INTO space_targets (space_id, principal, principal_type, principal_id )
            |   VALUES (:space_id, :principal, :principal_type, :principal_id)
            |""".stripMargin
        )
      unassignSpaceTargetPrincipalStmt <- session.prepare(
                                            """
                                              | DELETE FROM space_targets
                                              |   WHERE
                                              |     space_id = :space_id AND
                                              |     principal = :principal
                                              |""".stripMargin
                                          )
      activateSpaceStmt                <- session.prepare(
                                            """
                               | UPDATE spaces SET
                               |   active = true,
                               |   updated_at = :updated_at,
                               |   updated_by_type = :updated_by_type,
                               |   updated_by_id = :updated_by_id
                               |  WHERE id = :id
                               |""".stripMargin
                                          )
      deactivateSpaceStmt              <- session.prepare(
                                            """
                                 | UPDATE spaces SET
                                 |   active = false,
                                 |   updated_at = :updated_at,
                                 |   updated_by_type = :updated_by_type,
                                 |   updated_by_id = :updated_by_id
                                 |  WHERE id = :id
                                 |""".stripMargin
                                          )
      deleteSpaceStmt                  <- session.prepare(
                                            """
                             | DELETE FROM spaces
                             |  WHERE id = :id
                             |""".stripMargin
                                          )
      deleteSpaceTargetsStmt           <- session.prepare(
                                            """
                                    | DELETE FROM space_targets
                                    |  WHERE space_id = :space_id
                                    |""".stripMargin
                                          )
    } yield {
      createSpaceStatement = createSpaceStmt
      updateSpaceNameStatement = updateSpaceNameStmt
      updateSpaceDescriptionStatement = updateSpaceDescriptionStmt
      updateSpaceCategoryStatement = updateSpaceCategoryStmt
      updateSpaceTimestampStatement = updateSpaceTimestampStmt
      assignSpaceTargetPrincipalStatement = assignSpaceTargetPrincipalStmt
      unassignSpaceTargetPrincipalStatement = unassignSpaceTargetPrincipalStmt
      activateSpaceStatement = activateSpaceStmt
      deactivateSpaceStatement = deactivateSpaceStmt
      deleteSpaceStatement = deleteSpaceStmt
      deleteSpaceTargetsStatement = deleteSpaceTargetsStmt
      Done
    }

  def createSpace(event: SpaceEntity.SpaceCreated): Future[Seq[BoundStatement]] = {
    val targets = event.targets
      .map(target =>
        assignSpaceTargetPrincipalStatement
          .bind()
          .setString("space_id", event.id)
          .setString("principal", target.code)
          .setString("principal_type", target.principalType)
          .setString("principal_id", target.principalId)
      )
      .toSeq
    Future.successful(
      createSpaceStatement
        .bind()
        .setString("id", event.id)
        .setString("name", event.name)
        .setString("description", event.description)
        .setString("space_type", event.spaceType.toString)
        .setString("category_id", event.categoryId)
        .setBool("active", true)
        .setString("updated_at", event.createdAt.toString)
        .setString("updated_by_type", event.createdBy.principalType)
        .setString("updated_by_id", event.createdBy.principalId)
        +: targets
    )
  }

  def updateSpaceName(event: SpaceEntity.SpaceNameUpdated): Future[Seq[BoundStatement]] =
    execute(
      updateSpaceNameStatement
        .bind()
        .setString("id", event.id)
        .setString("name", event.name)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def updateSpaceDescription(event: SpaceEntity.SpaceDescriptionUpdated): Future[Seq[BoundStatement]] =
    execute(
      updateSpaceDescriptionStatement
        .bind()
        .setString("id", event.id)
        .setString("description", event.description)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def updateSpaceCategory(event: SpaceEntity.SpaceCategoryUpdated): Future[Seq[BoundStatement]] =
    execute(
      updateSpaceCategoryStatement
        .bind()
        .setString("id", event.id)
        .setString("category_id", event.categoryId)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def assignSpaceTargetPrincipal(event: SpaceEntity.SpaceTargetPrincipalAssigned): Future[Seq[BoundStatement]] =
    execute(
      assignSpaceTargetPrincipalStatement
        .bind()
        .setString("space_id", event.id)
        .setString("principal", event.principal.code)
        .setString("principal_type", event.principal.principalType)
        .setString("principal_id", event.principal.principalId),
      updateSpaceTimestampStatement
        .bind()
        .setString("id", event.id)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def unassignSpaceTargetPrincipal(event: SpaceEntity.SpaceTargetPrincipalUnassigned): Future[Seq[BoundStatement]] =
    execute(
      unassignSpaceTargetPrincipalStatement
        .bind()
        .setString("space_id", event.id)
        .setString("principal", event.principal.code),
      updateSpaceTimestampStatement
        .bind()
        .setString("id", event.id)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def activateSpace(event: SpaceEntity.SpaceActivated): Future[Seq[BoundStatement]] =
    execute(
      activateSpaceStatement
        .bind()
        .setString("id", event.id)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def deactivateSpace(event: SpaceEntity.SpaceDeactivated): Future[Seq[BoundStatement]] =
    execute(
      deactivateSpaceStatement
        .bind()
        .setString("id", event.id)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def deleteSpace(event: SpaceEntity.SpaceDeleted): Future[Seq[BoundStatement]] =
    execute(
      deleteSpaceStatement
        .bind()
        .setString("id", event.id),
      deleteSpaceTargetsStatement
        .bind()
        .setString("space_id", event.id)
    )

  def getSpaceById(id: SpaceId): Future[Option[Space]] =
    for {
      stmt        <- session.prepare("SELECT * FROM spaces WHERE id = :id")
      maybeEntity <- session.selectOne(stmt.bind().setString("id", id)).map(_.map(convertSpace))
      targetStmt  <- session.prepare("SELECT principal_type, principal_id FROM space_targets WHERE space_id = :space_id")
      targets     <- session.selectAll(targetStmt.bind().setString("space_id", id)).map(_.map(convertTarget))
    } yield maybeEntity.map(_.copy(targets = targets.toSet))

  def getSpacesById(ids: Set[SpaceId]): Future[Seq[Space]] =
    Source(ids)
      .mapAsync(1)(getSpaceById)
      .runWith(Sink.seq)
      .map(_.flatten.toSeq)

  def getSpaceViews(ids: Set[SpaceId], principals: Set[AnnettePrincipal]): Future[Seq[SpaceView]] =
    for {
      stmt     <- session.prepare(
                    "SELECT space_id FROM space_targets WHERE space_id IN :ids AND principal IN :principals"
                  )
      spaceIds <- session
                    .selectAll(
                      stmt
                        .bind()
                        .setList("ids", ids.toList.asJava)
                        .setList("principals", principals.map(_.code).toList.asJava)
                    )
                    .map(_.map(_.getString("space_id")).toSet)
      stmt     <- session.prepare(
                    "SELECT * FROM spaces WHERE id IN ?"
                  )
      result   <- session.selectAll(stmt.bind(spaceIds.toList.asJava)).map(_.map(convertSpaceView))
    } yield result

  def canAccessToSpace(id: SpaceId, principals: Set[AnnettePrincipal]): Future[Boolean] =
    for {
      stmt  <- session.prepare("SELECT count(*) FROM space_targets WHERE space_id=:id AND principal IN :principals")
      count <- session
                 .selectOne(
                   stmt
                     .bind()
                     .setString("id", id)
                     .setList("principals", principals.map(_.code).toList.asJava)
                 )
                 .map(_.map(_.getLong("count").toInt).getOrElse(0))
    } yield count > 0

  private def convertSpace(row: Row): Space =
    Space(
      id = row.getString("id"),
      name = row.getString("name"),
      description = row.getString("description"),
      spaceType = SpaceType.from(row.getString("space_type")),
      categoryId = row.getString("category_id"),
      active = row.getBool("active"),
      updatedAt = OffsetDateTime.parse(row.getString("updated_at")),
      updatedBy = AnnettePrincipal(
        principalType = row.getString("updated_by_type"),
        principalId = row.getString("updated_by_id")
      )
    )

  private def convertTarget(row: Row): AnnettePrincipal =
    AnnettePrincipal(
      principalType = row.getString("principal_type"),
      principalId = row.getString("principal_id")
    )

  private def convertSpaceView(row: Row): SpaceView =
    SpaceView(
      id = row.getString("id"),
      name = row.getString("name"),
      description = row.getString("description"),
      spaceType = SpaceType.from(row.getString("space_type")),
      categoryId = row.getString("category_id"),
      active = row.getBool("active"),
      updatedAt = OffsetDateTime.parse(row.getString("updated_at")),
      updatedBy = AnnettePrincipal(
        principalType = row.getString("updated_by_type"),
        principalId = row.getString("updated_by_id")
      )
    )

  private def execute(statements: BoundStatement*): Future[List[BoundStatement]] =
    for (
      _ <- Source(statements)
             .mapAsync(1) { statement =>
               val future = session.executeWrite(statement)
               future.failed.foreach(th => log.error("Failed to process statement {}", statement, th))
               future
             }
             .runWith(Sink.seq)
    ) yield List.empty

}
