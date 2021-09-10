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
import biz.lobachev.annette.microservice_core.elastic.{AbstractElasticIndexDao, ElasticSettings}
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
import com.sksamuel.elastic4s.requests.common.RefreshPolicy
import com.sksamuel.elastic4s.requests.indexes.CreateIndexRequest
import com.sksamuel.elastic4s.requests.searches.{SearchRequest, SearchResponse}
import com.sksamuel.elastic4s.requests.searches.sort.FieldSort
import org.slf4j.LoggerFactory

import java.time.OffsetDateTime
import scala.collection.immutable.Seq
import scala.concurrent.{ExecutionContext, Future}

class SubscriptionElasticIndexDao(elasticSettings: ElasticSettings, elasticClient: ElasticClient)(implicit
  override val ec: ExecutionContext
) extends AbstractElasticIndexDao(elasticSettings, elasticClient) {

  override val log = LoggerFactory.getLogger(this.getClass)

  override def indexSuffix = "subscriptions-subscription"

  // *************************** Index API ***************************

  def createIndexRequest: CreateIndexRequest =
    createIndex(indexName)
      .mapping(
        properties(
          keywordField("id"),
          keywordField("subscriptionType"),
          keywordField("principal"),
          keywordField("principalType"),
          keywordField("principalId"),
          keywordField("objectId"),
          dateField("updatedAt")
        )
      )

  def createSubscription(event: SubscriptionEntity.SubscriptionCreated): Future[Unit] = {
    val id = s"${event.subscriptionType}~${event.objectId}~${event.principal.code}"
    elasticClient.execute {
      indexInto(indexName)
        .id(id)
        .fields(
          "id"               -> id,
          "subscriptionType" -> event.subscriptionType,
          "principal"        -> event.principal.code,
          "principalType"    -> event.principal.principalType,
          "principalId"      -> event.principal.principalId,
          "objectId"         -> event.objectId,
          "updatedAt"        -> event.createdAt
        )
        .refresh(RefreshPolicy.Immediate)
    }.map(processResponse("createSubscription", id)(_))
  }

  def deleteSubscription(event: SubscriptionDeleted): Future[Unit] = {
    val id = s"${event.subscriptionType}~${event.objectId}~${event.principal.code}"
    elasticClient.execute {
      deleteById(indexName, id)
    }
      .map(processResponse("deleteSubscription", id)(_))
  }

  // *************************** Search API ***************************

  def findSubscription(query: SubscriptionFindQuery): Future[SubscriptionFindResult] = {

    val sortBy: Seq[FieldSort] = buildSortBy(query.sortBy)

    val subscriptionTypeQuery =
      query.subscriptionType.map(subscriptionTypes => termsQuery("subscriptionType", subscriptionTypes)).toSeq
    val objectQuery           = query.objects.map(objectIds => termsQuery("objectId", objectIds)).toSeq
    val principalQuery        = query.principals.map(principals => termsQuery("principal", principals.map(_.code))).toSeq

    val searchRequest = search(indexName)
      .bool(must(subscriptionTypeQuery ++ objectQuery ++ principalQuery))
      .from(query.offset)
      .size(query.size)
      .sortBy(sortBy)
      .sourceInclude("subscriptionType", "principalType", "principalId", "objectId", "updatedAt")
      .trackTotalHits(true)

    findSubscriptionKeys(searchRequest)

  }

  protected def findSubscriptionKeys(searchRequest: SearchRequest): Future[SubscriptionFindResult] = {
    log.debug(s"findEntity: searchRequest: ${searchRequest.toString} ")
    for {
      resp <- elasticClient.execute(searchRequest)
    } yield resp match {
      case failure: RequestFailure                 =>
        log.error("findEntity: failed " + failure.error)
        SubscriptionFindResult(0, Seq.empty)
      case results: RequestSuccess[SearchResponse] =>
        log.trace(s"findEntity: results ${results.toString}")
        val total = results.result.hits.total.value
        val hits  = results.result.hits.hits.map { hit =>
          val subscriptionKey = SubscriptionKey(
            hit.sourceAsMap.get("subscriptionType").map(_.toString).getOrElse(""),
            hit.sourceAsMap.get("objectId").map(_.toString).getOrElse(""),
            AnnettePrincipal(
              hit.sourceAsMap.get("principalType").map(_.toString).getOrElse(""),
              hit.sourceAsMap.get("principalId").map(_.toString).getOrElse("")
            )
          )
          val updatedAt       = hit.sourceAsMap
            .get("updatedAt")
            .map(v => OffsetDateTime.parse(v.toString))
            .getOrElse(OffsetDateTime.now)
          SubscriptionHitResult(subscriptionKey, hit.score, updatedAt)
        }.toSeq
        SubscriptionFindResult(total, hits)
    }
  }
}
