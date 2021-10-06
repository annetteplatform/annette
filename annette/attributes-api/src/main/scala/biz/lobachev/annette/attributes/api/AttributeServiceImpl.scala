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
import akka.Done
import biz.lobachev.annette.attributes.api.assignment.{
  AssignAttributePayload,
  AttributeAssignment,
  ComposedAssignmentId,
  UnassignAttributePayload
}
import biz.lobachev.annette.attributes.api.schema._
import biz.lobachev.annette.core.model.indexing.FindResult
import io.scalaland.chimney.dsl._

import scala.concurrent.{ExecutionContext, Future}

class AttributeServiceImpl(api: AttributeServiceApi, implicit val ec: ExecutionContext) extends AttributeService {
  override def createSchema(payload: CreateSchemaPayload): Future[Done] = api.createSchema.invoke(payload)

  override def updateSchema(payload: UpdateSchemaPayload): Future[Done] = api.updateSchema.invoke(payload)

  def createOrUpdateSchema(payload: CreateSchemaPayload): Future[Done] =
    createSchema(payload).recoverWith {
      case SchemaAlreadyExist(_) =>
        val updatePayload = payload
          .into[UpdateSchemaPayload]
          .transform
        updateSchema(updatePayload)
      case th                    => Future.failed(th)
    }

  override def activateSchema(payload: ActivateSchemaPayload): Future[Done] = api.activateSchema.invoke(payload)

  override def deleteSchema(payload: DeleteSchemaPayload): Future[Done] = api.deleteSchema.invoke(payload)

  override def getSchemaById(id: ComposedSchemaId, fromReadSide: Boolean): Future[Schema] =
    api.getSchemaById(id, fromReadSide).invoke()

  override def getSchemasById(
    fromReadSide: Boolean,
    ids: Set[ComposedSchemaId]
  ): Future[Map[ComposedSchemaId, Schema]] = api.getSchemasById(fromReadSide).invoke(ids)

  override def findSchemas(payload: FindSchemaQuery): Future[FindResult] = api.findSchemas.invoke(payload)

  override def assignAttribute(payload: AssignAttributePayload): Future[Done] = api.assignAttribute.invoke(payload)

  override def unassignAttribute(payload: UnassignAttributePayload): Future[Done] =
    api.unassignAttribute.invoke(payload)

  override def getAssignmentById(id: ComposedAssignmentId, fromReadSide: Boolean): Future[AttributeAssignment] =
    api.getAssignmentById(id, fromReadSide).invoke()

  override def getAssignmentsById(
    fromReadSide: Boolean,
    ids: Set[ComposedAssignmentId]
  ): Future[Map[ComposedAssignmentId, AttributeAssignment]] = api.getAssignmentsById(fromReadSide).invoke(ids)

  override def getObjectAssignments(id: ComposedAssignmentId): Future[Map[ComposedAssignmentId, AttributeAssignment]] =
    api.getObjectAssignments(id).invoke()
}
