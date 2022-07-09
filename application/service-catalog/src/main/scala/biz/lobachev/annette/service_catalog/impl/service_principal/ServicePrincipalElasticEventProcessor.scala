package biz.lobachev.annette.service_catalog.impl.service_principal

import java.time.OffsetDateTime
import akka.Done
import com.datastax.driver.core.BoundStatement
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}
import scala.concurrent.{ExecutionContext, Future}
import org.slf4j.LoggerFactory

import biz.lobachev.annette.service_catalog.api.service_principal._

private[impl] class ServicePrincipalElasticEventProcessor(
    readSide: CassandraReadSide,
    elasticRepository: ServicePrincipalElasticIndexDao,
)(
    implicit ec: ExecutionContext
) extends ReadSideProcessor[ServicePrincipalEntity.Event] {

  val log = LoggerFactory.getLogger(this.getClass)

  def buildHandler: ReadSideProcessor.ReadSideHandler[ServicePrincipalEntity.Event] = {
    readSide
      .builder[ServicePrincipalEntity.Event]("ServiceCatalog_ServicePrincipal_ElasticEventOffset")
      .setGlobalPrepare(globalPrepare)
      .setEventHandler[ServicePrincipalEntity.ServicePrincipalAssigned](e => assignServicePrincipal(e.event))
      .setEventHandler[ServicePrincipalEntity.ServicePrincipalUnassigned](e => unassignServicePrincipal(e.event))
      .build
  }

  def aggregateTags: Set[AggregateEventTag[ServicePrincipalEntity.Event]] = ServicePrincipalEntity.Event.Tag.allTags

  def globalPrepare(): Future[Done] =
    elasticRepository.createEntityIndex
      .map(_ => Done)

  def assignServicePrincipal(event: ServicePrincipalEntity.ServicePrincipalAssigned): Future[Seq[BoundStatement]] =
    elasticRepository
     .assignServicePrincipal(event)
     .map(_ => Seq.empty)

  def unassignServicePrincipal(event: ServicePrincipalEntity.ServicePrincipalUnassigned): Future[Seq[BoundStatement]] =
    elasticRepository
     .unassignServicePrincipal(event)
     .map(_ => Seq.empty)


}
