package biz.lobachev.annette.cms.impl.space

import biz.lobachev.annette.cms.impl.space.dao.SpaceCassandraDbDao
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}
import org.slf4j.LoggerFactory

private[impl] class SpaceDbEventProcessor(
  readSide: CassandraReadSide,
  casDao: SpaceCassandraDbDao
) extends ReadSideProcessor[SpaceEntity.Event] {

  val log = LoggerFactory.getLogger(this.getClass)

  def buildHandler(): ReadSideProcessor.ReadSideHandler[SpaceEntity.Event] =
    readSide
      .builder[SpaceEntity.Event]("CMS_Space_CasEventOffset")
      .setGlobalPrepare(() => casDao.createTables())
      .setPrepare(_ => casDao.prepareStatements())
      .setEventHandler[SpaceEntity.SpaceCreated](e => casDao.createSpace(e.event))
      .setEventHandler[SpaceEntity.SpaceNameUpdated](e => casDao.updateSpaceName(e.event))
      .setEventHandler[SpaceEntity.SpaceDescriptionUpdated](e => casDao.updateSpaceDescription(e.event))
      .setEventHandler[SpaceEntity.SpaceCategoryUpdated](e => casDao.updateSpaceCategory(e.event))
      .setEventHandler[SpaceEntity.SpaceTargetPrincipalAssigned](e => casDao.assignSpaceTargetPrincipal(e.event))
      .setEventHandler[SpaceEntity.SpaceTargetPrincipalUnassigned](e => casDao.unassignSpaceTargetPrincipal(e.event))
      .setEventHandler[SpaceEntity.SpaceActivated](e => casDao.activateSpace(e.event))
      .setEventHandler[SpaceEntity.SpaceDeactivated](e => casDao.deactivateSpace(e.event))
      .setEventHandler[SpaceEntity.SpaceDeleted](e => casDao.deleteSpace(e.event))
      .build()

  def aggregateTags: Set[AggregateEventTag[SpaceEntity.Event]] = SpaceEntity.Event.Tag.allTags

}
