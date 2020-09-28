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

private[impl] class SchemaCasEventProcessor(
  readSide: CassandraReadSide,
  casRepository: SchemaCasRepository
)(implicit
  ec: ExecutionContext
) extends ReadSideProcessor[SchemaEntity.Event] {

  val log = LoggerFactory.getLogger(this.getClass)

  def buildHandler(): ReadSideProcessor.ReadSideHandler[SchemaEntity.Event] =
    readSide
      .builder[SchemaEntity.Event]("Attributes_Schema_CasEventOffset")
      .setGlobalPrepare(globalPrepare)
      .setPrepare(_ => casRepository.prepareStatements())
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
    casRepository
      .createTables()
      .map(_ => Done)

  def createSchema(event: SchemaEntity.SchemaCreated): Future[Seq[BoundStatement]] =
    Future.successful(
      casRepository.createSchema(event)
    )

  def updateSchemaName(event: SchemaEntity.SchemaNameUpdated): Future[Seq[BoundStatement]] =
    Future.successful(
      casRepository.updateSchemaName(event)
    )

  def updateActiveAttributeName(event: SchemaEntity.ActiveAttributeNameUpdated): Future[Seq[BoundStatement]] =
    Future.successful(
      casRepository.updateActiveAttributeName(event)
    )
  def createActiveAttribute(event: SchemaEntity.ActiveAttributeCreated): Future[Seq[BoundStatement]]         =
    Future.successful(
      casRepository.createActiveAttribute(event)
    )

  def updateActiveAttribute(event: SchemaEntity.ActiveAttributeUpdated): Future[Seq[BoundStatement]] =
    Future.successful(
      casRepository.updateActiveAttribute(event)
    )

  def removeActiveAttribute(event: SchemaEntity.ActiveAttributeRemoved): Future[Seq[BoundStatement]] =
    Future.successful(
      casRepository.removeActiveAttribute(event)
    )

  def createPreparedAttribute(event: SchemaEntity.PreparedAttributeCreated): Future[Seq[BoundStatement]] =
    Future.successful(
      casRepository.createPreparedAttribute(event)
    )

  def updatePreparedAttribute(event: SchemaEntity.PreparedAttributeUpdated): Future[Seq[BoundStatement]] =
    Future.successful(
      casRepository.updatePreparedAttribute(event)
    )

  def removePreparedAttribute(event: SchemaEntity.PreparedAttributeRemoved): Future[Seq[BoundStatement]] =
    Future.successful(
      casRepository.removePreparedAttribute(event)
    )
  def deleteSchema(event: SchemaEntity.SchemaDeleted): Future[Seq[BoundStatement]]                       =
    Future.successful(
      casRepository.deleteSchema(event)
    )
}
