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

package biz.lobachev.annette.cms.impl.blogs.category

import biz.lobachev.annette.microservice_core.event_processing.SimpleEventHandling
import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide

import scala.concurrent.ExecutionContext

class BlogCategoryDbEventProcessor(
  readSide: CassandraReadSide,
  dbDao: dao.BlogCategoryDbDao,
  readSideId: String
)(implicit ec: ExecutionContext)
    extends ReadSideProcessor[BlogCategoryEntity.Event]
    with SimpleEventHandling {

  def buildHandler() =
    readSide
      .builder[BlogCategoryEntity.Event](readSideId)
      .setGlobalPrepare(dbDao.createTables)
      .setEventHandler[BlogCategoryEntity.CategoryCreated](handle(dbDao.createCategory))
      .setEventHandler[BlogCategoryEntity.CategoryUpdated](handle(dbDao.updateCategory))
      .setEventHandler[BlogCategoryEntity.CategoryDeleted](handle(dbDao.deleteCategory))
      .build()

  def aggregateTags = BlogCategoryEntity.Event.Tag.allTags

}
