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

package biz.lobachev.annette.cms.impl.hierarchy

import biz.lobachev.annette.cms.impl.hierarchy.dao.HierarchyDbDao
import biz.lobachev.annette.microservice_core.event_processing.SimpleEventHandling
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}

import scala.concurrent.ExecutionContext

private[impl] class HierarchyDbEventProcessor(
  readSide: CassandraReadSide,
  dbDao: HierarchyDbDao
)(implicit ec: ExecutionContext)
    extends ReadSideProcessor[HierarchyEntity.Event]
    with SimpleEventHandling {

  def buildHandler(): ReadSideProcessor.ReadSideHandler[HierarchyEntity.Event] =
    readSide
      .builder[HierarchyEntity.Event]("hierarchy-cas")
      .setGlobalPrepare(dbDao.createTables)
      .setEventHandler[HierarchyEntity.SpaceCreated](handle(dbDao.createSpace))
      .setEventHandler[HierarchyEntity.RootPostAdded](handle(dbDao.addRootPost))
      .setEventHandler[HierarchyEntity.PostAdded](handle(dbDao.addPost))
      .setEventHandler[HierarchyEntity.PostMoved](handle(dbDao.movePost))
      .setEventHandler[HierarchyEntity.RootPostRemoved](handle(dbDao.removeRootPost))
      .setEventHandler[HierarchyEntity.PostRemoved](handle(dbDao.removePost))
      .setEventHandler[HierarchyEntity.SpaceDeleted](handle(dbDao.deleteSpace))
      .build()

  def aggregateTags: Set[AggregateEventTag[HierarchyEntity.Event]] = HierarchyEntity.Event.Tag.allTags

}
