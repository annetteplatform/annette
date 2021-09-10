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
      .builder[SpaceEntity.Event]("cms-space-cas")
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
