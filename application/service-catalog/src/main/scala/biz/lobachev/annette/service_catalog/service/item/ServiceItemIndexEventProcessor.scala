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

package biz.lobachev.annette.service_catalog.service.item

import biz.lobachev.annette.microservice_core.event_processing.SimpleEventHandling
import biz.lobachev.annette.service_catalog.service.item.dao.ServiceItemIndexDao
import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide

import scala.concurrent.ExecutionContext

private[service_catalog] class ServiceItemIndexEventProcessor(
  readSide: CassandraReadSide,
  indexDao: ServiceItemIndexDao
)(implicit
  ec: ExecutionContext
) extends ReadSideProcessor[ServiceItemEntity.Event]
    with SimpleEventHandling {

  def buildHandler() =
    readSide
      .builder[ServiceItemEntity.Event]("service-indexing")
      .setGlobalPrepare(indexDao.createEntityIndex)
      .setEventHandler[ServiceItemEntity.GroupCreated](handle(indexDao.createGroup))
      .setEventHandler[ServiceItemEntity.GroupUpdated](handle(indexDao.updateGroup))
      .setEventHandler[ServiceItemEntity.ServiceCreated](handle(indexDao.createService))
      .setEventHandler[ServiceItemEntity.ServiceUpdated](handle(indexDao.updateService))
      .setEventHandler[ServiceItemEntity.ServiceItemActivated](handle(indexDao.activateService))
      .setEventHandler[ServiceItemEntity.ServiceItemDeactivated](handle(indexDao.deactivateService))
      .setEventHandler[ServiceItemEntity.ServiceItemDeleted](handle(indexDao.deleteService))
      .build()

  def aggregateTags = ServiceItemEntity.Event.Tag.allTags

}
