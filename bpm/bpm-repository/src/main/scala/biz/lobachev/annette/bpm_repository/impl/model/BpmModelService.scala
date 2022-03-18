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

package biz.lobachev.annette.bpm_repository.impl.model

import akka.Done
import biz.lobachev.annette.bpm_repository.api.domain.BpmModelId
import biz.lobachev.annette.bpm_repository.api.model._
import biz.lobachev.annette.bpm_repository.api.rdb.SQLErrorCodes
import biz.lobachev.annette.bpm_repository.impl.PostgresDatabase
import biz.lobachev.annette.bpm_repository.impl.db.{BpmModelRecord, BpmRepositorySchema, BpmRepositorySchemaImplicits}
import biz.lobachev.annette.core.model.indexing.FindResult
import io.scalaland.chimney.dsl._
import org.postgresql.util.PSQLException
import slick.jdbc.PostgresProfile.api._

import java.time.{Instant, ZoneOffset}
import scala.concurrent.{ExecutionContext, Future}

class BpmModelService(db: PostgresDatabase, actions: BpmModelActions)(implicit
  executionContext: ExecutionContext
) extends BpmRepositorySchemaImplicits
    with CodeExtractor {

  def createBpmModel(payload: CreateBpmModelPayload): Future[BpmModel] = {
    val bpmModelRecord = payload
      .into[BpmModelRecord]
      .withFieldConst(_.code, extractCode(payload.notation, payload.xml))
      .withFieldConst(_.updatedAt, Instant.now())
      .transform
    val action         = BpmRepositorySchema.bpmModels += bpmModelRecord
    db
      .run(action)
      .transform(
        _ =>
          bpmModelRecord
            .into[BpmModel]
            .withFieldComputed(_.updatedAt, _.updatedAt.atOffset(ZoneOffset.UTC))
            .transform,
        {
          case e: PSQLException if e.getSQLState == SQLErrorCodes.UNIQUE_VIOLATION =>
            BpmModelAlreadyExist(payload.id.value)
          case e                                                                   => e
        }
      )
  }

  def updateBpmModel(payload: UpdateBpmModelPayload): Future[BpmModel] = {
    val bpmModelRecord = payload
      .into[BpmModelRecord]
      .withFieldConst(_.code, extractCode(payload.notation, payload.xml))
      .withFieldConst(_.updatedAt, Instant.now())
      .transform
    val action         = BpmRepositorySchema.bpmModels.filter(_.id === bpmModelRecord.id).update(bpmModelRecord)
    db
      .run(action)
      .transform(
        _ =>
          bpmModelRecord
            .into[BpmModel]
            .withFieldComputed(_.updatedAt, _.updatedAt.atOffset(ZoneOffset.UTC))
            .transform,
        e => e
      )
  }

  def updateBpmModelName(payload: UpdateBpmModelNamePayload): Future[BpmModel] = {
    val action = for {
      _   <- actions.updateNameAction(payload, Instant.now)
      rec <- BpmModelQueries.getBpmModelWithXmlQuery(payload.id)
    } yield rec.headOption
      .map(
        _.into[BpmModel]
          .withFieldComputed(_.updatedAt, _.updatedAt.atOffset(ZoneOffset.UTC))
          .transform
      )
      .getOrElse(throw BpmModelNotFound(payload.id.value))
    db.run(action.transactionally)
  }

  def updateBpmModelDescription(payload: UpdateBpmModelDescriptionPayload): Future[BpmModel] = {
    val action = for {
      _   <- actions.updateDescriptionAction(payload, Instant.now)
      rec <- BpmModelQueries.getBpmModelWithXmlQuery(payload.id)
    } yield rec.headOption
      .map(
        _.into[BpmModel]
          .withFieldComputed(_.updatedAt, _.updatedAt.atOffset(ZoneOffset.UTC))
          .transform
      )
      .getOrElse(throw BpmModelNotFound(payload.id.value))
    db.run(action.transactionally)
  }

  def updateBpmModelXml(payload: UpdateBpmModelXmlPayload): Future[BpmModel] = {
    val code   = extractCode(payload.notation, payload.xml)
    val action = for {
      _   <- actions.updateXmlAction(payload, code, Instant.now)
      rec <- BpmModelQueries.getBpmModelWithXmlQuery(payload.id)
    } yield rec.headOption
      .map(
        _.into[BpmModel]
          .withFieldComputed(_.updatedAt, _.updatedAt.atOffset(ZoneOffset.UTC))
          .transform
      )
      .getOrElse(throw BpmModelNotFound(payload.id.value))
    db.run(action.transactionally)
  }

  def deleteBpmModel(payload: DeleteBpmModelPayload): Future[Done] = {
    val action = actions.deleteBpmModelAction(payload)
    db.run(action.transactionally)
      .transform(
        res => res,
        {
          case e: PSQLException
              if e.getSQLState == SQLErrorCodes.FOREIGN_KEY_VIOLATION &&
                e.getMessage.contains("business_process_fk_bpm_model") =>
            BpmModelHasReference(payload.id.value)
          case e => e
        }
      )

  }

  def getBpmModelById(id: String, withXml: Boolean): Future[BpmModel] = {
    val bpmModelId = BpmModelId(id)
    val action     =
      if (withXml) actions.getBpmModelByIdWithXmlAction(bpmModelId)
      else actions.getBpmModelByIdWithoutXmlAction(bpmModelId)
    for {
      res <- db.run(action)
    } yield res.getOrElse(throw BpmModelNotFound(id))
  }

  def getBpmModelsById(ids: Seq[BpmModelId], withXml: Boolean): Future[Seq[BpmModel]] = {
    val action =
      if (withXml) actions.getBpmModelsByIdWithXmlAction(ids)
      else actions.getBpmModelsByIdWithoutXmlAction(ids)
    db.run(action)
  }

  def findBpmModels(query: BpmModelFindQuery): Future[FindResult] = {
    val action = actions.findBpmModelsAction(query)
    db.run(action)
  }

}
