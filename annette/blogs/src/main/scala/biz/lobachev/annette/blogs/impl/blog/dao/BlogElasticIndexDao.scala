package biz.lobachev.annette.blogs.impl.blog.dao

import biz.lobachev.annette.blogs.api.blog.BlogFindQuery
import biz.lobachev.annette.blogs.impl.blog.BlogEntity
import biz.lobachev.annette.core.model.elastic.FindResult
import biz.lobachev.annette.microservice_core.elastic.{AbstractElasticIndexDao, ElasticSettings}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.requests.indexes.CreateIndexRequest
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class BlogElasticIndexDao(elasticSettings: ElasticSettings, elasticClient: ElasticClient)(implicit
  override val ec: ExecutionContext
) extends AbstractElasticIndexDao(elasticSettings, elasticClient) {

  override val log = LoggerFactory.getLogger(this.getClass)

  override def indexSuffix = "blogs-blog"

  def createIndexRequest: CreateIndexRequest =
    createIndex(indexName)
      .mapping(
        properties(
          keywordField("id")
        )
      )

  def createBlog(event: BlogEntity.BlogCreated): Future[Unit] = ???
//    elasticClient.execute {
//      // TODO: insert elastic code
//      ???
//    }
//      .map(processResponse("createBlog", event.id)(_))

  def updateBlogName(event: BlogEntity.BlogNameUpdated): Future[Unit] = ???
//    elasticClient.execute {
//      // TODO: insert elastic code
//      ???
//    }
//      .map(processResponse("updateBlogName", event.id)(_))

  def updateBlogDescription(event: BlogEntity.BlogDescriptionUpdated): Future[Unit] = ???
//    elasticClient.execute {
//      // TODO: insert elastic code
//      ???
//    }
//      .map(processResponse("updateBlogDescription", event.id)(_))

  def updateBlogCategory(event: BlogEntity.BlogCategoryUpdated): Future[Unit] = ???
//    elasticClient.execute {
//      // TODO: insert elastic code
//      ???
//    }
//      .map(processResponse("updateBlogCategory", event.id)(_))

  def assignBlogTargetPrincipal(event: BlogEntity.BlogTargetPrincipalAssigned): Future[Unit] = ???
//    elasticClient.execute {
//      // TODO: insert elastic code
//      ???
//    }
//      .map(processResponse("assignBlogTargetPrincipal", event.id)(_))

  def unassignBlogTargetPrincipal(event: BlogEntity.BlogTargetPrincipalUnassigned): Future[Unit] = ???
//    elasticClient.execute {
//      // TODO: insert elastic code
//      ???
//    }
//      .map(processResponse("unassignBlogTargetPrincipal", event.id)(_))

  def activateBlog(event: BlogEntity.BlogActivated): Future[Unit] = ???
//    elasticClient.execute {
//      // TODO: insert elastic code
//      ???
//    }
//      .map(processResponse("activateBlog", event.id)(_))

  def deactivateBlog(event: BlogEntity.BlogDeactivated): Future[Unit] = ???
//    elasticClient.execute {
//      // TODO: insert elastic code
//      ???
//    }
//      .map(processResponse("deactivateBlog", event.id)(_))

  def deleteBlog(event: BlogEntity.BlogDeleted): Future[Unit] = ???
//    elasticClient.execute {
//      // TODO: insert elastic code
//      ???
//    }
//      .map(processResponse("deleteBlog", event.id)(_))

  def findBlogs(query: BlogFindQuery): Future[FindResult] = ???

//  private def processResponse[T](method: String, id: String): PartialFunction[Response[T], Unit] = {
//    case success: RequestSuccess[_] =>
//      log.debug("{}( {} ): {}", method, id, success)
//    case failure: RequestFailure    =>
//      log.error("{}( {} ): {}", method, id, failure)
//      throw failure.error.asException
//  }
}
