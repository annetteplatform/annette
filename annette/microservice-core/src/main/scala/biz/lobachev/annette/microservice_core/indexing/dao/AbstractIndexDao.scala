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

package biz.lobachev.annette.microservice_core.indexing.dao

import akka.Done
import biz.lobachev.annette.core.model.indexing.{FindResult, HitResult, SortBy}
import biz.lobachev.annette.microservice_core.indexing.config.TextFieldConf
import biz.lobachev.annette.microservice_core.indexing.{
  IndexingProvider,
  IndexingRequestFailure,
  InvalidAlias,
  InvalidIndexError
}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.analysis.Analysis
import com.sksamuel.elastic4s.requests.common.RefreshPolicy
import com.sksamuel.elastic4s.requests.indexes.admin.IndexExistsResponse
import com.sksamuel.elastic4s.requests.indexes.{CreateIndexResponse, GetIndexResponse, PutMappingResponse}
import com.sksamuel.elastic4s.requests.searches.SearchRequest
import com.sksamuel.elastic4s.requests.searches.queries.matches.{
  FieldWithOptionalBoost,
  MultiMatchQuery,
  MultiMatchQueryBuilderType
}
import com.sksamuel.elastic4s.requests.searches.sort.{FieldSort, SortOrder}
import org.slf4j.Logger

import java.time.OffsetDateTime
import scala.collection.immutable.Seq
import scala.concurrent.{ExecutionContext, Future}

abstract class AbstractIndexDao(client: ElasticClient)(implicit
  val ec: ExecutionContext
) {
  protected val log: Logger

  def indexConfigPath: String

  val indexConfig = IndexingProvider.loadIndexConfig(indexConfigPath)

  val indexName = indexConfig.index

  def createEntityIndex(): Future[Done] =
    for {
      status <- checkIndex()
      result <- status match {
                  case IndexNotExist               => buildIndex()
                  case IndexRequireUpdate(aliases) => rebuildIndex(aliases)
                  case IndexValid                  => Future.successful(Done)
                  case IndexInvalid(aliases)       => throw InvalidIndexError(aliases.mkString(", "))
                }

    } yield result

  protected def processResponse[T]: PartialFunction[Response[T], T] = {
    case failure: RequestFailure    =>
      log.error("indexing request failed", failure.error.asException)
      throw IndexingRequestFailure(failure.error.reason, failure.error.causedBy.map(_.reason).getOrElse(""))
    case success: RequestSuccess[T] => success.result
  }
  private def validateIndex(): Future[ValidationStatus]             =
    for {
      indexResponse <- client.execute(getIndex(indexName)).map { response =>
                         processResponse[Map[String, GetIndexResponse]](response)(indexName)
                       }
    } yield {
      val result = indexConfig.mappings.map {
        case alias -> field =>
          val fieldName       = field.field.getOrElse(alias)
          val maybeIndexField = indexResponse.mappings.properties.get(fieldName)
          maybeIndexField.map { indexField =>
            if (indexField.`type` == Some(field.fieldType))
              IndexValid
            else IndexInvalid(Seq(alias))
          }.getOrElse(IndexRequireUpdate(Seq(alias)))
      }

      val invalidAliases = result.map {
        case IndexInvalid(aliases) => Some(aliases)
        case _                     => None
      }.flatten.flatten.toSeq
      if (invalidAliases.nonEmpty)
        IndexInvalid(invalidAliases)
      else {
        val requiredUpdateAliases = result.map {
          case IndexRequireUpdate(aliases) => Some(aliases)
          case _                           => None
        }.flatten.flatten.toSeq
        if (requiredUpdateAliases.nonEmpty)
          IndexRequireUpdate(requiredUpdateAliases)
        else IndexValid
      }
    }

  private def checkIndex(): Future[ValidationStatus] =
    for {
      isExist <- client
                   .execute(indexExists(indexName))
                   .map(processResponse[IndexExistsResponse](_).exists)
      result  <- if (isExist) validateIndex()
                 else Future.successful(IndexNotExist)
    } yield result

  private def rebuildIndex(aliases: Seq[String]): Future[Done] =
    client
      .execute(
        putMapping(Indexes(indexName))
          .fields(indexConfig.getProperties(aliases))
      )
      .map { res =>
        processResponse[PutMappingResponse](res)
        Done
      }

  private def buildIndex(): Future[Done] =
    client
      .execute(
        createIndex(indexName)
          .mapping(
            properties(
              indexConfig.getProperties(indexConfig.mappings.keys.toSeq)
            )
          )
          .analysis(
            Analysis(
              analyzers = indexConfig.getAnalyzers,
              tokenizers = indexConfig.getTokenizers
            )
          )
      )
      .map { res =>
        processResponse[CreateIndexResponse](res)
        Done
      }

  protected def alias2FieldName(alias: String): String = {
    val aliases = alias.split("\\.")
    if (aliases.length == 1 || aliases.length == 2)
      indexConfig.mappings
        .get(aliases(0))
        .map {
          case textField: TextFieldConf =>
            val field1 = textField.field.getOrElse(aliases(0))
            if (aliases.length == 1)
              field1
            else {
              val field2 = textField.fields
                .get(aliases(1))
                .map(_.field.getOrElse(aliases(1)))
                .getOrElse(throw InvalidAlias(alias))
              s"$field1.$field2"
            }
          case indexField               =>
            val field1 = indexField.field.getOrElse(aliases(0))
            if (aliases.length == 1)
              field1
            else
              throw InvalidAlias(alias)
        }
        .getOrElse(throw InvalidAlias(alias))
    else
      throw InvalidAlias(alias)
  }

  protected def alias2FieldNameDoc(doc: Seq[(String, Any)]): Seq[(String, Any)] =
    doc.map {
      case alias -> value =>
        alias2FieldName(alias) -> value
    }

  def createIndexDoc(id: String, doc: (String, Any)*): Future[Done] =
    client.execute {
      indexInto(indexName)
        .id(id)
        .fields(alias2FieldNameDoc(doc))
        .refresh(RefreshPolicy.Immediate)
    }.map { res =>
      processResponse(res)
      Done
    }

  def updateIndexDoc(id: String, doc: (String, Any)*): Future[Done] =
    client.execute {
      updateById(indexName, id)
        .doc(alias2FieldNameDoc(doc))
        .refresh(RefreshPolicy.Immediate)
    }.map { res =>
      processResponse(res)
      Done
    }

  def deleteIndexDoc(id: String): Future[Done] =
    client.execute {
      deleteById(indexName, id)
    }.map { res =>
      processResponse(res)
      Done
    }

  protected def findEntity(searchRequest: SearchRequest): Future[FindResult] =
    client.execute(searchRequest).map { res =>
      val result = processResponse(res)
      val total  = result.hits.total.value
      val hits   = result.hits.hits.map { hit =>
        val updatedAt = hit.sourceAsMap
          .get(alias2FieldName("updatedAt"))
          .map(v => OffsetDateTime.parse(v.toString))
          .getOrElse(OffsetDateTime.now)
        HitResult(hit.id, hit.score, updatedAt)
      }.toSeq
      FindResult(total, hits)
    }

  protected def buildFilterQuery(filter: Option[String], fieldBoosts: Seq[(String, Double)]) =
    filter
      .filter(!_.isBlank)
      .map { filterString =>
        val fields = fieldBoosts.map {
          case (alias, boost) => FieldWithOptionalBoost(alias2FieldName(alias), Some(boost))
        }
        dismax(
          Seq(
            MultiMatchQuery(filterString, fields = fields, `type` = Some(MultiMatchQueryBuilderType.CROSS_FIELDS))
          )
        )
      }
      .toSeq

  protected def buildSortBySeq(sortBySeq: Option[Seq[SortBy]]): Seq[FieldSort] =
    sortBySeq
      .map(_.map(sortBy2FieldSort))
      .getOrElse(Seq.empty)

  protected def sortBy2FieldSort: PartialFunction[SortBy, FieldSort] = {
    case SortBy(alias, maybeDescending) =>
      val sortOrder =
        if (maybeDescending.getOrElse(true)) SortOrder.Desc
        else SortOrder.Asc
      FieldSort(alias2SortFieldName(alias), order = sortOrder)
  }

  protected def alias2SortFieldName(alias: String): String = {
    val aliases = alias.split("\\.")
    if (aliases.length == 1 || aliases.length == 2)
      indexConfig.mappings
        .get(aliases(0))
        .map {
          case textField: TextFieldConf =>
            val field1 = textField.field.getOrElse(aliases(0))
            if (aliases.length == 1)
              if (textField.fielddata) field1
              else {
                val field2 = textField.fields
                  .find(_._2.fieldType == "keyword")
                  .map { case k -> v => v.field.getOrElse(k) }
                  .getOrElse(throw InvalidAlias(alias))
                s"$field1.$field2"
              }
            else {
              val field2 = textField.fields
                .get(aliases(1))
                .map(_.field.getOrElse(aliases(1)))
                .getOrElse(throw InvalidAlias(alias))
              s"$field1.$field2"
            }
          case indexField               =>
            val field1 = indexField.field.getOrElse(aliases(0))
            if (aliases.length == 1)
              field1
            else
              throw InvalidAlias(alias)
        }
        .getOrElse(throw InvalidAlias(alias))
    else
      throw InvalidAlias(alias)
  }

}
