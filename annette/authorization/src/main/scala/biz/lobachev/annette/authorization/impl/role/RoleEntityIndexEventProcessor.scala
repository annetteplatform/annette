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
import com.datastax.driver.core.BoundStatement
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}

import scala.concurrent.{ExecutionContext, Future}

private[impl] class RoleEntityIndexEventProcessor(
  readSide: CassandraReadSide,
  indexDao: RoleIndexDao
)(implicit
  ec: ExecutionContext
) extends ReadSideProcessor[RoleEntity.Event] {

  def buildHandler(): ReadSideProcessor.ReadSideHandler[RoleEntity.Event] =
    readSide
      .builder[RoleEntity.Event]("role-indexing")
      .setGlobalPrepare(indexDao.createEntityIndex)
      .setEventHandler[RoleEntity.RoleCreated](e => createRole(e.event))
      .setEventHandler[RoleEntity.RoleUpdated](e => updateRole(e.event))
      .setEventHandler[RoleEntity.RoleDeleted](e => deleteRole(e.event))
      .build()

  def aggregateTags: Set[AggregateEventTag[RoleEntity.Event]] = RoleEntity.Event.Tag.allTags

  def createRole(event: RoleEntity.RoleCreated): Future[Seq[BoundStatement]] =
    for {
      _ <- indexDao.createRole(event)
    } yield List.empty

  def updateRole(event: RoleEntity.RoleUpdated): Future[Seq[BoundStatement]] =
    for {
      _ <- indexDao.updateRole(event)
    } yield List.empty

  def deleteRole(event: RoleEntity.RoleDeleted): Future[Seq[BoundStatement]] =
    for {
      _ <- indexDao.deleteRole(event)
    } yield List.empty

}
