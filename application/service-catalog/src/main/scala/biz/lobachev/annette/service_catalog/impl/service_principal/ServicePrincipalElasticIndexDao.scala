package biz.lobachev.annette.service_catalog.impl.service_principal

import java.time.OffsetDateTime
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.requests.common.RefreshPolicy
import com.sksamuel.elastic4s.requests.indexes.CreateIndexRequest
import com.sksamuel.elastic4s.requests.searches.queries.matches.{
  FieldWithOptionalBoost,
  MultiMatchQuery,
  MultiMatchQueryBuilderType
}
import com.sksamuel.elastic4s.requests.searches.sort.{FieldSort, SortOrder}
import org.slf4j.LoggerFactory
import scala.collection.immutable.Seq
import scala.concurrent.{ExecutionContext, Future}

import biz.lobachev.annette.core.elastic.{AbstractElasticIndexDao, ElasticSettings, FindResult, SortBy}
import biz.lobachev.annette.service_catalog.api.service_principal._

class ServicePrincipalElasticIndexDao(elasticSettings: ElasticSettings, elasticClient: ElasticClient)(
    implicit override val ec: ExecutionContext

) extends AbstractElasticIndexDao(elasticSettings, elasticClient) {

  override val log = LoggerFactory.getLogger(this.getClass)

  override def indexSuffix = "service-catalog-service-principal"

  def createIndexRequest: CreateIndexRequest =
    createIndex(indexName)
      .mapping(
        properties(
          keywordField("id"),
        )
      )

  def assignServicePrincipal(event: ServicePrincipalEntity.ServicePrincipalAssigned): Future[Unit] = ??? // {
//    elasticClient
//      .execute {
//        // TODO: insert elastic code
//      }
//      .map(processResponse("assignServicePrincipal", event.id)(_))
//  }

  def unassignServicePrincipal(event: ServicePrincipalEntity.ServicePrincipalUnassigned): Future[Unit] = ??? // {
//    elasticClient
//      .execute {
//        // TODO: insert elastic code
//      }
//      .map(processResponse("unassignServicePrincipal", event.id)(_))
//  }


  private def processResponse[T](method: String, id: String): PartialFunction[Response[T], Unit] = {
    case success: RequestSuccess[_] =>
      log.debug("{}( {} ): {}", method, id, success)
    case failure: RequestFailure =>
      log.error("{}( {} ): {}", method, id, failure)
      throw failure.error.asException
  }
}
