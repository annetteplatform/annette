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

package biz.lobachev.annette.cms.impl.home_pages

import biz.lobachev.annette.cms.impl.home_pages.dao.HomePageIndexDao
import biz.lobachev.annette.microservice_core.event_processing.SimpleEventHandling
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}

import scala.concurrent.{ExecutionContext}

class HomePageIndexEventProcessor(
  readSide: CassandraReadSide,
  indexDao: HomePageIndexDao
)(implicit ec: ExecutionContext)
    extends ReadSideProcessor[HomePageEntity.Event]
    with SimpleEventHandling {

  def buildHandler(): ReadSideProcessor.ReadSideHandler[HomePageEntity.Event] =
    readSide
      .builder[HomePageEntity.Event]("home-page-index")
      .setGlobalPrepare(indexDao.createEntityIndex)
      .setEventHandler[HomePageEntity.HomePageAssigned](handle(indexDao.assignHomePage))
      .setEventHandler[HomePageEntity.HomePageUnassigned](handle(indexDao.unassignHomePage))
      .build()

  def aggregateTags: Set[AggregateEventTag[HomePageEntity.Event]] = HomePageEntity.Event.Tag.allTags

}
