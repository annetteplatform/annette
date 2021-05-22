package biz.lobachev.annette.cms.impl.space.dao

import biz.lobachev.annette.cms.api.space.SpaceFindQuery
import biz.lobachev.annette.cms.impl.space.SpaceEntity
import biz.lobachev.annette.core.model.elastic.FindResult
import biz.lobachev.annette.microservice_core.elastic.{AbstractElasticIndexDao, ElasticSettings}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.requests.indexes.CreateIndexRequest
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class SpaceElasticIndexDao(elasticSettings: ElasticSettings, elasticClient: ElasticClient)(implicit
  override val ec: ExecutionContext
) extends AbstractElasticIndexDao(elasticSettings, elasticClient) {

  override val log = LoggerFactory.getLogger(this.getClass)

  override def indexSuffix = "cms-space"

  def createIndexRequest: CreateIndexRequest =
    createIndex(indexName)
      .mapping(
        properties(
          keywordField("id")
        )
      )

  def createSpace(event: SpaceEntity.SpaceCreated): Future[Unit] = ???
//    elasticClient.execute {
//      // TODO: insert elastic code
//      ???
//    }
//      .map(processResponse("createSpace", event.id)(_))

  def updateSpaceName(event: SpaceEntity.SpaceNameUpdated): Future[Unit] = ???
//    elasticClient.execute {
//      // TODO: insert elastic code
//      ???
//    }
//      .map(processResponse("updateSpaceName", event.id)(_))

  def updateSpaceDescription(event: SpaceEntity.SpaceDescriptionUpdated): Future[Unit] = ???
//    elasticClient.execute {
//      // TODO: insert elastic code
//      ???
//    }
//      .map(processResponse("updateSpaceDescription", event.id)(_))

  def updateSpaceCategory(event: SpaceEntity.SpaceCategoryUpdated): Future[Unit] = ???
//    elasticClient.execute {
//      // TODO: insert elastic code
//      ???
//    }
//      .map(processResponse("updateSpaceCategory", event.id)(_))

  def assignSpaceTargetPrincipal(event: SpaceEntity.SpaceTargetPrincipalAssigned): Future[Unit] = ???
//    elasticClient.execute {
//      // TODO: insert elastic code
//      ???
//    }
//      .map(processResponse("assignSpaceTargetPrincipal", event.id)(_))

  def unassignSpaceTargetPrincipal(event: SpaceEntity.SpaceTargetPrincipalUnassigned): Future[Unit] = ???
//    elasticClient.execute {
//      // TODO: insert elastic code
//      ???
//    }
//      .map(processResponse("unassignSpaceTargetPrincipal", event.id)(_))

  def activateSpace(event: SpaceEntity.SpaceActivated): Future[Unit] = ???
//    elasticClient.execute {
//      // TODO: insert elastic code
//      ???
//    }
//      .map(processResponse("activateSpace", event.id)(_))

  def deactivateSpace(event: SpaceEntity.SpaceDeactivated): Future[Unit] = ???
//    elasticClient.execute {
//      // TODO: insert elastic code
//      ???
//    }
//      .map(processResponse("deactivateSpace", event.id)(_))

  def deleteSpace(event: SpaceEntity.SpaceDeleted): Future[Unit] = ???
//    elasticClient.execute {
//      // TODO: insert elastic code
//      ???
//    }
//      .map(processResponse("deleteSpace", event.id)(_))

  def findSpaces(query: SpaceFindQuery): Future[FindResult] = ???

//  private def processResponse[T](method: String, id: String): PartialFunction[Response[T], Unit] = {
//    case success: RequestSuccess[_] =>
//      log.debug("{}( {} ): {}", method, id, success)
//    case failure: RequestFailure    =>
//      log.error("{}( {} ): {}", method, id, failure)
//      throw failure.error.asException
//  }
}
