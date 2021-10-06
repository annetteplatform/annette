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

package biz.lobachev.annette.microservice_core.elastic

import akka.Done
import biz.lobachev.annette.core.model.indexing.{FindResult, HitResult, SortBy}
import com.sksamuel.elastic4s.ElasticDsl.{deleteById, indexExists, _}
import com.sksamuel.elastic4s.requests.delete.DeleteResponse
import com.sksamuel.elastic4s.requests.indexes.admin.IndexExistsResponse
import com.sksamuel.elastic4s.requests.indexes.{CreateIndexRequest, IndexRequest}
import com.sksamuel.elastic4s.requests.searches.queries.matches.{
  FieldWithOptionalBoost,
  MultiMatchQuery,
  MultiMatchQueryBuilderType
}
import com.sksamuel.elastic4s.requests.searches.sort.{FieldSort, SortOrder}
import com.sksamuel.elastic4s.requests.searches.{SearchRequest, SearchResponse}
import com.sksamuel.elastic4s.{ElasticClient, RequestFailure, RequestSuccess, Response}
import org.slf4j.Logger

import java.time.OffsetDateTime
import scala.collection.immutable.Seq
import scala.concurrent.{ExecutionContext, Future}

abstract class AbstractElasticIndexDao(elasticSettings: ElasticSettings, val elasticClient: ElasticClient)(implicit
  val ec: ExecutionContext
) {

  protected val log: Logger

  val prefix              = if (elasticSettings.prefix.isEmpty) "" else s"${elasticSettings.prefix}-"
  protected val indexName = s"$prefix${indexSuffix}"

  def indexSuffix: String

  def createIndexRequest: CreateIndexRequest

  def createEntityIndex(): Future[Done] =
    elasticClient
      .execute(indexExists(indexName))
      .map {
        case failure: RequestFailure                  =>
          log.error("createEntityIndex: indexExists validation failed", failure.error)
          false
        case res: RequestSuccess[IndexExistsResponse] =>
          res.result.exists
      }
      .flatMap {
        case true  =>
          log.debug(s"createEntityIndex: Index ${indexName} exists")
          Future.successful(Done)
        case false =>
          log.debug(s"createEntityIndex: Index ${indexName} does not exists")
          val createFuture = for {
            res <- elasticClient.execute(createIndexRequest)
          } yield {
            res match {
              case _: RequestSuccess[_]    =>
                log.debug("createEntityIndex Success: {}", res.toString)
              case failure: RequestFailure =>
                log.error("createEntityIndex Failure: {}", failure)
                throw new Exception(failure.error.toString)
            }
            Done
          }
          createFuture.failed.map(th => log.error("createEntityIndex: failure", th))
          createFuture
      }

  protected def indexEntity(indexRequest: IndexRequest): Future[Unit] = {
    val indexFuture = for {
      res <- elasticClient.execute(indexRequest)
    } yield {
      res match {
        case _: RequestSuccess[_]    =>
          log.trace("indexEntity Success: {}", res.toString)
        case failure: RequestFailure =>
          log.error("indexEntity Failure: {}", failure)
          throw new Exception(failure.error.toString)
      }
      ()
    }
    indexFuture.failed.map(th => log.error("indexEntity: failure", th))
    indexFuture
  }

  protected def deleteEntity(id: String): Future[Unit] =
    for {
      resp <- elasticClient.execute(deleteById(indexName, id))
    } yield resp match {
      case failure: RequestFailure                 =>
        log.error("deleteEntity: failed " + failure.error)
        ()
      case results: RequestSuccess[DeleteResponse] =>
        log.trace(s"deleteEntity: id: ${id} results: ${results.toString}")
        ()
    }

  protected def findEntity(searchRequest: SearchRequest): Future[FindResult] = {
    log.trace(s"findEntity: searchRequest: ${searchRequest.toString} ")
    for {
      resp <- elasticClient.execute(searchRequest)
    } yield resp match {
      case failure: RequestFailure                 =>
        log.error("findEntity: failed " + failure.error)
        FindResult(0, Seq.empty)
      case results: RequestSuccess[SearchResponse] =>
        log.trace(s"findEntity: results ${results.toString}")
        val total = results.result.hits.total.value
        log.trace(s"findEntity: total= $total, hits= ${results.result.hits.hits.length}")
        val hits  = results.result.hits.hits.map { hit =>
          val updatedAt = hit.sourceAsMap
            .get("updatedAt")
            .map(v => OffsetDateTime.parse(v.toString))
            .getOrElse(OffsetDateTime.now)
          HitResult(hit.id, hit.score, updatedAt)
        }.toSeq
        FindResult(total, hits)
    }
  }

  protected def buildFilterQuery(filter: Option[String], fieldBoosts: Seq[(String, Double)]) =
    filter
      .filter(!_.isBlank)
      .map { filterString =>
        val fields = fieldBoosts.map { case (field, boost) => FieldWithOptionalBoost(field, Some(boost)) }
        dismax(
          Seq(
            MultiMatchQuery(filterString, fields = fields, `type` = Some(MultiMatchQueryBuilderType.CROSS_FIELDS))
          )
        )
      }
      .toSeq

  protected def buildSortBy(sortBy: Option[SortBy]): Seq[FieldSort] =
    sortBy.map(sortBy2FieldSort).toSeq

  protected def buildSortBySeq(sortBySeq: Option[Seq[SortBy]]): Seq[FieldSort] =
    sortBySeq
      .map(_.map(sortBy2FieldSort))
      .getOrElse(Seq.empty)

  protected def sortBy2FieldSort: PartialFunction[SortBy, FieldSort] = {
    case SortBy(field, maybeDescending) =>
      val sortOrder =
        if (maybeDescending.getOrElse(true)) SortOrder.Desc
        else SortOrder.Asc
      FieldSort(field, order = sortOrder)
  }

  protected def processResponse[T](method: String, id: String): PartialFunction[Response[T], Unit] = {
    case success: RequestSuccess[_] =>
      log.trace("{}( {} ): {}", method, id, success)
    case failure: RequestFailure    =>
      log.error("{}( {} ): {}", method, id, failure)
      throw failure.error.asException
  }

}
