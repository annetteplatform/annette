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

import biz.lobachev.annette.authorization.impl.assignment.dao.{AssignmentIndexDao}
import com.datastax.driver.core.BoundStatement
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}

import scala.concurrent.{ExecutionContext, Future}

private[impl] class AssignmentEntityIndexEventProcessor(
  readSide: CassandraReadSide,
  indexDao: AssignmentIndexDao
)(implicit
  ec: ExecutionContext
) extends ReadSideProcessor[AssignmentEntity.Event] {

  def buildHandler(): ReadSideProcessor.ReadSideHandler[AssignmentEntity.Event] =
    readSide
      .builder[AssignmentEntity.Event]("assignment-indexing")
      .setGlobalPrepare(indexDao.createEntityIndex)
      .setEventHandler[AssignmentEntity.PermissionAssigned](e => assignPermission(e.event))
      .setEventHandler[AssignmentEntity.PermissionUnassigned](e => unassignPermission(e.event))
      .build()

  def aggregateTags: Set[AggregateEventTag[AssignmentEntity.Event]] = AssignmentEntity.Event.Tag.allTags

  def assignPermission(event: AssignmentEntity.PermissionAssigned): Future[Seq[BoundStatement]] =
    for {
      _ <- indexDao.assignPermission(event)
    } yield List.empty

  def unassignPermission(event: AssignmentEntity.PermissionUnassigned): Future[Seq[BoundStatement]] =
    for {
      _ <- indexDao.unassignPermission(event)
    } yield List.empty

}
