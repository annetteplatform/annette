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

package biz.lobachev.annette.attributes.api

import akka.{Done, NotUsed}
import biz.lobachev.annette.attributes.api.assignment._
import biz.lobachev.annette.attributes.api.index.IndexEvent
import biz.lobachev.annette.attributes.api.schema._
import biz.lobachev.annette.core.exception.AnnetteTransportExceptionSerializer
import biz.lobachev.annette.core.model.elastic.FindResult
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.broker.kafka.{KafkaProperties, PartitionKeyStrategy}
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}
import com.typesafe.config.ConfigFactory

import scala.collection.immutable.Map

trait AttributeServiceApi extends Service {

  final val indexTopicId: String = {
    val config = ConfigFactory.load()
    config.getString("annette.attributes-service.indexTopic")
  }

  def createSchema: ServiceCall[CreateSchemaPayload, Done]
  def updateSchema: ServiceCall[UpdateSchemaPayload, Done]
  def activateSchema: ServiceCall[ActivateSchemaPayload, Done]
  def deleteSchema: ServiceCall[DeleteSchemaPayload, Done]
  def getSchemaById(id: ComposedSchemaId, fromReadSide: Boolean = true): ServiceCall[NotUsed, Schema]
  def getSchemasById(fromReadSide: Boolean = true): ServiceCall[Set[ComposedSchemaId], Map[ComposedSchemaId, Schema]]
  def findSchemas: ServiceCall[FindSchemaQuery, FindResult]

  def assignAttribute: ServiceCall[AssignAttributePayload, Done]
  def unassignAttribute: ServiceCall[UnassignAttributePayload, Done]
  def getAssignmentById(
    id: ComposedAssignmentId,
    fromReadSide: Boolean = true
  ): ServiceCall[NotUsed, AttributeAssignment]
  def getAssignmentsById(
    fromReadSide: Boolean = true
  ): ServiceCall[Set[ComposedAssignmentId], Map[ComposedAssignmentId, AttributeAssignment]]
  def getObjectAssignments(
    id: ComposedAssignmentId
  ): ServiceCall[NotUsed, Map[ComposedAssignmentId, AttributeAssignment]]

  def indexTopic: Topic[IndexEvent]

  final override def descriptor = {
    import Service._
    // @formatter:off
    named("attributes")
      .withCalls(
        pathCall("/api/attributes/v1/createSchema",                    createSchema),
        pathCall("/api/attributes/v1/updateSchema",                    updateSchema),
        pathCall("/api/attributes/v1/activateSchema",                  activateSchema),
        pathCall("/api/attributes/v1/deleteSchema",                    deleteSchema),
        pathCall("/api/attributes/v1/getSchemaById/:id/:fromReadSide", getSchemaById _),
        pathCall("/api/attributes/v1/getSchemasById/:fromReadSide",    getSchemasById _),
        pathCall("/api/attributes/v1/findSchemas",                     findSchemas ),

        pathCall("/api/attributes/v1/assignAttribute",                     assignAttribute ),
        pathCall("/api/attributes/v1/unassignAttribute",                   unassignAttribute ),
        pathCall("/api/attributes/v1/getAssignmentById/:id/:fromReadSide", getAssignmentById _),
        pathCall("/api/attributes/v1/getAssignmentsById/:fromReadSide",    getAssignmentsById _),
        pathCall("/api/attributes/v1/getObjectAssignments/:id",            getObjectAssignments _)
      )
      .withExceptionSerializer(new AnnetteTransportExceptionSerializer())
      .withTopics(
        topic(indexTopicId,
            indexTopic)
          .addProperty(
            KafkaProperties.partitionKeyStrategy,
            PartitionKeyStrategy[IndexEvent](_.id.toComposed)
          )
      )
      .withAutoAcl(true)
    // @formatter:on
  }
}
