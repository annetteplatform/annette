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

package biz.lobachev.annette.authorization.impl.role

import biz.lobachev.annette.authorization.impl.role.dao.RoleIndexDao
import biz.lobachev.annette.microservice_core.event_processing.SimpleEventHandling
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}

import scala.concurrent.ExecutionContext

private[impl] class RoleEntityIndexEventProcessor(
  readSide: CassandraReadSide,
  indexDao: RoleIndexDao
)(implicit
  ec: ExecutionContext
) extends ReadSideProcessor[RoleEntity.Event]
    with SimpleEventHandling {

  def buildHandler(): ReadSideProcessor.ReadSideHandler[RoleEntity.Event] =
    readSide
      .builder[RoleEntity.Event]("role-indexing")
      .setGlobalPrepare(indexDao.createEntityIndex)
      .setEventHandler[RoleEntity.RoleCreated](handle(indexDao.createRole))
      .setEventHandler[RoleEntity.RoleUpdated](handle(indexDao.updateRole))
      .setEventHandler[RoleEntity.RoleDeleted](handle(indexDao.deleteRole))
      .build()

  def aggregateTags: Set[AggregateEventTag[RoleEntity.Event]] = RoleEntity.Event.Tag.allTags
}
