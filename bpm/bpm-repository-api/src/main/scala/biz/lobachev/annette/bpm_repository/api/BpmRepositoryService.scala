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

package biz.lobachev.annette.bpm_repository.api

import akka.Done
import biz.lobachev.annette.bpm_repository.api.bp._
import biz.lobachev.annette.bpm_repository.api.domain.{BpmModelId, BusinessProcessId, DataSchemaId}
import biz.lobachev.annette.bpm_repository.api.model._
import biz.lobachev.annette.bpm_repository.api.schema._
import biz.lobachev.annette.core.model.indexing.FindResult

import scala.concurrent.Future

trait BpmRepositoryService {

  def createBpmModel(payload: CreateBpmModelPayload): Future[BpmModel]
  def updateBpmModel(payload: UpdateBpmModelPayload): Future[BpmModel]
  def updateBpmModelName(payload: UpdateBpmModelNamePayload): Future[BpmModel]
  def updateBpmModelDescription(payload: UpdateBpmModelDescriptionPayload): Future[BpmModel]
  def updateBpmModelXml(payload: UpdateBpmModelXmlPayload): Future[BpmModel]
  def deleteBpmModel(payload: DeleteBpmModelPayload): Future[Done]
  def getBpmModelById(id: String, withXml: Boolean): Future[BpmModel]
  def getBpmModelsById(ids: Seq[BpmModelId], withXml: Boolean): Future[Seq[BpmModel]]
  def findBpmModels(query: BpmModelFindQuery): Future[FindResult]

  def createDataSchema(payload: CreateDataSchemaPayload): Future[DataSchema]
  def updateDataSchema(payload: UpdateDataSchemaPayload): Future[DataSchema]
  def updateDataSchemaName(payload: UpdateDataSchemaNamePayload): Future[DataSchema]
  def updateDataSchemaDescription(payload: UpdateDataSchemaDescriptionPayload): Future[DataSchema]
  def storeDataSchemaVariable(payload: StoreDataSchemaVariablePayload): Future[DataSchema]
  def deleteDataSchemaVariable(payload: DeleteDataSchemaVariablePayload): Future[DataSchema]
  def deleteDataSchema(payload: DeleteDataSchemaPayload): Future[Done]
  def getDataSchemaById(id: String, withVariables: Boolean): Future[DataSchema]
  def getDataSchemasById(ids: Seq[DataSchemaId], withVariables: Boolean): Future[Seq[DataSchema]]
  def findDataSchemas(query: DataSchemaFindQuery): Future[FindResult]

  def createBusinessProcess(payload: CreateBusinessProcessPayload): Future[BusinessProcess]
  def updateBusinessProcess(payload: UpdateBusinessProcessPayload): Future[BusinessProcess]
  def updateBusinessProcessName(payload: UpdateBusinessProcessNamePayload): Future[BusinessProcess]
  def updateBusinessProcessDescription(payload: UpdateBusinessProcessDescriptionPayload): Future[BusinessProcess]
  def updateBusinessProcessBmpModel(payload: UpdateBusinessProcessBmpModelPayload): Future[BusinessProcess]
  def updateBusinessProcessDataSchema(payload: UpdateBusinessProcessDataSchemaPayload): Future[BusinessProcess]
  def updateBusinessProcessProcessDefinition(
    payload: UpdateBusinessProcessProcessDefinitionPayload
  ): Future[BusinessProcess]
  def storeBusinessProcessVariable(payload: StoreBusinessProcessVariablePayload): Future[Done]
  def deleteBusinessProcessVariable(payload: DeleteBusinessProcessVariablePayload): Future[Done]
  def deleteBusinessProcess(payload: DeleteBusinessProcessPayload): Future[Done]
  def getBusinessProcessById(id: String, withVariables: Boolean): Future[BusinessProcess]
  def getBusinessProcessesById(ids: Seq[BusinessProcessId], withVariables: Boolean): Future[Seq[BusinessProcess]]
  def findBusinessProcesses(query: BusinessProcessFindQuery): Future[FindResult]
}
