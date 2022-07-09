package biz.lobachev.annette.service_catalog.impl.scope

import java.time.OffsetDateTime
import akka.Done
import com.datastax.driver.core.BoundStatement
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}
import scala.concurrent.{ExecutionContext, Future}
import org.slf4j.LoggerFactory

import biz.lobachev.annette.service_catalog.api.scope._

private[impl] class ScopeCasEventProcessor(
    readSide: CassandraReadSide,
    dbDao: ScopeCasRepository
)(
    implicit ec: ExecutionContext
) extends ReadSideProcessor[ScopeEntity.Event] {

  val log = LoggerFactory.getLogger(this.getClass)

  def buildHandler(): ReadSideProcessor.ReadSideHandler[ScopeEntity.Event] = {
    readSide
      .builder[ScopeEntity.Event]("ServiceCatalog_Scope_CasEventOffset")
      .setGlobalPrepare(casRepository.createTables)
      .setPrepare(_ => casRepository.prepareStatements())
      .setEventHandler[ScopeEntity.ScopeCreated](e => dbDao.createScope(e.event))
      .setEventHandler[ScopeEntity.ScopeUpdated](e => dbDao.updateScopeName(e.event))
      .setEventHandler[ScopeEntity.ScopeActivated](e => dbDao.activateScope(e.event))
      .setEventHandler[ScopeEntity.ScopeDeactivated](e => dbDao.deactivateScope(e.event))
      .setEventHandler[ScopeEntity.ScopeDeleted](e => dbDao.deleteScope(e.event))
      .build()
  }

  def aggregateTags: Set[AggregateEventTag[ScopeEntity.Event]] = ScopeEntity.Event.Tag.allTags


  def createScope(event: ScopeEntity.ScopeCreated): Future[Seq[BoundStatement]] =
    Future.successful(
      casRepository.createScope(event)
    )

  def updateScopeName(event: ScopeEntity.ScopeUpdated): Future[Seq[BoundStatement]] =
    Future.successful(
      casRepository.updateScopeName(event)
    )

  def activateScope(event: ScopeEntity.ScopeActivated): Future[Seq[BoundStatement]] =
    Future.successful(
      casRepository.activateScope(event)
    )

  def deactivateScope(event: ScopeEntity.ScopeDeactivated): Future[Seq[BoundStatement]] =
    Future.successful(
      casRepository.deactivateScope(event)
    )

  def deleteScope(event: ScopeEntity.ScopeDeleted): Future[Seq[BoundStatement]] =
    Future.successful(
      casRepository.deleteScope(event)
    )


}
