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

package biz.lobachev.annette.service_catalog.impl.scope

import biz.lobachev.annette.microservice_core.event_processing.SimpleEventHandling
import biz.lobachev.annette.service_catalog.impl.scope.dao.ScopeDbDao
import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide

import scala.concurrent.ExecutionContext

private[service_catalog] class ScopeDbEventProcessor(
  readSide: CassandraReadSide,
  dbDao: ScopeDbDao
)(implicit ec: ExecutionContext)
    extends ReadSideProcessor[ScopeEntity.Event]
    with SimpleEventHandling {

  def buildHandler() =
    readSide
      .builder[ScopeEntity.Event]("scope-cassandra")
      .setGlobalPrepare(dbDao.createTables)
      .setEventHandler[ScopeEntity.ScopeCreated](handle(dbDao.createScope))
      .setEventHandler[ScopeEntity.ScopeUpdated](handle(dbDao.updateScope))
      .setEventHandler[ScopeEntity.ScopeActivated](handle(dbDao.activateScope))
      .setEventHandler[ScopeEntity.ScopeDeactivated](handle(dbDao.deactivateScope))
      .setEventHandler[ScopeEntity.ScopeDeleted](handle(dbDao.deleteScope))
      .build()

  def aggregateTags = ScopeEntity.Event.Tag.allTags

}
