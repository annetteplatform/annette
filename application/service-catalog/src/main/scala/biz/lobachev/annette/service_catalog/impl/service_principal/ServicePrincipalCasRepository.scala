package biz.lobachev.annette.service_catalog.impl.service_principal

import java.time.OffsetDateTime
import akka.Done
import com.datastax.driver.core.{BoundStatement, PreparedStatement, Row}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import org.slf4j.LoggerFactory

import scala.collection.immutable.{Seq, _}
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

import biz.lobachev.annette.service_catalog.api.service_principal._

private[impl] class ServicePrincipalCasRepository(session: CassandraSession)(implicit ec: ExecutionContext) {

  val log = LoggerFactory.getLogger(this.getClass)

  private var assignServicePrincipalStatement: PreparedStatement = _
  private var unassignServicePrincipalStatement: PreparedStatement = _

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
      assignServicePrincipalStmt <- session.prepare(
        """
          |
          |""".stripMargin
      )
      unassignServicePrincipalStmt <- session.prepare(
        """
          |
          |""".stripMargin
      )
    } yield {
      assignServicePrincipalStatement = assignServicePrincipalStmt
      unassignServicePrincipalStatement = unassignServicePrincipalStmt
      Done
    }
  }

  def assignServicePrincipal(event: ServicePrincipalEntity.ServicePrincipalAssigned): Future[Seq[BoundStatement]] =
    build(
      assignServicePrincipalStatement
        .bind()
        // TODO: insert .set
        // .setString("id", event.id)
    )

  def unassignServicePrincipal(event: ServicePrincipalEntity.ServicePrincipalUnassigned): Future[Seq[BoundStatement]] =
    build(
      unassignServicePrincipalStatement
        .bind()
        // TODO: insert .set
        // .setString("id", event.id)
    )





  private def build(statements: BoundStatement*): Future[List[BoundStatement]] =
    Future.successful(statements.toList)


}
