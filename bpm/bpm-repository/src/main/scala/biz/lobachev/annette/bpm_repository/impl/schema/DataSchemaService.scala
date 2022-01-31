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

package biz.lobachev.annette.bpm_repository.impl.schema

import akka.Done
import biz.lobachev.annette.bpm_repository.api.domain.DataSchemaId
import biz.lobachev.annette.bpm_repository.api.rdb.SQLErrorCodes
import biz.lobachev.annette.bpm_repository.api.schema._
import biz.lobachev.annette.bpm_repository.impl.PostgresDatabase
import biz.lobachev.annette.bpm_repository.impl.db.BpmRepositorySchemaImplicits
import biz.lobachev.annette.core.model.indexing.FindResult
import org.postgresql.util.PSQLException
import slick.jdbc.PostgresProfile.api._

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class DataSchemaService(db: PostgresDatabase, actions: DataSchemaActions)(implicit
  executionContext: ExecutionContext
) extends BpmRepositorySchemaImplicits {

  def createDataSchema(payload: CreateDataSchemaPayload): Future[DataSchema] = {
    val action = actions.createDataSchemaAction(payload, Instant.now())
    db
      .run(action.transactionally)
      .transform(
        res => res,
        {
          case e: PSQLException if e.getSQLState == SQLErrorCodes.UNIQUE_VIOLATION =>
            DataSchemaAlreadyExist(payload.id.value)
          case e                                                                   => e
        }
      )
  }

  def updateDataSchema(payload: UpdateDataSchemaPayload): Future[DataSchema] = {
    val action = actions.updateDataSchemaAction(payload, Instant.now())
    db.run(action.transactionally)
  }

  def updateDataSchemaName(payload: UpdateDataSchemaNamePayload): Future[DataSchema] = {
    val action = for {
      _   <- actions.updateNameAction(payload, Instant.now)
      rec <- actions.getDataSchemaAction(payload.id, true)
    } yield rec
      .getOrElse(throw DataSchemaNotFound(payload.id.value))
    db.run(action.transactionally)
  }

  def updateDataSchemaDescription(payload: UpdateDataSchemaDescriptionPayload): Future[DataSchema] = {
    val action = for {
      _   <- actions.updateDescriptionAction(payload, Instant.now)
      rec <- actions.getDataSchemaAction(payload.id, true)
    } yield rec
      .getOrElse(throw DataSchemaNotFound(payload.id.value))
    db.run(action.transactionally)
  }

  def storeDataSchemaVariable(payload: StoreDataSchemaVariablePayload): Future[DataSchema] = {
    val action = for {
      _   <- actions.storeDataSchemaVariableAction(payload, Instant.now)
      rec <- actions.getDataSchemaAction(payload.dataSchemaId, true)
    } yield rec.getOrElse(throw DataSchemaNotFound(payload.dataSchemaId.value))
    db.run(action.transactionally)
      .transform(
        res => res,
        {
          case e: PSQLException
              if e.getSQLState == SQLErrorCodes.FOREIGN_KEY_VIOLATION &&
                e.getMessage.contains("data_schema_variable_fk_data_schema") =>
            DataSchemaNotFound(payload.dataSchemaId.value)
          case e => e
        }
      )
  }

  def deleteDataSchemaVariable(payload: DeleteDataSchemaVariablePayload): Future[DataSchema] = {
    val action = for {
      _   <- actions.deleteDataSchemaVariableAction(payload, Instant.now)
      rec <- actions.getDataSchemaAction(payload.dataSchemaId, true)
    } yield rec.getOrElse(throw DataSchemaNotFound(payload.dataSchemaId.value))
    db.run(action.transactionally)
      .transform(
        res => res,
        {
          case e: PSQLException
              if e.getSQLState == SQLErrorCodes.FOREIGN_KEY_VIOLATION &&
                e.getMessage.contains("data_schema_variable_fk_data_schema") =>
            DataSchemaNotFound(payload.dataSchemaId.value)
          case e => e
        }
      )
  }

  def deleteDataSchema(payload: DeleteDataSchemaPayload): Future[Done] = {
    val action = actions.deleteDataSchemaAction(payload)
    db.run(action.transactionally)
  }

  def getDataSchemaById(id: String, withVariables: Boolean): Future[DataSchema] = {
    val dataSchemaId = DataSchemaId(id)
    val action       = actions.getDataSchemaAction(dataSchemaId, withVariables)
    for {
      res <- db.run(action.transactionally)
    } yield res.getOrElse(throw DataSchemaNotFound(id))
  }

  def getDataSchemasById(ids: Seq[DataSchemaId], withVariables: Boolean): Future[Seq[DataSchema]] = {
    val action = actions.getDataSchemasAction(ids, withVariables)
    db.run(action.transactionally)
  }

  def findDataSchemas(query: DataSchemaFindQuery): Future[FindResult] = {
    val action = actions.findDataSchemasAction(query)
    db.run(action)
  }

}
