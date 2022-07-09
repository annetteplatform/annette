package biz.lobachev.annette.service_catalog.impl.scope

import java.time.OffsetDateTime
import akka.Done
import com.datastax.driver.core.BoundStatement
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}
import scala.concurrent.{ExecutionContext, Future}
import org.slf4j.LoggerFactory

import biz.lobachev.annette.service_catalog.api.scope._

private[impl] class ScopeElasticEventProcessor(
    readSide: CassandraReadSide,
    elasticRepository: ScopeElasticIndexDao,
)(
    implicit ec: ExecutionContext
) extends ReadSideProcessor[ScopeEntity.Event] {

  val log = LoggerFactory.getLogger(this.getClass)

  def buildHandler: ReadSideProcessor.ReadSideHandler[ScopeEntity.Event] = {
    readSide
      .builder[ScopeEntity.Event]("ServiceCatalog_Scope_ElasticEventOffset")
      .setGlobalPrepare(globalPrepare)
      .setEventHandler[ScopeEntity.ScopeCreated](e => createScope(e.event))
      .setEventHandler[ScopeEntity.ScopeUpdated](e => updateScopeName(e.event))
      .setEventHandler[ScopeEntity.ScopeActivated](e => activateScope(e.event))
      .setEventHandler[ScopeEntity.ScopeDeactivated](e => deactivateScope(e.event))
      .setEventHandler[ScopeEntity.ScopeDeleted](e => deleteScope(e.event))
      .build
  }

  def aggregateTags: Set[AggregateEventTag[ScopeEntity.Event]] = ScopeEntity.Event.Tag.allTags

  def globalPrepare(): Future[Done] =
    elasticRepository.createEntityIndex
      .map(_ => Done)

  def createScope(event: ScopeEntity.ScopeCreated): Future[Seq[BoundStatement]] =
    elasticRepository
     .createScope(event)
     .map(_ => Seq.empty)

  def updateScopeName(event: ScopeEntity.ScopeUpdated): Future[Seq[BoundStatement]] =
    elasticRepository
     .updateScopeName(event)
     .map(_ => Seq.empty)

  def activateScope(event: ScopeEntity.ScopeActivated): Future[Seq[BoundStatement]] =
    elasticRepository
     .activateScope(event)
     .map(_ => Seq.empty)

  def deactivateScope(event: ScopeEntity.ScopeDeactivated): Future[Seq[BoundStatement]] =
    elasticRepository
     .deactivateScope(event)
     .map(_ => Seq.empty)

  def deleteScope(event: ScopeEntity.ScopeDeleted): Future[Seq[BoundStatement]] =
    elasticRepository
     .deleteScope(event)
     .map(_ => Seq.empty)


}
