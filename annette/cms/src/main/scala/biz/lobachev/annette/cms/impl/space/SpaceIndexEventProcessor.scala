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

import biz.lobachev.annette.cms.impl.space.dao.SpaceIndexDao
import biz.lobachev.annette.microservice_core.event_processing.SimpleEventHandling
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}

import scala.concurrent.{ExecutionContext}

private[impl] class SpaceIndexEventProcessor(
  readSide: CassandraReadSide,
  indexDao: SpaceIndexDao
)(implicit ec: ExecutionContext)
    extends ReadSideProcessor[SpaceEntity.Event]
    with SimpleEventHandling {

  def buildHandler(): ReadSideProcessor.ReadSideHandler[SpaceEntity.Event] =
    readSide
      .builder[SpaceEntity.Event]("space-index")
      .setGlobalPrepare(indexDao.createEntityIndex)
      .setEventHandler[SpaceEntity.SpaceCreated](handle(indexDao.createSpace))
      .setEventHandler[SpaceEntity.SpaceNameUpdated](handle(indexDao.updateSpaceName))
      .setEventHandler[SpaceEntity.SpaceDescriptionUpdated](handle(indexDao.updateSpaceDescription))
      .setEventHandler[SpaceEntity.SpaceCategoryUpdated](handle(indexDao.updateSpaceCategory))
      .setEventHandler[SpaceEntity.SpaceTargetPrincipalAssigned](handle(indexDao.assignSpaceTargetPrincipal))
      .setEventHandler[SpaceEntity.SpaceTargetPrincipalUnassigned](handle(indexDao.unassignSpaceTargetPrincipal))
      .setEventHandler[SpaceEntity.SpaceActivated](handle(indexDao.activateSpace))
      .setEventHandler[SpaceEntity.SpaceDeactivated](handle(indexDao.deactivateSpace))
      .setEventHandler[SpaceEntity.SpaceDeleted](handle(indexDao.deleteSpace))
      .build()

  def aggregateTags: Set[AggregateEventTag[SpaceEntity.Event]] = SpaceEntity.Event.Tag.allTags

}
