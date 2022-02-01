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

package biz.lobachev.annette.bpm_repository.impl

import akka.{Done, NotUsed}
import biz.lobachev.annette.bpm_repository.api.BpmRepositoryServiceApi
import biz.lobachev.annette.bpm_repository.api.domain.{BpmModelId, BusinessProcessId, DataSchemaId}
import biz.lobachev.annette.bpm_repository.api.model._
import biz.lobachev.annette.bpm_repository.api.schema._
import biz.lobachev.annette.bpm_repository.api.bp._
import biz.lobachev.annette.bpm_repository.impl.bp.BusinessProcessService
import biz.lobachev.annette.bpm_repository.impl.db.BpmRepositorySchemaImplicits
import biz.lobachev.annette.bpm_repository.impl.model.BpmModelService
import biz.lobachev.annette.bpm_repository.impl.schema.DataSchemaService
import biz.lobachev.annette.core.model.indexing.FindResult
import com.lightbend.lagom.scaladsl.api.ServiceCall

class BpmRepositoryServiceApiImpl(
  bpmModelService: BpmModelService,
  dataSchemaService: DataSchemaService,
  businessProcessService: BusinessProcessService
) extends BpmRepositoryServiceApi
    with BpmRepositorySchemaImplicits {

  override def createBpmModel: ServiceCall[CreateBpmModelPayload, BpmModel] =
    ServiceCall { payload =>
      bpmModelService.createBpmModel(payload)
    }

  override def updateBpmModel: ServiceCall[UpdateBpmModelPayload, BpmModel] =
    ServiceCall { payload =>
      bpmModelService.updateBpmModel(payload)
    }

  override def updateBpmModelName: ServiceCall[UpdateBpmModelNamePayload, BpmModel] =
    ServiceCall { payload =>
      bpmModelService.updateBpmModelName(payload)
    }

  override def updateBpmModelDescription: ServiceCall[UpdateBpmModelDescriptionPayload, BpmModel] =
    ServiceCall { payload =>
      bpmModelService.updateBpmModelDescription(payload)
    }

  override def updateBpmModelXml: ServiceCall[UpdateBpmModelXmlPayload, BpmModel] =
    ServiceCall { payload =>
      bpmModelService.updateBpmModelXml(payload)
    }

  override def deleteBpmModel: ServiceCall[DeleteBpmModelPayload, Done] =
    ServiceCall { payload =>
      bpmModelService.deleteBpmModel(payload)
    }

  override def getBpmModelById(id: String, withXml: Boolean): ServiceCall[NotUsed, BpmModel] =
    ServiceCall { _ =>
      bpmModelService.getBpmModelById(id, withXml)
    }

  override def getBpmModelsById(withXml: Boolean): ServiceCall[Seq[BpmModelId], Seq[BpmModel]] =
    ServiceCall { ids =>
      bpmModelService.getBpmModelsById(ids, withXml)
    }

  override def findBpmModels: ServiceCall[BpmModelFindQuery, FindResult] =
    ServiceCall { query =>
      bpmModelService.findBpmModels(query)
    }

  override def createDataSchema: ServiceCall[CreateDataSchemaPayload, DataSchema] =
    ServiceCall { payload =>
      dataSchemaService.createDataSchema(payload)
    }

  override def updateDataSchema: ServiceCall[UpdateDataSchemaPayload, DataSchema] =
    ServiceCall { payload =>
      dataSchemaService.updateDataSchema(payload)
    }

  override def updateDataSchemaName: ServiceCall[UpdateDataSchemaNamePayload, DataSchema] =
    ServiceCall { payload =>
      dataSchemaService.updateDataSchemaName(payload)
    }

  override def updateDataSchemaDescription: ServiceCall[UpdateDataSchemaDescriptionPayload, DataSchema] =
    ServiceCall { payload =>
      dataSchemaService.updateDataSchemaDescription(payload)
    }

  def storeDataSchemaVariable: ServiceCall[StoreDataSchemaVariablePayload, DataSchema] =
    ServiceCall { payload =>
      dataSchemaService.storeDataSchemaVariable(payload)
    }

  def deleteDataSchemaVariable: ServiceCall[DeleteDataSchemaVariablePayload, DataSchema] =
    ServiceCall { payload =>
      dataSchemaService.deleteDataSchemaVariable(payload)
    }

  override def deleteDataSchema: ServiceCall[DeleteDataSchemaPayload, Done] =
    ServiceCall { payload =>
      dataSchemaService.deleteDataSchema(payload)
    }

  override def getDataSchemaById(id: String, withVariables: Boolean): ServiceCall[NotUsed, DataSchema] =
    ServiceCall { _ =>
      dataSchemaService.getDataSchemaById(id, withVariables)
    }

  override def getDataSchemasById(withVariables: Boolean): ServiceCall[Seq[DataSchemaId], Seq[DataSchema]] =
    ServiceCall { ids =>
      dataSchemaService.getDataSchemasById(ids, withVariables)
    }

  override def findDataSchemas: ServiceCall[DataSchemaFindQuery, FindResult] =
    ServiceCall { query =>
      dataSchemaService.findDataSchemas(query)
    }

  override def createBusinessProcess: ServiceCall[CreateBusinessProcessPayload, BusinessProcess] =
    ServiceCall { payload =>
      businessProcessService.createBusinessProcess(payload)
    }

  override def updateBusinessProcess: ServiceCall[UpdateBusinessProcessPayload, BusinessProcess] =
    ServiceCall { payload =>
      businessProcessService.updateBusinessProcess(payload)
    }

  override def updateBusinessProcessName: ServiceCall[UpdateBusinessProcessNamePayload, BusinessProcess] =
    ServiceCall { payload =>
      businessProcessService.updateBusinessProcessName(payload)
    }

  override def updateBusinessProcessDescription: ServiceCall[UpdateBusinessProcessDescriptionPayload, BusinessProcess] =
    ServiceCall { payload =>
      businessProcessService.updateBusinessProcessDescription(payload)
    }

  def updateBusinessProcessBmpModel: ServiceCall[UpdateBusinessProcessBmpModelPayload, BusinessProcess] =
    ServiceCall { payload =>
      businessProcessService.updateBusinessProcessBmpModel(payload)
    }

  def updateBusinessProcessDataSchema: ServiceCall[UpdateBusinessProcessDataSchemaPayload, BusinessProcess] =
    ServiceCall { payload =>
      businessProcessService.updateBusinessProcessDataSchema(payload)
    }

  def updateBusinessProcessProcessDefinition
    : ServiceCall[UpdateBusinessProcessProcessDefinitionPayload, BusinessProcess] =
    ServiceCall { payload =>
      businessProcessService.updateBusinessProcessProcessDefinition(payload)
    }

  def storeBusinessProcessVariable: ServiceCall[StoreBusinessProcessVariablePayload, BusinessProcess] =
    ServiceCall { payload =>
      businessProcessService.storeBusinessProcessVariable(payload)
    }

  def deleteBusinessProcessVariable: ServiceCall[DeleteBusinessProcessVariablePayload, BusinessProcess] =
    ServiceCall { payload =>
      businessProcessService.deleteBusinessProcessVariable(payload)
    }

  override def deleteBusinessProcess: ServiceCall[DeleteBusinessProcessPayload, Done] =
    ServiceCall { payload =>
      businessProcessService.deleteBusinessProcess(payload)
    }

  override def getBusinessProcessById(id: String, withVariables: Boolean): ServiceCall[NotUsed, BusinessProcess] =
    ServiceCall { _ =>
      businessProcessService.getBusinessProcessById(id, withVariables)
    }

  override def getBusinessProcessesById(
    withVariables: Boolean
  ): ServiceCall[Seq[BusinessProcessId], Seq[BusinessProcess]] =
    ServiceCall { ids =>
      businessProcessService.getBusinessProcessesById(ids, withVariables)
    }

  override def findBusinessProcesses: ServiceCall[BusinessProcessFindQuery, FindResult] =
    ServiceCall { query =>
      businessProcessService.findBusinessProcesses(query)
    }
}
