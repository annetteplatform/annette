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

package biz.lobachev.annette.attributes.impl.schema

import akka.Done
import biz.lobachev.annette.attributes.api.schema.SchemaAttributeId
import biz.lobachev.annette.attributes.impl.AttributeUtil
import biz.lobachev.annette.attributes.impl.assignment.AssignmentEntityService
import biz.lobachev.annette.attributes.impl.attribute_def.AttributeDefEntityService
import biz.lobachev.annette.attributes.impl.index.IndexEntityService
import com.datastax.driver.core.BoundStatement
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

private[impl] class SchemaIndexEventProcessor(
  readSide: CassandraReadSide,
  attributeDefEntityService: AttributeDefEntityService,
  assignmentEntityService: AssignmentEntityService,
  indexEntityService: IndexEntityService
)(implicit
  ec: ExecutionContext
) extends ReadSideProcessor[SchemaEntity.Event] {

  val log = LoggerFactory.getLogger(this.getClass)

  def buildHandler(): ReadSideProcessor.ReadSideHandler[SchemaEntity.Event] =
    readSide
      .builder[SchemaEntity.Event]("Attributes_SchemaIndex_EventOffset")
      .setEventHandler[SchemaEntity.IndexAttributeCreated](e => onIndexAttributeCreated(e.event))
      .setEventHandler[SchemaEntity.IndexAttributeRemoved](e => onIndexAttributeRemoved(e.event))
      .build()

  def aggregateTags: Set[AggregateEventTag[SchemaEntity.Event]] = SchemaEntity.Event.Tag.allTags

  def onIndexAttributeCreated(event: SchemaEntity.IndexAttributeCreated): Future[Seq[BoundStatement]] = {
    val id        = SchemaAttributeId(event.id.id, event.id.sub, event.attributeId)
    val fieldName = AttributeUtil.fieldName(event.id.id, event.id.sub, event.alias)
    for {
      attributeType <- attributeDefEntityService
                         .getAttributeDefById(event.attributeId, true)
                         .map(_.attributeType)
      _             <- indexEntityService.createIndexAttribute(
                         id = id,
                         attributeType = attributeType,
                         index = event.index.toAttributeIndex(event.attributeId),
                         fieldName = fieldName
                       )
    } yield {
      if (event.reindexAssignments)
        reindexAttributes(id, fieldName)
      Seq()
    }
  }

  def onIndexAttributeRemoved(event: SchemaEntity.IndexAttributeRemoved): Future[Seq[BoundStatement]] = {
    val id        = SchemaAttributeId(event.id.id, event.id.sub, event.attributeId)
    val fieldName = AttributeUtil.fieldName(event.id.id, event.id.sub, event.alias)
    for {
      _ <- if (event.removeAssignments)
             unassignAttributes(id, fieldName)
           else Future.unit
      _ <- indexEntityService.removeIndexAttribute(
             id = id,
             fieldName = fieldName
           )
    } yield Seq()

  }

  // TODO: reindex all attributes
  private def reindexAttributes(id: SchemaAttributeId, fieldName: String): Future[Done] =
    for {
      assignments <- assignmentEntityService.getAttributeAssignments(id)
      _            = assignments.foreach(println)
      _           <- Future.traverse(assignments.values) { assignment =>
                       indexEntityService.assignIndexAttribute(id, assignment.id.objectId, assignment.attribute, fieldName)
                     }
    } yield Done

  // TODO: remove all attributes
  private def unassignAttributes(id: SchemaAttributeId, fieldName: String): Future[Done] =
    for {
      assignments <- assignmentEntityService.getAttributeAssignments(id)
      _           <- Future.traverse(assignments.values) { assignment =>
                       indexEntityService.unassignIndexAttribute(id, assignment.id.objectId, fieldName)
                     }
    } yield Done

}
