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

package biz.lobachev.annette.cms.impl.home_pages.dao

import biz.lobachev.annette.cms.api.home_pages.{HomePage, HomePageFindQuery}
import biz.lobachev.annette.cms.api.pages.page.PageId
import biz.lobachev.annette.cms.impl.home_pages.HomePageEntity
import biz.lobachev.annette.core.model.indexing.FindResult
import biz.lobachev.annette.microservice_core.indexing.dao.AbstractIndexDao
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.requests.searches.sort.{FieldSort, SortOrder}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class HomePageIndexDao(client: ElasticClient)(implicit
  override val ec: ExecutionContext
) extends AbstractIndexDao(client) {

  override val log = LoggerFactory.getLogger(this.getClass)

  override def indexConfigPath = "indexing.home-page-index"

  def assignHomePage(event: HomePageEntity.HomePageAssigned) = {
    val id = HomePage.toCompositeId(event.applicationId, event.principal)
    createIndexDoc(
      id,
      "id"            -> id,
      "applicationId" -> event.applicationId,
      "principalCode" -> event.principal.code,
      "principalType" -> event.principal.principalType,
      "principalId"   -> event.principal.principalId,
      "priority"      -> event.priority,
      "pageId"        -> event.pageId,
      "updatedBy"     -> event.updatedBy.code,
      "updatedAt"     -> event.updatedAt
    )
  }

  def unassignHomePage(event: HomePageEntity.HomePageUnassigned) =
    deleteIndexDoc(event.id)

  def findHomePages(query: HomePageFindQuery): Future[FindResult] = {

    val applicationIdQuery  = query.applicationId.map(matchQuery(alias2FieldName("applicationId"), _)).toSeq
    val principalCodesQuery =
      query.principalCodes
        .map(principalCodes => termsSetQuery(alias2FieldName("principalCode"), principalCodes, script("1")))
        .toSeq
    val principalTypeQuery  = query.principalType.map(matchQuery(alias2FieldName("principalType"), _)).toSeq
    val principalIdQuery    = query.principalId.map(matchQuery(alias2FieldName("principalId"), _)).toSeq
    val pageIdQuery         = query.pageId.map(matchQuery(alias2FieldName("pageId"), _)).toSeq

    val sortBy: Seq[FieldSort] = buildSortBySeq(query.sortBy)

    val searchRequest = search(indexName)
      .bool(must(applicationIdQuery ++ principalCodesQuery ++ principalTypeQuery ++ principalIdQuery ++ pageIdQuery))
      .from(query.offset)
      .size(query.size)
      .sortBy(sortBy)
      .sourceInclude(alias2FieldName("updatedAt"))
      .trackTotalHits(true)

    findEntity(searchRequest)

  }

  def getHomePageByPrincipalCodes(applicationId: String, principalCodes: Seq[String]): Future[Option[PageId]] = {
    val applicationIdQuery     = matchQuery(alias2FieldName("applicationId"), applicationId)
    val principalCodesQuery    = termsSetQuery(alias2FieldName("principalCode"), principalCodes.toSet, script("1"))
    val sortBy: Seq[FieldSort] = Seq(FieldSort(alias2FieldName("priority"), order = SortOrder.Desc))
    val searchRequest          = search(indexName)
      .bool(must(Seq(applicationIdQuery, principalCodesQuery)))
      .from(0)
      .size(1)
      .sortBy(sortBy)
      .sourceInclude(alias2FieldName("pageId"))
      .trackTotalHits(false)
    client.execute(searchRequest).map { res =>
      val result = processResponse(res)
      result.hits.hits.headOption
        .map(
          _.sourceAsMap
            .get(alias2FieldName("pageId"))
            .map(v => v.toString)
        )
        .flatten
    }
  }

}
