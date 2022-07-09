package biz.lobachev.annette.service_catalog.impl.group

import java.time.OffsetDateTime
import akka.Done
import com.datastax.driver.core.BoundStatement
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}
import scala.concurrent.{ExecutionContext, Future}
import org.slf4j.LoggerFactory

import biz.lobachev.annette.service_catalog.api.group._

private[impl] class GroupCasEventProcessor(
    readSide: CassandraReadSide,
    dbDao: GroupCasRepository
)(
    implicit ec: ExecutionContext
) extends ReadSideProcessor[GroupEntity.Event] {

  val log = LoggerFactory.getLogger(this.getClass)

  def buildHandler(): ReadSideProcessor.ReadSideHandler[GroupEntity.Event] = {
    readSide
      .builder[GroupEntity.Event]("ServiceCatalog_Group_CasEventOffset")
      .setGlobalPrepare(casRepository.createTables)
      .setPrepare(_ => casRepository.prepareStatements())
      .setEventHandler[GroupEntity.GroupCreated](e => dbDao.createGroup(e.event))
      .setEventHandler[GroupEntity.GroupUpdated](e => dbDao.updateGroupName(e.event))
      .setEventHandler[GroupEntity.GroupActivated](e => dbDao.activateGroup(e.event))
      .setEventHandler[GroupEntity.GroupDeactivated](e => dbDao.deactivateGroup(e.event))
      .setEventHandler[GroupEntity.GroupDeleted](e => dbDao.deleteGroup(e.event))
      .build()
  }

  def aggregateTags: Set[AggregateEventTag[GroupEntity.Event]] = GroupEntity.Event.Tag.allTags


  def createGroup(event: GroupEntity.GroupCreated): Future[Seq[BoundStatement]] =
    Future.successful(
      casRepository.createGroup(event)
    )

  def updateGroupName(event: GroupEntity.GroupUpdated): Future[Seq[BoundStatement]] =
    Future.successful(
      casRepository.updateGroupName(event)
    )

  def activateGroup(event: GroupEntity.GroupActivated): Future[Seq[BoundStatement]] =
    Future.successful(
      casRepository.activateGroup(event)
    )

  def deactivateGroup(event: GroupEntity.GroupDeactivated): Future[Seq[BoundStatement]] =
    Future.successful(
      casRepository.deactivateGroup(event)
    )

  def deleteGroup(event: GroupEntity.GroupDeleted): Future[Seq[BoundStatement]] =
    Future.successful(
      casRepository.deleteGroup(event)
    )


}
