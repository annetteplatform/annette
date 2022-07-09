package biz.lobachev.annette.service_catalog.impl.scope_principal

import java.time.OffsetDateTime
import akka.Done
import com.datastax.driver.core.BoundStatement
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}
import scala.concurrent.{ExecutionContext, Future}
import org.slf4j.LoggerFactory

import biz.lobachev.annette.service_catalog.api.scope_principal._

private[impl] class ScopePrincipalElasticEventProcessor(
    readSide: CassandraReadSide,
    elasticRepository: ScopePrincipalElasticIndexDao,
)(
    implicit ec: ExecutionContext
) extends ReadSideProcessor[ScopePrincipalEntity.Event] {

  val log = LoggerFactory.getLogger(this.getClass)

  def buildHandler: ReadSideProcessor.ReadSideHandler[ScopePrincipalEntity.Event] = {
    readSide
      .builder[ScopePrincipalEntity.Event]("ServiceCatalog_ScopePrincipal_ElasticEventOffset")
      .setGlobalPrepare(globalPrepare)
      .setEventHandler[ScopePrincipalEntity.ScopePrincipalAssigned](e => assignScopePrincipal(e.event))
      .setEventHandler[ScopePrincipalEntity.ScopePrincipalUnassigned](e => unassignScopePrincipal(e.event))
      .build
  }

  def aggregateTags: Set[AggregateEventTag[ScopePrincipalEntity.Event]] = ScopePrincipalEntity.Event.Tag.allTags

  def globalPrepare(): Future[Done] =
    elasticRepository.createEntityIndex
      .map(_ => Done)

  def assignScopePrincipal(event: ScopePrincipalEntity.ScopePrincipalAssigned): Future[Seq[BoundStatement]] =
    elasticRepository
     .assignScopePrincipal(event)
     .map(_ => Seq.empty)

  def unassignScopePrincipal(event: ScopePrincipalEntity.ScopePrincipalUnassigned): Future[Seq[BoundStatement]] =
    elasticRepository
     .unassignScopePrincipal(event)
     .map(_ => Seq.empty)


}
