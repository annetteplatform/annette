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

import scala.collection.immutable.Map
import scala.concurrent.Future

trait AttributeService {

  def createSchema(payload: CreateSchemaPayload): Future[Done]
  def updateSchema(payload: UpdateSchemaPayload): Future[Done]
  def createOrUpdateSchema(payload: CreateSchemaPayload): Future[Done]
  def activateSchema(payload: ActivateSchemaPayload): Future[Done]
  def deleteSchema(payload: DeleteSchemaPayload): Future[Done]
  def getSchemaById(id: ComposedSchemaId, fromReadSide: Boolean = true): Future[Schema]
  def getSchemasById(fromReadSide: Boolean = true, ids: Set[ComposedSchemaId]): Future[Map[ComposedSchemaId, Schema]]
  def findSchemas(payload: FindSchemaQuery): Future[FindResult]

  def assignAttribute(payload: AssignAttributePayload): Future[Done]
  def unassignAttribute(payload: UnassignAttributePayload): Future[Done]
  def getAssignmentById(
    id: ComposedAssignmentId,
    fromReadSide: Boolean = true
  ): Future[AttributeAssignment]
  def getAssignmentsById(
    fromReadSide: Boolean = true,
    ids: Set[ComposedAssignmentId]
  ): Future[Map[ComposedAssignmentId, AttributeAssignment]]
  def getObjectAssignments(
    id: ComposedAssignmentId
  ): Future[Map[ComposedAssignmentId, AttributeAssignment]]

}
