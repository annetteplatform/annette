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

import akka.{Done, NotUsed}
import biz.lobachev.annette.bpm_repository.api.bp.{
  BusinessProcess,
  BusinessProcessFindQuery,
  CreateBusinessProcessPayload,
  DeleteBusinessProcessPayload,
  DeleteBusinessProcessVariablePayload,
  StoreBusinessProcessVariablePayload,
  UpdateBusinessProcessBpmModelPayload,
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
import biz.lobachev.annette.core.exception.AnnetteTransportExceptionSerializer
import biz.lobachev.annette.core.model.indexing.FindResult
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}

trait BpmRepositoryServiceApi extends Service {

  def createBpmModel: ServiceCall[CreateBpmModelPayload, BpmModel]
  def updateBpmModel: ServiceCall[UpdateBpmModelPayload, BpmModel]
  def updateBpmModelName: ServiceCall[UpdateBpmModelNamePayload, BpmModel]
  def updateBpmModelDescription: ServiceCall[UpdateBpmModelDescriptionPayload, BpmModel]
  def updateBpmModelXml: ServiceCall[UpdateBpmModelXmlPayload, BpmModel]
  def deleteBpmModel: ServiceCall[DeleteBpmModelPayload, Done]
  def getBpmModel(id: String, withXml: Option[Boolean]): ServiceCall[NotUsed, BpmModel]
  def getBpmModels(withXml: Option[Boolean]): ServiceCall[Seq[BpmModelId], Seq[BpmModel]]
  def findBpmModels: ServiceCall[BpmModelFindQuery, FindResult]

  def createDataSchema: ServiceCall[CreateDataSchemaPayload, DataSchema]
  def updateDataSchema: ServiceCall[UpdateDataSchemaPayload, DataSchema]
  def updateDataSchemaName: ServiceCall[UpdateDataSchemaNamePayload, DataSchema]
  def updateDataSchemaDescription: ServiceCall[UpdateDataSchemaDescriptionPayload, DataSchema]
  def storeDataSchemaVariable: ServiceCall[StoreDataSchemaVariablePayload, DataSchema]
  def deleteDataSchemaVariable: ServiceCall[DeleteDataSchemaVariablePayload, DataSchema]
  def deleteDataSchema: ServiceCall[DeleteDataSchemaPayload, Done]
  def getDataSchema(id: String, withVariables: Option[Boolean]): ServiceCall[NotUsed, DataSchema]
  def getDataSchemas(withVariables: Option[Boolean]): ServiceCall[Seq[DataSchemaId], Seq[DataSchema]]
  def findDataSchemas: ServiceCall[DataSchemaFindQuery, FindResult]

  def createBusinessProcess: ServiceCall[CreateBusinessProcessPayload, BusinessProcess]
  def updateBusinessProcess: ServiceCall[UpdateBusinessProcessPayload, BusinessProcess]
  def updateBusinessProcessName: ServiceCall[UpdateBusinessProcessNamePayload, BusinessProcess]
  def updateBusinessProcessDescription: ServiceCall[UpdateBusinessProcessDescriptionPayload, BusinessProcess]
  def updateBusinessProcessBpmModel: ServiceCall[UpdateBusinessProcessBpmModelPayload, BusinessProcess]
  def updateBusinessProcessDataSchema: ServiceCall[UpdateBusinessProcessDataSchemaPayload, BusinessProcess]
  def updateBusinessProcessProcessDefinition
    : ServiceCall[UpdateBusinessProcessProcessDefinitionPayload, BusinessProcess]
  def storeBusinessProcessVariable: ServiceCall[StoreBusinessProcessVariablePayload, BusinessProcess]
  def deleteBusinessProcessVariable: ServiceCall[DeleteBusinessProcessVariablePayload, BusinessProcess]
  def deleteBusinessProcess: ServiceCall[DeleteBusinessProcessPayload, Done]
  def getBusinessProcess(id: String, withVariables: Option[Boolean]): ServiceCall[NotUsed, BusinessProcess]
  def getBusinessProcesses(
    withVariables: Option[Boolean]
  ): ServiceCall[Seq[BusinessProcessId], Seq[BusinessProcess]]
  def findBusinessProcesses: ServiceCall[BusinessProcessFindQuery, FindResult]

  final override def descriptor = {
    import Service._
    named("bpm-repository")
      .withCalls(
        pathCall("/api/bpm-repository/v1/createBpmModel", createBpmModel),
        pathCall("/api/bpm-repository/v1/updateBpmModel", updateBpmModel),
        pathCall("/api/bpm-repository/v1/updateBpmModelName", updateBpmModelName),
        pathCall("/api/bpm-repository/v1/updateBpmModelDescription", updateBpmModelDescription),
        pathCall("/api/bpm-repository/v1/updateBpmModelXml", updateBpmModelXml),
        pathCall("/api/bpm-repository/v1/deleteBpmModel", deleteBpmModel),
        pathCall("/api/bpm-repository/v1/getBpmModel/:id?withXml", getBpmModel _),
        pathCall("/api/bpm-repository/v1/getBpmModels?withXml", getBpmModels _),
        pathCall("/api/bpm-repository/v1/findBpmModels", findBpmModels),
        pathCall("/api/bpm-repository/v1/createDataSchema", createDataSchema),
        pathCall("/api/bpm-repository/v1/updateDataSchema", updateDataSchema),
        pathCall("/api/bpm-repository/v1/updateDataSchemaName", updateDataSchemaName),
        pathCall("/api/bpm-repository/v1/updateDataSchemaDescription", updateDataSchemaDescription),
        pathCall("/api/bpm-repository/v1/storeDataSchemaVariable", storeDataSchemaVariable),
        pathCall("/api/bpm-repository/v1/deleteDataSchemaVariable", deleteDataSchemaVariable),
        pathCall("/api/bpm-repository/v1/deleteDataSchema", deleteDataSchema),
        pathCall("/api/bpm-repository/v1/getDataSchema/:id?withVariables", getDataSchema _),
        pathCall("/api/bpm-repository/v1/getDataSchemas?withVariables", getDataSchemas _),
        pathCall("/api/bpm-repository/v1/findDataSchemas", findDataSchemas),
        pathCall("/api/bpm-repository/v1/createBusinessProcess", createBusinessProcess),
        pathCall("/api/bpm-repository/v1/updateBusinessProcess", updateBusinessProcess),
        pathCall("/api/bpm-repository/v1/updateBusinessProcessName", updateBusinessProcessName),
        pathCall("/api/bpm-repository/v1/updateBusinessProcessDescription", updateBusinessProcessDescription),
        pathCall("/api/bpm-repository/v1/updateBusinessProcessBmpModel", updateBusinessProcessBpmModel),
        pathCall("/api/bpm-repository/v1/updateBusinessProcessDataSchema", updateBusinessProcessDataSchema),
        pathCall(
          "/api/bpm-repository/v1/updateBusinessProcessProcessDefinition",
          updateBusinessProcessProcessDefinition
        ),
        pathCall("/api/bpm-repository/v1/storeBusinessProcessVariable", storeBusinessProcessVariable),
        pathCall("/api/bpm-repository/v1/deleteBusinessProcessVariable", deleteBusinessProcessVariable),
        pathCall("/api/bpm-repository/v1/deleteBusinessProcess", deleteBusinessProcess),
        pathCall("/api/bpm-repository/v1/getBusinessProcess/:id?withVariables", getBusinessProcess _),
        pathCall("/api/bpm-repository/v1/getBusinessProcesses?withVariables", getBusinessProcesses _),
        pathCall("/api/bpm-repository/v1/findBusinessProcesses", findBusinessProcesses)
      )
      .withExceptionSerializer(new AnnetteTransportExceptionSerializer())
      .withAutoAcl(true)
  }
}
