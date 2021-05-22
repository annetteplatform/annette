package biz.lobachev.annette.cms.impl.space

import biz.lobachev.annette.cms.impl.space.dao.SpaceElasticIndexDao
import com.datastax.driver.core.BoundStatement
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

private[impl] class SpaceIndexEventProcessor(
  readSide: CassandraReadSide,
  elasticRepository: SpaceElasticIndexDao
)(implicit
  ec: ExecutionContext
) extends ReadSideProcessor[SpaceEntity.Event] {

  val log = LoggerFactory.getLogger(this.getClass)

  def buildHandler(): ReadSideProcessor.ReadSideHandler[SpaceEntity.Event] =
    readSide
      .builder[SpaceEntity.Event]("CMS_Space_ElasticEventOffset")
      .setGlobalPrepare(elasticRepository.createEntityIndex)
      .setEventHandler[SpaceEntity.SpaceCreated](e => createSpace(e.event))
      .setEventHandler[SpaceEntity.SpaceNameUpdated](e => updateSpaceName(e.event))
      .setEventHandler[SpaceEntity.SpaceDescriptionUpdated](e => updateSpaceDescription(e.event))
      .setEventHandler[SpaceEntity.SpaceCategoryUpdated](e => updateSpaceCategory(e.event))
      .setEventHandler[SpaceEntity.SpaceTargetPrincipalAssigned](e => assignSpaceTargetPrincipal(e.event))
      .setEventHandler[SpaceEntity.SpaceTargetPrincipalUnassigned](e => unassignSpaceTargetPrincipal(e.event))
      .setEventHandler[SpaceEntity.SpaceActivated](e => activateSpace(e.event))
      .setEventHandler[SpaceEntity.SpaceDeactivated](e => deactivateSpace(e.event))
      .setEventHandler[SpaceEntity.SpaceDeleted](e => deleteSpace(e.event))
      .build()

  def aggregateTags: Set[AggregateEventTag[SpaceEntity.Event]] = SpaceEntity.Event.Tag.allTags

  def createSpace(event: SpaceEntity.SpaceCreated): Future[Seq[BoundStatement]] =
    elasticRepository
      .createSpace(event)
      .map(_ => Seq.empty)

  def updateSpaceName(event: SpaceEntity.SpaceNameUpdated): Future[Seq[BoundStatement]] =
    elasticRepository
      .updateSpaceName(event)
      .map(_ => Seq.empty)

  def updateSpaceDescription(event: SpaceEntity.SpaceDescriptionUpdated): Future[Seq[BoundStatement]] =
    elasticRepository
      .updateSpaceDescription(event)
      .map(_ => Seq.empty)

  def updateSpaceCategory(event: SpaceEntity.SpaceCategoryUpdated): Future[Seq[BoundStatement]] =
    elasticRepository
      .updateSpaceCategory(event)
      .map(_ => Seq.empty)

  def assignSpaceTargetPrincipal(event: SpaceEntity.SpaceTargetPrincipalAssigned): Future[Seq[BoundStatement]] =
    elasticRepository
      .assignSpaceTargetPrincipal(event)
      .map(_ => Seq.empty)

  def unassignSpaceTargetPrincipal(event: SpaceEntity.SpaceTargetPrincipalUnassigned): Future[Seq[BoundStatement]] =
    elasticRepository
      .unassignSpaceTargetPrincipal(event)
      .map(_ => Seq.empty)

  def activateSpace(event: SpaceEntity.SpaceActivated): Future[Seq[BoundStatement]] =
    elasticRepository
      .activateSpace(event)
      .map(_ => Seq.empty)

  def deactivateSpace(event: SpaceEntity.SpaceDeactivated): Future[Seq[BoundStatement]] =
    elasticRepository
      .deactivateSpace(event)
      .map(_ => Seq.empty)

  def deleteSpace(event: SpaceEntity.SpaceDeleted): Future[Seq[BoundStatement]] =
    elasticRepository
      .deleteSpace(event)
      .map(_ => Seq.empty)

}
