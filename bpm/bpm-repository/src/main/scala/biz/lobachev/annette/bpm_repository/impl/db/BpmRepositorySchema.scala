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

package biz.lobachev.annette.bpm_repository.impl.db

import biz.lobachev.annette.bpm_repository.api.domain.{
  BpmModelId,
  BusinessProcessId,
  DataSchemaId,
  Datatype,
  Notation,
  ProcessDefinitionId,
  VariableName
}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._

import java.time.Instant

object BpmRepositorySchema extends BpmRepositorySchemaImplicits {

  class BpmModelTable(tag: Tag) extends Table[BpmModelRecord](tag, "bpm_models") {
    def id          = column[BpmModelId]("id", O.Length(BpmModelId.maxLength))
    def code        = column[String]("code", O.SqlType("VARCHAR"), O.Length(80))
    def name        = column[String]("name", O.SqlType("VARCHAR"), O.Length(128))
    def description = column[String]("description", O.SqlType("TEXT"))
    def notation    = column[Notation.Notation]("notation", O.SqlType("VARCHAR"), O.Length(Notation.maxLength))
    def xml         = column[String]("xml", O.SqlType("TEXT"))
    def updatedAt   = column[Instant]("updated_at", O.SqlType("TIMESTAMP"))
    def updatedBy   = column[AnnettePrincipal]("updated_by", O.SqlType("VARCHAR"), O.Length(100))

    def *                  =
      (id, code, name, description, notation, xml, updatedAt, updatedBy).<>(
        (BpmModelRecord.apply _).tupled,
        BpmModelRecord.unapply
      )
    def bpmModelPrimaryKey = primaryKey("bpm_model_pk", id)
  }
  lazy val bpmModels: TableQuery[BpmModelTable] = TableQuery[BpmModelTable]

  class DataSchemaTable(tag: Tag) extends Table[DataSchemaRecord](tag, "data_schemas") {
    def id          = column[DataSchemaId]("id", O.Length(DataSchemaId.maxLength))
    def name        = column[String]("name", O.SqlType("VARCHAR"), O.Length(128))
    def description = column[String]("description", O.SqlType("TEXT"))
    def updatedAt   = column[Instant]("updated_at", O.SqlType("TIMESTAMP"))
    def updatedBy   = column[AnnettePrincipal]("updated_by", O.SqlType("VARCHAR"), O.Length(100))

    def *                    =
      (id, name, description, updatedAt, updatedBy).<>(
        (DataSchemaRecord.apply _).tupled,
        DataSchemaRecord.unapply
      )
    def dataSchemaPrimaryKey = primaryKey("data_schema_pk", id)
  }
  lazy val dataSchemas: TableQuery[DataSchemaTable] = TableQuery[DataSchemaTable]

  class DataSchemaVariableTable(tag: Tag) extends Table[DataSchemaVariableRecord](tag, "data_schema_variables") {
    def dataSchemaId = column[DataSchemaId]("data_schema_id", O.SqlType("VARCHAR"), O.Length(DataSchemaId.maxLength))
    def variableName = column[VariableName]("variable_name", O.SqlType("VARCHAR"), O.Length(VariableName.maxLength))
    def name         = column[String]("name", O.SqlType("VARCHAR"), O.Length(128))
    def caption      = column[String]("caption", O.SqlType("VARCHAR"), O.Length(128))
    def datatype     = column[Datatype.Datatype]("datatype", O.SqlType("VARCHAR"), O.Length(Datatype.maxLength))
    def defaultValue = column[String]("default_value", O.SqlType("TEXT"))

    def *                            =
      (dataSchemaId, variableName, name, caption, datatype, defaultValue).<>(
        (DataSchemaVariableRecord.apply _).tupled,
        DataSchemaVariableRecord.unapply
      )
    def dataSchemaVariablePrimaryKey = primaryKey("data_schema_variable_pk", (dataSchemaId, variableName))
    def dataSchemaVariableForeignKey =
      foreignKey("data_schema_variable_fk_data_schema", dataSchemaId, dataSchemas)(
        _.id,
        onUpdate = ForeignKeyAction.Restrict,
        onDelete = ForeignKeyAction.Cascade
      )
  }
  lazy val dataSchemaVariables: TableQuery[DataSchemaVariableTable] = TableQuery[DataSchemaVariableTable]

  class BusinessProcessTable(tag: Tag) extends Table[BusinessProcessRecord](tag, "business_processes") {
    def id                  = column[BusinessProcessId]("id", O.Length(BusinessProcessId.maxLength))
    def name                = column[String]("name", O.SqlType("VARCHAR"), O.Length(128))
    def description         = column[String]("description", O.SqlType("TEXT"))
    def bpmModelId          = column[Option[BpmModelId]]("bpm_model_id", O.Length(BpmModelId.maxLength))
    def processDefinitionId =
      column[Option[ProcessDefinitionId]]("process_definition_id", O.Length(ProcessDefinitionId.maxLength))
    def dataSchemaId        = column[Option[DataSchemaId]]("data_schema_id", O.Length(DataSchemaId.maxLength))
    def updatedAt           = column[Instant]("updated_at", O.SqlType("TIMESTAMP"))
    def updatedBy           = column[AnnettePrincipal]("updated_by", O.SqlType("VARCHAR"), O.Length(100))

    def *                                   =
      (id, name, description, bpmModelId, processDefinitionId, dataSchemaId, updatedAt, updatedBy).<>(
        (BusinessProcessRecord.apply _).tupled,
        BusinessProcessRecord.unapply
      )
    def businessProcessPK                   = primaryKey("business_process_pk", id)
    def businessProcessVariableFKBpmModel   =
      foreignKey("business_process_variable_fk_bpm_model", bpmModelId, bpmModels)(
        _.id.?,
        onUpdate = ForeignKeyAction.Restrict,
        onDelete = ForeignKeyAction.Restrict
      )
    def businessProcessVariableFKDataSchema =
      foreignKey("business_process_variable_fk_data_schema", dataSchemaId, dataSchemas)(
        _.id.?,
        onUpdate = ForeignKeyAction.Restrict,
        onDelete = ForeignKeyAction.Restrict
      )
  }
  lazy val businessProcesses: TableQuery[BusinessProcessTable] = TableQuery[BusinessProcessTable]

  class BusinessProcessVariableTable(tag: Tag)
      extends Table[BusinessProcessVariableRecord](tag, "business_process_variables") {
    def businessProcessId =
      column[BusinessProcessId]("business_process_id", O.SqlType("VARCHAR"), O.Length(BusinessProcessId.maxLength))
    def variableName      = column[VariableName]("variable_name", O.SqlType("VARCHAR"), O.Length(VariableName.maxLength))
    def name              = column[String]("name", O.SqlType("VARCHAR"), O.Length(128))
    def caption           = column[String]("caption", O.SqlType("VARCHAR"), O.Length(128))
    def datatype          = column[Datatype.Datatype]("datatype", O.SqlType("VARCHAR"), O.Length(Datatype.maxLength))
    def defaultValue      = column[String]("default_value", O.SqlType("TEXT"))

    def *                                 =
      (businessProcessId, variableName, name, caption, datatype, defaultValue).<>(
        (BusinessProcessVariableRecord.apply _).tupled,
        BusinessProcessVariableRecord.unapply
      )
    def businessProcessVariablePrimaryKey =
      primaryKey("business_process_variable_pk", (businessProcessId, variableName))
    def businessProcessVariableForeignKey =
      foreignKey("business_process_variable_fk_business_process", businessProcessId, businessProcesses)(
        _.id,
        onUpdate = ForeignKeyAction.Restrict,
        onDelete = ForeignKeyAction.Cascade
      )
  }
  lazy val businessProcessVariables: TableQuery[BusinessProcessVariableTable] = TableQuery[BusinessProcessVariableTable]

  val dataDefinition: PostgresProfile.SchemaDescription =
    bpmModels.schema ++ dataSchemas.schema ++ dataSchemaVariables.schema ++ businessProcesses.schema ++ businessProcessVariables.schema

}
