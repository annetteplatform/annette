package biz.lobachev.annette.cms.impl.post.dao

import biz.lobachev.annette.cms.api.post.PostFindQuery
import biz.lobachev.annette.cms.impl.post.PostEntity
import biz.lobachev.annette.core.model.elastic.FindResult
import biz.lobachev.annette.microservice_core.elastic.{AbstractElasticIndexDao, ElasticSettings}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.requests.indexes.CreateIndexRequest
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class PostElasticIndexDao(elasticSettings: ElasticSettings, elasticClient: ElasticClient)(implicit
  override val ec: ExecutionContext
) extends AbstractElasticIndexDao(elasticSettings, elasticClient) {

  override val log = LoggerFactory.getLogger(this.getClass)

  override def indexSuffix = "cms-post"

  def createIndexRequest: CreateIndexRequest =
    createIndex(indexName)
      .mapping(
        properties(
          keywordField("id")
        )
      )

  def createPost(event: PostEntity.PostCreated): Future[Unit] = ???
//    elasticClient.execute {
//      // TODO: insert elastic code
//      ???
//    }
//      .map(processResponse("createPost", event.id)(_))

  def updatePostFeatured(event: PostEntity.PostFeaturedUpdated): Future[Unit] = ???
//    elasticClient.execute {
//      // TODO: insert elastic code
//      ???
//    }
//      .map(processResponse("updatePostFeatured", event.id)(_))

  def updatePostAuthor(event: PostEntity.PostAuthorUpdated): Future[Unit] = ???
//    elasticClient.execute {
//      // TODO: insert elastic code
//      ???
//    }
//      .map(processResponse("updatePostAuthor", event.id)(_))

  def updatePostTitle(event: PostEntity.PostTitleUpdated): Future[Unit] = ???
//    elasticClient.execute {
//      // TODO: insert elastic code
//
//    }
//      .map(processResponse("updatePostTitle", event.id)(_))

  def updatePostIntro(event: PostEntity.PostIntroUpdated): Future[Unit] = ???
//    elasticClient.execute {
//      // TODO: insert elastic code
//      ???
//    }
//      .map(processResponse("updatePostIntro", event.id)(_))

  def updatePostContent(event: PostEntity.PostContentUpdated): Future[Unit] = ???
//    elasticClient.execute {
//      // TODO: insert elastic code
//      ???
//    }
//      .map(processResponse("updatePostContent", event.id)(_))

  def updatePostPublicationTimestamp(event: PostEntity.PostPublicationTimestampUpdated): Future[Unit] = ???
//    elasticClient.execute {
//      // TODO: insert elastic code
//      ???
//    }
//      .map(processResponse("updatePostPublicationTimestamp", event.id)(_))

  def publishPost(event: PostEntity.PostPublished): Future[Unit] = ???
//    elasticClient.execute {
//      // TODO: insert elastic code
//      ???
//    }
//      .map(processResponse("publishPost", event.id)(_))

  def unpublishPost(event: PostEntity.PostUnpublished): Future[Unit] = ???
//    elasticClient.execute {
//      // TODO: insert elastic code
//      ???
//    }
//      .map(processResponse("unpublishPost", event.id)(_))

  def assignPostTargetPrincipal(event: PostEntity.PostTargetPrincipalAssigned): Future[Unit] = ???
//    elasticClient.execute {
//      // TODO: insert elastic code
//      ???
//    }
//      .map(processResponse("assignPostTargetPrincipal", event.id)(_))

  def unassignPostTargetPrincipal(event: PostEntity.PostTargetPrincipalUnassigned): Future[Unit] = ???
//    elasticClient.execute {
//      // TODO: insert elastic code
//      ???
//    }
//      .map(processResponse("unassignPostTargetPrincipal", event.id)(_))

  def deletePost(event: PostEntity.PostDeleted): Future[Unit] = ???
//    elasticClient.execute {
//      // TODO: insert elastic code
//      ???
//    }
//      .map(processResponse("deletePost", event.id)(_))

  def addPostMedia(event: PostEntity.PostMediaAdded): Future[Unit] = ???
//    elasticClient.execute {
//      // TODO: insert elastic code
//      ???
//    }
//      .map(processResponse("addPostMedia", event.id)(_))

  def removePostMedia(event: PostEntity.PostMediaRemoved): Future[Unit] = ???
//    elasticClient.execute {
//      // TODO: insert elastic code
//      ???
//    }
//      .map(processResponse("removePostMedia", event.id)(_))

  def addPostDoc(event: PostEntity.PostDocAdded): Future[Unit] = ???
//    elasticClient.execute {
//      // TODO: insert elastic code
//      ???
//    }
//      .map(processResponse("addPostDoc", event.id)(_))

  def updatePostDocName(event: PostEntity.PostDocNameUpdated): Future[Unit] = ???
//    elasticClient.execute {
//      // TODO: insert elastic code
//      ???
//    }
//      .map(processResponse("updatePostDocName", event.id)(_))

  def removePostDoc(event: PostEntity.PostDocRemoved): Future[Unit] = ???
//    elasticClient.execute {
//      // TODO: insert elastic code
//      ???
//    }
//      .map(processResponse("removePostDoc", event.id)(_))

  def findPosts(query: PostFindQuery): Future[FindResult] = ???

  //  private def processResponse[T](method: String, id: String): PartialFunction[Response[T], Unit] = {
//    case success: RequestSuccess[_] =>
//      log.debug("{}( {} ): {}", method, id, success)
//    case failure: RequestFailure    =>
//      log.error("{}( {} ): {}", method, id, failure)
//      throw failure.error.asException
//  }
}
