package biz.lobachev.annette.service_catalog.impl.service

import java.time.OffsetDateTime
import akka.Done
import com.datastax.driver.core.{BoundStatement, PreparedStatement, Row}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import org.slf4j.LoggerFactory

import scala.collection.immutable.{Seq, _}
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

import biz.lobachev.annette.service_catalog.api.service._

private[impl] class ServiceCasRepository(session: CassandraSession)(implicit ec: ExecutionContext) {

  val log = LoggerFactory.getLogger(this.getClass)

  private var createServiceStatement: PreparedStatement = _
  private var updateServiceNameStatement: PreparedStatement = _
  private var activateServiceStatement: PreparedStatement = _
  private var deactivateServiceStatement: PreparedStatement = _
  private var deleteServiceStatement: PreparedStatement = _

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
      createServiceStmt <- session.prepare(
        """
          |
          |""".stripMargin
      )
      updateServiceNameStmt <- session.prepare(
        """
          |
          |""".stripMargin
      )
      activateServiceStmt <- session.prepare(
        """
          |
          |""".stripMargin
      )
      deactivateServiceStmt <- session.prepare(
        """
          |
          |""".stripMargin
      )
      deleteServiceStmt <- session.prepare(
        """
          |
          |""".stripMargin
      )
    } yield {
      createServiceStatement = createServiceStmt
      updateServiceNameStatement = updateServiceNameStmt
      activateServiceStatement = activateServiceStmt
      deactivateServiceStatement = deactivateServiceStmt
      deleteServiceStatement = deleteServiceStmt
      Done
    }
  }

  def createService(event: ServiceEntity.ServiceCreated): Future[Seq[BoundStatement]] =
    build(
      createServiceStatement
        .bind()
        // TODO: insert .set
        // .setString("id", event.id)
    )

  def updateServiceName(event: ServiceEntity.ServiceUpdated): Future[Seq[BoundStatement]] =
    build(
      updateServiceNameStatement
        .bind()
        // TODO: insert .set
        // .setString("id", event.id)
    )

  def activateService(event: ServiceEntity.ServiceActivated): Future[Seq[BoundStatement]] =
    build(
      activateServiceStatement
        .bind()
        // TODO: insert .set
        // .setString("id", event.id)
    )

  def deactivateService(event: ServiceEntity.ServiceDeactivated): Future[Seq[BoundStatement]] =
    build(
      deactivateServiceStatement
        .bind()
        // TODO: insert .set
        // .setString("id", event.id)
    )

  def deleteService(event: ServiceEntity.ServiceDeleted): Future[Seq[BoundStatement]] =
    build(
      deleteServiceStatement
        .bind()
        // TODO: insert .set
        // .setString("id", event.id)
    )




  def getServiceById(id: ServiceId): Future[Option[Service]] = {
    for {
      // TODO: change the following code
      stmt        <- session.prepare("SELECT * FROM ??? WHERE id = :id")
      maybeEntity <- session.selectOne(stmt.bind().setString("id", id)).map(_.map(convertService))
    } yield maybeEntity
  }


  def getServicesById(ids: Set[ServiceId]): Future[Map[ServiceId, Service]] = {
    for {
      // TODO: change the following code
      stmt   <- session.prepare("SELECT * FROM ??? WHERE id IN ?")
      result <- session.selectAll(stmt.bind(ids.toList.asJava)).map(_.map(convertService))
    } yield result.map(a => a.id -> a).toMap
  }

  private def build(statements: BoundStatement*): Future[List[BoundStatement]] =
    Future.successful(statements.toList)


}
