package biz.lobachev.annette.service_catalog.impl.group

import java.time.OffsetDateTime
import akka.Done
import com.datastax.driver.core.{BoundStatement, PreparedStatement, Row}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import org.slf4j.LoggerFactory

import scala.collection.immutable.{Seq, _}
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

import biz.lobachev.annette.service_catalog.api.group._

private[impl] class GroupCasRepository(session: CassandraSession)(implicit ec: ExecutionContext) {

  val log = LoggerFactory.getLogger(this.getClass)

  private var createGroupStatement: PreparedStatement = _
  private var updateGroupNameStatement: PreparedStatement = _
  private var activateGroupStatement: PreparedStatement = _
  private var deactivateGroupStatement: PreparedStatement = _
  private var deleteGroupStatement: PreparedStatement = _

  def createTables(): Future[Done] = {
    for {
      _ <- session.executeCreateTable(
           """
            |CREATE TABLE IF NOT EXISTS ?????? (
            |          id               text PRIMARY KEY,
            |)
            |""".stripMargin)
    } yield Done
  }

  def prepareStatements(): Future[Done] = {
    for {
      createGroupStmt <- session.prepare(
        """
          |
          |""".stripMargin
      )
      updateGroupNameStmt <- session.prepare(
        """
          |
          |""".stripMargin
      )
      activateGroupStmt <- session.prepare(
        """
          |
          |""".stripMargin
      )
      deactivateGroupStmt <- session.prepare(
        """
          |
          |""".stripMargin
      )
      deleteGroupStmt <- session.prepare(
        """
          |
          |""".stripMargin
      )
    } yield {
      createGroupStatement = createGroupStmt
      updateGroupNameStatement = updateGroupNameStmt
      activateGroupStatement = activateGroupStmt
      deactivateGroupStatement = deactivateGroupStmt
      deleteGroupStatement = deleteGroupStmt
      Done
    }
  }

  def createGroup(event: GroupEntity.GroupCreated): Future[Seq[BoundStatement]] =
    build(
      createGroupStatement
        .bind()
        // TODO: insert .set
        // .setString("id", event.id)
    )

  def updateGroupName(event: GroupEntity.GroupUpdated): Future[Seq[BoundStatement]] =
    build(
      updateGroupNameStatement
        .bind()
        // TODO: insert .set
        // .setString("id", event.id)
    )

  def activateGroup(event: GroupEntity.GroupActivated): Future[Seq[BoundStatement]] =
    build(
      activateGroupStatement
        .bind()
        // TODO: insert .set
        // .setString("id", event.id)
    )

  def deactivateGroup(event: GroupEntity.GroupDeactivated): Future[Seq[BoundStatement]] =
    build(
      deactivateGroupStatement
        .bind()
        // TODO: insert .set
        // .setString("id", event.id)
    )

  def deleteGroup(event: GroupEntity.GroupDeleted): Future[Seq[BoundStatement]] =
    build(
      deleteGroupStatement
        .bind()
        // TODO: insert .set
        // .setString("id", event.id)
    )




  def getGroupById(id: GroupId): Future[Option[Group]] = {
    for {
      // TODO: change the following code
      stmt        <- session.prepare("SELECT * FROM ??? WHERE id = :id")
      maybeEntity <- session.selectOne(stmt.bind().setString("id", id)).map(_.map(convertGroup))
    } yield maybeEntity
  }


  def getGroupsById(ids: Set[GroupId]): Future[Map[GroupId, Group]] = {
    for {
      // TODO: change the following code
      stmt   <- session.prepare("SELECT * FROM ??? WHERE id IN ?")
      result <- session.selectAll(stmt.bind(ids.toList.asJava)).map(_.map(convertGroup))
    } yield result.map(a => a.id -> a).toMap
  }

  private def build(statements: BoundStatement*): Future[List[BoundStatement]] =
    Future.successful(statements.toList)


}
