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

package biz.lobachev.annette.cms.impl.pages.category

import biz.lobachev.annette.microservice_core.event_processing.SimpleEventHandling
import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide

import scala.concurrent.ExecutionContext

class SpaceCategoryIndexEventProcessor(
  readSide: CassandraReadSide,
  indexDao: dao.SpaceCategoryIndexDao,
  readSideId: String
)(implicit
  ec: ExecutionContext
) extends ReadSideProcessor[SpaceCategoryEntity.Event]
    with SimpleEventHandling {

  def buildHandler() =
    readSide
      .builder[SpaceCategoryEntity.Event](readSideId)
      .setGlobalPrepare(indexDao.createEntityIndex)
      .setEventHandler[SpaceCategoryEntity.CategoryCreated](handle(indexDao.createCategory))
      .setEventHandler[SpaceCategoryEntity.CategoryUpdated](handle(indexDao.updateCategory))
      .setEventHandler[SpaceCategoryEntity.CategoryDeleted](handle(indexDao.deleteCategory))
      .build()

  def aggregateTags = SpaceCategoryEntity.Event.Tag.allTags
}
