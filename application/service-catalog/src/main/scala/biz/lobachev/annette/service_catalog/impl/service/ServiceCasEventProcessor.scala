package biz.lobachev.annette.service_catalog.impl.service

import java.time.OffsetDateTime
import akka.Done
import com.datastax.driver.core.BoundStatement
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}
import scala.concurrent.{ExecutionContext, Future}
import org.slf4j.LoggerFactory

import biz.lobachev.annette.service_catalog.api.service._

private[impl] class ServiceCasEventProcessor(
    readSide: CassandraReadSide,
    dbDao: ServiceCasRepository
)(
    implicit ec: ExecutionContext
) extends ReadSideProcessor[ServiceEntity.Event] {

  val log = LoggerFactory.getLogger(this.getClass)

  def buildHandler(): ReadSideProcessor.ReadSideHandler[ServiceEntity.Event] = {
    readSide
      .builder[ServiceEntity.Event]("ServiceCatalog_Service_CasEventOffset")
      .setGlobalPrepare(casRepository.createTables)
      .setPrepare(_ => casRepository.prepareStatements())
      .setEventHandler[ServiceEntity.ServiceCreated](e => dbDao.createService(e.event))
      .setEventHandler[ServiceEntity.ServiceUpdated](e => dbDao.updateServiceName(e.event))
      .setEventHandler[ServiceEntity.ServiceActivated](e => dbDao.activateService(e.event))
      .setEventHandler[ServiceEntity.ServiceDeactivated](e => dbDao.deactivateService(e.event))
      .setEventHandler[ServiceEntity.ServiceDeleted](e => dbDao.deleteService(e.event))
      .build()
  }

  def aggregateTags: Set[AggregateEventTag[ServiceEntity.Event]] = ServiceEntity.Event.Tag.allTags


  def createService(event: ServiceEntity.ServiceCreated): Future[Seq[BoundStatement]] =
    Future.successful(
      casRepository.createService(event)
    )

  def updateServiceName(event: ServiceEntity.ServiceUpdated): Future[Seq[BoundStatement]] =
    Future.successful(
      casRepository.updateServiceName(event)
    )

  def activateService(event: ServiceEntity.ServiceActivated): Future[Seq[BoundStatement]] =
    Future.successful(
      casRepository.activateService(event)
    )

  def deactivateService(event: ServiceEntity.ServiceDeactivated): Future[Seq[BoundStatement]] =
    Future.successful(
      casRepository.deactivateService(event)
    )

  def deleteService(event: ServiceEntity.ServiceDeleted): Future[Seq[BoundStatement]] =
    Future.successful(
      casRepository.deleteService(event)
    )


}
