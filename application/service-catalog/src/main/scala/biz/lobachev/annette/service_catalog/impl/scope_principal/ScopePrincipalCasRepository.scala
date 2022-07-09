package biz.lobachev.annette.service_catalog.impl.scope_principal

import java.time.OffsetDateTime
import akka.Done
import com.datastax.driver.core.{BoundStatement, PreparedStatement, Row}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import org.slf4j.LoggerFactory

import scala.collection.immutable.{Seq, _}
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

import biz.lobachev.annette.service_catalog.api.scope_principal._

private[impl] class ScopePrincipalCasRepository(session: CassandraSession)(implicit ec: ExecutionContext) {

  val log = LoggerFactory.getLogger(this.getClass)

  private var assignScopePrincipalStatement: PreparedStatement = _
  private var unassignScopePrincipalStatement: PreparedStatement = _

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
      assignScopePrincipalStmt <- session.prepare(
        """
          |
          |""".stripMargin
      )
      unassignScopePrincipalStmt <- session.prepare(
        """
          |
          |""".stripMargin
      )
    } yield {
      assignScopePrincipalStatement = assignScopePrincipalStmt
      unassignScopePrincipalStatement = unassignScopePrincipalStmt
      Done
    }
  }

  def assignScopePrincipal(event: ScopePrincipalEntity.ScopePrincipalAssigned): Future[Seq[BoundStatement]] =
    build(
      assignScopePrincipalStatement
        .bind()
        // TODO: insert .set
        // .setString("id", event.id)
    )

  def unassignScopePrincipal(event: ScopePrincipalEntity.ScopePrincipalUnassigned): Future[Seq[BoundStatement]] =
    build(
      unassignScopePrincipalStatement
        .bind()
        // TODO: insert .set
        // .setString("id", event.id)
    )





  private def build(statements: BoundStatement*): Future[List[BoundStatement]] =
    Future.successful(statements.toList)


}
