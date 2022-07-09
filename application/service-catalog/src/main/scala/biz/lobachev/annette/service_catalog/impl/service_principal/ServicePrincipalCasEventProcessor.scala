package biz.lobachev.annette.service_catalog.impl.service_principal

import java.time.OffsetDateTime
import akka.Done
import com.datastax.driver.core.BoundStatement
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}
import scala.concurrent.{ExecutionContext, Future}
import org.slf4j.LoggerFactory

import biz.lobachev.annette.service_catalog.api.service_principal._

private[impl] class ServicePrincipalCasEventProcessor(
    readSide: CassandraReadSide,
    dbDao: ServicePrincipalCasRepository
)(
    implicit ec: ExecutionContext
) extends ReadSideProcessor[ServicePrincipalEntity.Event] {

  val log = LoggerFactory.getLogger(this.getClass)

  def buildHandler(): ReadSideProcessor.ReadSideHandler[ServicePrincipalEntity.Event] = {
    readSide
      .builder[ServicePrincipalEntity.Event]("ServiceCatalog_ServicePrincipal_CasEventOffset")
      .setGlobalPrepare(casRepository.createTables)
      .setPrepare(_ => casRepository.prepareStatements())
      .setEventHandler[ServicePrincipalEntity.ServicePrincipalAssigned](e => dbDao.assignServicePrincipal(e.event))
      .setEventHandler[ServicePrincipalEntity.ServicePrincipalUnassigned](e => dbDao.unassignServicePrincipal(e.event))
      .build()
  }

  def aggregateTags: Set[AggregateEventTag[ServicePrincipalEntity.Event]] = ServicePrincipalEntity.Event.Tag.allTags


  def assignServicePrincipal(event: ServicePrincipalEntity.ServicePrincipalAssigned): Future[Seq[BoundStatement]] =
    Future.successful(
      casRepository.assignServicePrincipal(event)
    )

  def unassignServicePrincipal(event: ServicePrincipalEntity.ServicePrincipalUnassigned): Future[Seq[BoundStatement]] =
    Future.successful(
      casRepository.unassignServicePrincipal(event)
    )


}
