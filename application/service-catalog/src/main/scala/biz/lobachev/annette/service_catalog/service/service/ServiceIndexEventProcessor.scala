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

package biz.lobachev.annette.service_catalog.service.service

import biz.lobachev.annette.microservice_core.event_processing.SimpleEventHandling
import biz.lobachev.annette.service_catalog.service.service.dao.ServiceIndexDao
import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide

import scala.concurrent.ExecutionContext

private[impl] class ServiceIndexEventProcessor(
  readSide: CassandraReadSide,
  indexDao: ServiceIndexDao
)(implicit
  ec: ExecutionContext
) extends ReadSideProcessor[ServiceEntity.Event]
    with SimpleEventHandling {

  def buildHandler() =
    readSide
      .builder[ServiceEntity.Event]("service-indexing")
      .setGlobalPrepare(indexDao.createEntityIndex)
      .setEventHandler[ServiceEntity.ServiceCreated](handle(indexDao.createService))
      .setEventHandler[ServiceEntity.ServiceUpdated](handle(indexDao.updateService))
      .setEventHandler[ServiceEntity.ServiceActivated](handle(indexDao.activateService))
      .setEventHandler[ServiceEntity.ServiceDeactivated](handle(indexDao.deactivateService))
      .setEventHandler[ServiceEntity.ServiceDeleted](handle(indexDao.deleteService))
      .build()

  def aggregateTags = ServiceEntity.Event.Tag.allTags

}
