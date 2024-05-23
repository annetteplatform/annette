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
import biz.lobachev.annette.bpm_repository.impl.db.{BpmRepositorySchema, BpmRepositorySchemaImplicits}
import biz.lobachev.annette.core.model.indexing.{FindResult, HitResult}
import biz.lobachev.annette.core.utils.ChimneyCommons._
import io.scalaland.chimney.dsl._
import slick.jdbc.PostgresProfile.api._

import java.time.{Instant, ZoneOffset}
import scala.concurrent.ExecutionContext

class BpmModelActions(implicit executionContext: ExecutionContext) extends BpmRepositorySchemaImplicits {

  def updateNameAction(payload: UpdateBpmModelNamePayload, updatedAt: Instant) =
    BpmRepositorySchema.bpmModels
      .filter(_.id === payload.id)
      .map(rec => (rec.name, rec.updatedBy, rec.updatedAt))
      .update((payload.name, payload.updatedBy, updatedAt))

  def updateDescriptionAction(payload: UpdateBpmModelDescriptionPayload, updatedAt: Instant) =
    BpmRepositorySchema.bpmModels
      .filter(_.id === payload.id)
      .map(rec => (rec.description, rec.updatedBy, rec.updatedAt))
      .update((payload.description, payload.updatedBy, updatedAt))

  def updateXmlAction(payload: UpdateBpmModelXmlPayload, code: String, updatedAt: Instant) =
    BpmRepositorySchema.bpmModels
      .filter(_.id === payload.id)
      .map(rec => (rec.notation, rec.xml, rec.code, rec.updatedBy, rec.updatedAt))
      .update((payload.notation, payload.xml, code, payload.updatedBy, updatedAt))

  def deleteBpmModelAction(payload: DeleteBpmModelPayload) =
    for {
      _ <- BpmRepositorySchema.bpmModels
             .filter(_.id === payload.id)
             .delete
    } yield Done

  def getBpmModelWithXmlAction(id: BpmModelId) =
    for {
      res <- BpmModelQueries.getBpmModelWithXmlQuery(id)
    } yield res.headOption.map(
      _.into[BpmModel]
        .withFieldComputed(_.updatedAt, _.updatedAt.atOffset(ZoneOffset.UTC))
        .transform
    )

  def getBpmModelWithoutXmlAction(id: BpmModelId) =
    for {
      res <- BpmRepositorySchema.bpmModels
               .filter(_.id === id)
               .map(r => (r.id, r.code, r.name, r.description, r.notation, r.updatedAt, r.updatedBy))
               .result
    } yield res.headOption.map(r => BpmModel(r._1, r._2, r._3, r._4, r._5, None, r._6.atOffset(ZoneOffset.UTC), r._7))

  def getBpmModelsWithXmlAction(ids: Seq[BpmModelId]) =
    for {
      res <- BpmRepositorySchema.bpmModels
               .filter(_.id.inSet(ids))
               .result
    } yield res.map(
      _.into[BpmModel]
        .withFieldComputed(_.updatedAt, _.updatedAt.atOffset(ZoneOffset.UTC))
        .transform
    )

  def getBpmModelsWithoutXmlAction(ids: Seq[BpmModelId]) =
    for {
      res <- BpmRepositorySchema.bpmModels
               .filter(_.id.inSet(ids))
               .map(r => (r.id, r.code, r.name, r.description, r.notation, r.updatedAt, r.updatedBy))
               .result
    } yield res.map(r => BpmModel(r._1, r._2, r._3, r._4, r._5, None, r._6.atOffset(ZoneOffset.UTC), r._7))

  def findBpmModelsAction(query: BpmModelFindQuery) = {
    val filteredQuery = BpmModelQueries.getFilteredQuery(query)
    val sortedQuery   = query.sortBy
      .flatMap(_.headOption)
      .map(sortBy => BpmModelQueries.getSortedQuery(filteredQuery, sortBy))
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
