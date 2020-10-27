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

package biz.lobachev.annette.attributes.api.query

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}

import akka.Done
import biz.lobachev.annette.attributes.api.assignment._
import biz.lobachev.annette.attributes.api.attribute._
import biz.lobachev.annette.core.elastic.AbstractElasticIndexDao
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.requests.common.RefreshPolicy
import com.sksamuel.elastic4s.requests.indexes.PutMappingResponse
import com.sksamuel.elastic4s.requests.mappings.FieldDefinition
import com.sksamuel.elastic4s.requests.searches.queries.{Query, RangeQuery}

import scala.concurrent.Future

trait AttributeElastic extends AttributeIndexDao {
  this: AbstractElasticIndexDao =>

  // *************************** Attribute API ***************************

  def createAttribute(index: AttributeIndex): Future[Done] =
    createAttributeInt(index).flatMap {
      case success: RequestSuccess[PutMappingResponse]                                    =>
        log.debug("createAttribute {} index: {} success: {}", index.fieldName, index.toString, success)
        Future.successful(Done)
      case failure: RequestFailure if failure.error.`type` == "index_not_found_exception" =>
        log.debug("createAttribute: Index not found. Creating... ")
        for {
          _   <- createEntityIndex()
          res <- createAttributeInt(index)
        } yield {
          res match {
            case success: RequestSuccess[PutMappingResponse] =>
              log.debug(
                "createAttribute second iteration {} index: {} success: {}",
                index.fieldName,
                index.toString,
                success
              )
            case failure: RequestFailure                     =>
              log.error(
                "createAttribute second iteration {} index: {} failed: {}",
                index.fieldName,
                index.toString,
                failure
              )
          }
          Done
        }
      case failure: RequestFailure                                                        =>
        log.error("createAttribute {} index: {} failed: {}", index.fieldName, index.toString, failure)
        Future.successful(Done)
    }

  def createAttributeInt(
    index: AttributeIndex
  ): Future[Response[PutMappingResponse]] = {
    val field: FieldDefinition = index match {
      case index: TextIndex if index.keyword =>
        textField(index.fieldName)
          .fielddata(index.fielddata)
          .fields(keywordField("keyword"))
      case index: TextIndex                  =>
        textField(index.fieldName)
          .fielddata(index.fielddata)
      case _: KeywordIndex                   => keywordField(index.fieldName)
      case _: BooleanIndex                   => booleanField(index.fieldName)
      case _: LongIndex                      => longField(index.fieldName)
      case _: DoubleIndex                    => doubleField(index.fieldName)
      case _: OffsetDateTimeIndex            => dateField(index.fieldName)
      case _: LocalDateIndex                 => dateField(index.fieldName)
      case _: LocalTimeIndex                 => dateField(index.fieldName)
    }
    elasticClient
      .execute(
        putMapping(Indexes(indexName)).fields(field)
      )

  }

  def assignAttribute(id: ObjectId, fieldName: String, attribute: AttributeValue): Future[Done] = {
    val value: AnyRef = attribute match {
      case StringAttributeValue(value)         => value
      case BooleanAttributeValue(value)        => Boolean.box(value)
      case LongAttributeValue(value)           => Long.box(value)
      case DoubleAttributeValue(value)         => Double.box(value)
      case OffsetDateTimeAttributeValue(value) => value
      case LocalDateAttributeValue(value)      => value
      case LocalTimeAttributeValue(value)      => LocalDateTime.of(LocalDate.of(2000, 1, 1), value)
      // TODO: change this
      case JSONAttributeValue(value)           => value
    }
    elasticClient.execute {
      updateById(indexName, id)
        .doc(fieldName -> value)
        .refresh(RefreshPolicy.Immediate)
    }.map { f =>
      println(f); Done
    }
  }

  def unassignAttribute(id: ObjectId, fieldName: String): Future[Done] =
    elasticClient.execute {
      updateById(indexName, id)
        .script(s"""ctx._source.remove("${fieldName}")""")
        .refresh(RefreshPolicy.Immediate)
    }.map(_ => Done)

  // *************************** Search API ***************************

  def buildAttributeQuery(maybeQuery: Option[AttributeQuery]): Seq[Query] =
    maybeQuery.map { query =>
      query.map {
        case Exist(fieldName)                   => existsQuery(fieldName)
        case NotExist(fieldName)                => not(existsQuery(fieldName))
        case Equal(fieldName, attribute)        => termQuery(fieldName, attributeValue(attribute))
        case NotEqual(fieldName, attribute)     => not(termQuery(fieldName, attributeValue(attribute)))
        case AnyOf(fieldName, values)           => termsQuery(fieldName, values.map(attributeValue))
        case Range(fieldName, gt, gte, lt, lte) =>
          val query: RangeQuery = rangeQuery(fieldName)
          val query1            = gt.map(t => rangeCond(query, "gt", t)).getOrElse(query)
          val query2            = gte.map(t => rangeCond(query1, "gte", t)).getOrElse(query1)
          val query3            = lt.map(t => rangeCond(query2, "lt", t)).getOrElse(query2)
          val query4            = lte.map(t => rangeCond(query3, "lte", t)).getOrElse(query3)
          query4
      }.toSeq
    }.getOrElse(Seq.empty)

  def attributeValue(attribute: AttributeValue): Any =
    attribute match {
      case StringAttributeValue(value)         => value
      case BooleanAttributeValue(value)        => Boolean.box(value)
      case LongAttributeValue(value)           => Long.box(value)
      case DoubleAttributeValue(value)         => Double.box(value)
      case OffsetDateTimeAttributeValue(value) => value
      case LocalDateAttributeValue(value)      => value
      case LocalTimeAttributeValue(value)      => LocalDateTime.of(LOCAL_TIME_DATE, value)
      // TODO: change this
      case JSONAttributeValue(value)           => value
    }

  def rangeCond(q: RangeQuery, cond: String, attr: AttributeValue) =
    cond match {
      case "gt"  =>
        attr match {
          case StringAttributeValue(value)         => q.gt(value)
          case LongAttributeValue(value)           => q.gt(value)
          case DoubleAttributeValue(value)         => q.gt(value)
          case OffsetDateTimeAttributeValue(value) => q.gt(value.toString)
          case LocalDateAttributeValue(value)      => q.gt(ElasticDateMath(value.format(DateTimeFormatter.ISO_LOCAL_DATE)))
          case LocalTimeAttributeValue(value)      =>
            q.gt(
              ElasticDateMath(
                LocalDateTime.of(LOCAL_TIME_DATE, value).format(DateTimeFormatter.ISO_LOCAL_DATE)
              )
            )
          case _                                   => q
        }
      case "gte" =>
        attr match {
          case StringAttributeValue(value)         => q.gte(value)
          case LongAttributeValue(value)           => q.gte(value)
          case DoubleAttributeValue(value)         => q.gte(value)
          case OffsetDateTimeAttributeValue(value) => q.gte(value.toString)
          case LocalDateAttributeValue(value)      => q.gte(ElasticDateMath(value.format(DateTimeFormatter.ISO_LOCAL_DATE)))
          case LocalTimeAttributeValue(value)      =>
            q.gte(ElasticDateMath(LocalDateTime.of(LOCAL_TIME_DATE, value).format(DateTimeFormatter.ISO_LOCAL_DATE)))
          case _                                   => q
        }
      case "lt"  =>
        attr match {
          case StringAttributeValue(value)         => q.lt(value)
          case LongAttributeValue(value)           => q.lt(value)
          case DoubleAttributeValue(value)         => q.lt(value)
          case OffsetDateTimeAttributeValue(value) => q.lt(value.toString)
          case LocalDateAttributeValue(value)      => q.lt(ElasticDateMath(value.format(DateTimeFormatter.ISO_LOCAL_DATE)))
          case LocalTimeAttributeValue(value)      =>
            q.lt(ElasticDateMath(LocalDateTime.of(LOCAL_TIME_DATE, value).format(DateTimeFormatter.ISO_LOCAL_DATE)))
          case _                                   => q
        }
      case "lte" =>
        attr match {
          case StringAttributeValue(value)         => q.lte(value)
          case LongAttributeValue(value)           => q.lte(value)
          case DoubleAttributeValue(value)         => q.lte(value)
          case OffsetDateTimeAttributeValue(value) => q.lte(value.toString)
          case LocalDateAttributeValue(value)      => q.lte(ElasticDateMath(value.format(DateTimeFormatter.ISO_LOCAL_DATE)))
          case LocalTimeAttributeValue(value)      =>
            q.lte(ElasticDateMath(LocalDateTime.of(LOCAL_TIME_DATE, value).format(DateTimeFormatter.ISO_LOCAL_DATE)))
          case _                                   => q
        }
    }

}
