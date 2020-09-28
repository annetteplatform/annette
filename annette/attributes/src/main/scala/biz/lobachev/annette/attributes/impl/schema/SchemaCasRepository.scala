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

import java.time.OffsetDateTime

import akka.Done
import biz.lobachev.annette.attributes.api.attribute_def.AttributeId
import biz.lobachev.annette.attributes.api.schema._
import biz.lobachev.annette.core.model.AnnettePrincipal
import com.datastax.driver.core.{BoundStatement, PreparedStatement, Row}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

private[impl] class SchemaCasRepository(session: CassandraSession)(implicit ec: ExecutionContext) {

  val log = LoggerFactory.getLogger(this.getClass)

  private var onSchemaCreatedStatement: PreparedStatement              = _
  private var onSchemaNameUpdatedStatement: PreparedStatement          = _
  private var updateUpdatedStatement: PreparedStatement                = _
  private var updateActivatedStatement: PreparedStatement              = _
  private var onActiveAttributeNameUpdatedStatement: PreparedStatement = _
  private var onActiveAttributeCreatedStatement: PreparedStatement     = _
  private var onActiveAttributeUpdatedStatement: PreparedStatement     = _
  private var onActiveAttributeRemovedStatement: PreparedStatement     = _
  private var onPreparedAttributeCreatedStatement: PreparedStatement   = _
  private var onPreparedAttributeUpdatedStatement: PreparedStatement   = _
  private var onPreparedAttributeRemovedStatement: PreparedStatement   = _
  private var onSchemaDeletedStatement: PreparedStatement              = _
  private var preparedAttributesRemoveStatement: PreparedStatement     = _
  private var activeAttributesRemoveStatement: PreparedStatement       = _

  def createTables(): Future[Unit] =
    for {
      _ <- session.executeCreateTable(
             """
               |CREATE TABLE IF NOT EXISTS schemas (
               |          id                text,
               |          name              text,
               |          activated_at      text,
               |          activated_by_type text,
               |          activated_by_id   text,
               |          updated_at        text,
               |          updated_by_type   text,
               |          updated_by_id     text,
               |          PRIMARY KEY (id)
               |)
               |""".stripMargin
           )
      _ <- session.executeCreateTable(
             """
               |CREATE TABLE IF NOT EXISTS active_attributes (
               |          id                 text,
               |          attribute_id       text,
               |          name               text,
               |          caption            text,
               |          attribute_def_id   text,
               |          indexed              boolean,
               |          text_content_index   boolean,
               |          alias_no           int,
               |          PRIMARY KEY (id, attribute_id)
               |)
               |""".stripMargin
           )
      _ <- session.executeCreateTable(
             """
               |CREATE TABLE IF NOT EXISTS prepared_attributes (
               |          id                 text,
               |          attribute_id       text,
               |          name               text,
               |          caption            text,
               |          attribute_def_id   text,  
               |          indexed              boolean,
               |          text_content_index   boolean,
               |          PRIMARY KEY (id, attribute_id)
               |)
               |""".stripMargin
           )
    } yield ()

  def prepareStatements(): Future[Done] = {
    for {
      onSchemaCreatedStmt              <- session.prepare(
                                            """
                                 | INSERT INTO schemas (
                                 |   id                ,
                                 |   name              ,
                                 |   updated_at        ,
                                 |   updated_by_type   ,
                                 |   updated_by_id
                                 | )
                                 | VALUES (
                                 |   :id                ,
                                 |   :name              ,
                                 |   :updated_at        ,
                                 |   :updated_by_type   ,
                                 |   :updated_by_id
                                 | )
                                 |""".stripMargin
                                          )
      onSchemaNameUpdatedStmt          <- session.prepare(
                                            """
                                     | UPDATE schemas SET
                                     |    name            =  :name           ,
                                     |    updated_at      =  :updated_at     ,
                                     |    updated_by_type =  :updated_by_type,
                                     |    updated_by_id   =  :updated_by_id
                                     | WHERE id = :id
                                     |""".stripMargin
                                          )
      updateUpdatedStmt                <- session.prepare(
                                            """
                               | UPDATE schemas SET
                               |    updated_at      =  :updated_at     ,
                               |    updated_by_type =  :updated_by_type,
                               |    updated_by_id   =  :updated_by_id
                               | WHERE id = :id
                               |""".stripMargin
                                          )
      updateActivatedStmt              <- session.prepare(
                                            """
                                 | UPDATE schemas SET
                                 |    updated_at      =  :activated_at     ,
                                 |    updated_by_type =  :activated_by_type,
                                 |    updated_by_id   =  :activated_by_id,
                                 |    activated_at      =  :activated_at     ,
                                 |    activated_by_type =  :activated_by_type,
                                 |    activated_by_id   =  :activated_by_id
                                 | WHERE id = :id
                                 |""".stripMargin
                                          )
      onActiveAttributeNameUpdatedStmt <- session.prepare(
                                            """
                                              | UPDATE active_attributes SET
                                              |     name            =  :name           ,
                                              |     caption         =  :caption
                                              | WHERE id = :id AND attribute_id = :attribute_id
                                              |""".stripMargin
                                          )
      onActiveAttributeCreatedStmt     <- session.prepare(
                                            """
                                          | INSERT INTO active_attributes (
                                          |   id                 ,
                                          |   attribute_id       ,
                                          |   name               ,
                                          |   caption            ,
                                          |   attribute_def_id   ,
                                          |   indexed            ,
                                          |   text_content_index ,
                                          |   alias_no
                                          | )
                                          | VALUES (
                                          |   :id                 ,
                                          |   :attribute_id       ,
                                          |   :name               ,
                                          |   :caption            ,
                                          |   :attribute_def_id   ,
                                          |   :indexed            ,
                                          |   :text_content_index ,
                                          |   :alias_no
                                          | )
                                          |""".stripMargin
                                          )
      onActiveAttributeUpdatedStmt     <- session.prepare(
                                            """
                                          | UPDATE active_attributes SET
                                          |    name               = :name               ,
                                          |    caption            = :caption            ,
                                          |    attribute_def_id   = :attribute_def_id   ,
                                          |    indexed              = :indexed              ,
                                          |    text_content_index   = :text_content_index   ,
                                          |    alias_no             = :alias_no
                                          | WHERE id = :id AND attribute_id = :attribute_id
                                          |""".stripMargin
                                          )
      onActiveAttributeRemovedStmt     <- session.prepare(
                                            """
                                          | DELETE FROM active_attributes
                                          | WHERE id = :id AND attribute_id = :attribute_id
                                          |""".stripMargin
                                          )

      onPreparedAttributeCreatedStmt   <- session.prepare(
                                            """
                                            | INSERT INTO prepared_attributes (
                                            |   id                 ,
                                            |   attribute_id       ,
                                            |   name               ,
                                            |   caption            ,
                                            |   attribute_def_id   ,
                                            |   indexed            ,
                                            |   text_content_index   
                                            | )
                                            | VALUES (
                                            |   :id                 ,
                                            |   :attribute_id       ,
                                            |   :name               ,
                                            |   :caption            ,
                                            |   :attribute_def_id   ,
                                            |   :indexed            ,
                                            |   :text_content_index   
                                            | )
                                            |""".stripMargin
                                          )
      onPreparedAttributeUpdatedStmt   <- session.prepare(
                                            """
                                            | UPDATE prepared_attributes SET
                                            |    name               = :name               ,
                                            |    caption            = :caption            ,
                                            |    attribute_def_id   = :attribute_def_id   ,
                                            |    indexed              = :indexed          ,
                                            |    text_content_index   = :text_content_index   
                                            | WHERE id = :id AND attribute_id = :attribute_id
                                            |""".stripMargin
                                          )
      onPreparedAttributeRemovedStmt   <- session.prepare(
                                            """
                                            | DELETE FROM prepared_attributes
                                            | WHERE id = :id AND attribute_id = :attribute_id
                                            |""".stripMargin
                                          )
      onSchemaDeletedStmt              <- session.prepare(
                                            """
                                 | DELETE FROM schemas
                                 | WHERE id = :id
                                 |""".stripMargin
                                          )
      preparedAttributesRemoveStmt     <- session.prepare(
                                            """
                                          | DELETE FROM prepared_attributes
                                          | WHERE id = :id
                                          |""".stripMargin
                                          )
      activeAttributesRemoveStmt       <- session.prepare(
                                            """
                                        | DELETE FROM active_attributes
                                        | WHERE id = :id
                                        |""".stripMargin
                                          )
    } yield {
      onSchemaCreatedStatement = onSchemaCreatedStmt
      onSchemaNameUpdatedStatement = onSchemaNameUpdatedStmt
      updateUpdatedStatement = updateUpdatedStmt
      updateActivatedStatement = updateActivatedStmt
      onActiveAttributeNameUpdatedStatement = onActiveAttributeNameUpdatedStmt
      onActiveAttributeCreatedStatement = onActiveAttributeCreatedStmt
      onActiveAttributeUpdatedStatement = onActiveAttributeUpdatedStmt
      onActiveAttributeRemovedStatement = onActiveAttributeRemovedStmt
      onPreparedAttributeCreatedStatement = onPreparedAttributeCreatedStmt
      onPreparedAttributeUpdatedStatement = onPreparedAttributeUpdatedStmt
      onPreparedAttributeRemovedStatement = onPreparedAttributeRemovedStmt
      onSchemaDeletedStatement = onSchemaDeletedStmt
      preparedAttributesRemoveStatement = preparedAttributesRemoveStmt
      activeAttributesRemoveStatement = activeAttributesRemoveStmt
      Done
    }
  }

  def createSchema(event: SchemaEntity.SchemaCreated): Seq[BoundStatement] =
    Seq(
      onSchemaCreatedStatement
        .bind()
        .setString("id", event.id.toComposed)
        .setString("name", event.name)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    ) ++ event.preparedAttributes.map(attr =>
      onPreparedAttributeCreatedStatement
        .bind()
        .setString("id", event.id.toComposed)
        .setString("attribute_id", attr.attributeId)
        .setString("name", attr.name)
        .setString("caption", attr.caption.orNull)
        .setString("attribute_def_id", attr.attributeDefId)
        .setBool("indexed", attr.index.isDefined)
        .setBool("text_content_index", attr.index.map(_.textContentIndex).getOrElse(false))
    )

  def updateSchemaName(event: SchemaEntity.SchemaNameUpdated): Seq[BoundStatement] =
    Seq(
      onSchemaNameUpdatedStatement
        .bind()
        .setString("id", event.id.toComposed)
        .setString("name", event.name)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def updateActiveAttributeName(event: SchemaEntity.ActiveAttributeNameUpdated): Seq[BoundStatement] =
    Seq(
      onActiveAttributeNameUpdatedStatement
        .bind()
        .setString("id", event.id.toComposed)
        .setString("attribute_id", event.attributeId)
        .setString("name", event.name)
        .setString("caption", event.caption.orNull),
      updateUpdatedStatement
        .bind()
        .setString("id", event.id.toComposed)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def createActiveAttribute(event: SchemaEntity.ActiveAttributeCreated): Seq[BoundStatement] =
    Seq(
      onActiveAttributeCreatedStatement
        .bind()
        .setString("id", event.id.toComposed)
        .setString("attribute_id", event.attributeId)
        .setString("name", event.name)
        .setString("caption", event.caption.orNull)
        .setString("attribute_def_id", event.attributeDefId)
        .setBool("indexed", event.index.isDefined)
        .setBool("text_content_index", event.index.map(_.textContentIndex).getOrElse(false))
        .setInt("alias_no", event.index.map(_.aliasNo).getOrElse(0)),
      updateActivatedStatement
        .bind()
        .setString("id", event.id.toComposed)
        .setString("activated_at", event.activatedAt.toString)
        .setString("activated_by_type", event.activatedBy.principalType)
        .setString("activated_by_id", event.activatedBy.principalId)
    )

  def updateActiveAttribute(event: SchemaEntity.ActiveAttributeUpdated): Seq[BoundStatement] =
    Seq(
      onActiveAttributeUpdatedStatement
        .bind()
        .setString("id", event.id.toComposed)
        .setString("attribute_id", event.attributeId)
        .setString("name", event.name)
        .setString("caption", event.caption.orNull)
        .setBool("indexed", event.index.isDefined)
        .setBool("text_content_index", event.index.map(_.textContentIndex).getOrElse(false))
        .setInt("alias_no", event.index.map(_.aliasNo).getOrElse(0)),
      updateActivatedStatement
        .bind()
        .setString("id", event.id.toComposed)
        .setString("activated_at", event.activatedAt.toString)
        .setString("activated_by_type", event.activatedBy.principalType)
        .setString("activated_by_id", event.activatedBy.principalId)
    )

  def removeActiveAttribute(event: SchemaEntity.ActiveAttributeRemoved): Seq[BoundStatement] =
    Seq(
      onActiveAttributeRemovedStatement
        .bind()
        .bind()
        .setString("id", event.id.toComposed)
        .setString("attribute_id", event.attributeId),
      updateActivatedStatement
        .bind()
        .setString("id", event.id.toComposed)
        .setString("activated_at", event.activatedAt.toString)
        .setString("activated_by_type", event.activatedBy.principalType)
        .setString("activated_by_id", event.activatedBy.principalId)
    )

  def createPreparedAttribute(event: SchemaEntity.PreparedAttributeCreated): Seq[BoundStatement] =
    Seq(
      onPreparedAttributeCreatedStatement
        .bind()
        .setString("id", event.id.toComposed)
        .setString("attribute_id", event.attributeId)
        .setString("name", event.name)
        .setString("caption", event.caption.orNull)
        .setString("attribute_def_id", event.attributeDefId)
        .setBool("indexed", event.index.isDefined)
        .setBool("text_content_index", event.index.map(_.textContentIndex).getOrElse(false)),
      updateUpdatedStatement
        .bind()
        .setString("id", event.id.toComposed)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def updatePreparedAttribute(event: SchemaEntity.PreparedAttributeUpdated): Seq[BoundStatement] =
    Seq(
      onPreparedAttributeUpdatedStatement
        .bind()
        .setString("id", event.id.toComposed)
        .setString("attribute_id", event.attributeId)
        .setString("name", event.name)
        .setString("caption", event.caption.orNull)
        .setString("attribute_def_id", event.attributeDefId)
        .setBool("indexed", event.index.isDefined)
        .setBool("text_content_index", event.index.map(_.textContentIndex).getOrElse(false)),
      updateUpdatedStatement
        .bind()
        .setString("id", event.id.toComposed)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def removePreparedAttribute(event: SchemaEntity.PreparedAttributeRemoved): Seq[BoundStatement] =
    Seq(
      onPreparedAttributeRemovedStatement
        .bind()
        .setString("id", event.id.toComposed)
        .setString("attribute_id", event.attributeId),
      updateUpdatedStatement
        .bind()
        .setString("id", event.id.toComposed)
        .setString("updated_at", event.updatedAt.toString)
        .setString("updated_by_type", event.updatedBy.principalType)
        .setString("updated_by_id", event.updatedBy.principalId)
    )

  def deleteSchema(event: SchemaEntity.SchemaDeleted): Seq[BoundStatement] =
    Seq(
      onSchemaDeletedStatement
        .bind()
        .setString("id", event.id.toComposed),
      preparedAttributesRemoveStatement
        .bind()
        .setString("id", event.id.toComposed),
      activeAttributesRemoveStatement
        .bind()
        .setString("id", event.id.toComposed)
    )

  def getSchemaById(composedSchemaId: ComposedSchemaId): Future[Option[Schema]] =
    for {
      schemaStmt            <- session.prepare("SELECT * FROM schemas WHERE id = :id")
      schema                <- session.selectOne(schemaStmt.bind().setString("id", composedSchemaId)).map(_.map(convertSchema))
      activeAttributeStmt   <- session.prepare("SELECT * FROM active_attributes WHERE id = :id")
      activeAttributes      <- session
                                 .selectAll(activeAttributeStmt.bind().setString("id", composedSchemaId))
                                 .map(_.map { row =>
                                   val attr = convertActiveAttribute(row)
                                   attr.attributeId -> attr
                                 }.toMap)
      preparedAttributeStmt <- session.prepare("SELECT * FROM prepared_attributes WHERE id = :id")
      preparedAttributes    <- session
                                 .selectAll(preparedAttributeStmt.bind().setString("id", composedSchemaId))
                                 .map(_.map { row =>
                                   val attr = convertPreparedAttribute(row)
                                   attr.attributeId -> attr
                                 }.toMap)
    } yield schema.map(
      _.copy(
        activeAttributes = activeAttributes,
        preparedAttributes = preparedAttributes
      )
    )

  def getSchemaAttribute(schemaId: SchemaId, attributeId: AttributeId): Future[Option[ActiveSchemaAttribute]] =
    for {
      activeAttributeStmt <- session.prepare(
                               "SELECT * FROM active_attributes WHERE id = :id AND attribute_id = :attribute_id"
                             )
      activeAttribute     <- session
                               .selectOne(
                                 activeAttributeStmt
                                   .bind()
                                   .setString("id", schemaId.toComposed)
                                   .setString("attribute_id", attributeId)
                               )
                               .map(_.map(convertActiveAttribute))
    } yield activeAttribute

  def getSchemasById(ids: Set[ComposedSchemaId]): Future[Map[ComposedSchemaId, Schema]] =
    Future
      .traverse(ids) { composedSchemaId =>
        getSchemaById(composedSchemaId)
      }
      .map(_.flatten.map(schema => schema.id.toComposed -> schema).toMap)

  private def convertSchema(row: Row): Schema = {
    val id = SchemaId.fromComposed(row.getString("id"))
    Schema(
      id = id,
      name = row.getString("name"),
      activatedAt = Option(row.getString("activated_at")).map(OffsetDateTime.parse),
      activatedBy = Option(row.getString("activated_by_type")).map(principalType =>
        AnnettePrincipal(
          principalType = principalType,
          principalId = row.getString("activated_by_id")
        )
      ),
      updatedAt = OffsetDateTime.parse(row.getString("updated_at")),
      updatedBy = AnnettePrincipal(
        principalType = row.getString("updated_by_type"),
        principalId = row.getString("updated_by_id")
      )
    )
  }

  private def convertActiveAttribute(row: Row) = {
    val attributeId = row.getString("attribute_id")
    val isIndexed   = row.getBool("indexed")
    val index       =
      if (isIndexed)
        Some(
          ActiveIndexParam(
            textContentIndex = row.getBool("text_content_index"),
            alias = SchemaEntity.alias(attributeId, row.getInt("alias_no"))
          )
        )
      else
        None
    ActiveSchemaAttribute(
      attributeId = attributeId,
      name = row.getString("name"),
      caption = Option(row.getString("caption")),
      attributeDefId = row.getString("attribute_def_id"),
      index = index
    )
  }

  private def convertPreparedAttribute(row: Row) = {
    val isIndexed = row.getBool("indexed")
    val index     =
      if (isIndexed)
        Some(
          PreparedIndexParam(
            textContentIndex = row.getBool("text_content_index")
          )
        )
      else
        None
    PreparedSchemaAttribute(
      attributeId = row.getString("attribute_id"),
      name = row.getString("name"),
      caption = Option(row.getString("caption")),
      attributeDefId = row.getString("attribute_def_id"),
      index = index
    )
  }

}
