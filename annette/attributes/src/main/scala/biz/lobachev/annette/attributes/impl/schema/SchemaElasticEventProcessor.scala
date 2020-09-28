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
import com.datastax.driver.core.BoundStatement
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraReadSide
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

private[impl] class SchemaElasticEventProcessor(
  readSide: CassandraReadSide,
  elasticRepository: SchemaElasticIndexDao
)(implicit
  ec: ExecutionContext
) extends ReadSideProcessor[SchemaEntity.Event] {

  val log = LoggerFactory.getLogger(this.getClass)

  def buildHandler(): ReadSideProcessor.ReadSideHandler[SchemaEntity.Event] =
    readSide
      .builder[SchemaEntity.Event]("Attributes_Schema_ElasticEventOffset")
      .setGlobalPrepare(globalPrepare)
      .setEventHandler[SchemaEntity.SchemaCreated](e => createSchema(e.event))
      .setEventHandler[SchemaEntity.SchemaNameUpdated](e => updateSchemaName(e.event))
      .setEventHandler[SchemaEntity.ActiveAttributeNameUpdated](e => updateActiveAttributeName(e.event))
      .setEventHandler[SchemaEntity.ActiveAttributeCreated](e => createActiveAttribute(e.event))
      .setEventHandler[SchemaEntity.ActiveAttributeUpdated](e => updateActiveAttribute(e.event))
      .setEventHandler[SchemaEntity.ActiveAttributeRemoved](e => removeActiveAttribute(e.event))
      .setEventHandler[SchemaEntity.PreparedAttributeCreated](e => createPreparedAttribute(e.event))
      .setEventHandler[SchemaEntity.PreparedAttributeUpdated](e => updatePreparedAttribute(e.event))
      .setEventHandler[SchemaEntity.PreparedAttributeRemoved](e => removePreparedAttribute(e.event))
      .setEventHandler[SchemaEntity.SchemaDeleted](e => deleteSchema(e.event))
      .build()

  def aggregateTags: Set[AggregateEventTag[SchemaEntity.Event]] = SchemaEntity.Event.Tag.allTags

  def globalPrepare(): Future[Done] =
    elasticRepository
      .createEntityIndex()
      .map(_ => Done)

  def createSchema(event: SchemaEntity.SchemaCreated): Future[Seq[BoundStatement]] =
    elasticRepository
      .createSchema(event)
      .map(_ => Seq.empty)

  def updateSchemaName(event: SchemaEntity.SchemaNameUpdated): Future[Seq[BoundStatement]] =
    elasticRepository
      .updateSchemaName(event)
      .map(_ => Seq.empty)

  def updateActiveAttributeName(event: SchemaEntity.ActiveAttributeNameUpdated): Future[Seq[BoundStatement]] =
    elasticRepository
      .updateActiveAttributeName(event)
      .map(_ => Seq.empty)

  def createActiveAttribute(event: SchemaEntity.ActiveAttributeCreated): Future[Seq[BoundStatement]] =
    elasticRepository
      .createActiveAttribute(event)
      .map(_ => Seq.empty)

  def updateActiveAttribute(event: SchemaEntity.ActiveAttributeUpdated): Future[Seq[BoundStatement]] =
    elasticRepository
      .updateActiveAttribute(event)
      .map(_ => Seq.empty)

  def removeActiveAttribute(event: SchemaEntity.ActiveAttributeRemoved): Future[Seq[BoundStatement]] =
    elasticRepository
      .removeActiveAttribute(event)
      .map(_ => Seq.empty)

  def createPreparedAttribute(event: SchemaEntity.PreparedAttributeCreated): Future[Seq[BoundStatement]] =
    elasticRepository
      .createPreparedAttribute(event)
      .map(_ => Seq.empty)

  def updatePreparedAttribute(event: SchemaEntity.PreparedAttributeUpdated): Future[Seq[BoundStatement]] =
    elasticRepository
      .updatePreparedAttribute(event)
      .map(_ => Seq.empty)

  def removePreparedAttribute(event: SchemaEntity.PreparedAttributeRemoved): Future[Seq[BoundStatement]] =
    elasticRepository
      .removePreparedAttribute(event)
      .map(_ => Seq.empty)

  def deleteSchema(event: SchemaEntity.SchemaDeleted): Future[Seq[BoundStatement]] =
    elasticRepository
      .deleteSchema(event)
      .map(_ => Seq.empty)

}
