package biz.lobachev.annette.bpm_repository.impl.schema

import akka.Done
import biz.lobachev.annette.bpm_repository.api.domain.DataSchemaId
import biz.lobachev.annette.bpm_repository.api.schema._
import biz.lobachev.annette.bpm_repository.impl.db.{
  BpmRepositorySchema,
  BpmRepositorySchemaImplicits,
  DataSchemaRecord,
  DataSchemaVariableRecord
}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.indexing.{FindResult, HitResult}
import io.scalaland.chimney.dsl._
import slick.jdbc.PostgresProfile.api._

import java.time.{Instant, ZoneOffset}
import scala.concurrent.ExecutionContext

class DataSchemaActions(implicit executionContext: ExecutionContext) extends BpmRepositorySchemaImplicits {

  def createDataSchemaAction(payload: CreateDataSchemaPayload, updatedAt: Instant) = {
    val dataSchemaRecord     = payload
      .into[DataSchemaRecord]
      .withFieldConst(_.updatedAt, updatedAt)
      .transform
    val dataSchemaVarRecords = DataSchemaVariableRecord.fromDataSchemaVariables(payload.id, payload.variables)
    for {
      _ <- BpmRepositorySchema.dataSchemas += dataSchemaRecord
      _ <- BpmRepositorySchema.dataSchemaVariables ++= dataSchemaVarRecords
    } yield dataSchemaRecord
      .into[DataSchema]
      .withFieldConst(_.variables, payload.variables)
      .withFieldComputed(_.updatedAt, _.updatedAt.atOffset(ZoneOffset.UTC))
      .transform
  }

  def updateDataSchemaAction(payload: UpdateDataSchemaPayload, updatedAt: Instant) = {
    val dataSchemaRecord     = payload
      .into[DataSchemaRecord]
      .withFieldConst(_.updatedAt, updatedAt)
      .transform
    val dataSchemaVarRecords = DataSchemaVariableRecord.fromDataSchemaVariables(payload.id, payload.variables)
    for {
      _ <- BpmRepositorySchema.dataSchemas.filter(_.id === dataSchemaRecord.id).update(dataSchemaRecord)
      _ <- BpmRepositorySchema.dataSchemaVariables.filter(_.dataSchemaId === dataSchemaRecord.id).delete
      _ <- BpmRepositorySchema.dataSchemaVariables ++= dataSchemaVarRecords
    } yield dataSchemaRecord
      .into[DataSchema]
      .withFieldConst(_.variables, payload.variables)
      .withFieldComputed(_.updatedAt, _.updatedAt.atOffset(ZoneOffset.UTC))
      .transform
  }

  def updateNameAction(payload: UpdateDataSchemaNamePayload, updatedAt: Instant) =
    BpmRepositorySchema.dataSchemas
      .filter(_.id === payload.id)
      .map(rec => (rec.name, rec.updatedBy, rec.updatedAt))
      .update((payload.name, payload.updatedBy, updatedAt))

  def updateDescriptionAction(payload: UpdateDataSchemaDescriptionPayload, updatedAt: Instant) =
    BpmRepositorySchema.dataSchemas
      .filter(_.id === payload.id)
      .map(rec => (rec.description, rec.updatedBy, rec.updatedAt))
      .update((payload.description, payload.updatedBy, updatedAt))

  def updateUpdatedAction(id: DataSchemaId, updatedBy: AnnettePrincipal, updatedAt: Instant) =
    BpmRepositorySchema.dataSchemas
      .filter(_.id === id)
      .map(rec => (rec.updatedBy, rec.updatedAt))
      .update((updatedBy, updatedAt))

  def storeDataSchemaVariableAction(payload: StoreDataSchemaVariablePayload, updatedAt: Instant) = {
    val dataSchemaVarRecord = payload.transformInto[DataSchemaVariableRecord]
    for {
      _ <- updateUpdatedAction(payload.dataSchemaId, payload.updatedBy, updatedAt)
      _ <- payload.oldVariableName
             .map(variableName =>
               BpmRepositorySchema.dataSchemaVariables
                 .filter(r => r.dataSchemaId === payload.dataSchemaId && r.variableName === variableName)
                 .delete
             )
             .getOrElse(DBIO.successful(0))
      _ <- BpmRepositorySchema.dataSchemaVariables.insertOrUpdate(dataSchemaVarRecord)
    } yield Done
  }

  def deleteDataSchemaVariableAction(payload: DeleteDataSchemaVariablePayload, updatedAt: Instant) =
    for {
      _ <- BpmRepositorySchema.dataSchemas
             .filter(_.id === payload.dataSchemaId)
             .map(rec => (rec.updatedBy, rec.updatedAt))
             .update((payload.updatedBy, updatedAt))
      _ <- BpmRepositorySchema.dataSchemaVariables
             .filter(r => r.dataSchemaId === payload.dataSchemaId && r.variableName === payload.variableName)
             .delete
    } yield Done

  def deleteDataSchemaAction(payload: DeleteDataSchemaPayload) =
    for {
      _ <- BpmRepositorySchema.dataSchemaVariables.filter(_.dataSchemaId === payload.id).delete
      _ <- BpmRepositorySchema.dataSchemas.filter(_.id === payload.id).delete
    } yield Done

  def getDataSchemaAction(id: DataSchemaId, withVariables: Boolean) =
    for {
      ds   <- BpmRepositorySchema.dataSchemas
                .filter(_.id === id)
                .result
      vars <- if (ds.nonEmpty && withVariables) DataSchemaQueries.getDataSchemaVariables(id).result
              else DBIO.successful(Seq.empty)
    } yield ds.headOption.map(
      _.into[DataSchema]
        .withFieldConst(_.variables, DataSchemaVariableRecord.toDataSchemaVariableMap(vars))
        .withFieldComputed(_.updatedAt, _.updatedAt.atOffset(ZoneOffset.UTC))
        .transform
    )

  def getDataSchemasAction(ids: Seq[DataSchemaId], withVariables: Boolean) =
    for {
      ds   <- BpmRepositorySchema.dataSchemas
                .filter(_.id inSet ids)
                .result
      vars <- if (ds.nonEmpty && withVariables)
                BpmRepositorySchema.dataSchemaVariables
                  .filter(_.dataSchemaId inSet ids)
                  .result
              else DBIO.successful(Seq.empty)

    } yield {
      val varMap = vars
        .groupBy(_.dataSchemaId)
        .map { case k -> v => k -> DataSchemaVariableRecord.toDataSchemaVariableMap(v) }
      ds.map(d =>
        d.into[DataSchema]
          .withFieldConst(_.variables, varMap.getOrElse(d.id, Map.empty))
          .withFieldComputed(_.updatedAt, _.updatedAt.atOffset(ZoneOffset.UTC))
          .transform
      )
    }

  def findDataSchemasAction(query: DataSchemaFindQuery) = {
    val filteredQuery = DataSchemaQueries.getFilteredQuery(query)
    val sortedQuery   = query.sortBy
      .flatMap(_.headOption)
      .map(sortBy => DataSchemaQueries.getSortedQuery(filteredQuery, sortBy))
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
