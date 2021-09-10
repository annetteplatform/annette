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

import biz.lobachev.annette.cms.impl.hierarchy.dao.HierarchyCassandraDbDao
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}
import org.slf4j.LoggerFactory

private[impl] class HierarchyDbEventProcessor(
  readSide: CassandraReadSide,
  casDao: HierarchyCassandraDbDao
) extends ReadSideProcessor[HierarchyEntity.Event] {

  val log = LoggerFactory.getLogger(this.getClass)

  def buildHandler(): ReadSideProcessor.ReadSideHandler[HierarchyEntity.Event] =
    readSide
      .builder[HierarchyEntity.Event]("cms-hierarchy-cas")
      .setGlobalPrepare(() => casDao.createTables())
      .setPrepare(_ => casDao.prepareStatements())
      .setEventHandler[HierarchyEntity.SpaceCreated](e => casDao.createSpace(e.event))
      .setEventHandler[HierarchyEntity.RootPostAdded](e => casDao.addRootPost(e.event))
      .setEventHandler[HierarchyEntity.PostAdded](e => casDao.addPost(e.event))
      .setEventHandler[HierarchyEntity.PostMoved](e => casDao.movePost(e.event))
      .setEventHandler[HierarchyEntity.RootPostRemoved](e => casDao.removeRootPost(e.event))
      .setEventHandler[HierarchyEntity.PostRemoved](e => casDao.removePost(e.event))
      .setEventHandler[HierarchyEntity.SpaceDeleted](e => casDao.deleteSpace(e.event))
      .build()

  def aggregateTags: Set[AggregateEventTag[HierarchyEntity.Event]] = HierarchyEntity.Event.Tag.allTags

}
