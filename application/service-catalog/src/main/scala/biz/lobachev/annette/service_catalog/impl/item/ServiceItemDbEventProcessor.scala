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

import biz.lobachev.annette.microservice_core.pekko.event_processing.Tagger
import biz.lobachev.annette.microservice_core.pekko.projection.ProjectionBase
import biz.lobachev.annette.service_catalog.impl.item.dao.ServiceItemDbDao
import org.apache.pekko.Done
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.projection.eventsourced.EventEnvelope

import scala.concurrent.{ExecutionContext, Future}

private[impl] class ServiceItemDbEventProcessor(
  dbDao: ServiceItemDbDao
)(implicit
  val system: ActorSystem[_],
  override val ec: ExecutionContext
) extends ProjectionBase[ServiceItemEntity.Event] {

  override val projectionName: String = "service-cassandra"
  override val tags: Seq[String] = Tagger.fromEventName[ServiceItemEntity.Event](10).allTags

  override def process(envelope: EventEnvelope[ServiceItemEntity.Event]): Future[Done] =
    envelope.event match {
      case evt: ServiceItemEntity.GroupCreated           => dbDao.createGroup(evt).map(_ => Done)
      case evt: ServiceItemEntity.GroupUpdated           => dbDao.updateGroup(evt).map(_ => Done)
      case evt: ServiceItemEntity.ServiceCreated         => dbDao.createService(evt).map(_ => Done)
      case evt: ServiceItemEntity.ServiceUpdated         => dbDao.updateService(evt).map(_ => Done)
      case evt: ServiceItemEntity.ServiceItemActivated   => dbDao.activateService(evt).map(_ => Done)
      case evt: ServiceItemEntity.ServiceItemDeactivated => dbDao.deactivateService(evt).map(_ => Done)
      case evt: ServiceItemEntity.ServiceItemDeleted     => dbDao.deleteService(evt).map(_ => Done)
      case _                                             => Future.successful(Done)
    }
}
