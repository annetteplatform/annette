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

import biz.lobachev.annette.attributes.api.schema.SchemaAttributeId
import biz.lobachev.annette.attributes.impl.AttributeUtil
import biz.lobachev.annette.attributes.impl.index.IndexEntityService
import com.datastax.driver.core.BoundStatement
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

private[impl] class AssignmentIndexEventProcessor(
  readSide: CassandraReadSide,
  indexEntityService: IndexEntityService
)(implicit
  ec: ExecutionContext
) extends ReadSideProcessor[AssignmentEntity.Event] {

  val log = LoggerFactory.getLogger(this.getClass)

  def buildHandler(): ReadSideProcessor.ReadSideHandler[AssignmentEntity.Event] =
    readSide
      .builder[AssignmentEntity.Event]("Attributes_AssignmentIndex_EventOffset")
      .setEventHandler[AssignmentEntity.AttributeAssigned](e => onAttributeAssigned(e.event))
      .setEventHandler[AssignmentEntity.AttributeUnassigned](e => onAttributeUnassigned(e.event))
      .build()

  def aggregateTags: Set[AggregateEventTag[AssignmentEntity.Event]] = AssignmentEntity.Event.Tag.allTags

  def onAttributeAssigned(event: AssignmentEntity.AttributeAssigned): Future[Seq[BoundStatement]] =
    for {
      _ <- indexEntityService.assignIndexAttribute(
             id = SchemaAttributeId(event.id.schemaId, event.id.subSchemaId, event.id.attributeId),
             objectId = event.id.objectId,
             attribute = event.attribute,
             fieldName = AttributeUtil.fieldName(event.id.schemaId, event.id.subSchemaId, event.indexAlias.get)
           )
    } yield Seq()

  def onAttributeUnassigned(event: AssignmentEntity.AttributeUnassigned): Future[Seq[BoundStatement]] =
    for {
      _ <- indexEntityService.unassignIndexAttribute(
             id = SchemaAttributeId(event.id.schemaId, event.id.subSchemaId, event.id.attributeId),
             objectId = event.id.objectId,
             fieldName = AttributeUtil.fieldName(event.id.schemaId, event.id.subSchemaId, event.indexAlias.get)
           )
    } yield Seq()

}
