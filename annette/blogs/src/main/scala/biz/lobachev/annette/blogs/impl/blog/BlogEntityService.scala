package biz.lobachev.annette.blogs.impl.blog

import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.util.Timeout
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import biz.lobachev.annette.blogs.api.blog._
import biz.lobachev.annette.blogs.impl.blog.dao.{BlogCassandraDbDao, BlogElasticIndexDao}
import biz.lobachev.annette.core.model.elastic.FindResult
import io.scalaland.chimney.dsl._

class BlogEntityService(
  clusterSharding: ClusterSharding,
  dbDao: BlogCassandraDbDao,
  indexDao: BlogElasticIndexDao
)(implicit
  ec: ExecutionContext
) {
  val log = LoggerFactory.getLogger(this.getClass)

  implicit val timeout = Timeout(50.seconds)

  private def refFor(id: BlogId): EntityRef[BlogEntity.Command] =
    clusterSharding.entityRefFor(BlogEntity.typeKey, id)

  private def convertSuccess(confirmation: BlogEntity.Confirmation, id: BlogId): Done =
    confirmation match {
      case BlogEntity.Success          => Done
      case BlogEntity.BlogAlreadyExist => throw BlogAlreadyExist(id)
      case BlogEntity.BlogNotFound     => throw BlogNotFound(id)
      case _                           => throw new RuntimeException("Match fail")
    }

  private def convertSuccessBlog(confirmation: BlogEntity.Confirmation, id: BlogId): Blog =
    confirmation match {
      case BlogEntity.SuccessBlog(blog) => blog
      case BlogEntity.BlogAlreadyExist  => throw BlogAlreadyExist(id)
      case BlogEntity.BlogNotFound      => throw BlogNotFound(id)
      case _                            => throw new RuntimeException("Match fail")
    }

  private def convertSuccessBlogAnnotation(confirmation: BlogEntity.Confirmation, id: BlogId): BlogAnnotation =
    confirmation match {
      case BlogEntity.SuccessBlogAnnotation(blogAnnotation) => blogAnnotation
      case BlogEntity.BlogAlreadyExist                      => throw BlogAlreadyExist(id)
      case BlogEntity.BlogNotFound                          => throw BlogNotFound(id)
      case _                                                => throw new RuntimeException("Match fail")
    }

  def createBlog(payload: CreateBlogPayload): Future[Done] =
    refFor(payload.id)
      .ask[BlogEntity.Confirmation](replyTo =>
        payload
          .into[BlogEntity.CreateBlog]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def updateBlogName(payload: UpdateBlogNamePayload): Future[Done] =
    refFor(payload.id)
      .ask[BlogEntity.Confirmation](replyTo =>
        payload
          .into[BlogEntity.UpdateBlogName]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def updateBlogDescription(payload: UpdateBlogDescriptionPayload): Future[Done] =
    refFor(payload.id)
      .ask[BlogEntity.Confirmation](replyTo =>
        payload
          .into[BlogEntity.UpdateBlogDescription]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def updateBlogCategory(payload: UpdateBlogCategoryPayload): Future[Done] =
    refFor(payload.id)
      .ask[BlogEntity.Confirmation](replyTo =>
        payload
          .into[BlogEntity.UpdateBlogCategory]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def assignBlogTargetPrincipal(payload: AssignBlogTargetPrincipalPayload): Future[Done] =
    refFor(payload.id)
      .ask[BlogEntity.Confirmation](replyTo =>
        payload
          .into[BlogEntity.AssignBlogTargetPrincipal]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def unassignBlogTargetPrincipal(payload: UnassignBlogTargetPrincipalPayload): Future[Done] =
    refFor(payload.id)
      .ask[BlogEntity.Confirmation](replyTo =>
        payload
          .into[BlogEntity.UnassignBlogTargetPrincipal]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def activateBlog(payload: ActivateBlogPayload): Future[Done] =
    refFor(payload.id)
      .ask[BlogEntity.Confirmation](replyTo =>
        payload
          .into[BlogEntity.ActivateBlog]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def deactivateBlog(payload: DeactivateBlogPayload): Future[Done] =
    refFor(payload.id)
      .ask[BlogEntity.Confirmation](replyTo =>
        payload
          .into[BlogEntity.DeactivateBlog]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def deleteBlog(payload: DeleteBlogPayload): Future[Done] =
    refFor(payload.id)
      .ask[BlogEntity.Confirmation](replyTo =>
        payload
          .into[BlogEntity.DeleteBlog]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def getBlog(id: BlogId): Future[Blog] =
    refFor(id)
      .ask[BlogEntity.Confirmation](BlogEntity.GetBlog(id, _))
      .map(convertSuccessBlog(_, id))

  def getBlogAnnotation(id: BlogId): Future[BlogAnnotation] =
    refFor(id)
      .ask[BlogEntity.Confirmation](BlogEntity.GetBlogAnnotation(id, _))
      .map(convertSuccessBlogAnnotation(_, id))

  def getBlogById(id: BlogId, fromReadSide: Boolean): Future[Blog] =
    if (fromReadSide)
      dbDao
        .getBlogById(id)
        .map(_.getOrElse(throw BlogNotFound(id)))
    else
      getBlog(id)

  def getBlogAnnotationById(id: BlogId, fromReadSide: Boolean): Future[BlogAnnotation] =
    if (fromReadSide)
      dbDao
        .getBlogAnnotationById(id)
        .map(_.getOrElse(throw BlogNotFound(id)))
    else
      getBlogAnnotation(id)

  def getBlogsById(ids: Set[BlogId], fromReadSide: Boolean): Future[Map[BlogId, Blog]]                     =
    if (fromReadSide)
      dbDao.getBlogsById(ids)
    else
      Future
        .traverse(ids) { id =>
          refFor(id)
            .ask[BlogEntity.Confirmation](BlogEntity.GetBlog(id, _))
            .map {
              case BlogEntity.SuccessBlog(blog) => Some(blog)
              case _                            => None
            }
        }
        .map(_.flatten.map(a => a.id -> a).toMap)

  def getBlogAnnotationsById(ids: Set[BlogId], fromReadSide: Boolean): Future[Map[BlogId, BlogAnnotation]] =
    if (fromReadSide)
      dbDao.getBlogAnnotationsById(ids)
    else
      Future
        .traverse(ids) { id =>
          refFor(id)
            .ask[BlogEntity.Confirmation](BlogEntity.GetBlogAnnotation(id, _))
            .map {
              case BlogEntity.SuccessBlogAnnotation(blog) => Some(blog)
              case _                                      => None
            }
        }
        .map(_.flatten.map(a => a.id -> a).toMap)

  def findBlogs(query: BlogFindQuery): Future[FindResult]                                                  = indexDao.findBlogs(query)

}
