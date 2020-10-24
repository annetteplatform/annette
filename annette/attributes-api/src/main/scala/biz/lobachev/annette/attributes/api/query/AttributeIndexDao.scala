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

import akka.Done
import biz.lobachev.annette.attributes.api.assignment._
import biz.lobachev.annette.attributes.api.schema.AttributeIndex
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.requests.indexes.PutMappingResponse
import com.sksamuel.elastic4s.requests.searches.queries.{Query, RangeQuery}

import scala.concurrent.Future

trait AttributeIndexDao {

  // *************************** Attribute API ***************************

  def createAttribute(fieldName: String, index: AttributeIndex): Future[Done]

  def createAttributeInt(
    fieldName: String,
    index: AttributeIndex
  ): Future[Response[PutMappingResponse]]

  def assignAttribute(id: ObjectId, fieldName: String, attribute: AttributeValue): Future[Done]

  def unassignAttribute(id: ObjectId, fieldName: String): Future[Done]

  // *************************** Search API ***************************

  def buildAttributeQuery(maybeQuery: Option[AttributeQuery]): Seq[Query]

  def attributeValue(attribute: AttributeValue): Any

  def rangeCond(q: RangeQuery, cond: String, attr: AttributeValue): RangeQuery
}
