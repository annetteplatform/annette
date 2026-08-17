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

import com.google.protobuf.empty.Empty
import biz.lobachev.annette.bpm_repository.api.domain.{
  BpmModelId,
  BusinessProcessId,
  DataSchemaId,
  Datatype,
  Notation,
  ProcessDefinition,
  ProcessDefinitionType,
  VariableName
}
import biz.lobachev.annette.bpm_repository.api.model._
import biz.lobachev.annette.bpm_repository.api.schema._
import biz.lobachev.annette.bpm_repository.api.bp._
import biz.lobachev.annette.bpm_repository.api.{grpc => g}
import biz.lobachev.annette.bpm_repository.api.grpc.BpmRepositoryService
import biz.lobachev.annette.bpm_repository.impl.bp.BusinessProcessService
import biz.lobachev.annette.bpm_repository.impl.model.BpmModelService
import biz.lobachev.annette.bpm_repository.impl.schema.DataSchemaService
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.indexing.{FindResult, SortBy}

import scala.concurrent.{ExecutionContext, Future}

class BpmRepositoryServiceApiImpl(
  bpmModelService: BpmModelService,
  dataSchemaService: DataSchemaService,
  businessProcessService: BusinessProcessService
)(implicit
  ec: ExecutionContext
) extends BpmRepositoryService {

  // === proto <-> domain converters ===

  private def toDomain(p: g.AnnettePrincipal): AnnettePrincipal =
    AnnettePrincipal(p.code)

  private def fromDomain(p: AnnettePrincipal): g.AnnettePrincipal =
    g.AnnettePrincipal(p.code)

  private def unwrapPrincipal(p: Option[g.AnnettePrincipal]): AnnettePrincipal =
    p.map(toDomain).getOrElse(throw new IllegalArgumentException("missing principal"))

  // Notation: REST JSON wire representation is lowercase (EnumerationJsonSerializer).
  private def fromDomainNotation(n: Notation.Notation): String = n.toString.toLowerCase

  private def toNotation(s: String): Notation.Notation = Notation.withName(s.toUpperCase)

  // Datatype: REST JSON wire representation is the enumeration value name.
  private def fromDomainDatatype(d: Datatype.Datatype): String = d.toString

  private def toDatatype(s: String): Datatype.Datatype = Datatype.withName(s)

  // ProcessDefinitionType: REST JSON wire representation is lowercase.
  private def fromDomainProcessDefinitionType(t: ProcessDefinitionType.ProcessDefinitionType): String =
    t.toString.toLowerCase

  private def toProcessDefinitionType(s: String): ProcessDefinitionType.ProcessDefinitionType =
    ProcessDefinitionType.withName(s.toUpperCase)

  private def toDomainSortBy(seq: Seq[g.SortBy]): Option[Seq[SortBy]] =
    if (seq.isEmpty) None else Some(seq.map(s => SortBy(field = s.field, descending = s.descending)))

  private def fromDomain(f: FindResult): g.FindResult =
    g.FindResult(
      total = f.total,
      hits = f.hits.map(h => g.HitResult(id = h.id, score = h.score, updatedAt = h.updatedAt.toString))
    )

  // === BPM Models ===

  private def fromDomain(m: BpmModel): g.BpmModel =
    g.BpmModel(
      id = m.id.value,
      code = m.code,
      name = m.name,
      description = m.description,
      notation = fromDomainNotation(m.notation),
      xml = m.xml,
      updatedAt = m.updatedAt.toString,
      updatedBy = Some(fromDomain(m.updatedBy))
    )

  private def toDomain(p: g.CreateBpmModelPayload): CreateBpmModelPayload =
    CreateBpmModelPayload(
      id = BpmModelId(p.id),
      name = p.name,
      description = p.description,
      notation = toNotation(p.notation),
      xml = p.xml,
      updatedBy = unwrapPrincipal(p.updatedBy)
    )

  private def toDomain(p: g.UpdateBpmModelPayload): UpdateBpmModelPayload =
    UpdateBpmModelPayload(
      id = BpmModelId(p.id),
      name = p.name,
      description = p.description,
      notation = toNotation(p.notation),
      xml = p.xml,
      updatedBy = unwrapPrincipal(p.updatedBy)
    )

  private def toDomain(p: g.UpdateBpmModelNamePayload): UpdateBpmModelNamePayload =
    UpdateBpmModelNamePayload(
      id = BpmModelId(p.id),
      name = p.name,
      updatedBy = unwrapPrincipal(p.updatedBy)
    )

  private def toDomain(p: g.UpdateBpmModelDescriptionPayload): UpdateBpmModelDescriptionPayload =
    UpdateBpmModelDescriptionPayload(
      id = BpmModelId(p.id),
      description = p.description,
      updatedBy = unwrapPrincipal(p.updatedBy)
    )

  private def toDomain(p: g.UpdateBpmModelXmlPayload): UpdateBpmModelXmlPayload =
    UpdateBpmModelXmlPayload(
      id = BpmModelId(p.id),
      notation = toNotation(p.notation),
      xml = p.xml,
      updatedBy = unwrapPrincipal(p.updatedBy)
    )

  private def toDomain(p: g.DeleteBpmModelPayload): DeleteBpmModelPayload =
    DeleteBpmModelPayload(
      id = BpmModelId(p.id),
      updatedBy = unwrapPrincipal(p.updatedBy)
    )

  private def toDomain(q: g.BpmModelFindQuery): BpmModelFindQuery =
    BpmModelFindQuery(
      offset = q.offset,
      size = q.size,
      filter = q.filter,
      notations = if (q.notations.isEmpty) None else Some(q.notations.map(toNotation)),
      sortBy = toDomainSortBy(q.sortBy)
    )

  override def createBpmModel(in: g.CreateBpmModelPayload): Future[g.BpmModel] =
    bpmModelService.createBpmModel(toDomain(in)).map(fromDomain)

  override def updateBpmModel(in: g.UpdateBpmModelPayload): Future[g.BpmModel] =
    bpmModelService.updateBpmModel(toDomain(in)).map(fromDomain)

  override def updateBpmModelName(in: g.UpdateBpmModelNamePayload): Future[g.BpmModel] =
    bpmModelService.updateBpmModelName(toDomain(in)).map(fromDomain)

  override def updateBpmModelDescription(in: g.UpdateBpmModelDescriptionPayload): Future[g.BpmModel] =
    bpmModelService.updateBpmModelDescription(toDomain(in)).map(fromDomain)

  override def updateBpmModelXml(in: g.UpdateBpmModelXmlPayload): Future[g.BpmModel] =
    bpmModelService.updateBpmModelXml(toDomain(in)).map(fromDomain)

  override def deleteBpmModel(in: g.DeleteBpmModelPayload): Future[Empty] =
    bpmModelService.deleteBpmModel(toDomain(in)).map(_ => Empty())

  override def getBpmModel(in: g.GetBpmModelRequest): Future[g.BpmModel] =
    bpmModelService.getBpmModel(in.id, in.withXml.getOrElse(true)).map(fromDomain)

  override def getBpmModels(in: g.GetBpmModelsRequest): Future[g.GetBpmModelsResponse] =
    bpmModelService
      .getBpmModels(in.ids.map(BpmModelId(_)), in.withXml.getOrElse(true))
      .map(models => g.GetBpmModelsResponse(models.map(fromDomain)))

  override def findBpmModels(in: g.BpmModelFindQuery): Future[g.FindResult] =
    bpmModelService.findBpmModels(toDomain(in)).map(fromDomain)

  // === Data Schemas ===

  private def fromDomain(v: DataSchemaVariable): g.DataSchemaVariable =
    g.DataSchemaVariable(
      name = v.name,
      caption = v.caption,
      datatype = fromDomainDatatype(v.datatype),
      defaultValue = v.defaultValue
    )

  private def toDomain(v: g.DataSchemaVariable): DataSchemaVariable =
    DataSchemaVariable(
      name = v.name,
      caption = v.caption,
      datatype = toDatatype(v.datatype),
      defaultValue = v.defaultValue
    )

  private def fromDomain(s: DataSchema): g.DataSchema =
    g.DataSchema(
      id = s.id.value,
      name = s.name,
      description = s.description,
      variables = s.variables.map { case (k, v) => k -> fromDomain(v) },
      updatedAt = s.updatedAt.toString,
      updatedBy = Some(fromDomain(s.updatedBy))
    )

  private def toDomain(p: g.CreateDataSchemaPayload): CreateDataSchemaPayload =
    CreateDataSchemaPayload(
      id = DataSchemaId(p.id),
      name = p.name,
      description = p.description,
      variables = p.variables.map { case (k, v) => k -> toDomain(v) },
      updatedBy = unwrapPrincipal(p.updatedBy)
    )

  private def toDomain(p: g.UpdateDataSchemaPayload): UpdateDataSchemaPayload =
    UpdateDataSchemaPayload(
      id = DataSchemaId(p.id),
      name = p.name,
      description = p.description,
      variables = p.variables.map { case (k, v) => k -> toDomain(v) },
      updatedBy = unwrapPrincipal(p.updatedBy)
    )

  private def toDomain(p: g.UpdateDataSchemaNamePayload): UpdateDataSchemaNamePayload =
    UpdateDataSchemaNamePayload(
      id = DataSchemaId(p.id),
      name = p.name,
      updatedBy = unwrapPrincipal(p.updatedBy)
    )

  private def toDomain(p: g.UpdateDataSchemaDescriptionPayload): UpdateDataSchemaDescriptionPayload =
    UpdateDataSchemaDescriptionPayload(
      id = DataSchemaId(p.id),
      description = p.description,
      updatedBy = unwrapPrincipal(p.updatedBy)
    )

  private def toDomain(p: g.StoreDataSchemaVariablePayload): StoreDataSchemaVariablePayload =
    StoreDataSchemaVariablePayload(
      dataSchemaId = DataSchemaId(p.dataSchemaId),
      variableName = VariableName(p.variableName),
      oldVariableName = p.oldVariableName.map(VariableName(_)),
      name = p.name,
      caption = p.caption,
      datatype = toDatatype(p.datatype),
      defaultValue = p.defaultValue,
      updatedBy = unwrapPrincipal(p.updatedBy)
    )

  private def toDomain(p: g.DeleteDataSchemaVariablePayload): DeleteDataSchemaVariablePayload =
    DeleteDataSchemaVariablePayload(
      dataSchemaId = DataSchemaId(p.dataSchemaId),
      variableName = VariableName(p.variableName),
      updatedBy = unwrapPrincipal(p.updatedBy)
    )

  private def toDomain(p: g.DeleteDataSchemaPayload): DeleteDataSchemaPayload =
    DeleteDataSchemaPayload(
      id = DataSchemaId(p.id),
      updatedBy = unwrapPrincipal(p.updatedBy)
    )

  private def toDomain(q: g.DataSchemaFindQuery): DataSchemaFindQuery =
    DataSchemaFindQuery(
      offset = q.offset,
      size = q.size,
      filter = q.filter,
      sortBy = toDomainSortBy(q.sortBy)
    )

  override def createDataSchema(in: g.CreateDataSchemaPayload): Future[g.DataSchema] =
    dataSchemaService.createDataSchema(toDomain(in)).map(fromDomain)

  override def updateDataSchema(in: g.UpdateDataSchemaPayload): Future[g.DataSchema] =
    dataSchemaService.updateDataSchema(toDomain(in)).map(fromDomain)

  override def updateDataSchemaName(in: g.UpdateDataSchemaNamePayload): Future[g.DataSchema] =
    dataSchemaService.updateDataSchemaName(toDomain(in)).map(fromDomain)

  override def updateDataSchemaDescription(in: g.UpdateDataSchemaDescriptionPayload): Future[g.DataSchema] =
    dataSchemaService.updateDataSchemaDescription(toDomain(in)).map(fromDomain)

  override def storeDataSchemaVariable(in: g.StoreDataSchemaVariablePayload): Future[g.DataSchema] =
    dataSchemaService.storeDataSchemaVariable(toDomain(in)).map(fromDomain)

  override def deleteDataSchemaVariable(in: g.DeleteDataSchemaVariablePayload): Future[g.DataSchema] =
    dataSchemaService.deleteDataSchemaVariable(toDomain(in)).map(fromDomain)

  override def deleteDataSchema(in: g.DeleteDataSchemaPayload): Future[Empty] =
    dataSchemaService.deleteDataSchema(toDomain(in)).map(_ => Empty())

  override def getDataSchema(in: g.GetDataSchemaRequest): Future[g.DataSchema] =
    dataSchemaService.getDataSchema(in.id, in.withVariables.getOrElse(true)).map(fromDomain)

  override def getDataSchemas(in: g.GetDataSchemasRequest): Future[g.GetDataSchemasResponse] =
    dataSchemaService
      .getDataSchemas(in.ids.map(DataSchemaId(_)), in.withVariables.getOrElse(true))
      .map(schemas => g.GetDataSchemasResponse(schemas.map(fromDomain)))

  override def findDataSchemas(in: g.DataSchemaFindQuery): Future[g.FindResult] =
    dataSchemaService.findDataSchemas(toDomain(in)).map(fromDomain)

  // === Business Processes ===

  private def fromDomain(v: BusinessProcessVariable): g.BusinessProcessVariable =
    g.BusinessProcessVariable(
      name = v.name,
      caption = v.caption,
      datatype = fromDomainDatatype(v.datatype),
      defaultValue = v.defaultValue
    )

  private def toDomain(v: g.BusinessProcessVariable): BusinessProcessVariable =
    BusinessProcessVariable(
      name = v.name,
      caption = v.caption,
      datatype = toDatatype(v.datatype),
      defaultValue = v.defaultValue
    )

  private def fromDomain(b: BusinessProcess): g.BusinessProcess =
    g.BusinessProcess(
      id = b.id.value,
      name = b.name,
      description = b.description,
      processDefinitionType = fromDomainProcessDefinitionType(b.processDefinitionType),
      processDefinition = b.processDefinition.value,
      bpmModelId = b.bpmModelId.map(_.value),
      dataSchemaId = b.dataSchemaId.map(_.value),
      variables = b.variables.map { case (k, v) => k -> fromDomain(v) },
      updatedAt = b.updatedAt.toString,
      updatedBy = Some(fromDomain(b.updatedBy))
    )

  private def toDomain(p: g.CreateBusinessProcessPayload): CreateBusinessProcessPayload =
    CreateBusinessProcessPayload(
      id = BusinessProcessId(p.id),
      name = p.name,
      description = p.description,
      processDefinitionType = toProcessDefinitionType(p.processDefinitionType),
      processDefinition = ProcessDefinition(p.processDefinition),
      bpmModelId = p.bpmModelId.map(BpmModelId(_)),
      dataSchemaId = p.dataSchemaId.map(DataSchemaId(_)),
      variables = p.variables.map { case (k, v) => k -> toDomain(v) },
      updatedBy = unwrapPrincipal(p.updatedBy)
    )

  private def toDomain(p: g.UpdateBusinessProcessPayload): UpdateBusinessProcessPayload =
    UpdateBusinessProcessPayload(
      id = BusinessProcessId(p.id),
      name = p.name,
      description = p.description,
      processDefinitionType = toProcessDefinitionType(p.processDefinitionType),
      processDefinition = ProcessDefinition(p.processDefinition),
      bpmModelId = p.bpmModelId.map(BpmModelId(_)),
      dataSchemaId = p.dataSchemaId.map(DataSchemaId(_)),
      variables = p.variables.map { case (k, v) => k -> toDomain(v) },
      updatedBy = unwrapPrincipal(p.updatedBy)
    )

  private def toDomain(p: g.UpdateBusinessProcessNamePayload): UpdateBusinessProcessNamePayload =
    UpdateBusinessProcessNamePayload(
      id = BusinessProcessId(p.id),
      name = p.name,
      updatedBy = unwrapPrincipal(p.updatedBy)
    )

  private def toDomain(p: g.UpdateBusinessProcessDescriptionPayload): UpdateBusinessProcessDescriptionPayload =
    UpdateBusinessProcessDescriptionPayload(
      id = BusinessProcessId(p.id),
      description = p.description,
      updatedBy = unwrapPrincipal(p.updatedBy)
    )

  private def toDomain(p: g.UpdateBusinessProcessBpmModelPayload): UpdateBusinessProcessBpmModelPayload =
    UpdateBusinessProcessBpmModelPayload(
      id = BusinessProcessId(p.id),
      bpmModelId = p.bpmModelId.map(BpmModelId(_)),
      updatedBy = unwrapPrincipal(p.updatedBy)
    )

  private def toDomain(p: g.UpdateBusinessProcessDataSchemaPayload): UpdateBusinessProcessDataSchemaPayload =
    UpdateBusinessProcessDataSchemaPayload(
      id = BusinessProcessId(p.id),
      dataSchemaId = p.dataSchemaId.map(DataSchemaId(_)),
      updatedBy = unwrapPrincipal(p.updatedBy)
    )

  private def toDomain(
    p: g.UpdateBusinessProcessProcessDefinitionPayload
  ): UpdateBusinessProcessProcessDefinitionPayload =
    UpdateBusinessProcessProcessDefinitionPayload(
      id = BusinessProcessId(p.id),
      processDefinitionType = toProcessDefinitionType(p.processDefinitionType),
      processDefinition = ProcessDefinition(p.processDefinition),
      updatedBy = unwrapPrincipal(p.updatedBy)
    )

  private def toDomain(p: g.StoreBusinessProcessVariablePayload): StoreBusinessProcessVariablePayload =
    StoreBusinessProcessVariablePayload(
      businessProcessId = BusinessProcessId(p.businessProcessId),
      variableName = VariableName(p.variableName),
      oldVariableName = p.oldVariableName.map(VariableName(_)),
      name = p.name,
      caption = p.caption,
      datatype = toDatatype(p.datatype),
      defaultValue = p.defaultValue,
      updatedBy = unwrapPrincipal(p.updatedBy)
    )

  private def toDomain(p: g.DeleteBusinessProcessVariablePayload): DeleteBusinessProcessVariablePayload =
    DeleteBusinessProcessVariablePayload(
      businessProcessId = BusinessProcessId(p.businessProcessId),
      variableName = VariableName(p.variableName),
      updatedBy = unwrapPrincipal(p.updatedBy)
    )

  private def toDomain(p: g.DeleteBusinessProcessPayload): DeleteBusinessProcessPayload =
    DeleteBusinessProcessPayload(
      id = BusinessProcessId(p.id),
      updatedBy = unwrapPrincipal(p.updatedBy)
    )

  private def toDomain(q: g.BusinessProcessFindQuery): BusinessProcessFindQuery =
    BusinessProcessFindQuery(
      offset = q.offset,
      size = q.size,
      filter = q.filter,
      sortBy = toDomainSortBy(q.sortBy)
    )

  override def createBusinessProcess(in: g.CreateBusinessProcessPayload): Future[g.BusinessProcess] =
    businessProcessService.createBusinessProcess(toDomain(in)).map(fromDomain)

  override def updateBusinessProcess(in: g.UpdateBusinessProcessPayload): Future[g.BusinessProcess] =
    businessProcessService.updateBusinessProcess(toDomain(in)).map(fromDomain)

  override def updateBusinessProcessName(in: g.UpdateBusinessProcessNamePayload): Future[g.BusinessProcess] =
    businessProcessService.updateBusinessProcessName(toDomain(in)).map(fromDomain)

  override def updateBusinessProcessDescription(
    in: g.UpdateBusinessProcessDescriptionPayload
  ): Future[g.BusinessProcess] =
    businessProcessService.updateBusinessProcessDescription(toDomain(in)).map(fromDomain)

  override def updateBusinessProcessBpmModel(
    in: g.UpdateBusinessProcessBpmModelPayload
  ): Future[g.BusinessProcess] =
    businessProcessService.updateBusinessProcessBpmModel(toDomain(in)).map(fromDomain)

  override def updateBusinessProcessDataSchema(
    in: g.UpdateBusinessProcessDataSchemaPayload
  ): Future[g.BusinessProcess] =
    businessProcessService.updateBusinessProcessDataSchema(toDomain(in)).map(fromDomain)

  override def updateBusinessProcessProcessDefinition(
    in: g.UpdateBusinessProcessProcessDefinitionPayload
  ): Future[g.BusinessProcess] =
    businessProcessService.updateBusinessProcessProcessDefinition(toDomain(in)).map(fromDomain)

  override def storeBusinessProcessVariable(
    in: g.StoreBusinessProcessVariablePayload
  ): Future[g.BusinessProcess] =
    businessProcessService.storeBusinessProcessVariable(toDomain(in)).map(fromDomain)

  override def deleteBusinessProcessVariable(
    in: g.DeleteBusinessProcessVariablePayload
  ): Future[g.BusinessProcess] =
    businessProcessService.deleteBusinessProcessVariable(toDomain(in)).map(fromDomain)

  override def deleteBusinessProcess(in: g.DeleteBusinessProcessPayload): Future[Empty] =
    businessProcessService.deleteBusinessProcess(toDomain(in)).map(_ => Empty())

  override def getBusinessProcess(in: g.GetBusinessProcessRequest): Future[g.BusinessProcess] =
    businessProcessService.getBusinessProcess(in.id, in.withVariables.getOrElse(true)).map(fromDomain)

  override def getBusinessProcesses(in: g.GetBusinessProcessesRequest): Future[g.GetBusinessProcessesResponse] =
    businessProcessService
      .getBusinessProcesses(in.ids.map(BusinessProcessId(_)), in.withVariables.getOrElse(true))
      .map(processes => g.GetBusinessProcessesResponse(processes.map(fromDomain)))

  override def findBusinessProcesses(in: g.BusinessProcessFindQuery): Future[g.FindResult] =
    businessProcessService.findBusinessProcesses(toDomain(in)).map(fromDomain)
}
