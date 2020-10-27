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

package biz.lobachev.annette.attributes.impl.attribute_def

import java.time.OffsetDateTime

import akka.Done
import biz.lobachev.annette.attributes.api.attribute._
import biz.lobachev.annette.core.model.AnnettePrincipal
import com.datastax.driver.core.DataType.CollectionType
import com.datastax.driver.core._
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import org.slf4j.LoggerFactory

import scala.collection.immutable._
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

private[impl] class AttributeDefRepository(session: CassandraSession)(implicit ec: ExecutionContext) {

  val log = LoggerFactory.getLogger(this.getClass)

  private var onAttributeDefCreatedStatement: PreparedStatement = _
  private var onAttributeDefUpdatedStatement: PreparedStatement = _
  private var onAttributeDefDeletedStatement: PreparedStatement = _
  private var allowedValuesUdt: UserType                        = _

  def createTables(): Future[Unit] =
    for {
      _ <- session.executeCreateTable("""
                                        |CREATE TYPE IF NOT EXISTS allowed_value_type (
                                        |          value              text,
                                        |          caption            text,
                                        |)
                                        |""".stripMargin)
      _ <- session.executeCreateTable("""
                                        |CREATE TABLE IF NOT EXISTS attribute_defs (
                                        |          id               text PRIMARY KEY,
                                        |          name             text,
                                        |          caption          text,
                                        |          attribute_type   text,
                                        |          attribute_id     text,
                                        |          subtype          text,
                                        |          allowed_values   list<FROZEN<allowed_value_type>>,
                                        |          updated_at       text,
                                        |          updated_by_type  text,
                                        |          updated_by_id    text
                                        |)
                                        |""".stripMargin)
    } yield ()

  def prepareStatements(): Future[Done] =
    for {
      onAttributeDefCreatedStmt <- session.prepare(
                                     """
                                       | INSERT INTO attribute_defs (
                                       |        id             ,
                                       |        name           ,
                                       |        caption        ,
                                       |        attribute_type ,
                                       |        attribute_id   ,
                                       |        subtype        ,
                                       |        allowed_values ,
                                       |        updated_at     ,
                                       |        updated_by_type,
                                       |        updated_by_id
                                       |  )
                                       |  VALUES (
                                       |       :id             ,
                                       |       :name           ,
                                       |       :caption        ,
                                       |       :attribute_type ,
                                       |       :attribute_id   ,
                                       |       :subtype        ,
                                       |       :allowed_values ,
                                       |       :updated_at     ,
                                       |       :updated_by_type,
                                       |       :updated_by_id
                                       |  )
                                       |""".stripMargin
                                   )
      onAttributeDefUpdatedStmt <- session.prepare(
                                     """
                                       | UPDATE attribute_defs SET
                                       |    name            =  :name           ,
                                       |    caption         =  :caption        ,
                                       |    attribute_id    =  :attribute_id   ,
                                       |    subtype         =  :subtype        ,
                                       |    allowed_values  =  :allowed_values ,
                                       |    updated_at      =  :updated_at     ,
                                       |    updated_by_type =  :updated_by_type,
                                       |    updated_by_id   =  :updated_by_id
                                       | WHERE id = :id
                                       |""".stripMargin
                                   )

      onAttributeDefDeletedStmt <- session.prepare(
                                     """
                                       | DELETE FROM attribute_defs
                                       | WHERE id = :id
                                       |""".stripMargin
                                   )
    } yield {
      onAttributeDefCreatedStatement = onAttributeDefCreatedStmt
      onAttributeDefUpdatedStatement = onAttributeDefUpdatedStmt
      onAttributeDefDeletedStatement = onAttributeDefDeletedStmt
      allowedValuesUdt = onAttributeDefCreatedStatement
        .getVariables()
        .getType("allowed_values")
        .asInstanceOf[CollectionType]
        .getTypeArguments
        .get(0)
        .asInstanceOf[UserType]
      Done
    }

  def onAttributeDefCreated(event: AttributeDefEntity.AttributeDefCreated): BoundStatement = {
    val allowedValues = event.allowedValues.map {
      case (value, caption) =>
        allowedValuesUdt
          .newValue()
          .setString("value", value)
          .setString("caption", caption)
    }.toList.asJava
    onAttributeDefCreatedStatement
      .bind()
      .setString("id", event.id)
      .setString("name", event.name)
      .setString("caption", event.caption)
      .setString("attribute_type", event.attributeType.toString)
      .setString("attribute_id", event.attributeId)
      .setString("subtype", event.subType.orNull)
      .setList("allowed_values", allowedValues, classOf[UDTValue])
      .setString("updated_at", event.updatedAt.toString)
      .setString("updated_by_type", event.updatedBy.principalType)
      .setString("updated_by_id", event.updatedBy.principalId)
  }
  def onAttributeDefUpdated(event: AttributeDefEntity.AttributeDefUpdated): BoundStatement = {
    val allowedValues = event.allowedValues.map {
      case (value, caption) =>
        allowedValuesUdt
          .newValue()
          .setString("value", value)
          .setString("caption", caption)
    }.toList.asJava
    onAttributeDefUpdatedStatement
      .bind()
      .setString("id", event.id)
      .setString("name", event.name)
      .setString("caption", event.caption)
      .setString("attribute_id", event.attributeId)
      .setString("subtype", event.subType.orNull)
      .setList("allowed_values", allowedValues, classOf[UDTValue])
      .setString("updated_at", event.updatedAt.toString)
      .setString("updated_by_type", event.updatedBy.principalType)
      .setString("updated_by_id", event.updatedBy.principalId)
      .bind()
  }

  def onAttributeDefDeleted(event: AttributeDefEntity.AttributeDefDeleted): BoundStatement =
    onAttributeDefDeletedStatement
      .bind()
      .setString("id", event.id)

  def getAttributeDefById(id: AttributeDefId): Future[Option[AttributeDef]] =
    for {
      stmt         <- session.prepare("SELECT * FROM attribute_defs WHERE id = :id")
      attributeDef <- session.selectOne(stmt.bind().setString("id", id)).map(_.map(convertAttributeDef))
    } yield attributeDef

  def getAttributeDefsById(ids: Set[AttributeDefId]): Future[Map[AttributeDefId, AttributeDef]] =
    for {
      stmt   <- session.prepare("SELECT * FROM attribute_defs WHERE id IN ?")
      result <- session.selectAll(stmt.bind(ids.toList.asJava)).map(_.map(convertAttributeDef))
    } yield result.map(a => a.id -> a).toMap

  private def convertAttributeDef(row: Row): AttributeDef = {
    val allowedValues = row
      .getList("allowed_values", classOf[UDTValue])
      .asScala
      .map(udt => udt.getString("value") -> udt.getString("caption"))
      .toMap
    AttributeDef(
      id = row.getString("id"),
      name = row.getString("name"),
      caption = row.getString("caption"),
      attributeType = AttributeValueType.withName(row.getString("attribute_type")),
      attributeId = row.getString("attribute_id"),
      subType = Option(row.getString("subtype")),
      allowedValues = allowedValues,
      updatedAt = OffsetDateTime.parse(row.getString("updated_at")),
      updatedBy = AnnettePrincipal(
        principalType = row.getString("updated_by_type"),
        principalId = row.getString("updated_by_id")
      )
    )
  }

}
