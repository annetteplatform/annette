package biz.lobachev.annette.service_catalog.impl.group

import java.time.OffsetDateTime
import akka.Done
import com.datastax.driver.core.BoundStatement
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}
import scala.concurrent.{ExecutionContext, Future}
import org.slf4j.LoggerFactory

import biz.lobachev.annette.service_catalog.api.group._

private[impl] class GroupElasticEventProcessor(
    readSide: CassandraReadSide,
    elasticRepository: GroupElasticIndexDao,
)(
    implicit ec: ExecutionContext
) extends ReadSideProcessor[GroupEntity.Event] {

  val log = LoggerFactory.getLogger(this.getClass)

  def buildHandler: ReadSideProcessor.ReadSideHandler[GroupEntity.Event] = {
    readSide
      .builder[GroupEntity.Event]("ServiceCatalog_Group_ElasticEventOffset")
      .setGlobalPrepare(globalPrepare)
      .setEventHandler[GroupEntity.GroupCreated](e => createGroup(e.event))
      .setEventHandler[GroupEntity.GroupUpdated](e => updateGroupName(e.event))
      .setEventHandler[GroupEntity.GroupActivated](e => activateGroup(e.event))
      .setEventHandler[GroupEntity.GroupDeactivated](e => deactivateGroup(e.event))
      .setEventHandler[GroupEntity.GroupDeleted](e => deleteGroup(e.event))
      .build
  }

  def aggregateTags: Set[AggregateEventTag[GroupEntity.Event]] = GroupEntity.Event.Tag.allTags

  def globalPrepare(): Future[Done] =
    elasticRepository.createEntityIndex
      .map(_ => Done)

  def createGroup(event: GroupEntity.GroupCreated): Future[Seq[BoundStatement]] =
    elasticRepository
     .createGroup(event)
     .map(_ => Seq.empty)

  def updateGroupName(event: GroupEntity.GroupUpdated): Future[Seq[BoundStatement]] =
    elasticRepository
     .updateGroupName(event)
     .map(_ => Seq.empty)

  def activateGroup(event: GroupEntity.GroupActivated): Future[Seq[BoundStatement]] =
    elasticRepository
     .activateGroup(event)
     .map(_ => Seq.empty)

  def deactivateGroup(event: GroupEntity.GroupDeactivated): Future[Seq[BoundStatement]] =
    elasticRepository
     .deactivateGroup(event)
     .map(_ => Seq.empty)

  def deleteGroup(event: GroupEntity.GroupDeleted): Future[Seq[BoundStatement]] =
    elasticRepository
     .deleteGroup(event)
     .map(_ => Seq.empty)


}
