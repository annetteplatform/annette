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
import biz.lobachev.annette.bpm_repository.api.domain.BusinessProcessId
import biz.lobachev.annette.bpm_repository.api.bp._
import biz.lobachev.annette.bpm_repository.impl.db.{
  BpmRepositorySchema,
  BpmRepositorySchemaImplicits,
  BusinessProcessRecord,
  BusinessProcessVariableRecord
}
import biz.lobachev.annette.bpm_repository.impl.schema.DataSchemaQueries
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.indexing.{FindResult, HitResult}
import io.scalaland.chimney.dsl._
import slick.dbio.DBIOAction
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._

import java.time.{Instant, ZoneOffset}
import scala.concurrent.ExecutionContext

class BusinessProcessActions(implicit executionContext: ExecutionContext) extends BpmRepositorySchemaImplicits {

  def createBusinessProcessAction(payload: CreateBusinessProcessPayload, updatedAt: Instant) = {
    val businessProcessRecord = payload
      .into[BusinessProcessRecord]
      .withFieldConst(_.updatedAt, updatedAt)
      .transform
    val payloadVarRecMap      =
      BusinessProcessVariableRecord
        .fromBusinessProcessVariables(payload.id, payload.variables)
        .map(v => v.variableName -> v)
        .toMap
    for {
      dsVars <- payload.dataSchemaId
                  .map(dataSchemaId => DataSchemaQueries.getDataSchemaVariables(dataSchemaId).result)
                  .getOrElse(DBIOAction.successful(Seq.empty))
      dsVarRecMap               = dsVars
                                    .map(v =>
                                      v.variableName -> v
                                        .into[BusinessProcessVariableRecord]
                                        .withFieldConst(_.businessProcessId, payload.id)
                                        .transform
                                    )
                                    .toMap
      businessProcessVarRecords = (dsVarRecMap ++ payloadVarRecMap).values
      _                        <- BpmRepositorySchema.businessProcesses += businessProcessRecord
      _                        <- BpmRepositorySchema.businessProcessVariables ++= businessProcessVarRecords
    } yield Done
  }

  def updateBusinessProcessAction(payload: UpdateBusinessProcessPayload, updatedAt: Instant) = {
    val createPayload = payload.transformInto[CreateBusinessProcessPayload]
    for {
      _ <- deleteBusinessProcessAction(payload.id)
      _ <- createBusinessProcessAction(createPayload, updatedAt)
    } yield Done
  }

  def updateNameAction(payload: UpdateBusinessProcessNamePayload, updatedAt: Instant) =
    BpmRepositorySchema.businessProcesses
      .filter(_.id === payload.id)
      .map(rec => (rec.name, rec.updatedBy, rec.updatedAt))
      .update((payload.name, payload.updatedBy, updatedAt))

  def updateDescriptionAction(payload: UpdateBusinessProcessDescriptionPayload, updatedAt: Instant) =
    BpmRepositorySchema.businessProcesses
      .filter(_.id === payload.id)
      .map(rec => (rec.description, rec.updatedBy, rec.updatedAt))
      .update((payload.description, payload.updatedBy, updatedAt))

  def updateBusinessProcessBpmModelAction(payload: UpdateBusinessProcessBpmModelPayload, updatedAt: Instant) =
    BpmRepositorySchema.businessProcesses
      .filter(_.id === payload.id)
      .map(rec => (rec.bpmModelId, rec.updatedBy, rec.updatedAt))
      .update((payload.bpmModelId, payload.updatedBy, updatedAt))

  def updateBusinessProcessDataSchemaAction(payload: UpdateBusinessProcessDataSchemaPayload, updatedAt: Instant) =
    for {
      dataSchemaVars   <- payload.dataSchemaId
                            .map(dataSchemaId => DataSchemaQueries.getDataSchemaVariables(dataSchemaId).result)
                            .getOrElse(DBIOAction.successful(Seq.empty))
      dataSchemaVarRecs = dataSchemaVars
                            .map(
                              _.into[BusinessProcessVariableRecord]
                                .withFieldConst(_.businessProcessId, payload.id)
                                .transform
                            )
      _                <- BpmRepositorySchema.businessProcesses
                            .filter(_.id === payload.id)
                            .map(rec => (rec.dataSchemaId, rec.updatedBy, rec.updatedAt))
                            .update((payload.dataSchemaId, payload.updatedBy, updatedAt))
      _                <- DBIO.sequence(
                            dataSchemaVarRecs.map(r => BpmRepositorySchema.businessProcessVariables.insertOrUpdate(r))
                          )
    } yield Done

  def updateBusinessProcessProcessDefinitionAction(
    payload: UpdateBusinessProcessProcessDefinitionPayload,
    updatedAt: Instant
  ) =
    BpmRepositorySchema.businessProcesses
      .filter(_.id === payload.id)
      .map(rec => (rec.processDefinitionId, rec.updatedBy, rec.updatedAt))
      .update((payload.processDefinitionId, payload.updatedBy, updatedAt))

  def updateUpdatedAction(id: BusinessProcessId, updatedBy: AnnettePrincipal, updatedAt: Instant) =
    BpmRepositorySchema.businessProcesses
      .filter(_.id === id)
      .map(rec => (rec.updatedBy, rec.updatedAt))
      .update((updatedBy, updatedAt))

  def storeBusinessProcessVariableAction(payload: StoreBusinessProcessVariablePayload, updatedAt: Instant) = {
    val businessProcessVarRecord = payload.transformInto[BusinessProcessVariableRecord]
    for {
      _ <- updateUpdatedAction(payload.businessProcessId, payload.updatedBy, updatedAt)
      _ <- payload.oldVariableName
             .map(variableName =>
               BpmRepositorySchema.businessProcessVariables
                 .filter(r => r.businessProcessId === payload.businessProcessId && r.variableName === variableName)
                 .delete
             )
             .getOrElse(DBIO.successful(0))
      _ <- BpmRepositorySchema.businessProcessVariables.insertOrUpdate(businessProcessVarRecord)
    } yield Done
  }

  def deleteBusinessProcessVariableAction(payload: DeleteBusinessProcessVariablePayload, updatedAt: Instant) =
    for {
      _ <- BpmRepositorySchema.businessProcesses
             .filter(_.id === payload.businessProcessId)
             .map(rec => (rec.updatedBy, rec.updatedAt))
             .update((payload.updatedBy, updatedAt))
      _ <- BpmRepositorySchema.businessProcessVariables
             .filter(r => r.businessProcessId === payload.businessProcessId && r.variableName === payload.variableName)
             .delete
    } yield Done

  def deleteBusinessProcessAction(
    payload: DeleteBusinessProcessPayload
  ): DBIOAction[Done.type, PostgresProfile.api.NoStream, Effect.Write] =
    deleteBusinessProcessAction(payload.id)

  def deleteBusinessProcessAction(id: BusinessProcessId): DBIOAction[Done.type, NoStream, Effect.Write] =
    for {
      _ <- BpmRepositorySchema.businessProcessVariables.filter(_.businessProcessId === id).delete
      _ <- BpmRepositorySchema.businessProcesses.filter(_.id === id).delete
    } yield Done

  def getBusinessProcessAction(id: BusinessProcessId, withVariables: Boolean) =
    for {
      ds   <- BpmRepositorySchema.businessProcesses
                .filter(_.id === id)
                .result
      vars <- if (ds.nonEmpty && withVariables)
                BpmRepositorySchema.businessProcessVariables
                  .filter(_.businessProcessId === id)
                  .result
              else DBIO.successful(Seq.empty)

    } yield ds.headOption.map(
      _.into[BusinessProcess]
        .withFieldConst(_.variables, BusinessProcessVariableRecord.toBusinessProcessVariableMap(vars))
        .withFieldComputed(_.updatedAt, _.updatedAt.atOffset(ZoneOffset.UTC))
        .transform
    )

  def getBusinessProcessesAction(ids: Seq[BusinessProcessId], withVariables: Boolean) =
    for {
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
        vars.groupBy(_.businessProcessId).map {
          case k -> v => k -> BusinessProcessVariableRecord.toBusinessProcessVariableMap(v)
        }
      ds.map(d =>
        d.into[BusinessProcess]
          .withFieldConst(_.variables, varMap.getOrElse(d.id, Map.empty))
          .withFieldComputed(_.updatedAt, _.updatedAt.atOffset(ZoneOffset.UTC))
          .transform
      )
    }

  def findBusinessProcessesAction(query: BusinessProcessFindQuery) = {
    val filteredQuery = BusinessProcessQueries.getFilteredQuery(query)
    val sortedQuery   = query.sortBy
      .flatMap(_.headOption)
      .map(sortBy => BusinessProcessQueries.getSortedQuery(filteredQuery, sortBy))
      .getOrElse(filteredQuery)
    for {
      total <- filteredQuery.length.result
      res   <- sortedQuery.drop(query.offset).take(query.size).map(r => r.id -> r.updatedAt).result
    } yield FindResult(
      total.toLong,
      res.map { case id -> updatedAt => HitResult(id.value, 0, updatedAt.atOffset(ZoneOffset.UTC)) }
    )
  }
}
