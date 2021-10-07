/*
 * Copyright 2013 Valery Lobachev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package biz.lobachev.annette.cms.impl.space

import biz.lobachev.annette.cms.impl.space.dao.SpaceDbDao
import biz.lobachev.annette.microservice_core.event_processing.SimpleEventHandling
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext

private[impl] class SpaceDbEventProcessor(
  readSide: CassandraReadSide,
  dbDao: SpaceDbDao
)(implicit ec: ExecutionContext)
    extends ReadSideProcessor[SpaceEntity.Event]
    with SimpleEventHandling {

  val log = LoggerFactory.getLogger(this.getClass)

  def buildHandler(): ReadSideProcessor.ReadSideHandler[SpaceEntity.Event] =
    readSide
      .builder[SpaceEntity.Event]("cms-space-cas")
      .setGlobalPrepare(() => dbDao.createTables())
      .setPrepare(_ => dbDao.prepareStatements())
      .setEventHandler[SpaceEntity.SpaceCreated](handle(dbDao.createSpace))
      .setEventHandler[SpaceEntity.SpaceNameUpdated](handle(dbDao.updateSpaceName))
      .setEventHandler[SpaceEntity.SpaceDescriptionUpdated](handle(dbDao.updateSpaceDescription))
      .setEventHandler[SpaceEntity.SpaceCategoryUpdated](handle(dbDao.updateSpaceCategory))
      .setEventHandler[SpaceEntity.SpaceTargetPrincipalAssigned](handle(dbDao.assignSpaceTargetPrincipal))
      .setEventHandler[SpaceEntity.SpaceTargetPrincipalUnassigned](handle(dbDao.unassignSpaceTargetPrincipal))
      .setEventHandler[SpaceEntity.SpaceActivated](handle(dbDao.activateSpace))
      .setEventHandler[SpaceEntity.SpaceDeactivated](handle(dbDao.deactivateSpace))
      .setEventHandler[SpaceEntity.SpaceDeleted](handle(dbDao.deleteSpace))
      .build()

  def aggregateTags: Set[AggregateEventTag[SpaceEntity.Event]] = SpaceEntity.Event.Tag.allTags

}
