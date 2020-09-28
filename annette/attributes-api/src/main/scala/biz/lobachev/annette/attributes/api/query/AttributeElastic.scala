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
import biz.lobachev.annette.attributes.api.attribute_def.AttributeType
import biz.lobachev.annette.attributes.api.attribute_def.AttributeType.AttributeType
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

  def createAttribute(attributeType: AttributeType, fieldName: String, textContentIndex: Boolean): Future[Done] =
    createAttributeInt(attributeType, fieldName, textContentIndex).flatMap {
      case success: RequestSuccess[PutMappingResponse]                                    =>
        log.debug("createAttribute {} type: {} success: {}", fieldName, attributeType.toString, success)
        Future.successful(Done)
      case failure: RequestFailure if failure.error.`type` == "index_not_found_exception" =>
        log.debug("createAttribute: Index not found. Creating... ")
        for {
          _   <- createEntityIndex()
          res <- createAttributeInt(attributeType, fieldName, textContentIndex)
        } yield {
          res match {
            case success: RequestSuccess[PutMappingResponse] =>
              log.debug(
                "createAttribute second iteration {} type: {} success: {}",
                fieldName,
                attributeType.toString,
                success
              )
            case failure: RequestFailure                     =>
              log.error(
                "createAttribute second iteration {} type: {} failed: {}",
                fieldName,
                attributeType.toString,
                failure
              )
          }
          Done
        }
      case failure: RequestFailure                                                        =>
        log.error("createAttribute {} type: {} failed: {}", fieldName, attributeType.toString, failure)
        Future.successful(Done)
    }

  def createAttributeInt(
    attributeType: AttributeType,
    fieldName: String,
    textContentIndex: Boolean
  ): Future[Response[PutMappingResponse]] = {
    val field: FieldDefinition = attributeType match {
      case AttributeType.String if textContentIndex => textField(fieldName).fielddata(true)
      case AttributeType.String                     => keywordField(fieldName)
      case AttributeType.Long                       => longField(fieldName)
      case AttributeType.Double                     => doubleField(fieldName)
      case AttributeType.OffsetDateTime             => dateField(fieldName)
      case AttributeType.LocalDate                  => dateField(fieldName)
      case AttributeType.LocalTime                  => dateField(fieldName)
    }
    elasticClient
      .execute(
        putMapping(Indexes(indexName)).fields(field)
      )

  }

  def assignAttribute(id: ObjectId, fieldName: String, attribute: Attribute): Future[Done] = {
    val value: AnyRef = attribute match {
      case StringAttribute(value)         => value
      case BooleanAttribute(value)        => Boolean.box(value)
      case LongAttribute(value)           => Long.box(value)
      case DoubleAttribute(value)         => Double.box(value)
      case OffsetDateTimeAttribute(value) => value
      case LocalDateAttribute(value)      => value
      case LocalTimeAttribute(value)      => LocalDateTime.of(LocalDate.of(2000, 1, 1), value)
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

  def attributeValue(attribute: Attribute): Any =
    attribute match {
      case StringAttribute(value)         => value
      case BooleanAttribute(value)        => Boolean.box(value)
      case LongAttribute(value)           => Long.box(value)
      case DoubleAttribute(value)         => Double.box(value)
      case OffsetDateTimeAttribute(value) => value
      case LocalDateAttribute(value)      => value
      case LocalTimeAttribute(value)      => LocalDateTime.of(LOCAL_TIME_DATE, value)
    }

  def rangeCond(q: RangeQuery, cond: String, attr: Attribute) =
    cond match {
      case "gt"  =>
        attr match {
          case StringAttribute(value)         => q.gt(value)
          case LongAttribute(value)           => q.gt(value)
          case DoubleAttribute(value)         => q.gt(value)
          case OffsetDateTimeAttribute(value) => q.gt(value.toString)
          case LocalDateAttribute(value)      => q.gt(ElasticDateMath(value.format(DateTimeFormatter.ISO_LOCAL_DATE)))
          case LocalTimeAttribute(value)      =>
            q.gt(
              ElasticDateMath(
                LocalDateTime.of(LOCAL_TIME_DATE, value).format(DateTimeFormatter.ISO_LOCAL_DATE)
              )
            )
          case _                              => q
        }
      case "gte" =>
        attr match {
          case StringAttribute(value)         => q.gte(value)
          case LongAttribute(value)           => q.gte(value)
          case DoubleAttribute(value)         => q.gte(value)
          case OffsetDateTimeAttribute(value) => q.gte(value.toString)
          case LocalDateAttribute(value)      => q.gte(ElasticDateMath(value.format(DateTimeFormatter.ISO_LOCAL_DATE)))
          case LocalTimeAttribute(value)      =>
            q.gte(ElasticDateMath(LocalDateTime.of(LOCAL_TIME_DATE, value).format(DateTimeFormatter.ISO_LOCAL_DATE)))
          case _                              => q
        }
      case "lt"  =>
        attr match {
          case StringAttribute(value)         => q.lt(value)
          case LongAttribute(value)           => q.lt(value)
          case DoubleAttribute(value)         => q.lt(value)
          case OffsetDateTimeAttribute(value) => q.lt(value.toString)
          case LocalDateAttribute(value)      => q.lt(ElasticDateMath(value.format(DateTimeFormatter.ISO_LOCAL_DATE)))
          case LocalTimeAttribute(value)      =>
            q.lt(ElasticDateMath(LocalDateTime.of(LOCAL_TIME_DATE, value).format(DateTimeFormatter.ISO_LOCAL_DATE)))
          case _                              => q
        }
      case "lte" =>
        attr match {
          case StringAttribute(value)         => q.lte(value)
          case LongAttribute(value)           => q.lte(value)
          case DoubleAttribute(value)         => q.lte(value)
          case OffsetDateTimeAttribute(value) => q.lte(value.toString)
          case LocalDateAttribute(value)      => q.lte(ElasticDateMath(value.format(DateTimeFormatter.ISO_LOCAL_DATE)))
          case LocalTimeAttribute(value)      =>
            q.lte(ElasticDateMath(LocalDateTime.of(LOCAL_TIME_DATE, value).format(DateTimeFormatter.ISO_LOCAL_DATE)))
          case _                              => q
        }
    }

}
