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

import java.time.OffsetDateTime
import biz.lobachev.annette.bpm_repository.api.domain.{
  BpmModelId,
  BusinessProcessId,
  DataSchemaId,
  Datatype,
  Notation,
  ProcessDefinition,
  ProcessDefinitionType
}
import biz.lobachev.annette.bpm_repository.api.model._
import biz.lobachev.annette.bpm_repository.api.schema._
import biz.lobachev.annette.bpm_repository.api.bp._
import biz.lobachev.annette.bpm_repository.api.{grpc => g}
import biz.lobachev.annette.core.exception.AnnetteGrpcExceptionMapping
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.indexing.{FindResult, HitResult, SortBy}
import org.apache.pekko.Done

import scala.concurrent.{ExecutionContext, Future}

class BpmRepositoryServiceGrpcImpl(client: g.BpmRepositoryServiceClient)(implicit val ec: ExecutionContext)
    extends BpmRepositoryService {

  private def unwrapPrincipal(p: Option[g.AnnettePrincipal]): AnnettePrincipal =
    p.map(pr => AnnettePrincipal(pr.code)).getOrElse(throw new IllegalArgumentException("missing principal"))

  private def fromDomain(p: AnnettePrincipal): g.AnnettePrincipal = g.AnnettePrincipal(p.code)

  private def fromDomainNotation(n: Notation.Notation): String = n.toString.toLowerCase

  private def toNotation(s: String): Notation.Notation = Notation.withName(s.toUpperCase)

  private def fromDomainDatatype(d: Datatype.Datatype): String = d.toString

  private def toDatatype(s: String): Datatype.Datatype = Datatype.withName(s)

  private def fromDomainProcessDefinitionType(t: ProcessDefinitionType.ProcessDefinitionType): String =
    t.toString.toLowerCase

  private def toProcessDefinitionType(s: String): ProcessDefinitionType.ProcessDefinitionType =
    ProcessDefinitionType.withName(s.toUpperCase)

  private def sortByToProto(sortBy: Option[Seq[SortBy]]): Seq[g.SortBy] =
    sortBy.map(_.map(s => g.SortBy(field = s.field, descending = s.descending))).getOrElse(Seq.empty)

  private def toDomain(f: g.FindResult): FindResult =
    FindResult(
      total = f.total,
      hits = f.hits.map(h => HitResult(id = h.id, score = h.score, updatedAt = OffsetDateTime.parse(h.updatedAt)))
    )

  private def call[T](f: Future[T]): Future[T] = AnnetteGrpcExceptionMapping.recoverAnnette(f)

  // === BPM Models ===

  private def fromProto(m: g.BpmModel): BpmModel =
    BpmModel(
      id = BpmModelId(m.id),
      code = m.code,
      name = m.name,
      description = m.description,
      notation = toNotation(m.notation),
      xml = m.xml,
      updatedAt = OffsetDateTime.parse(m.updatedAt),
      updatedBy = unwrapPrincipal(m.updatedBy)
    )

  override def createBpmModel(payload: CreateBpmModelPayload): Future[BpmModel] =
    call(
      client.createBpmModel(
        g.CreateBpmModelPayload(
          id = payload.id.value,
          name = payload.name,
          description = payload.description,
          notation = fromDomainNotation(payload.notation),
          xml = payload.xml,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(fromProto)

  override def updateBpmModel(payload: UpdateBpmModelPayload): Future[BpmModel] =
    call(
      client.updateBpmModel(
        g.UpdateBpmModelPayload(
          id = payload.id.value,
          name = payload.name,
          description = payload.description,
          notation = fromDomainNotation(payload.notation),
          xml = payload.xml,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(fromProto)

  override def updateBpmModelName(payload: UpdateBpmModelNamePayload): Future[BpmModel] =
    call(
      client.updateBpmModelName(
        g.UpdateBpmModelNamePayload(
          id = payload.id.value,
          name = payload.name,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(fromProto)

  override def updateBpmModelDescription(payload: UpdateBpmModelDescriptionPayload): Future[BpmModel] =
    call(
      client.updateBpmModelDescription(
        g.UpdateBpmModelDescriptionPayload(
          id = payload.id.value,
          description = payload.description,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(fromProto)

  override def updateBpmModelXml(payload: UpdateBpmModelXmlPayload): Future[BpmModel] =
    call(
      client.updateBpmModelXml(
        g.UpdateBpmModelXmlPayload(
          id = payload.id.value,
          notation = fromDomainNotation(payload.notation),
          xml = payload.xml,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(fromProto)

  override def deleteBpmModel(payload: DeleteBpmModelPayload): Future[Done] =
    call(
      client.deleteBpmModel(
        g.DeleteBpmModelPayload(id = payload.id.value, updatedBy = Some(fromDomain(payload.updatedBy)))
      )
    ).map(_ => Done)

  override def getBpmModel(id: String, withXml: Option[Boolean]): Future[BpmModel] =
    call(client.getBpmModel(g.GetBpmModelRequest(id = id, withXml = withXml))).map(fromProto)

  override def getBpmModels(ids: Seq[BpmModelId], withXml: Option[Boolean]): Future[Seq[BpmModel]] =
    call(
      client.getBpmModels(g.GetBpmModelsRequest(ids = ids.map(_.value), withXml = withXml))
    ).map(_.models.map(fromProto))

  override def findBpmModels(query: BpmModelFindQuery): Future[FindResult] =
    call(
      client.findBpmModels(
        g.BpmModelFindQuery(
          offset = query.offset,
          size = query.size,
          filter = query.filter,
          notations = query.notations.map(_.map(fromDomainNotation)).getOrElse(Seq.empty),
          sortBy = sortByToProto(query.sortBy)
        )
      )
    ).map(toDomain)

  // === Data Schemas ===

  private def fromProto(v: g.DataSchemaVariable): DataSchemaVariable =
    DataSchemaVariable(
      name = v.name,
      caption = v.caption,
      datatype = toDatatype(v.datatype),
      defaultValue = v.defaultValue
    )

  private def toProto(v: DataSchemaVariable): g.DataSchemaVariable =
    g.DataSchemaVariable(
      name = v.name,
      caption = v.caption,
      datatype = fromDomainDatatype(v.datatype),
      defaultValue = v.defaultValue
    )

  private def fromProto(s0: g.DataSchema): DataSchema =
    DataSchema(
      id = DataSchemaId(s0.id),
      name = s0.name,
      description = s0.description,
      variables = s0.variables.map { case (k, v) => k -> fromProto(v) },
      updatedAt = OffsetDateTime.parse(s0.updatedAt),
      updatedBy = unwrapPrincipal(s0.updatedBy)
    )

  override def createDataSchema(payload: CreateDataSchemaPayload): Future[DataSchema] =
    call(
      client.createDataSchema(
        g.CreateDataSchemaPayload(
          id = payload.id.value,
          name = payload.name,
          description = payload.description,
          variables = payload.variables.map { case (k, v) => k -> toProto(v) },
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(fromProto)

  override def updateDataSchema(payload: UpdateDataSchemaPayload): Future[DataSchema] =
    call(
      client.updateDataSchema(
        g.UpdateDataSchemaPayload(
          id = payload.id.value,
          name = payload.name,
          description = payload.description,
          variables = payload.variables.map { case (k, v) => k -> toProto(v) },
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(fromProto)

  override def updateDataSchemaName(payload: UpdateDataSchemaNamePayload): Future[DataSchema] =
    call(
      client.updateDataSchemaName(
        g.UpdateDataSchemaNamePayload(
          id = payload.id.value,
          name = payload.name,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(fromProto)

  override def updateDataSchemaDescription(payload: UpdateDataSchemaDescriptionPayload): Future[DataSchema] =
    call(
      client.updateDataSchemaDescription(
        g.UpdateDataSchemaDescriptionPayload(
          id = payload.id.value,
          description = payload.description,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(fromProto)

  override def storeDataSchemaVariable(payload: StoreDataSchemaVariablePayload): Future[DataSchema] =
    call(
      client.storeDataSchemaVariable(
        g.StoreDataSchemaVariablePayload(
          dataSchemaId = payload.dataSchemaId.value,
          variableName = payload.variableName.value,
          oldVariableName = payload.oldVariableName.map(_.value),
          name = payload.name,
          caption = payload.caption,
          datatype = fromDomainDatatype(payload.datatype),
          defaultValue = payload.defaultValue,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(fromProto)

  override def deleteDataSchemaVariable(payload: DeleteDataSchemaVariablePayload): Future[DataSchema] =
    call(
      client.deleteDataSchemaVariable(
        g.DeleteDataSchemaVariablePayload(
          dataSchemaId = payload.dataSchemaId.value,
          variableName = payload.variableName.value,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(fromProto)

  override def deleteDataSchema(payload: DeleteDataSchemaPayload): Future[Done] =
    call(
      client.deleteDataSchema(
        g.DeleteDataSchemaPayload(id = payload.id.value, updatedBy = Some(fromDomain(payload.updatedBy)))
      )
    ).map(_ => Done)

  override def getDataSchema(id: String, withVariables: Option[Boolean]): Future[DataSchema] =
    call(client.getDataSchema(g.GetDataSchemaRequest(id = id, withVariables = withVariables))).map(fromProto)

  override def getDataSchemas(ids: Seq[DataSchemaId], withVariables: Option[Boolean]): Future[Seq[DataSchema]] =
    call(
      client.getDataSchemas(g.GetDataSchemasRequest(ids = ids.map(_.value), withVariables = withVariables))
    ).map(_.schemas.map(fromProto))

  override def findDataSchemas(query: DataSchemaFindQuery): Future[FindResult] =
    call(
      client.findDataSchemas(
        g.DataSchemaFindQuery(
          offset = query.offset,
          size = query.size,
          filter = query.filter,
          sortBy = sortByToProto(query.sortBy)
        )
      )
    ).map(toDomain)

  // === Business Processes ===

  private def fromProto(v: g.BusinessProcessVariable): BusinessProcessVariable =
    BusinessProcessVariable(
      name = v.name,
      caption = v.caption,
      datatype = toDatatype(v.datatype),
      defaultValue = v.defaultValue
    )

  private def toProto(v: BusinessProcessVariable): g.BusinessProcessVariable =
    g.BusinessProcessVariable(
      name = v.name,
      caption = v.caption,
      datatype = fromDomainDatatype(v.datatype),
      defaultValue = v.defaultValue
    )

  private def fromProto(b: g.BusinessProcess): BusinessProcess =
    BusinessProcess(
      id = BusinessProcessId(b.id),
      name = b.name,
      description = b.description,
      processDefinitionType = toProcessDefinitionType(b.processDefinitionType),
      processDefinition = ProcessDefinition(b.processDefinition),
      bpmModelId = b.bpmModelId.map(BpmModelId(_)),
      dataSchemaId = b.dataSchemaId.map(DataSchemaId(_)),
      variables = b.variables.map { case (k, v) => k -> fromProto(v) },
      updatedAt = OffsetDateTime.parse(b.updatedAt),
      updatedBy = unwrapPrincipal(b.updatedBy)
    )

  override def createBusinessProcess(payload: CreateBusinessProcessPayload): Future[BusinessProcess] =
    call(
      client.createBusinessProcess(
        g.CreateBusinessProcessPayload(
          id = payload.id.value,
          name = payload.name,
          description = payload.description,
          processDefinitionType = fromDomainProcessDefinitionType(payload.processDefinitionType),
          processDefinition = payload.processDefinition.value,
          bpmModelId = payload.bpmModelId.map(_.value),
          dataSchemaId = payload.dataSchemaId.map(_.value),
          variables = payload.variables.map { case (k, v) => k -> toProto(v) },
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(fromProto)

  override def updateBusinessProcess(payload: UpdateBusinessProcessPayload): Future[BusinessProcess] =
    call(
      client.updateBusinessProcess(
        g.UpdateBusinessProcessPayload(
          id = payload.id.value,
          name = payload.name,
          description = payload.description,
          processDefinitionType = fromDomainProcessDefinitionType(payload.processDefinitionType),
          processDefinition = payload.processDefinition.value,
          bpmModelId = payload.bpmModelId.map(_.value),
          dataSchemaId = payload.dataSchemaId.map(_.value),
          variables = payload.variables.map { case (k, v) => k -> toProto(v) },
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(fromProto)

  override def updateBusinessProcessName(payload: UpdateBusinessProcessNamePayload): Future[BusinessProcess] =
    call(
      client.updateBusinessProcessName(
        g.UpdateBusinessProcessNamePayload(
          id = payload.id.value,
          name = payload.name,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(fromProto)

  override def updateBusinessProcessDescription(
    payload: UpdateBusinessProcessDescriptionPayload
  ): Future[BusinessProcess] =
    call(
      client.updateBusinessProcessDescription(
        g.UpdateBusinessProcessDescriptionPayload(
          id = payload.id.value,
          description = payload.description,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(fromProto)

  override def updateBusinessProcessBpmModel(payload: UpdateBusinessProcessBpmModelPayload): Future[BusinessProcess] =
    call(
      client.updateBusinessProcessBpmModel(
        g.UpdateBusinessProcessBpmModelPayload(
          id = payload.id.value,
          bpmModelId = payload.bpmModelId.map(_.value),
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(fromProto)

  override def updateBusinessProcessDataSchema(
    payload: UpdateBusinessProcessDataSchemaPayload
  ): Future[BusinessProcess] =
    call(
      client.updateBusinessProcessDataSchema(
        g.UpdateBusinessProcessDataSchemaPayload(
          id = payload.id.value,
          dataSchemaId = payload.dataSchemaId.map(_.value),
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(fromProto)

  override def updateBusinessProcessProcessDefinition(
    payload: UpdateBusinessProcessProcessDefinitionPayload
  ): Future[BusinessProcess] =
    call(
      client.updateBusinessProcessProcessDefinition(
        g.UpdateBusinessProcessProcessDefinitionPayload(
          id = payload.id.value,
          processDefinitionType = fromDomainProcessDefinitionType(payload.processDefinitionType),
          processDefinition = payload.processDefinition.value,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(fromProto)

  override def storeBusinessProcessVariable(payload: StoreBusinessProcessVariablePayload): Future[BusinessProcess] =
    call(
      client.storeBusinessProcessVariable(
        g.StoreBusinessProcessVariablePayload(
          businessProcessId = payload.businessProcessId.value,
          variableName = payload.variableName.value,
          oldVariableName = payload.oldVariableName.map(_.value),
          name = payload.name,
          caption = payload.caption,
          datatype = fromDomainDatatype(payload.datatype),
          defaultValue = payload.defaultValue,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(fromProto)

  override def deleteBusinessProcessVariable(
    payload: DeleteBusinessProcessVariablePayload
  ): Future[BusinessProcess] =
    call(
      client.deleteBusinessProcessVariable(
        g.DeleteBusinessProcessVariablePayload(
          businessProcessId = payload.businessProcessId.value,
          variableName = payload.variableName.value,
          updatedBy = Some(fromDomain(payload.updatedBy))
        )
      )
    ).map(fromProto)

  override def deleteBusinessProcess(payload: DeleteBusinessProcessPayload): Future[Done] =
    call(
      client.deleteBusinessProcess(
        g.DeleteBusinessProcessPayload(id = payload.id.value, updatedBy = Some(fromDomain(payload.updatedBy)))
      )
    ).map(_ => Done)

  override def getBusinessProcess(id: String, withVariables: Option[Boolean]): Future[BusinessProcess] =
    call(client.getBusinessProcess(g.GetBusinessProcessRequest(id = id, withVariables = withVariables)))
      .map(fromProto)

  override def getBusinessProcesses(
    ids: Seq[BusinessProcessId],
    withVariables: Option[Boolean]
  ): Future[Seq[BusinessProcess]] =
    call(
      client.getBusinessProcesses(
        g.GetBusinessProcessesRequest(ids = ids.map(_.value), withVariables = withVariables)
      )
    ).map(_.processes.map(fromProto))

  override def findBusinessProcesses(query: BusinessProcessFindQuery): Future[FindResult] =
    call(
      client.findBusinessProcesses(
        g.BusinessProcessFindQuery(
          offset = query.offset,
          size = query.size,
          filter = query.filter,
          sortBy = sortByToProto(query.sortBy)
        )
      )
    ).map(toDomain)
}
