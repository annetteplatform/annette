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
import biz.lobachev.annette.bpm_repository.api.bp.{
  BusinessProcess,
  BusinessProcessFindQuery,
  CreateBusinessProcessPayload,
  DeleteBusinessProcessPayload,
  DeleteBusinessProcessVariablePayload,
  StoreBusinessProcessVariablePayload,
  UpdateBusinessProcessBmpModelPayload,
  UpdateBusinessProcessDataSchemaPayload,
  UpdateBusinessProcessDescriptionPayload,
  UpdateBusinessProcessNamePayload,
  UpdateBusinessProcessPayload,
  UpdateBusinessProcessProcessDefinitionPayload
}
import biz.lobachev.annette.bpm_repository.api.domain.{BpmModelId, BusinessProcessId, DataSchemaId}
import biz.lobachev.annette.bpm_repository.api.model.{
  BpmModel,
  BpmModelFindQuery,
  CreateBpmModelPayload,
  DeleteBpmModelPayload,
  UpdateBpmModelDescriptionPayload,
  UpdateBpmModelNamePayload,
  UpdateBpmModelPayload,
  UpdateBpmModelXmlPayload
}
import biz.lobachev.annette.bpm_repository.api.schema.{
  CreateDataSchemaPayload,
  DataSchema,
  DataSchemaFindQuery,
  DeleteDataSchemaPayload,
  DeleteDataSchemaVariablePayload,
  StoreDataSchemaVariablePayload,
  UpdateDataSchemaDescriptionPayload,
  UpdateDataSchemaNamePayload,
  UpdateDataSchemaPayload
}
import biz.lobachev.annette.core.model.indexing.FindResult

import scala.concurrent.Future

class BpmRepositoryServiceImpl(api: BpmRepositoryServiceApi) extends BpmRepositoryService {
  override def createBpmModel(payload: CreateBpmModelPayload): Future[BpmModel] =
    api.createBpmModel.invoke(payload)

  def updateBpmModel(payload: UpdateBpmModelPayload): Future[BpmModel] =
    api.updateBpmModel.invoke(payload)

  override def updateBpmModelName(payload: UpdateBpmModelNamePayload): Future[BpmModel] =
    api.updateBpmModelName.invoke(payload)

  override def updateBpmModelDescription(payload: UpdateBpmModelDescriptionPayload): Future[BpmModel] =
    api.updateBpmModelDescription.invoke(payload)

  override def updateBpmModelXml(payload: UpdateBpmModelXmlPayload): Future[BpmModel] =
    api.updateBpmModelXml.invoke(payload)

  override def deleteBpmModel(payload: DeleteBpmModelPayload): Future[Done] =
    api.deleteBpmModel.invoke(payload)

  override def getBpmModelById(id: String, withXml: Boolean): Future[BpmModel] =
    api.getBpmModelById(id, withXml).invoke()

  override def getBpmModelsById(ids: Seq[BpmModelId], withXml: Boolean): Future[Seq[BpmModel]] =
    api.getBpmModelsById(withXml).invoke(ids)

  override def findBpmModels(query: BpmModelFindQuery): Future[FindResult] =
    api.findBpmModels.invoke(query)

  override def createDataSchema(payload: CreateDataSchemaPayload): Future[DataSchema] =
    api.createDataSchema.invoke(payload)

  override def updateDataSchema(payload: UpdateDataSchemaPayload): Future[DataSchema] =
    api.updateDataSchema.invoke(payload)

  override def updateDataSchemaName(payload: UpdateDataSchemaNamePayload): Future[DataSchema] =
    api.updateDataSchemaName.invoke(payload)

  override def updateDataSchemaDescription(payload: UpdateDataSchemaDescriptionPayload): Future[DataSchema] =
    api.updateDataSchemaDescription.invoke(payload)

  override def storeDataSchemaVariable(payload: StoreDataSchemaVariablePayload): Future[DataSchema] =
    api.storeDataSchemaVariable.invoke(payload)

  override def deleteDataSchemaVariable(payload: DeleteDataSchemaVariablePayload): Future[DataSchema] =
    api.deleteDataSchemaVariable.invoke(payload)

  override def deleteDataSchema(payload: DeleteDataSchemaPayload): Future[Done] =
    api.deleteDataSchema.invoke(payload)

  override def getDataSchemaById(id: String, withVariables: Boolean): Future[DataSchema] =
    api.getDataSchemaById(id, withVariables).invoke()

  override def getDataSchemasById(ids: Seq[DataSchemaId], withVariables: Boolean): Future[Seq[DataSchema]] =
    api.getDataSchemasById(withVariables).invoke(ids)

  override def findDataSchemas(query: DataSchemaFindQuery): Future[FindResult] =
    api.findDataSchemas.invoke(query)

  override def createBusinessProcess(payload: CreateBusinessProcessPayload): Future[BusinessProcess] =
    api.createBusinessProcess.invoke(payload)

  override def updateBusinessProcess(payload: UpdateBusinessProcessPayload): Future[BusinessProcess] =
    api.updateBusinessProcess.invoke(payload)

  override def updateBusinessProcessName(payload: UpdateBusinessProcessNamePayload): Future[BusinessProcess] =
    api.updateBusinessProcessName.invoke(payload)

  override def updateBusinessProcessDescription(
    payload: UpdateBusinessProcessDescriptionPayload
  ): Future[BusinessProcess] =
    api.updateBusinessProcessDescription.invoke(payload)

  override def updateBusinessProcessBmpModel(payload: UpdateBusinessProcessBmpModelPayload): Future[BusinessProcess] =
    api.updateBusinessProcessBmpModel.invoke(payload)

  override def updateBusinessProcessDataSchema(
    payload: UpdateBusinessProcessDataSchemaPayload
  ): Future[BusinessProcess] =
    api.updateBusinessProcessDataSchema.invoke(payload)

  override def updateBusinessProcessProcessDefinition(
    payload: UpdateBusinessProcessProcessDefinitionPayload
  ): Future[BusinessProcess] =
    api.updateBusinessProcessProcessDefinition.invoke(payload)

  override def storeBusinessProcessVariable(payload: StoreBusinessProcessVariablePayload): Future[Done] =
    api.storeBusinessProcessVariable.invoke(payload)

  override def deleteBusinessProcessVariable(payload: DeleteBusinessProcessVariablePayload): Future[Done] =
    api.deleteBusinessProcessVariable.invoke(payload)

  override def deleteBusinessProcess(payload: DeleteBusinessProcessPayload): Future[Done] =
    api.deleteBusinessProcess.invoke(payload)

  override def getBusinessProcessById(id: String, withVariables: Boolean): Future[BusinessProcess] =
    api.getBusinessProcessById(id, withVariables).invoke()

  override def getBusinessProcessesById(
    ids: Seq[BusinessProcessId],
    withVariables: Boolean
  ): Future[Seq[BusinessProcess]] =
    api.getBusinessProcessesById(withVariables).invoke(ids)

  override def findBusinessProcesses(query: BusinessProcessFindQuery): Future[FindResult] =
    api.findBusinessProcesses.invoke(query)
}
