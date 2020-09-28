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

package biz.lobachev.annette.attributes.impl.assignment

import akka.Done
import com.datastax.driver.core.BoundStatement
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

private[impl] class AssignmentEntityEventProcessor(
  readSide: CassandraReadSide,
  repository: AssignmentRepository
)(implicit
  ec: ExecutionContext
) extends ReadSideProcessor[AssignmentEntity.Event] {

  val log = LoggerFactory.getLogger(this.getClass)

  def buildHandler(): ReadSideProcessor.ReadSideHandler[AssignmentEntity.Event] =
    readSide
      .builder[AssignmentEntity.Event]("Attributes_Assignment_EventOffset")
      .setGlobalPrepare(globalPrepare)
      .setPrepare(_ => repository.prepareStatements())
      .setEventHandler[AssignmentEntity.AttributeAssigned](e => onAttributeAssigned(e.event))
      .setEventHandler[AssignmentEntity.AttributeUnassigned](e => onAttributeUnassigned(e.event))
      .build()

  def aggregateTags: Set[AggregateEventTag[AssignmentEntity.Event]] = AssignmentEntity.Event.Tag.allTags

  def globalPrepare(): Future[Done] =
    for {
      _ <- repository.createTables()
    } yield Done

  def onAttributeAssigned(event: AssignmentEntity.AttributeAssigned): Future[Seq[BoundStatement]] =
    Future.successful(
      repository.onAttributeAssigned(event)
    )

  def onAttributeUnassigned(event: AssignmentEntity.AttributeUnassigned): Future[Seq[BoundStatement]] =
    Future.successful(
      repository.onAttributeUnassigned(event)
    )

}
