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

package biz.lobachev.annette.org_structure.impl.category

import biz.lobachev.annette.microservice_core.event_processing.SimpleEventHandling
import biz.lobachev.annette.org_structure.impl.category.dao.CategoryDbDao
import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide

import scala.concurrent.ExecutionContext

private[impl] class CategoryDbEventProcessor(
  readSide: CassandraReadSide,
  dbDao: CategoryDbDao
)(implicit ec: ExecutionContext)
    extends ReadSideProcessor[CategoryEntity.Event]
    with SimpleEventHandling {

  def buildHandler() =
    readSide
      .builder[CategoryEntity.Event]("category-cassandra")
      .setGlobalPrepare(dbDao.createTables)
      .setEventHandler[CategoryEntity.CategoryCreated](handle(dbDao.createCategory))
      .setEventHandler[CategoryEntity.CategoryUpdated](handle(dbDao.updateCategory))
      .setEventHandler[CategoryEntity.CategoryDeleted](handle(dbDao.deleteCategory))
      .build()

  def aggregateTags = CategoryEntity.Event.Tag.allTags

}
