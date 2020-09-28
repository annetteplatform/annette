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

import biz.lobachev.annette.org_structure.impl.role.OrgRoleEntity._
import biz.lobachev.annette.org_structure.impl.role.dao.OrgRoleDbDao
import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide

import scala.concurrent.Future

private[impl] class OrgRoleDbEventProcessor(
  readSide: CassandraReadSide,
  dbDao: OrgRoleDbDao
) extends ReadSideProcessor[OrgRoleEntity.Event] {

  def buildHandler() =
    readSide
      .builder[OrgRoleEntity.Event]("OrgStructure_OrgRole_CasEventOffset")
      .setGlobalPrepare(dbDao.createTables)
      .setPrepare(_ => dbDao.prepareStatements())
      .setEventHandler[OrgRoleEntity.OrgRoleCreated](e => createOrgRole(e.event))
      .setEventHandler[OrgRoleEntity.OrgRoleUpdated](e => updateOrgRole(e.event))
      .setEventHandler[OrgRoleEntity.OrgRoleDeleted](e => deleteOrgRole(e.event))
      .build()

  def aggregateTags = OrgRoleEntity.Event.Tag.allTags

  private def createOrgRole(event: OrgRoleCreated) =
    Future.successful(dbDao.createOrgRole(event))

  private def updateOrgRole(event: OrgRoleUpdated) =
    Future.successful(dbDao.updateOrgRole(event))

  private def deleteOrgRole(event: OrgRoleDeleted) =
    Future.successful(dbDao.deleteOrgRole(event))

}
