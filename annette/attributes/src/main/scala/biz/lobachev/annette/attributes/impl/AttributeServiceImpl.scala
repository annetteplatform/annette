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

package biz.lobachev.annette.attributes.impl

import akka.util.Timeout
import akka.{Done, NotUsed}
import biz.lobachev.annette.attributes.api.AttributeService
import biz.lobachev.annette.attributes.api.assignment._
import biz.lobachev.annette.attributes.api.index._
import biz.lobachev.annette.attributes.api.schema._
import biz.lobachev.annette.attributes.impl.assignment.AssignmentEntityService
import biz.lobachev.annette.attributes.impl.index.IndexEntity
import biz.lobachev.annette.attributes.impl.schema.SchemaEntityService
import biz.lobachev.annette.core.elastic.FindResult
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRegistry}
import com.typesafe.config.Config
import io.scalaland.chimney.dsl._
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Try

class AttributeServiceImpl(
  schemaEntityService: SchemaEntityService,
  assignmentEntityService: AssignmentEntityService,
  persistentEntityRegistry: PersistentEntityRegistry,
  config: Config
)(implicit
  ec: ExecutionContext
) extends AttributeService {
  implicit val timeout = Try(config.getDuration("annette.timeout")).getOrElse(Timeout(60.seconds))

  val log = LoggerFactory.getLogger(this.getClass)

  override def createSchema: ServiceCall[CreateSchemaPayload, Done] =
    ServiceCall { payload =>
      schemaEntityService.createSchema(payload)
    }

  override def updateSchema: ServiceCall[UpdateSchemaPayload, Done] =
    ServiceCall { payload =>
      schemaEntityService.updateSchema(payload)
    }

  override def activateSchema: ServiceCall[ActivateSchemaPayload, Done] =
    ServiceCall { payload =>
      for {
        activeAttributes         <- schemaEntityService.getSchemaAttributes(payload.id)
        attributesWithAssignment <- assignmentEntityService
                                      .getAttributesWithAssignment(payload.id, activeAttributes.map(_.attributeId))
        res                      <- schemaEntityService.activateSchema(payload, attributesWithAssignment)
      } yield res
    }

  override def deleteSchema: ServiceCall[DeleteSchemaPayload, Done] =
    ServiceCall { payload =>
      for {
        activeAttributes         <- schemaEntityService.getSchemaAttributes(payload.id)
        attributesWithAssignment <- assignmentEntityService
                                      .getAttributesWithAssignment(payload.id, activeAttributes.map(_.attributeId))
        _                         =
          if (attributesWithAssignment.nonEmpty) throw AttributesHasAssignments(attributesWithAssignment.mkString(", "))
        res                      <- schemaEntityService.deleteSchema(payload)
      } yield res
    }

  override def getSchemaById(composedSchemaId: ComposedSchemaId, fromReadSide: Boolean): ServiceCall[NotUsed, Schema] =
    ServiceCall { _ =>
      schemaEntityService.getSchemaById(composedSchemaId, fromReadSide)
    }

  override def getSchemasById(
    fromReadSide: Boolean
  ): ServiceCall[Set[ComposedSchemaId], Map[ComposedSchemaId, Schema]] =
    ServiceCall { payload =>
      schemaEntityService.getSchemasById(payload, fromReadSide)
    }

  override def findSchemas: ServiceCall[FindSchemaQuery, FindResult] =
    ServiceCall { query =>
      schemaEntityService.findSchemas(query)
    }

  override def assignAttribute: ServiceCall[AssignAttributePayload, Done] =
    ServiceCall { payload =>
      for {
        // get schema attribute
        schemaAttribute <- schemaEntityService
                             .getSchemaAttribute(
                               schemaId = SchemaId(payload.id.schemaId, payload.id.subSchemaId),
                               attributeId = payload.id.attributeId,
                               readSide = true
                             )
                             .map(_.getOrElse(throw AttributeNotFound()))
        // assign attribute
        result          <- assignmentEntityService.assignAttribute(payload, schemaAttribute)
      } yield result
    }

  override def unassignAttribute: ServiceCall[UnassignAttributePayload, Done] =
    ServiceCall { payload =>
      for {
        // get schema attribute
        schemaAttribute <-
          schemaEntityService
            .getSchemaAttribute(SchemaId(payload.id.schemaId, payload.id.subSchemaId), payload.id.attributeId, true)
            .map(_.getOrElse(throw AttributeNotFound()))
        // unassign attribute
        result          <- assignmentEntityService.unassignAttribute(payload, schemaAttribute)
      } yield result
    }

  override def getAssignmentById(
    composedId: ComposedAssignmentId,
    fromReadSide: Boolean
  ): ServiceCall[NotUsed, AttributeAssignment] =
    ServiceCall { _ =>
      assignmentEntityService.getAssignmentById(composedId, fromReadSide)
    }

  override def getAssignmentsById(
    fromReadSide: Boolean
  ): ServiceCall[Set[ComposedAssignmentId], Map[ComposedAssignmentId, AttributeAssignment]] =
    ServiceCall { composedIds =>
      assignmentEntityService.getAssignmentsById(composedIds, fromReadSide)
    }

  override def getObjectAssignments(
    composedId: ComposedAssignmentId
  ): ServiceCall[NotUsed, Map[ComposedAssignmentId, AttributeAssignment]] =
    ServiceCall { _ =>
      assignmentEntityService.getObjectAssignments(composedId)
    }

  override def indexTopic: Topic[IndexEvent] =
    TopicProducer.taggedStreamWithOffset(IndexEntity.Event.Tag) { (tag, fromOffset) =>
      persistentEntityRegistry
        .eventStream(tag, fromOffset)
        .map {
          case EventStreamElement(_, event, offset) =>
            event match {
              case event: IndexEntity.IndexAttributeCreated    =>
                val message = event.transformInto[IndexAttributeCreated]
                message -> offset
              case event: IndexEntity.IndexAttributeRemoved    =>
                val message = event.transformInto[IndexAttributeRemoved]
                message -> offset
              case event: IndexEntity.IndexAttributeAssigned   =>
                val message = event.transformInto[IndexAttributeAssigned]
                message -> offset
              case event: IndexEntity.IndexAttributeUnassigned =>
                val message = event.transformInto[IndexAttributeUnassigned]
                message -> offset
            }
        }
    }

}
