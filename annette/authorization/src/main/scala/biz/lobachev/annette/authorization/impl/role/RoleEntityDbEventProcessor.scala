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

import biz.lobachev.annette.authorization.impl.role.dao.RoleDbDao
import com.datastax.driver.core.BoundStatement
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}

import scala.concurrent.Future

private[impl] class RoleEntityDbEventProcessor(
  readSide: CassandraReadSide,
  dbDao: RoleDbDao
) extends ReadSideProcessor[RoleEntity.Event] {

  def buildHandler(): ReadSideProcessor.ReadSideHandler[RoleEntity.Event] =
    readSide
      .builder[RoleEntity.Event]("Authorization_Role_Db_EventOffset")
      .setGlobalPrepare(dbDao.createTables)
      .setPrepare(_ => dbDao.prepareStatements())
      .setEventHandler[RoleEntity.RoleCreated](e => createRole(e.event))
      .setEventHandler[RoleEntity.RoleUpdated](e => updateRole(e.event))
      .setEventHandler[RoleEntity.RoleDeleted](e => deleteRole(e.event))
      .setEventHandler[RoleEntity.PrincipalAssigned](e => assignPrincipal(e.event))
      .setEventHandler[RoleEntity.PrincipalUnassigned](e => unassignPrincipal(e.event))
      .build()

  def aggregateTags: Set[AggregateEventTag[RoleEntity.Event]] = RoleEntity.Event.Tag.allTags

  def createRole(event: RoleEntity.RoleCreated): Future[Seq[BoundStatement]] =
    Future.successful(dbDao.createRole(event))

  def updateRole(event: RoleEntity.RoleUpdated): Future[Seq[BoundStatement]] =
    Future.successful(dbDao.updateRole(event))

  def deleteRole(event: RoleEntity.RoleDeleted): Future[Seq[BoundStatement]] =
    Future.successful(dbDao.deleteRole(event))

  def assignPrincipal(event: RoleEntity.PrincipalAssigned): Future[Seq[BoundStatement]] =
    Future.successful(dbDao.assignPrincipal(event))

  def unassignPrincipal(event: RoleEntity.PrincipalUnassigned): Future[Seq[BoundStatement]] =
    Future.successful(dbDao.unassignPrincipal(event))

}
