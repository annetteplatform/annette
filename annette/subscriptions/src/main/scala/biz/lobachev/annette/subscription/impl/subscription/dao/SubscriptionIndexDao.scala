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

package biz.lobachev.annette.subscription.impl.subscription.dao

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.microservice_core.indexing.dao.AbstractIndexDao
import biz.lobachev.annette.subscription.api.subscription.{
  SubscriptionFindQuery,
  SubscriptionFindResult,
  SubscriptionHitResult,
  SubscriptionKey
}
import biz.lobachev.annette.subscription.impl.subscription.SubscriptionEntity
import biz.lobachev.annette.subscription.impl.subscription.SubscriptionEntity.SubscriptionDeleted
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.requests.searches.SearchRequest
import com.sksamuel.elastic4s.requests.searches.sort.FieldSort
import org.slf4j.LoggerFactory

import java.time.OffsetDateTime
import scala.collection.immutable.Seq
import scala.concurrent.{ExecutionContext, Future}

class SubscriptionIndexDao(client: ElasticClient)(implicit
  override val ec: ExecutionContext
) extends AbstractIndexDao(client) {

  override val log = LoggerFactory.getLogger(this.getClass)

  override def indexConfigPath = "indexing.subscription-index"

  // *************************** Index API ***************************

  def createSubscription(event: SubscriptionEntity.SubscriptionCreated) = {
    val id = s"${event.subscriptionType}~${event.objectId}~${event.principal.code}"
    createIndexDoc(
      id,
      "id"               -> id,
      "subscriptionType" -> event.subscriptionType,
      "principal"        -> event.principal.code,
      "principalType"    -> event.principal.principalType,
      "principalId"      -> event.principal.principalId,
      "objectId"         -> event.objectId,
      "updatedAt"        -> event.createdAt
    )
  }

  def deleteSubscription(event: SubscriptionDeleted) = {
    val id = s"${event.subscriptionType}~${event.objectId}~${event.principal.code}"
    deleteIndexDoc(id)
  }

  // *************************** Search API ***************************

  def findSubscription(query: SubscriptionFindQuery): Future[SubscriptionFindResult] = {

    val sortBy: Seq[FieldSort] = buildSortBySeq(query.sortBy)

    val subscriptionTypeQuery =
      query.subscriptionType
        .map(subscriptionTypes => termsQuery(alias2FieldName("subscriptionType"), subscriptionTypes))
        .toSeq
    val objectQuery           = query.objects.map(objectIds => termsQuery(alias2FieldName("objectId"), objectIds)).toSeq
    val principalQuery        =
      query.principals.map(principals => termsQuery(alias2FieldName("principal"), principals.map(_.code))).toSeq

    val searchRequest = search(indexName)
      .bool(must(subscriptionTypeQuery ++ objectQuery ++ principalQuery))
      .from(query.offset)
      .size(query.size)
      .sortBy(sortBy)
      .sourceInclude(
        alias2FieldName("subscriptionType"),
        alias2FieldName("principalType"),
        alias2FieldName("principalId"),
        alias2FieldName("objectId"),
        alias2FieldName("updatedAt")
      )
      .trackTotalHits(true)

    findSubscriptionKeys(searchRequest)

  }

  protected def findSubscriptionKeys(searchRequest: SearchRequest): Future[SubscriptionFindResult] =
    client.execute(searchRequest).map { res =>
      val result = processResponse(res)
      val total  = result.hits.total.value
      val hits   = result.hits.hits.map { hit =>
        val subscriptionKey = SubscriptionKey(
          hit.sourceAsMap.get(alias2FieldName("subscriptionType")).map(_.toString).getOrElse(""),
          hit.sourceAsMap.get(alias2FieldName("objectId")).map(_.toString).getOrElse(""),
          AnnettePrincipal(
            hit.sourceAsMap.get(alias2FieldName("principalType")).map(_.toString).getOrElse(""),
            hit.sourceAsMap.get(alias2FieldName("principalId")).map(_.toString).getOrElse("")
          )
        )
        val updatedAt       = hit.sourceAsMap
          .get(alias2FieldName("updatedAt"))
          .map(v => OffsetDateTime.parse(v.toString))
          .getOrElse(OffsetDateTime.now)
        SubscriptionHitResult(subscriptionKey, hit.score, updatedAt)
      }.toSeq
      SubscriptionFindResult(total, hits)
    }
}
