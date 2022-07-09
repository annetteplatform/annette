package biz.lobachev.annette.service_catalog.impl.group

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
import biz.lobachev.annette.service_catalog.api.group._

class GroupElasticIndexDao(elasticSettings: ElasticSettings, elasticClient: ElasticClient)(
    implicit override val ec: ExecutionContext

) extends AbstractElasticIndexDao(elasticSettings, elasticClient) {

  override val log = LoggerFactory.getLogger(this.getClass)

  override def indexSuffix = "service-catalog-group"

  def createIndexRequest: CreateIndexRequest =
    createIndex(indexName)
      .mapping(
        properties(
          keywordField("id"),
        )
      )

  def createGroup(event: GroupEntity.GroupCreated): Future[Unit] = ??? // {
//    elasticClient
//      .execute {
//        // TODO: insert elastic code
//      }
//      .map(processResponse("createGroup", event.id)(_))
//  }

  def updateGroupName(event: GroupEntity.GroupUpdated): Future[Unit] = ??? // {
//    elasticClient
//      .execute {
//        // TODO: insert elastic code
//      }
//      .map(processResponse("updateGroupName", event.id)(_))
//  }

  def activateGroup(event: GroupEntity.GroupActivated): Future[Unit] = ??? // {
//    elasticClient
//      .execute {
//        // TODO: insert elastic code
//      }
//      .map(processResponse("activateGroup", event.id)(_))
//  }

  def deactivateGroup(event: GroupEntity.GroupDeactivated): Future[Unit] = ??? // {
//    elasticClient
//      .execute {
//        // TODO: insert elastic code
//      }
//      .map(processResponse("deactivateGroup", event.id)(_))
//  }

  def deleteGroup(event: GroupEntity.GroupDeleted): Future[Unit] = ??? // {
//    elasticClient
//      .execute {
//        // TODO: insert elastic code
//      }
//      .map(processResponse("deleteGroup", event.id)(_))
//  }


  private def processResponse[T](method: String, id: String): PartialFunction[Response[T], Unit] = {
    case success: RequestSuccess[_] =>
      log.debug("{}( {} ): {}", method, id, success)
    case failure: RequestFailure =>
      log.error("{}( {} ): {}", method, id, failure)
      throw failure.error.asException
  }
}
