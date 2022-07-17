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

package biz.lobachev.annette.service_catalog.service.scope

import biz.lobachev.annette.microservice_core.event_processing.SimpleEventHandling
import biz.lobachev.annette.service_catalog.service.scope.dao.ScopeIndexDao
import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide

import scala.concurrent.ExecutionContext

private[impl] class ScopeIndexEventProcessor(
  readSide: CassandraReadSide,
  indexDao: ScopeIndexDao
)(implicit
  ec: ExecutionContext
) extends ReadSideProcessor[ScopeEntity.Event]
    with SimpleEventHandling {

  def buildHandler() =
    readSide
      .builder[ScopeEntity.Event]("scope-indexing")
      .setGlobalPrepare(indexDao.createEntityIndex)
      .setEventHandler[ScopeEntity.ScopeCreated](handle(indexDao.createScope))
      .setEventHandler[ScopeEntity.ScopeUpdated](handle(indexDao.updateScope))
      .setEventHandler[ScopeEntity.ScopeActivated](handle(indexDao.activateScope))
      .setEventHandler[ScopeEntity.ScopeDeactivated](handle(indexDao.deactivateScope))
      .setEventHandler[ScopeEntity.ScopeDeleted](handle(indexDao.deleteScope))
      .build()

  def aggregateTags = ScopeEntity.Event.Tag.allTags

}
