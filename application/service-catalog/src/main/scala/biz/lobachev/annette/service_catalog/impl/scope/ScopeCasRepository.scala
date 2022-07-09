package biz.lobachev.annette.service_catalog.impl.scope

import java.time.OffsetDateTime
import akka.Done
import com.datastax.driver.core.{BoundStatement, PreparedStatement, Row}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import org.slf4j.LoggerFactory

import scala.collection.immutable.{Seq, _}
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

import biz.lobachev.annette.service_catalog.api.scope._

private[impl] class ScopeCasRepository(session: CassandraSession)(implicit ec: ExecutionContext) {

  val log = LoggerFactory.getLogger(this.getClass)

  private var createScopeStatement: PreparedStatement = _
  private var updateScopeNameStatement: PreparedStatement = _
  private var activateScopeStatement: PreparedStatement = _
  private var deactivateScopeStatement: PreparedStatement = _
  private var deleteScopeStatement: PreparedStatement = _

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
      createScopeStmt <- session.prepare(
        """
          |
          |""".stripMargin
      )
      updateScopeNameStmt <- session.prepare(
        """
          |
          |""".stripMargin
      )
      activateScopeStmt <- session.prepare(
        """
          |
          |""".stripMargin
      )
      deactivateScopeStmt <- session.prepare(
        """
          |
          |""".stripMargin
      )
      deleteScopeStmt <- session.prepare(
        """
          |
          |""".stripMargin
      )
    } yield {
      createScopeStatement = createScopeStmt
      updateScopeNameStatement = updateScopeNameStmt
      activateScopeStatement = activateScopeStmt
      deactivateScopeStatement = deactivateScopeStmt
      deleteScopeStatement = deleteScopeStmt
      Done
    }
  }

  def createScope(event: ScopeEntity.ScopeCreated): Future[Seq[BoundStatement]] =
    build(
      createScopeStatement
        .bind()
        // TODO: insert .set
        // .setString("id", event.id)
    )

  def updateScopeName(event: ScopeEntity.ScopeUpdated): Future[Seq[BoundStatement]] =
    build(
      updateScopeNameStatement
        .bind()
        // TODO: insert .set
        // .setString("id", event.id)
    )

  def activateScope(event: ScopeEntity.ScopeActivated): Future[Seq[BoundStatement]] =
    build(
      activateScopeStatement
        .bind()
        // TODO: insert .set
        // .setString("id", event.id)
    )

  def deactivateScope(event: ScopeEntity.ScopeDeactivated): Future[Seq[BoundStatement]] =
    build(
      deactivateScopeStatement
        .bind()
        // TODO: insert .set
        // .setString("id", event.id)
    )

  def deleteScope(event: ScopeEntity.ScopeDeleted): Future[Seq[BoundStatement]] =
    build(
      deleteScopeStatement
        .bind()
        // TODO: insert .set
        // .setString("id", event.id)
    )




  def getScopeById(id: ScopeId): Future[Option[Scope]] = {
    for {
      // TODO: change the following code
      stmt        <- session.prepare("SELECT * FROM ??? WHERE id = :id")
      maybeEntity <- session.selectOne(stmt.bind().setString("id", id)).map(_.map(convertScope))
    } yield maybeEntity
  }


  def getScopesById(ids: Set[ScopeId]): Future[Map[ScopeId, Scope]] = {
    for {
      // TODO: change the following code
      stmt   <- session.prepare("SELECT * FROM ??? WHERE id IN ?")
      result <- session.selectAll(stmt.bind(ids.toList.asJava)).map(_.map(convertScope))
    } yield result.map(a => a.id -> a).toMap
  }

  private def build(statements: BoundStatement*): Future[List[BoundStatement]] =
    Future.successful(statements.toList)


}
