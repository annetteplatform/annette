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

package biz.lobachev.annette.service_catalog.service.scope_principal

import biz.lobachev.annette.microservice_core.event_processing.SimpleEventHandling
import biz.lobachev.annette.service_catalog.service.scope_principal.dao.ScopePrincipalIndexDao
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}

import scala.concurrent.ExecutionContext

private[impl] class ScopePrincipalIndexEventProcessor(
  readSide: CassandraReadSide,
  indexDao: ScopePrincipalIndexDao
)(implicit
  ec: ExecutionContext
) extends ReadSideProcessor[ScopePrincipalEntity.Event]
    with SimpleEventHandling {

  def buildHandler(): ReadSideProcessor.ReadSideHandler[ScopePrincipalEntity.Event] =
    readSide
      .builder[ScopePrincipalEntity.Event]("scopePrincipal-indexing")
      .setGlobalPrepare(indexDao.createEntityIndex)
      .setEventHandler[ScopePrincipalEntity.ScopePrincipalAssigned](handle(indexDao.assignPrincipal))
      .setEventHandler[ScopePrincipalEntity.ScopePrincipalUnassigned](handle(indexDao.unassignPrincipal))
      .build()

  def aggregateTags: Set[AggregateEventTag[ScopePrincipalEntity.Event]] = ScopePrincipalEntity.Event.Tag.allTags
}
