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

package biz.lobachev.annette.service_catalog.impl.service

import biz.lobachev.annette.microservice_core.event_processing.SimpleEventHandling
import biz.lobachev.annette.service_catalog.impl.service.dao.ServiceDbDao
import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide

import scala.concurrent.ExecutionContext

private[impl] class ServiceDbEventProcessor(
  readSide: CassandraReadSide,
  dbDao: ServiceDbDao
)(implicit ec: ExecutionContext)
    extends ReadSideProcessor[ServiceEntity.Event]
    with SimpleEventHandling {

  def buildHandler() =
    readSide
      .builder[ServiceEntity.Event]("service-cassandra")
      .setGlobalPrepare(dbDao.createTables)
      .setEventHandler[ServiceEntity.ServiceCreated](handle(dbDao.createService))
      .setEventHandler[ServiceEntity.ServiceUpdated](handle(dbDao.updateService))
      .setEventHandler[ServiceEntity.ServiceActivated](handle(dbDao.activateService))
      .setEventHandler[ServiceEntity.ServiceDeactivated](handle(dbDao.deactivateService))
      .setEventHandler[ServiceEntity.ServiceDeleted](handle(dbDao.deleteService))
      .build()

  def aggregateTags = ServiceEntity.Event.Tag.allTags

}
