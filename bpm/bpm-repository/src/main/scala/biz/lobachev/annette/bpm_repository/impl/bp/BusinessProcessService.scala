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
import biz.lobachev.annette.bpm_repository.api.rdb.SQLErrorCodes
import biz.lobachev.annette.bpm_repository.impl.PostgresDatabase
import biz.lobachev.annette.bpm_repository.impl.db.{
  BpmRepositorySchema,
  BpmRepositorySchemaImplicits,
  BusinessProcessRecord,
  BusinessProcessVariableRecord
}
import biz.lobachev.annette.core.model.indexing.{FindResult, HitResult, SortBy}
import io.scalaland.chimney.dsl._
import org.postgresql.util.PSQLException
import slick.jdbc.PostgresProfile.api._

import java.time.{Instant, ZoneOffset}
import scala.concurrent.{ExecutionContext, Future}

class BusinessProcessService(db: PostgresDatabase)(implicit
  executionContext: ExecutionContext
) extends BpmRepositorySchemaImplicits {

  def createBusinessProcess(payload: CreateBusinessProcessPayload): Future[BusinessProcess] = {
    val businessProcessRecord = payload
      .into[BusinessProcessRecord]
      .withFieldConst(_.updatedAt, Instant.now())
      .transform
    val action                = for {
      varRecs                  <- payload.dataSchemaId
                                    .map(dsId => BpmRepositorySchema.dataSchemaVariables.filter(_.dataSchemaId === dsId).result)
                                    .getOrElse(DBIO.successful(Seq.empty))
      businessProcessVarRecords =
        if (varRecs.isEmpty)
          BusinessProcessVariableRecord.fromBusinessProcessVariables(payload.id, payload.variables)
        else
          varRecs.map(
            _.into[BusinessProcessVariableRecord]
              .withFieldConst(_.businessProcessId, payload.id)
              .transform
          )
      bpInserted               <- BpmRepositorySchema.businessProcesses += businessProcessRecord
      _                        <- BpmRepositorySchema.businessProcessVariables ++= businessProcessVarRecords
    } yield
      if (bpInserted == 1)
        businessProcessRecord
          .into[BusinessProcess]
          .withFieldConst(_.variables, businessProcessVarRecords.map(_.transformInto[BusinessProcessVariable]))
          .withFieldComputed(_.updatedAt, _.updatedAt.atOffset(ZoneOffset.UTC))
          .transform
      else throw BusinessProcessUpdateFailed("createBusinessProcess", bpInserted.toString)
    db.run(action.transactionally).recoverWith {
      case e: PSQLException if e.getSQLState == SQLErrorCodes.UNIQUE_VIOLATION =>
        Future.failed(BusinessProcessAlreadyExist(payload.id.value))
      case e                                                                   => Future.failed(e)
    }
  }

  def updateBusinessProcess(payload: UpdateBusinessProcessPayload): Future[BusinessProcess] = {
    val businessProcessRecord     = payload
      .into[BusinessProcessRecord]
      .withFieldConst(_.updatedAt, Instant.now())
      .transform
    val businessProcessVarRecords =
      BusinessProcessVariableRecord.fromBusinessProcessVariables(payload.id, payload.variables)
    val action                    = for {
      bpUpdated <-
        BpmRepositorySchema.businessProcesses.filter(_.id === businessProcessRecord.id).update(businessProcessRecord)
      _         <- BpmRepositorySchema.businessProcessVariables.filter(_.businessProcessId === businessProcessRecord.id).delete
      _         <- BpmRepositorySchema.businessProcessVariables ++= businessProcessVarRecords
    } yield
      if (bpUpdated == 1)
        businessProcessRecord
          .into[BusinessProcess]
          .withFieldConst(_.variables, payload.variables)
          .withFieldComputed(_.updatedAt, _.updatedAt.atOffset(ZoneOffset.UTC))
          .transform
      else throw BusinessProcessUpdateFailed("updateBusinessProcess", bpUpdated.toString)
    db.run(action.transactionally)
  }

  def updateBusinessProcessName(payload: UpdateBusinessProcessNamePayload): Future[BusinessProcess] = {
    val action = for {
      rowUpdated <- BpmRepositorySchema.businessProcesses
                      .filter(_.id === payload.id)
                      .map(rec => (rec.name, rec.updatedBy, rec.updatedAt))
                      .update((payload.name, payload.updatedBy, Instant.now))
      rec        <- BpmRepositorySchema.businessProcesses.filter(_.id === payload.id).result
    } yield
      if (rowUpdated == 1 && rec.headOption.nonEmpty)
        rec.head
          .into[BusinessProcess]
          .withFieldConst(_.variables, Seq.empty)
          .withFieldComputed(_.updatedAt, _.updatedAt.atOffset(ZoneOffset.UTC))
          .transform
      else throw BusinessProcessUpdateFailed("updateBusinessProcessName", rowUpdated.toString)
    db.run(action.transactionally)
  }

  def updateBusinessProcessDescription(payload: UpdateBusinessProcessDescriptionPayload): Future[BusinessProcess] = {
    val action = for {
      rowUpdated <- BpmRepositorySchema.businessProcesses
                      .filter(_.id === payload.id)
                      .map(rec => (rec.description, rec.updatedBy, rec.updatedAt))
                      .update((payload.description, payload.updatedBy, Instant.now))
      rec        <- BpmRepositorySchema.businessProcesses.filter(_.id === payload.id).result
    } yield
      if (rowUpdated == 1 && rec.headOption.nonEmpty)
        rec.head
          .into[BusinessProcess]
          .withFieldConst(_.variables, Seq.empty)
          .withFieldComputed(_.updatedAt, _.updatedAt.atOffset(ZoneOffset.UTC))
          .transform
      else throw BusinessProcessUpdateFailed("updateBusinessProcessDescription", rowUpdated.toString)
    db.run(action.transactionally)
  }

  def updateBusinessProcessBmpModel(payload: UpdateBusinessProcessBmpModelPayload): Future[BusinessProcess] = {
    val action = for {
      rowUpdated <- BpmRepositorySchema.businessProcesses
                      .filter(_.id === payload.id)
                      .map(rec => (rec.bpmModelId, rec.updatedBy, rec.updatedAt))
                      .update((payload.bpmModelId, payload.updatedBy, Instant.now))
      rec        <- BpmRepositorySchema.businessProcesses.filter(_.id === payload.id).result
    } yield
      if (rowUpdated == 1 && rec.headOption.nonEmpty)
        rec.head
          .into[BusinessProcess]
          .withFieldConst(_.variables, Seq.empty)
          .withFieldComputed(_.updatedAt, _.updatedAt.atOffset(ZoneOffset.UTC))
          .transform
      else throw BusinessProcessUpdateFailed("updateBusinessProcessBmpModel", rowUpdated.toString)
    db.run(action.transactionally)
  }

  def updateBusinessProcessDataSchema(
    payload: UpdateBusinessProcessDataSchemaPayload
  ): Future[BusinessProcess] = ???

  def updateBusinessProcessProcessDefinition(
    payload: UpdateBusinessProcessProcessDefinitionPayload
  ): Future[BusinessProcess] = {
    val action = for {
      rowUpdated <- BpmRepositorySchema.businessProcesses
                      .filter(_.id === payload.id)
                      .map(rec => (rec.processDefinitionId, rec.updatedBy, rec.updatedAt))
                      .update((payload.processDefinitionId, payload.updatedBy, Instant.now))
      rec        <- BpmRepositorySchema.businessProcesses.filter(_.id === payload.id).result
    } yield
      if (rowUpdated == 1 && rec.headOption.nonEmpty)
        rec.head
          .into[BusinessProcess]
          .withFieldConst(_.variables, Seq.empty)
          .withFieldComputed(_.updatedAt, _.updatedAt.atOffset(ZoneOffset.UTC))
          .transform
      else throw BusinessProcessUpdateFailed("updateBusinessProcessProcessDefinition", rowUpdated.toString)
    db.run(action.transactionally)
  }

  def storeBusinessProcessVariable(payload: StoreBusinessProcessVariablePayload): Future[Done] = {
    val businessProcessVarRecord = payload.transformInto[BusinessProcessVariableRecord]
    val action                   = for {
      rowUpdated <- BpmRepositorySchema.businessProcessVariables.insertOrUpdate(businessProcessVarRecord)
      _          <- payload.oldVariableName
                      .map(variableName =>
                        BpmRepositorySchema.businessProcessVariables
                          .filter(r => r.businessProcessId === payload.businessProcessId && r.variableName === variableName)
                          .delete
                      )
                      .getOrElse(DBIO.successful(0))
    } yield
      if (rowUpdated == 1) Done
      else throw BusinessProcessUpdateFailed("deleteBusinessProcessVariable", rowUpdated.toString)
    db.run(action.transactionally)
  }

  def deleteBusinessProcessVariable(payload: DeleteBusinessProcessVariablePayload): Future[Done] = {
    val action = for {
      rowDeleted <-
        BpmRepositorySchema.businessProcessVariables
          .filter(r => r.businessProcessId === payload.businessProcessId && r.variableName === payload.variableName)
          .delete
    } yield
      if (rowDeleted == 1) Done
      else throw BusinessProcessUpdateFailed("deleteBusinessProcessVariable", rowDeleted.toString)
    db.run(action.transactionally)
  }

  def deleteBusinessProcess(payload: DeleteBusinessProcessPayload): Future[Done] = {
    val action = for {
      _          <- BpmRepositorySchema.businessProcessVariables.filter(_.businessProcessId === payload.id).delete
      rowDeleted <- BpmRepositorySchema.businessProcesses.filter(_.id === payload.id).delete
    } yield
      if (rowDeleted == 1) Done
      else throw BusinessProcessUpdateFailed("deleteBusinessProcess", rowDeleted.toString)
    db.run(action.transactionally)
  }

  def getBusinessProcessById(id: String, withVariables: Boolean): Future[BusinessProcess] = {
    val businessProcessId = BusinessProcessId(id)
    val action            = for {
      ds   <- BpmRepositorySchema.businessProcesses
                .filter(_.id === businessProcessId)
                .result
      vars <- if (ds.nonEmpty && withVariables)
                BpmRepositorySchema.businessProcessVariables
                  .filter(_.businessProcessId === businessProcessId)
                  .result
              else DBIO.successful(Seq.empty)

    } yield ds.headOption.map(
      _.into[BusinessProcess]
        .withFieldConst(_.variables, vars.map(_.transformInto[BusinessProcessVariable]))
        .withFieldComputed(_.updatedAt, _.updatedAt.atOffset(ZoneOffset.UTC))
        .transform
    )
    for {
      res <- db.run(action.transactionally)
    } yield res.getOrElse(throw BusinessProcessNotFound(id))
  }

  def getBusinessProcessesById(ids: Seq[BusinessProcessId], withVariables: Boolean): Future[Seq[BusinessProcess]] = {
    val action = for {
      ds   <- BpmRepositorySchema.businessProcesses
                .filter(_.id inSet ids)
                .result
      vars <- if (ds.nonEmpty && withVariables)
                BpmRepositorySchema.businessProcessVariables
                  .filter(_.businessProcessId inSet ids)
                  .result
              else DBIO.successful(Seq.empty)

    } yield {
      val varMap =
        vars.groupBy(_.businessProcessId).map { case k -> v => k -> v.map(_.transformInto[BusinessProcessVariable]) }
      ds.map(d =>
        d.into[BusinessProcess]
          .withFieldConst(_.variables, varMap.get(d.id).getOrElse(Seq.empty))
          .withFieldComputed(_.updatedAt, _.updatedAt.atOffset(ZoneOffset.UTC))
          .transform
      )
    }
    db.run(action.transactionally)
  }

  def findBusinessProcesses(query: BusinessProcessFindQuery): Future[FindResult] = {
    val filteredQuery =
      BpmRepositorySchema.businessProcesses.filter(rec =>
        List(
          query.filter.map(filter => (rec.name like s"%$filter%"))
        ).collect({ case Some(criteria) => criteria }).reduceLeftOption(_ && _).getOrElse(true: Rep[Boolean])
      )
    val sortedQuery   = query.sortBy
      .flatMap(_.headOption)
      .map {
        case SortBy("id", descending)   =>
          filteredQuery.sortBy(r => if (descending.getOrElse(false)) r.id.desc else r.id.asc)
        case SortBy("name", descending) =>
          filteredQuery.sortBy(r => if (descending.getOrElse(false)) r.name.desc else r.name.asc)
        case _                          => filteredQuery
      }
      .getOrElse(filteredQuery)
    val action        = for {
      total <- filteredQuery.length.result
      res   <- sortedQuery.drop(query.offset).take(query.size).map(r => r.id -> r.updatedAt).result
    } yield FindResult(
      total.toLong,
      res.map { case id -> updatedAt => HitResult(id.value, 0, updatedAt.atOffset(ZoneOffset.UTC)) }
    )
    db.run(action)
  }

}
