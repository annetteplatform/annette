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

package biz.lobachev.annette.org_structure.impl.role

import biz.lobachev.annette.microservice_core.event_processing.SimpleEventHandling
import biz.lobachev.annette.org_structure.impl.role.dao.OrgRoleIndexDao
import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide

import scala.concurrent.ExecutionContext

private[impl] class OrgRoleIndexEventProcessor(
  readSide: CassandraReadSide,
  indexDao: OrgRoleIndexDao
)(implicit
  ec: ExecutionContext
) extends ReadSideProcessor[OrgRoleEntity.Event]
    with SimpleEventHandling {

  def buildHandler() =
    readSide
      .builder[OrgRoleEntity.Event]("role-indexing")
      .setGlobalPrepare(indexDao.createEntityIndex)
      .setEventHandler[OrgRoleEntity.OrgRoleCreated](handle(indexDao.createOrgRole))
      .setEventHandler[OrgRoleEntity.OrgRoleUpdated](handle(indexDao.updateOrgRole))
      .setEventHandler[OrgRoleEntity.OrgRoleDeleted](handle(indexDao.deleteOrgRole))
      .build()

  def aggregateTags = OrgRoleEntity.Event.Tag.allTags

}
