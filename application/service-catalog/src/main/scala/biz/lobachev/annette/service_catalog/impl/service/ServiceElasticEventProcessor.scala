package biz.lobachev.annette.service_catalog.impl.service

import java.time.OffsetDateTime
import akka.Done
import com.datastax.driver.core.BoundStatement
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}
import scala.concurrent.{ExecutionContext, Future}
import org.slf4j.LoggerFactory

import biz.lobachev.annette.service_catalog.api.service._

private[impl] class ServiceElasticEventProcessor(
    readSide: CassandraReadSide,
    elasticRepository: ServiceElasticIndexDao,
)(
    implicit ec: ExecutionContext
) extends ReadSideProcessor[ServiceEntity.Event] {

  val log = LoggerFactory.getLogger(this.getClass)

  def buildHandler: ReadSideProcessor.ReadSideHandler[ServiceEntity.Event] = {
    readSide
      .builder[ServiceEntity.Event]("ServiceCatalog_Service_ElasticEventOffset")
      .setGlobalPrepare(globalPrepare)
      .setEventHandler[ServiceEntity.ServiceCreated](e => createService(e.event))
      .setEventHandler[ServiceEntity.ServiceUpdated](e => updateServiceName(e.event))
      .setEventHandler[ServiceEntity.ServiceActivated](e => activateService(e.event))
      .setEventHandler[ServiceEntity.ServiceDeactivated](e => deactivateService(e.event))
      .setEventHandler[ServiceEntity.ServiceDeleted](e => deleteService(e.event))
      .build
  }

  def aggregateTags: Set[AggregateEventTag[ServiceEntity.Event]] = ServiceEntity.Event.Tag.allTags

  def globalPrepare(): Future[Done] =
    elasticRepository.createEntityIndex
      .map(_ => Done)

  def createService(event: ServiceEntity.ServiceCreated): Future[Seq[BoundStatement]] =
    elasticRepository
     .createService(event)
     .map(_ => Seq.empty)

  def updateServiceName(event: ServiceEntity.ServiceUpdated): Future[Seq[BoundStatement]] =
    elasticRepository
     .updateServiceName(event)
     .map(_ => Seq.empty)

  def activateService(event: ServiceEntity.ServiceActivated): Future[Seq[BoundStatement]] =
    elasticRepository
     .activateService(event)
     .map(_ => Seq.empty)

  def deactivateService(event: ServiceEntity.ServiceDeactivated): Future[Seq[BoundStatement]] =
    elasticRepository
     .deactivateService(event)
     .map(_ => Seq.empty)

  def deleteService(event: ServiceEntity.ServiceDeleted): Future[Seq[BoundStatement]] =
    elasticRepository
     .deleteService(event)
     .map(_ => Seq.empty)


}
