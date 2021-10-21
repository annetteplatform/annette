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

package biz.lobachev.annette.persons.impl.person.dao

import biz.lobachev.annette.core.model.indexing.FindResult
import biz.lobachev.annette.microservice_core.attribute.dao.AttributeIndexing
import biz.lobachev.annette.microservice_core.indexing.dao.AbstractIndexDao
import biz.lobachev.annette.persons.api.person._
import biz.lobachev.annette.persons.impl.person.{PersonEntity, PersonMetadata}
import biz.lobachev.annette.persons.impl.person.PersonEntity.PersonDeleted
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.requests.searches.sort.FieldSort
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class PersonIndexDao(client: ElasticClient)(implicit
  override val ec: ExecutionContext
) extends AbstractIndexDao(client)
    with AttributeIndexing {

  override val log = LoggerFactory.getLogger(this.getClass)

  override def indexConfigPath = "indexing.person-index"

  def createPerson(event: PersonEntity.PersonCreated) = {
    val doc        = List(
      "id"         -> event.id,
      "lastname"   -> event.lastname,
      "firstname"  -> event.firstname,
      "middlename" -> event.middlename,
      "categoryId" -> event.categoryId,
      "phone"      -> event.phone,
      "email"      -> event.email,
      "source"     -> event.source,
      "externalId" -> event.externalId,
      "updatedAt"  -> event.createdAt
    )
    val attributes = event.attributes.map(attrs => convertAttributes(attrs, PersonMetadata.metadata)).getOrElse(Nil)
    createIndexDoc(
      event.id,
      doc ++ attributes
    )
  }

  def updatePerson(event: PersonEntity.PersonUpdated) = {
    val doc        = List(
      "id"         -> event.id,
      "lastname"   -> event.lastname,
      "firstname"  -> event.firstname,
      "middlename" -> event.middlename,
      "categoryId" -> event.categoryId,
      "phone"      -> event.phone,
      "email"      -> event.email,
      "source"     -> event.source,
      "externalId" -> event.externalId,
      "updatedAt"  -> event.updatedAt
    )
    val attributes = event.attributes.map(attrs => convertAttributes(attrs, PersonMetadata.metadata)).getOrElse(Nil)
    updateIndexDoc(
      event.id,
      doc ++ attributes
    )
  }

  def updatePersonAttributes(event: PersonEntity.PersonAttributesUpdated) = {
    val doc        = List(
      "id"        -> event.id,
      "updatedAt" -> event.updatedAt
    )
    val attributes = convertAttributes(event.attributes, PersonMetadata.metadata)
    updateIndexDoc(
      event.id,
      doc ++ attributes
    )
  }

  def deletePerson(event: PersonDeleted) =
    deleteIndexDoc(event.id)

  // *************************** Search API ***************************

  def findPerson(query: PersonFindQuery): Future[FindResult] = {

    val fieldQuery             = Seq(
      query.firstname.map(matchQuery(alias2FieldName("firstname"), _)),
      query.lastname.map(matchQuery(alias2FieldName("lastname"), _)),
      query.middlename.map(matchQuery(alias2FieldName("middlename"), _)),
      query.phone.map(matchQuery(alias2FieldName("phone"), _)),
      query.email.map(matchQuery(alias2FieldName("email"), _))
    ).flatten
    val filterQuery            = buildFilterQuery(
      query.filter,
      Seq("lastname" -> 3.0, "firstname" -> 2.0, "middlename" -> 1.0, "email" -> 3.0, "phone" -> 1.0)
    )
    val sortBy: Seq[FieldSort] = buildSortBySeq(query.sortBy)
    val categoryQuery          =
      query.categories.map(category => termsSetQuery(alias2FieldName("categoryId"), category, script("1"))).toSeq
    val sourceQuery            = query.sources.map(sources => termsSetQuery(alias2FieldName("source"), sources, script("1"))).toSeq
    val externalIdQuery        =
      query.externalIds.map(externalIds => termsSetQuery(alias2FieldName("externalId"), externalIds, script("1"))).toSeq

    val advancedQueries = buildAdvancedQueries(query.query)

    val searchRequest = search(indexName)
      .bool(must(filterQuery ++ fieldQuery ++ categoryQuery ++ sourceQuery ++ externalIdQuery ++ advancedQueries))
      .from(query.offset)
      .size(query.size)
      .sortBy(sortBy)
      .sourceInclude(alias2FieldName("updatedAt"))
      .trackTotalHits(true)

    findEntity(searchRequest)

  }

}
