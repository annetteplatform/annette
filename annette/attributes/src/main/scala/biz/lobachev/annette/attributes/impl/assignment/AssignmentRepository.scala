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

import java.time.OffsetDateTime

import akka.Done
import biz.lobachev.annette.attributes.api.assignment._
import biz.lobachev.annette.attributes.api.attribute_def.AttributeId
import biz.lobachev.annette.attributes.api.schema.{SchemaAttributeId, SchemaId}
import biz.lobachev.annette.core.model.AnnettePrincipal
import com.datastax.driver.core.{BoundStatement, PreparedStatement, Row}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import org.slf4j.LoggerFactory
import play.api.libs.json.Json

import scala.collection.immutable._
import scala.concurrent.{ExecutionContext, Future}

private[impl] class AssignmentRepository(session: CassandraSession)(implicit ec: ExecutionContext) {

  val log = LoggerFactory.getLogger(this.getClass)

  private var onAttributeAssignedByObjStatement: PreparedStatement    = _
  private var onAttributeUnassignedByObjStatement: PreparedStatement  = _
  private var onAttributeAssignedByAttrStatement: PreparedStatement   = _
  private var onAttributeUnassignedByAttrStatement: PreparedStatement = _

  def createTables(): Future[Unit] =
    for {
      _ <- session.executeCreateTable(
             """
               |CREATE TABLE IF NOT EXISTS assignments_by_obj (
               |          schema_id         text,
               |          sub_schema_id     text,
               |          object_id         text,
               |          attribute_id      text,
               |          attribute         text,
               |          updated_at        text,
               |          updated_by_type   text,
               |          updated_by_id     text,
               |          PRIMARY KEY ( (schema_id, sub_schema_id), object_id, attribute_id)
               |)
               |""".stripMargin
           )
      _ <- session.executeCreateTable(
             """
               |CREATE TABLE IF NOT EXISTS assignments_by_attr (
               |          schema_id         text,
               |          sub_schema_id     text,
               |          attribute_id      text,
               |          object_id         text,
               |          attribute         text,
               |          updated_at        text,
               |          updated_by_type   text,
               |          updated_by_id     text,
               |          PRIMARY KEY ( (schema_id, sub_schema_id),  attribute_id, object_id)
               |)
               |""".stripMargin
           )
    } yield ()

  def prepareStatements(): Future[Done] =
    for {
      onAttributeAssignedByObjStmt    <- session.prepare(
                                           """
                                          | INSERT INTO assignments_by_obj (
                                          |    schema_id       ,
                                          |    sub_schema_id   ,
                                          |    object_id       ,
                                          |    attribute_id    ,
                                          |    attribute       ,
                                          |    updated_at      ,
                                          |    updated_by_type ,
                                          |    updated_by_id
                                          |   )
                                          | VALUES (
                                          |    :schema_id       ,
                                          |    :sub_schema_id   ,
                                          |    :object_id       ,
                                          |    :attribute_id    ,
                                          |    :attribute       ,
                                          |    :updated_at      ,
                                          |    :updated_by_type ,
                                          |    :updated_by_id
                                          |   )
                                          |""".stripMargin
                                         )
      onAttributeAssignedByAttrStmt   <- session.prepare(
                                           """
                                           | INSERT INTO assignments_by_attr (
                                           |    schema_id       ,
                                           |    sub_schema_id   ,
                                           |    attribute_id    ,
                                           |    object_id       ,
                                           |    attribute       ,
                                           |    updated_at      ,
                                           |    updated_by_type ,
                                           |    updated_by_id
                                           |   )
                                           | VALUES (
                                           |    :schema_id       ,
                                           |    :sub_schema_id   ,
                                           |    :attribute_id    ,
                                           |    :object_id       ,
                                           |    :attribute       ,
                                           |    :updated_at      ,
                                           |    :updated_by_type ,
                                           |    :updated_by_id
                                           |   )
                                           |""".stripMargin
                                         )
      onAttributeUnassignedByObjStmt  <- session.prepare(
                                           """
                                            | DELETE FROM assignments_by_obj
                                            | WHERE  schema_id     = :schema_id       AND
                                            |        sub_schema_id = :sub_schema_id   AND
                                            |        object_id     = :object_id       AND
                                            |        attribute_id  = :attribute_id    
                                            |""".stripMargin
                                         )
      onAttributeUnassignedByAttrStmt <- session.prepare(
                                           """
                                             | DELETE FROM assignments_by_attr
                                             | WHERE  schema_id     = :schema_id       AND
                                             |        sub_schema_id = :sub_schema_id   AND
                                             |        attribute_id  = :attribute_id    AND
                                             |        object_id     = :object_id       
                                             |""".stripMargin
                                         )
    } yield {
      onAttributeAssignedByObjStatement = onAttributeAssignedByObjStmt
      onAttributeUnassignedByObjStatement = onAttributeUnassignedByObjStmt
      onAttributeAssignedByAttrStatement = onAttributeAssignedByAttrStmt
      onAttributeUnassignedByAttrStatement = onAttributeUnassignedByAttrStmt
      Done
    }

  def onAttributeAssigned(event: AssignmentEntity.AttributeAssigned): List[BoundStatement]     =
    List(
      onAttributeAssignedByObjStatement
        .bind()
        .setString("schema_id", event.id.schemaId)
        .setString("sub_schema_id", event.id.subSchemaId.getOrElse(""))
        .setString("object_id", event.id.objectId)
        .setString("attribute_id", event.id.attributeId)
        .setString("attribute", Json.toJson(event.attribute).toString)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId),
      onAttributeAssignedByAttrStatement
        .bind()
        .setString("schema_id", event.id.schemaId)
        .setString("sub_schema_id", event.id.subSchemaId.getOrElse(""))
        .setString("object_id", event.id.objectId)
        .setString("attribute_id", event.id.attributeId)
        .setString("attribute", Json.toJson(event.attribute).toString)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )
  def onAttributeUnassigned(event: AssignmentEntity.AttributeUnassigned): List[BoundStatement] =
    List(
      onAttributeUnassignedByObjStatement
        .bind()
        .setString("schema_id", event.id.schemaId)
        .setString("sub_schema_id", event.id.subSchemaId.getOrElse(""))
        .setString("object_id", event.id.objectId)
        .setString("attribute_id", event.id.attributeId),
      onAttributeUnassignedByAttrStatement
        .bind()
        .setString("schema_id", event.id.schemaId)
        .setString("sub_schema_id", event.id.subSchemaId.getOrElse(""))
        .setString("object_id", event.id.objectId)
        .setString("attribute_id", event.id.attributeId)
    )

  def getSchemaAttributeAssignmentIds(
    schemaId: SchemaId,
    attributeId: AttributeId
  ): Future[Seq[AttributeAssignmentId]] =
    for {
      assignmentStmt <- session
                          .prepare("""
                                     | SELECT schema_id,  sub_schema_id, object_id, attribute_id FROM assignment_by_obj
                                     |   WHERE  schema_id     = :schema_id       AND
                                     |          sub_schema_id = : sub_schema_id AND
                                     |          object_id = : object_id  """.stripMargin)
      assignment     <- session
                          .selectAll(
                            assignmentStmt
                              .bind()
                              .setString("schema_id", schemaId.id)
                              .setString("sub_schema_id", schemaId.sub.getOrElse(""))
                              .setString("attribute_id", attributeId)
                          )
                          .map(_.map(convertAttributeAssignmentId))
    } yield assignment

  def getObjectAssignments(id: ObjectAssignmentsId): Future[Map[ComposedAssignmentId, AttributeAssignment]] =
    for {
      assignmentStmt <- session
                          .prepare("""
                                     | SELECT * FROM assignments_by_obj
                                     |   WHERE  schema_id     = :schema_id       AND
                                     |          sub_schema_id = :sub_schema_id AND
                                     |          object_id = :object_id  """.stripMargin)
      assignment     <- session
                          .selectAll(
                            assignmentStmt
                              .bind()
                              .setString("schema_id", id.schemaId)
                              .setString("sub_schema_id", id.subSchemaId.getOrElse(""))
                              .setString("object_id", id.objectId)
                          )
                          .map(_.map { row =>
                            val assignment = convertAssignment(row)
                            assignment.id.toComposed -> assignment
                          }.toMap)
    } yield assignment

  def getAttributeAssignments(id: SchemaAttributeId): Future[Map[ComposedAssignmentId, AttributeAssignment]] =
    for {
      assignmentStmt <- session
                          .prepare("""
                                     | SELECT * FROM assignments_by_attr
                                     |   WHERE  schema_id     = :schema_id       AND
                                     |          sub_schema_id = :sub_schema_id AND
                                     |          attribute_id = :attribute_id  """.stripMargin)
      assignment     <- session
                          .selectAll(
                            assignmentStmt
                              .bind()
                              .setString("schema_id", id.schemaId)
                              .setString("sub_schema_id", id.subSchemaId.getOrElse(""))
                              .setString("attribute_id", id.attributeId)
                          )
                          .map(_.map { row =>
                            val assignment = convertAssignment(row)
                            assignment.id.toComposed -> assignment
                          }.toMap)
    } yield assignment

  def getAssignmentsById(ids: Set[AttributeAssignmentId]): Future[Map[ComposedAssignmentId, AttributeAssignment]] =
    Future
      .traverse(ids) { id =>
        getAssignmentById(id)
      }
      .map(_.flatten.map(assignment => assignment.id.toComposed -> assignment).toMap)

  def getAssignmentById(id: AttributeAssignmentId): Future[Option[AttributeAssignment]]                           =
    for {
      assignmentStmt <- session
                          .prepare("""
                                     | SELECT * FROM assignments_by_obj  
                                     |   WHERE  schema_id     = :schema_id      AND
                                     |          sub_schema_id = :sub_schema_id AND
                                     |          object_id = :object_id AND
                                     |          attribute_id = :attribute_id """.stripMargin)
      assignment     <- session
                          .selectOne(
                            assignmentStmt
                              .bind()
                              .setString("schema_id", id.schemaId)
                              .setString("sub_schema_id", id.subSchemaId.getOrElse(""))
                              .setString("object_id", id.objectId)
                              .setString("attribute_id", id.attributeId)
                          )
                          .map(_.map(convertAssignment))
    } yield assignment

  def getAttributesWithAssignment(schemaId: SchemaId, attributeIds: Seq[AttributeId]): Future[Set[AttributeId]] =
    Future
      .traverse(attributeIds) { attributeId =>
        isAssignmentExist(schemaId, attributeId).map {
          case true  => Some(attributeId)
          case false => None
        }
      }
      .map(_.flatten.toSet)

  def isAssignmentExist(schemaId: SchemaId, attributeId: AttributeId): Future[Boolean] =
    for {
      assignmentStmt <- session.prepare(
                          """
                            | SELECT attribute_id FROM assignments_by_attr
                            |   WHERE  schema_id     = :schema_id       AND
                            |          sub_schema_id = :sub_schema_id AND
                            |          attribute_id = :attribute_id  
                            |   LIMIT 1 """.stripMargin
                        )
      assignment     <- session
                          .selectOne(
                            assignmentStmt
                              .bind()
                              .setString("schema_id", schemaId.id)
                              .setString("sub_schema_id", schemaId.sub.getOrElse(""))
                              .setString("attribute_id", attributeId)
                          )
                          .map(_.isDefined)
    } yield assignment

  private def convertAttributeAssignmentId(row: Row): AttributeAssignmentId = {
    val subSchemaId = row.getString("sub_schema_id")
    AttributeAssignmentId(
      schemaId = row.getString("schema_id"),
      subSchemaId = if (subSchemaId.isEmpty) None else Some(subSchemaId),
      objectId = row.getString("object_id"),
      attributeId = row.getString("attribute_id")
    )
  }

  private def convertAssignment(row: Row): AttributeAssignment = {
    val subSchemaId = row.getString("sub_schema_id")
    AttributeAssignment(
      id = AttributeAssignmentId(
        schemaId = row.getString("schema_id"),
        subSchemaId = if (subSchemaId.isEmpty) None else Some(subSchemaId),
        objectId = row.getString("object_id"),
        attributeId = row.getString("attribute_id")
      ),
      attribute = Json.parse(row.getString("attribute")).as[Attribute],
      updatedAt = OffsetDateTime.parse(row.getString("updated_at")),
      updatedBy = AnnettePrincipal(
        principalType = row.getString("updated_by_type"),
        principalId = row.getString("updated_by_id")
      )
    )
  }
}
