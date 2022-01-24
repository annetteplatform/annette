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
import biz.lobachev.annette.bpm_repository.api.domain.{BpmModelId, Code, Notation}
import biz.lobachev.annette.bpm_repository.api.model._
import biz.lobachev.annette.bpm_repository.api.rdb.SQLErrorCodes
import biz.lobachev.annette.bpm_repository.impl.BpmRepositoryDBProvider
import biz.lobachev.annette.bpm_repository.impl.schema.{
  BpmModelRecord,
  BpmRepositorySchema,
  BpmRepositorySchemaImplicits
}
import biz.lobachev.annette.core.model.indexing.{FindResult, HitResult, SortBy}
import io.scalaland.chimney.dsl._
import org.postgresql.util.PSQLException
import slick.jdbc.PostgresProfile.api._

import java.time.{Instant, ZoneOffset}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.xml.XML

class BpmModelService(dbProvider: BpmRepositoryDBProvider)(implicit
  executionContext: ExecutionContext
) extends BpmRepositorySchemaImplicits {

  def createBpmModel(payload: CreateBpmModelPayload): Future[BpmModel] = {
    val bpmModelRecord = payload
      .into[BpmModelRecord]
      .withFieldConst(_.code, extractCode(payload.notation, payload.xml))
      .withFieldConst(_.updatedAt, Instant.now())
      .transform
    val action         = for {
      rowInserted <- BpmRepositorySchema.bpmModels += bpmModelRecord
    } yield
      if (rowInserted == 1)
        bpmModelRecord
          .into[BpmModel]
          .withFieldComputed(_.updatedAt, _.updatedAt.atOffset(ZoneOffset.UTC))
          .transform
      else throw UpdateBpmModelFailed("createBpmModel", rowInserted.toString)
    dbProvider.db.run(action.transactionally).recoverWith {
      case e: PSQLException if e.getSQLState == SQLErrorCodes.UNIQUE_VIOLATION =>
        Future.failed(BpmModelAlreadyExist(payload.id.value))
      case e                                                                   => Future.failed(e)
    }
  }

  def updateBpmModel(payload: UpdateBpmModelPayload): Future[BpmModel] = {
    val bpmModelRecord = payload
      .into[BpmModelRecord]
      .withFieldConst(_.code, extractCode(payload.notation, payload.xml))
      .withFieldConst(_.updatedAt, Instant.now())
      .transform
    val action         = for {
      rowUpdated <- BpmRepositorySchema.bpmModels.filter(_.id === bpmModelRecord.id).update(bpmModelRecord)
    } yield
      if (rowUpdated == 1)
        bpmModelRecord
          .into[BpmModel]
          .withFieldComputed(_.updatedAt, _.updatedAt.atOffset(ZoneOffset.UTC))
          .transform
      else throw UpdateBpmModelFailed("updateBpmModel", rowUpdated.toString)
    dbProvider.db.run(action.transactionally)
  }

  def updateBpmModelName(payload: UpdateBpmModelNamePayload): Future[BpmModel] = {
    val action = for {
      rowUpdated <- BpmRepositorySchema.bpmModels
                      .filter(_.id === payload.id)
                      .map(rec => (rec.name, rec.updatedBy, rec.updatedAt))
                      .update((payload.name, payload.updatedBy, Instant.now))
      rec        <- BpmRepositorySchema.bpmModels.filter(_.id === payload.id).result
    } yield
      if (rowUpdated == 1 && rec.headOption.nonEmpty)
        rec.head
          .into[BpmModel]
          .withFieldComputed(_.updatedAt, _.updatedAt.atOffset(ZoneOffset.UTC))
          .transform
      else throw UpdateBpmModelFailed("updateBpmModelName", rowUpdated.toString)
    dbProvider.db.run(action.transactionally)
  }

  def updateBpmModelDescription(payload: UpdateBpmModelDescriptionPayload): Future[BpmModel] = {
    val action = for {
      rowUpdated <- BpmRepositorySchema.bpmModels
                      .filter(_.id === payload.id)
                      .map(rec => (rec.description, rec.updatedBy, rec.updatedAt))
                      .update((payload.description, payload.updatedBy, Instant.now))
      rec        <- BpmRepositorySchema.bpmModels.filter(_.id === payload.id).result
    } yield
      if (rowUpdated == 1 && rec.headOption.nonEmpty)
        rec.head
          .into[BpmModel]
          .withFieldComputed(_.updatedAt, _.updatedAt.atOffset(ZoneOffset.UTC))
          .transform
      else throw UpdateBpmModelFailed("updateBpmModelDescription", rowUpdated.toString)
    dbProvider.db.run(action.transactionally)
  }

  def updateBpmModelXml(payload: UpdateBpmModelXmlPayload): Future[BpmModel] = {
    val code   = extractCode(payload.notation, payload.xml)
    val action = for {
      rowUpdated <- BpmRepositorySchema.bpmModels
                      .filter(_.id === payload.id)
                      .map(rec => (rec.notation, rec.xml, rec.code, rec.updatedBy, rec.updatedAt))
                      .update((payload.notation, payload.xml, code, payload.updatedBy, Instant.now))
      rec        <- BpmRepositorySchema.bpmModels.filter(_.id === payload.id).result
    } yield
      if (rowUpdated == 1 && rec.headOption.nonEmpty)
        rec.head
          .into[BpmModel]
          .withFieldComputed(_.updatedAt, _.updatedAt.atOffset(ZoneOffset.UTC))
          .transform
      else throw UpdateBpmModelFailed("updateBpmModelXml", rowUpdated.toString)
    dbProvider.db.run(action.transactionally)
  }

  def deleteBpmModel(id: String): Future[Done] = {
    val action = for {
      rowDeleted <- BpmRepositorySchema.bpmModels
                      .filter(_.id === BpmModelId(id))
                      .delete
    } yield
      if (rowDeleted == 1) Done
      else throw UpdateBpmModelFailed("deleteBpmModel", rowDeleted.toString)
    dbProvider.db.run(action.transactionally)
  }

  def getBpmModelByIdWithXmlAction(id: BpmModelId) =
    for {
      res <- BpmRepositorySchema.bpmModels
               .filter(_.id === id)
               .result
    } yield res.headOption.map(
      _.into[BpmModel]
        .withFieldComputed(_.updatedAt, _.updatedAt.atOffset(ZoneOffset.UTC))
        .transform
    )

  def getBpmModelByIdWithoutXmlAction(id: BpmModelId) =
    for {
      res <- BpmRepositorySchema.bpmModels
               .filter(_.id === id)
               .map(r => (r.id, r.code, r.name, r.description, r.notation, r.updatedAt, r.updatedBy))
               .result
    } yield res.headOption.map(r => BpmModel(r._1, r._2, r._3, r._4, r._5, None, r._6.atOffset(ZoneOffset.UTC), r._7))

  def getBpmModelById(id: String, withXml: Boolean): Future[BpmModel] = {
    val bpmModelId = BpmModelId(id)
    val action     =
      if (withXml) getBpmModelByIdWithXmlAction(bpmModelId)
      else getBpmModelByIdWithoutXmlAction(bpmModelId)
    for {
      res <- dbProvider.db.run(action)
    } yield res.getOrElse(throw BpmModelNotFound(id))
  }

  def getBpmModelsByIdWithXmlAction(ids: Seq[BpmModelId]) =
    for {
      res <- BpmRepositorySchema.bpmModels
               .filter(_.id.inSet(ids))
               .result
    } yield res.map(
      _.into[BpmModel]
        .withFieldComputed(_.updatedAt, _.updatedAt.atOffset(ZoneOffset.UTC))
        .transform
    )

  def getBpmModelsByIdWithoutXmlAction(ids: Seq[BpmModelId]) =
    for {
      res <- BpmRepositorySchema.bpmModels
               .filter(_.id.inSet(ids))
               .map(r => (r.id, r.code, r.name, r.description, r.notation, r.updatedAt, r.updatedBy))
               .result
    } yield res.map(r => BpmModel(r._1, r._2, r._3, r._4, r._5, None, r._6.atOffset(ZoneOffset.UTC), r._7))

  def getBpmModelsById(ids: Seq[BpmModelId], withXml: Boolean): Future[Seq[BpmModel]] = {
    val action =
      if (withXml) getBpmModelsByIdWithXmlAction(ids)
      else getBpmModelsByIdWithoutXmlAction(ids)
    dbProvider.db.run(action)
  }

  def findBpmModels(query: BpmModelFindQuery): Future[FindResult] = {
    val filteredQuery =
      BpmRepositorySchema.bpmModels.filter(rec =>
        List(
          query.filter.map(filter => (rec.name like s"%$filter%") || (rec.description like s"%$filter%")),
          query.notations.map(notations => rec.notation.inSet(notations))
        ).collect({ case Some(criteria) => criteria }).reduceLeftOption(_ && _).getOrElse(true: Rep[Boolean])
      )
    val sortedQuery   = query.sortBy
      .flatMap(_.headOption)
      .map {
        case SortBy("id", descending)       =>
          filteredQuery.sortBy(r => if (descending.getOrElse(false)) r.id.desc else r.id.asc)
        case SortBy("code", descending)     =>
          filteredQuery.sortBy(r => if (descending.getOrElse(false)) r.code.desc else r.code.asc)
        case SortBy("name", descending)     =>
          filteredQuery.sortBy(r => if (descending.getOrElse(false)) r.name.desc else r.name.asc)
        case SortBy("notation", descending) =>
          filteredQuery.sortBy(r => if (descending.getOrElse(false)) r.notation.desc else r.notation.asc)
        case _                              => filteredQuery
      }
      .getOrElse(filteredQuery)
    val action        = for {
      total <- filteredQuery.length.result
      res   <- sortedQuery.drop(query.offset).take(query.size).map(r => r.id -> r.updatedAt).result
    } yield FindResult(
      total.toLong,
      res.map { case id -> updatedAt => HitResult(id.value, 0, updatedAt.atOffset(ZoneOffset.UTC)) }
    )
    dbProvider.db.run(action)
  }

  def extractCode(notation: Notation.Notation, xmlStr: String): Code =
    Try {
      val xml     = XML.loadString(xmlStr)
      val nodeSeq = notation match {
        case Notation.BPMN => xml \\ "definitions" \ "process" \ "@id"
        case Notation.DMN  => xml \\ "definitions" \ "decision" \ "@id"
        case Notation.CMMN => xml \\ "definitions" \ "case" \ "@id"
      }
      nodeSeq.text
    }.toOption
      .filter(_.trim.nonEmpty)
      .map(Code(_))
      .getOrElse(throw InvalidModel(notation, xmlStr))
}
