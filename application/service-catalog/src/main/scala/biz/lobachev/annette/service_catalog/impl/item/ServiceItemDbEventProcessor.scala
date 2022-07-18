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

package biz.lobachev.annette.service_catalog.impl.item

import biz.lobachev.annette.microservice_core.event_processing.SimpleEventHandling
import biz.lobachev.annette.service_catalog.impl.item.dao.ServiceItemDbDao
import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide

import scala.concurrent.ExecutionContext

private[service_catalog] class ServiceItemDbEventProcessor(
  readSide: CassandraReadSide,
  dbDao: ServiceItemDbDao
)(implicit ec: ExecutionContext)
    extends ReadSideProcessor[ServiceItemEntity.Event]
    with SimpleEventHandling {

  def buildHandler() =
    readSide
      .builder[ServiceItemEntity.Event]("service-cassandra")
      .setGlobalPrepare(dbDao.createTables)
      .setEventHandler[ServiceItemEntity.GroupCreated](handle(dbDao.createGroup))
      .setEventHandler[ServiceItemEntity.GroupUpdated](handle(dbDao.updateGroup))
      .setEventHandler[ServiceItemEntity.ServiceCreated](handle(dbDao.createService))
      .setEventHandler[ServiceItemEntity.ServiceUpdated](handle(dbDao.updateService))
      .setEventHandler[ServiceItemEntity.ServiceItemActivated](handle(dbDao.activateService))
      .setEventHandler[ServiceItemEntity.ServiceItemDeactivated](handle(dbDao.deactivateService))
      .setEventHandler[ServiceItemEntity.ServiceItemDeleted](handle(dbDao.deleteService))
      .build()

  def aggregateTags = ServiceItemEntity.Event.Tag.allTags

}
