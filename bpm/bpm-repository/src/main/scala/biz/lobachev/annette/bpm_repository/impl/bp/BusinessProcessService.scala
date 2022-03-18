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

package biz.lobachev.annette.bpm_repository.impl.bp

import akka.Done
import biz.lobachev.annette.bpm_repository.api.bp._
import biz.lobachev.annette.bpm_repository.api.domain.BusinessProcessId
import biz.lobachev.annette.bpm_repository.api.model.BpmModelNotFound
import biz.lobachev.annette.bpm_repository.api.rdb.SQLErrorCodes
import biz.lobachev.annette.bpm_repository.api.schema.DataSchemaNotFound
import biz.lobachev.annette.bpm_repository.impl.PostgresDatabase
import biz.lobachev.annette.bpm_repository.impl.db.BpmRepositorySchemaImplicits
import biz.lobachev.annette.core.model.indexing.FindResult
import org.postgresql.util.PSQLException
import slick.jdbc.PostgresProfile.api._

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class BusinessProcessService(db: PostgresDatabase, actions: BusinessProcessActions)(implicit
  executionContext: ExecutionContext
) extends BpmRepositorySchemaImplicits {

  def createBusinessProcess(payload: CreateBusinessProcessPayload): Future[BusinessProcess] = {

    val action = for {
      _   <- actions.createBusinessProcessAction(payload, Instant.now)
      res <- actions.getBusinessProcessAction(payload.id, true)
    } yield res

    db.run(action.transactionally)
      .transform(
        res => res.getOrElse(throw BusinessProcessNotFound(payload.id.value)),
        {
          case e: PSQLException if e.getSQLState == SQLErrorCodes.UNIQUE_VIOLATION =>
            BusinessProcessAlreadyExist(payload.id.value)
          case e: PSQLException
              if e.getSQLState == SQLErrorCodes.FOREIGN_KEY_VIOLATION &&
                e.getMessage.contains("business_process_fk_bpm_model") =>
            BpmModelNotFound(payload.bpmModelId.map(_.value).getOrElse(""))
          case e: PSQLException
              if e.getSQLState == SQLErrorCodes.FOREIGN_KEY_VIOLATION &&
                e.getMessage.contains("business_process_fk_data_schema") =>
            DataSchemaNotFound(payload.dataSchemaId.map(_.value).getOrElse(""))
          case e                                                                   => e
        }
      )
  }

  def updateBusinessProcess(payload: UpdateBusinessProcessPayload): Future[BusinessProcess] = {
    val action = for {
      _   <- actions.updateBusinessProcessAction(payload, Instant.now)
      res <- actions.getBusinessProcessAction(payload.id, true)
    } yield res

    db.run(action.transactionally)
      .transform(
        res => res.getOrElse(throw BusinessProcessNotFound(payload.id.value)),
        {
          case e: PSQLException
              if e.getSQLState == SQLErrorCodes.FOREIGN_KEY_VIOLATION &&
                e.getMessage.contains("business_process_fk_bpm_model") =>
            BpmModelNotFound(payload.bpmModelId.map(_.value).getOrElse(""))
          case e: PSQLException
              if e.getSQLState == SQLErrorCodes.FOREIGN_KEY_VIOLATION &&
                e.getMessage.contains("business_process_fk_data_schema") =>
            DataSchemaNotFound(payload.dataSchemaId.map(_.value).getOrElse(""))
          case e => e
        }
      )
  }

  def updateBusinessProcessName(payload: UpdateBusinessProcessNamePayload): Future[BusinessProcess] = {
    val action = for {
      _   <- actions.updateNameAction(payload, Instant.now)
      rec <- actions.getBusinessProcessAction(payload.id, true)
    } yield rec
      .getOrElse(throw BusinessProcessNotFound(payload.id.value))
    db.run(action.transactionally)
  }

  def updateBusinessProcessDescription(payload: UpdateBusinessProcessDescriptionPayload): Future[BusinessProcess] = {
    val action = for {
      _   <- actions.updateDescriptionAction(payload, Instant.now)
      rec <- actions.getBusinessProcessAction(payload.id, true)
    } yield rec
      .getOrElse(throw BusinessProcessNotFound(payload.id.value))
    db.run(action.transactionally)
  }

  def updateBusinessProcessBpmModel(payload: UpdateBusinessProcessBpmModelPayload): Future[BusinessProcess] = {
    val action = for {
      _   <- actions.updateBusinessProcessBpmModelAction(payload, Instant.now)
      rec <- actions.getBusinessProcessAction(payload.id, true)
    } yield rec
      .getOrElse(throw BusinessProcessNotFound(payload.id.value))
    db.run(action.transactionally)
      .transform(
        res => res,
        {
          case e: PSQLException
              if e.getSQLState == SQLErrorCodes.FOREIGN_KEY_VIOLATION &&
                e.getMessage.contains("business_process_fk_bpm_model") =>
            BpmModelNotFound(payload.bpmModelId.map(_.value).getOrElse(""))
          case e => e
        }
      )
  }

  def updateBusinessProcessDataSchema(
    payload: UpdateBusinessProcessDataSchemaPayload
  ): Future[BusinessProcess] = {
    val action = for {
      _   <- actions.updateBusinessProcessDataSchemaAction(payload, Instant.now)
      res <- actions.getBusinessProcessAction(payload.id, true)
    } yield res

    db.run(action.transactionally)
      .transform(
        res => res.getOrElse(throw BusinessProcessNotFound(payload.id.value)),
        {
          case e: PSQLException
              if e.getSQLState == SQLErrorCodes.FOREIGN_KEY_VIOLATION &&
                e.getMessage.contains("business_process_fk_data_schema") =>
            DataSchemaNotFound(payload.dataSchemaId.map(_.value).getOrElse(""))
          case e => e
        }
      )
  }

  def updateBusinessProcessProcessDefinition(
    payload: UpdateBusinessProcessProcessDefinitionPayload
  ): Future[BusinessProcess] = {
    val action = for {
      _   <- actions.updateBusinessProcessProcessDefinitionAction(payload, Instant.now)
      res <- actions.getBusinessProcessAction(payload.id, true)
    } yield res.getOrElse(throw BusinessProcessNotFound(payload.id.value))
    db.run(action.transactionally)
  }

  def storeBusinessProcessVariable(payload: StoreBusinessProcessVariablePayload): Future[BusinessProcess] = {
    val action = for {
      _   <- actions.storeBusinessProcessVariableAction(payload, Instant.now)
      rec <- actions.getBusinessProcessAction(payload.businessProcessId, true)
    } yield rec.getOrElse(throw BusinessProcessNotFound(payload.businessProcessId.value))
    db.run(action.transactionally)
      .transform(
        res => res,
        {
          case e: PSQLException
              if e.getSQLState == SQLErrorCodes.FOREIGN_KEY_VIOLATION &&
                e.getMessage.contains("business_process_variable_fk_business_process") =>
            BusinessProcessNotFound(payload.businessProcessId.value)
          case e => e
        }
      )
  }

  def deleteBusinessProcessVariable(payload: DeleteBusinessProcessVariablePayload): Future[BusinessProcess] = {
    val action = for {
      _   <- actions.deleteBusinessProcessVariableAction(payload, Instant.now)
      rec <- actions.getBusinessProcessAction(payload.businessProcessId, true)
    } yield rec.getOrElse(throw BusinessProcessNotFound(payload.businessProcessId.value))
    db.run(action.transactionally)
      .transform(
        res => res,
        {
          case e: PSQLException
              if e.getSQLState == SQLErrorCodes.FOREIGN_KEY_VIOLATION &&
                e.getMessage.contains("business_process_variable_fk_business_process") =>
            BusinessProcessNotFound(payload.businessProcessId.value)
          case e => e
        }
      )
  }

  def deleteBusinessProcess(payload: DeleteBusinessProcessPayload): Future[Done] = {
    val action = actions.deleteBusinessProcessAction(payload)
    db.run(action.transactionally)
  }

  def getBusinessProcessById(id: String, withVariables: Boolean): Future[BusinessProcess] = {
    val businessProcessId = BusinessProcessId(id)
    val action            = actions.getBusinessProcessAction(businessProcessId, withVariables)
    for {
      res <- db.run(action.transactionally)
    } yield res.getOrElse(throw BusinessProcessNotFound(id))
  }

  def getBusinessProcessesById(ids: Seq[BusinessProcessId], withVariables: Boolean): Future[Seq[BusinessProcess]] = {
    val action = actions.getBusinessProcessesAction(ids, withVariables)
    db.run(action.transactionally)
  }

  def findBusinessProcesses(query: BusinessProcessFindQuery): Future[FindResult] = {
    val action = actions.findBusinessProcessesAction(query)
    db.run(action)
  }
}
