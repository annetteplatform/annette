package biz.lobachev.annette.cms.impl.post

import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.util.Timeout
import org.slf4j.LoggerFactory
import io.scalaland.chimney.dsl._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import biz.lobachev.annette.cms.api.post._
import biz.lobachev.annette.cms.impl.post.dao.{PostCassandraDbDao, PostElasticIndexDao}
import biz.lobachev.annette.core.model.elastic.FindResult

class PostEntityService(
  clusterSharding: ClusterSharding,
  dbDao: PostCassandraDbDao,
  indexDao: PostElasticIndexDao
)(implicit
  ec: ExecutionContext
) {

  val log = LoggerFactory.getLogger(this.getClass)

  implicit val timeout = Timeout(50.seconds)

  private def refFor(id: PostId): EntityRef[PostEntity.Command] =
    clusterSharding.entityRefFor(PostEntity.typeKey, id)

  private def convertSuccess(confirmation: PostEntity.Confirmation, id: PostId, maybeId: Option[String] = None): Done =
    confirmation match {
      case PostEntity.Success                            => Done
      case PostEntity.PostAlreadyExist                   => throw PostAlreadyExist(id)
      case PostEntity.PostNotFound                       => throw PostNotFound(id)
      case PostEntity.PostPublicationDateClearNotAllowed => throw PostPublicationDateClearNotAllowed(id)
      case PostEntity.PostMediaAlreadyExist              => throw PostMediaAlreadyExist(id, maybeId.getOrElse(""))
      case PostEntity.PostMediaNotFound                  => throw PostMediaNotFound(id, maybeId.getOrElse(""))
      case PostEntity.PostDocAlreadyExist                => throw PostDocAlreadyExist(id, maybeId.getOrElse(""))
      case PostEntity.PostDocNotFound                    => throw PostDocNotFound(id, maybeId.getOrElse(""))
      case _                                             => throw new RuntimeException("Match fail")
    }

  private def convertSuccessPost(confirmation: PostEntity.Confirmation, id: PostId): Post =
    confirmation match {
      case PostEntity.SuccessPost(post)     => post
      case PostEntity.PostAlreadyExist      => throw PostAlreadyExist(id)
      case PostEntity.PostNotFound          => throw PostNotFound(id)
      case PostEntity.PostMediaAlreadyExist => throw PostMediaAlreadyExist(id, "")
      case PostEntity.PostMediaNotFound     => throw PostMediaNotFound(id, "")
      case PostEntity.PostDocAlreadyExist   => throw PostDocAlreadyExist(id, "")
      case PostEntity.PostDocNotFound       => throw PostDocNotFound(id, "")
      case _                                => throw new RuntimeException("Match fail")
    }

  private def convertSuccessPostAnnotation(confirmation: PostEntity.Confirmation, id: PostId): PostAnnotation =
    confirmation match {
      case PostEntity.SuccessPostAnnotation(postAnnotation) => postAnnotation
      case PostEntity.PostAlreadyExist                      => throw PostAlreadyExist(id)
      case PostEntity.PostNotFound                          => throw PostNotFound(id)
      case PostEntity.PostMediaAlreadyExist                 => throw PostMediaAlreadyExist(id, "")
      case PostEntity.PostMediaNotFound                     => throw PostMediaNotFound(id, "")
      case PostEntity.PostDocAlreadyExist                   => throw PostDocAlreadyExist(id, "")
      case PostEntity.PostDocNotFound                       => throw PostDocNotFound(id, "")
      case _                                                => throw new RuntimeException("Match fail")
    }

  def createPost(payload: CreatePostPayload): Future[Done] =
    refFor(payload.id)
      .ask[PostEntity.Confirmation](replyTo =>
        payload
          .into[PostEntity.CreatePost]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def updatePostFeatured(payload: UpdatePostFeaturedPayload): Future[Done] =
    refFor(payload.id)
      .ask[PostEntity.Confirmation](replyTo =>
        payload
          .into[PostEntity.UpdatePostFeatured]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def updatePostAuthor(payload: UpdatePostAuthorPayload): Future[Done] =
    refFor(payload.id)
      .ask[PostEntity.Confirmation](replyTo =>
        payload
          .into[PostEntity.UpdatePostAuthor]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def updatePostTitle(payload: UpdatePostTitlePayload): Future[Done] =
    refFor(payload.id)
      .ask[PostEntity.Confirmation](replyTo =>
        payload
          .into[PostEntity.UpdatePostTitle]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def updatePostIntro(payload: UpdatePostIntroPayload): Future[Done] =
    refFor(payload.id)
      .ask[PostEntity.Confirmation](replyTo =>
        payload
          .into[PostEntity.UpdatePostIntro]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def updatePostContent(payload: UpdatePostContentPayload): Future[Done] =
    refFor(payload.id)
      .ask[PostEntity.Confirmation](replyTo =>
        payload
          .into[PostEntity.UpdatePostContent]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def updatePostPublicationTimestamp(payload: UpdatePostPublicationTimestampPayload): Future[Done] =
    refFor(payload.id)
      .ask[PostEntity.Confirmation](replyTo =>
        payload
          .into[PostEntity.UpdatePostPublicationTimestamp]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def publishPost(payload: PublishPostPayload): Future[Done] =
    refFor(payload.id)
      .ask[PostEntity.Confirmation](replyTo =>
        payload
          .into[PostEntity.PublishPost]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def unpublishPost(payload: UnpublishPostPayload): Future[Done] =
    refFor(payload.id)
      .ask[PostEntity.Confirmation](replyTo =>
        payload
          .into[PostEntity.UnpublishPost]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def assignPostTargetPrincipal(payload: AssignPostTargetPrincipalPayload): Future[Done] =
    refFor(payload.id)
      .ask[PostEntity.Confirmation](replyTo =>
        payload
          .into[PostEntity.AssignPostTargetPrincipal]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def unassignPostTargetPrincipal(payload: UnassignPostTargetPrincipalPayload): Future[Done] =
    refFor(payload.id)
      .ask[PostEntity.Confirmation](replyTo =>
        payload
          .into[PostEntity.UnassignPostTargetPrincipal]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def deletePost(payload: DeletePostPayload): Future[Done] =
    refFor(payload.id)
      .ask[PostEntity.Confirmation](replyTo =>
        payload
          .into[PostEntity.DeletePost]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.id))

  def getPost(id: PostId): Future[Post] =
    refFor(id)
      .ask[PostEntity.Confirmation](PostEntity.GetPost(id, _))
      .map(convertSuccessPost(_, id))

  def getPostAnnotation(id: PostId): Future[PostAnnotation] =
    refFor(id)
      .ask[PostEntity.Confirmation](PostEntity.GetPostAnnotation(id, _))
      .map(convertSuccessPostAnnotation(_, id))

  def addPostMedia(payload: AddPostMediaPayload): Future[Done] =
    refFor(payload.postId)
      .ask[PostEntity.Confirmation](replyTo =>
        payload
          .into[PostEntity.AddPostMedia]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.postId, Some(payload.mediaId)))

  def removePostMedia(payload: RemovePostMediaPayload): Future[Done] =
    refFor(payload.postId)
      .ask[PostEntity.Confirmation](replyTo =>
        payload
          .into[PostEntity.RemovePostMedia]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.postId, Some(payload.mediaId)))

  def addPostDoc(payload: AddPostDocPayload): Future[Done] =
    refFor(payload.postId)
      .ask[PostEntity.Confirmation](replyTo =>
        payload
          .into[PostEntity.AddPostDoc]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.postId, Some(payload.docId)))

  def updatePostDocName(payload: UpdatePostDocNamePayload): Future[Done] =
    refFor(payload.postId)
      .ask[PostEntity.Confirmation](replyTo =>
        payload
          .into[PostEntity.UpdatePostDocName]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.postId, Some(payload.docId)))

  def removePostDoc(payload: RemovePostDocPayload): Future[Done] =
    refFor(payload.postId)
      .ask[PostEntity.Confirmation](replyTo =>
        payload
          .into[PostEntity.RemovePostDoc]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess(_, payload.postId, Some(payload.docId)))

  def getPostById(id: PostId, fromReadSide: Boolean): Future[Post] =
    if (fromReadSide)
      dbDao
        .getPostById(id)
        .map(_.getOrElse(throw PostNotFound(id)))
    else
      getPost(id)

  def getPostAnnotationById(
    id: PostId,
    fromReadSide: Boolean
  ): Future[PostAnnotation] =
    if (fromReadSide)
      dbDao
        .getPostAnnotationById(id)
        .map(_.getOrElse(throw PostNotFound(id)))
    else
      getPostAnnotation(id)

  def getPostsById(
    ids: Set[PostId],
    fromReadSide: Boolean
  ): Future[Map[PostId, Post]]                            =
    if (fromReadSide)
      dbDao.getPostsById(ids)
    else
      Future
        .traverse(ids) { id =>
          refFor(id)
            .ask[PostEntity.Confirmation](PostEntity.GetPost(id, _))
            .map {
              case PostEntity.SuccessPost(post) => Some(post)
              case _                            => None
            }
        }
        .map(_.flatten.map(a => a.id -> a).toMap)

  def getPostAnnotationsById(
    ids: Set[PostId],
    fromReadSide: Boolean
  ): Future[Map[PostId, PostAnnotation]]                  =
    if (fromReadSide)
      dbDao.getPostAnnotationsById(ids)
    else
      Future
        .traverse(ids) { id =>
          refFor(id)
            .ask[PostEntity.Confirmation](PostEntity.GetPostAnnotation(id, _))
            .map {
              case PostEntity.SuccessPostAnnotation(post) => Some(post)
              case _                                      => None
            }
        }
        .map(_.flatten.map(a => a.id -> a).toMap)

  def findPosts(query: PostFindQuery): Future[FindResult] = indexDao.findPosts(query)

  def viewPost(payload: ViewPostPayload): Future[Done] = dbDao.viewPost(payload.id, payload.updatedBy)

  def likePost(payload: LikePostPayload): Future[Done] = dbDao.likePost(payload.id, payload.updatedBy)

  def getPostMetricById(id: PostId): Future[PostMetric] = dbDao.getPostMetricById(id)

  def getPostMetricsById(ids: Set[PostId]): Future[Map[PostId, PostMetric]] = dbDao.getPostMetricsById(ids)

}
