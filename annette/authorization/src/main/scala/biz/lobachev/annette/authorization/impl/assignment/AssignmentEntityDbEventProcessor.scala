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

package biz.lobachev.annette.authorization.impl.assignment

import biz.lobachev.annette.authorization.impl.assignment.dao.AssignmentCassandraDbDao
import com.datastax.driver.core.BoundStatement
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}

import scala.concurrent.Future

private[impl] class AssignmentEntityDbEventProcessor(
  readSide: CassandraReadSide,
  dbDao: AssignmentCassandraDbDao
) extends ReadSideProcessor[AssignmentEntity.Event] {

  def buildHandler(): ReadSideProcessor.ReadSideHandler[AssignmentEntity.Event] =
    readSide
      .builder[AssignmentEntity.Event]("Authorization_Assignment_Db_EventOffset")
      .setGlobalPrepare(dbDao.createTables)
      .setPrepare(_ => dbDao.prepareStatements())
      .setEventHandler[AssignmentEntity.PermissionAssigned](e => assignPermission(e.event))
      .setEventHandler[AssignmentEntity.PermissionUnassigned](e => unassignPermission(e.event))
      .build()

  def aggregateTags: Set[AggregateEventTag[AssignmentEntity.Event]] = AssignmentEntity.Event.Tag.allTags

  def assignPermission(event: AssignmentEntity.PermissionAssigned): Future[Seq[BoundStatement]] =
    Future.successful(
      List(
        dbDao.assignPermission(event)
      )
    )

  def unassignPermission(event: AssignmentEntity.PermissionUnassigned): Future[Seq[BoundStatement]] =
    Future.successful(
      List(
        dbDao.unassignPermission(event)
      )
    )

}
