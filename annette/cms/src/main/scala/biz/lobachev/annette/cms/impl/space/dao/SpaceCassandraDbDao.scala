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

private[impl] class SpaceCassandraDbDao(session: CassandraSession)(implicit
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
                                        |          principal_type text,
                                        |          principal_id text,
                                        |          PRIMARY KEY (space_id, principal_type, principal_id)
                                        |)
                                        |""".stripMargin)
    } yield Done

  def prepareStatements(): Future[Done] =
    for {
      createSpaceStmt                  <- session.prepare(
                                            """
                             | INSERT INTO spaces (id, name, description, category_id, active,
                             |     updated_at, updated_by_type, updated_by_id
                             |     )
                             |   VALUES (:id, :name, :description, :category_id, :active,
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
      assignSpaceTargetPrincipalStmt   <- session.prepare(
                                            """
                                            | INSERT INTO space_targets (space_id, principal_type, principal_id )
                                            |   VALUES (:space_id, :principal_type, :principal_id)
                                            |""".stripMargin
                                          )
      unassignSpaceTargetPrincipalStmt <- session.prepare(
                                            """
                                              | DELETE FROM space_targets
                                              |   WHERE
                                              |     space_id = :space_id AND
                                              |     principal_type = :principal_type AND
                                              |     principal_id = :principal_id
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

  def createSpace(event: SpaceEntity.SpaceCreated): Future[Seq[BoundStatement]] =
    build(
      createSpaceStatement
        .bind()
        .setString("id", event.id)
        .setString("name", event.name)
        .setString("description", event.description)
        .setString("category_id", event.categoryId)
        .setBool("active", true)
        .setString("updated_at", event.createdAt.toString)
        .setString("updated_by_type", event.createdBy.principalType)
        .setString("updated_by_id", event.createdBy.principalId)
    )

  def updateSpaceName(event: SpaceEntity.SpaceNameUpdated): Future[Seq[BoundStatement]] =
    build(
      updateSpaceNameStatement
        .bind()
        .setString("id", event.id)
        .setString("name", event.name)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def updateSpaceDescription(event: SpaceEntity.SpaceDescriptionUpdated): Future[Seq[BoundStatement]] =
    build(
      updateSpaceDescriptionStatement
        .bind()
        .setString("id", event.id)
        .setString("description", event.description)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def updateSpaceCategory(event: SpaceEntity.SpaceCategoryUpdated): Future[Seq[BoundStatement]] =
    build(
      updateSpaceCategoryStatement
        .bind()
        .setString("id", event.id)
        .setString("category_id", event.categoryId)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def assignSpaceTargetPrincipal(event: SpaceEntity.SpaceTargetPrincipalAssigned): Future[Seq[BoundStatement]] =
    build(
      assignSpaceTargetPrincipalStatement
        .bind()
        .setString("space_id", event.id)
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
    build(
      unassignSpaceTargetPrincipalStatement
        .bind()
        .setString("space_id", event.id)
        .setString("principal_type", event.principal.principalType)
        .setString("principal_id", event.principal.principalId),
      updateSpaceTimestampStatement
        .bind()
        .setString("id", event.id)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def activateSpace(event: SpaceEntity.SpaceActivated): Future[Seq[BoundStatement]] =
    build(
      activateSpaceStatement
        .bind()
        .setString("id", event.id)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def deactivateSpace(event: SpaceEntity.SpaceDeactivated): Future[Seq[BoundStatement]] =
    build(
      deactivateSpaceStatement
        .bind()
        .setString("id", event.id)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def deleteSpace(event: SpaceEntity.SpaceDeleted): Future[Seq[BoundStatement]] =
    build(
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

  def getSpaceAnnotationById(id: SpaceId): Future[Option[SpaceAnnotation]] =
    for {
      stmt        <-
        session.prepare(
          "SELECT id, name, category_id, active, updated_at, updated_by_type, updated_by_id FROM spaces WHERE id = :id"
        )
      maybeEntity <- session.selectOne(stmt.bind().setString("id", id)).map(_.map(convertSpaceAnnotation))
    } yield maybeEntity

  def getSpacesById(ids: Set[SpaceId]): Future[Map[SpaceId, Space]]                     =
    Source(ids)
      .mapAsync(1)(getSpaceById)
      .runWith(Sink.seq)
      .map(_.flatten.map(a => a.id -> a).toMap)

  def getSpaceAnnotationsById(ids: Set[SpaceId]): Future[Map[SpaceId, SpaceAnnotation]] =
    for {
      stmt   <-
        session.prepare(
          "SELECT id, name, category_id, active, updated_at, updated_by_type, updated_by_id FROM spaces WHERE id IN ?"
        )
      result <- session.selectAll(stmt.bind(ids.toList.asJava)).map(_.map(convertSpaceAnnotation))
    } yield result.map(a => a.id -> a).toMap

  private def convertSpace(row: Row): Space                                             =
    Space(
      id = row.getString("id"),
      name = row.getString("name"),
      description = row.getString("description"),
      categoryId = row.getString("category_id"),
      active = row.getBool("active"),
      updatedAt = OffsetDateTime.parse(row.getString("updated_at")),
      updatedBy = AnnettePrincipal(
        principalType = row.getString("updated_by_type"),
        principalId = row.getString("updated_by_id")
      )
    )
  private def convertTarget(row: Row): AnnettePrincipal                                 =
    AnnettePrincipal(
      principalType = row.getString("principal_type"),
      principalId = row.getString("principal_id")
    )

  private def convertSpaceAnnotation(row: Row): SpaceAnnotation =
    SpaceAnnotation(
      id = row.getString("id"),
      name = row.getString("name"),
      categoryId = row.getString("category_id"),
      active = row.getBool("active"),
      updatedAt = OffsetDateTime.parse(row.getString("updated_at")),
      updatedBy = AnnettePrincipal(
        principalType = row.getString("updated_by_type"),
        principalId = row.getString("updated_by_id")
      )
    )

  private def build(statements: BoundStatement*): Future[List[BoundStatement]] =
    Future.successful(statements.toList)

}
