package biz.lobachev.annette.blogs.impl.post_metric

import akka.Done
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.util.Timeout
import biz.lobachev.annette.blogs.api.post.{PostId, PostNotFound}
import org.slf4j.LoggerFactory
import io.scalaland.chimney.dsl._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import biz.lobachev.annette.blogs.api.post_metric._
import biz.lobachev.annette.blogs.impl.post_metric.dao.PostMetricCassandraDbDao
import biz.lobachev.annette.core.model.auth.AnnettePrincipal

class PostMetricEntityService(
  clusterSharding: ClusterSharding,
  casRepository: PostMetricCassandraDbDao
)(implicit
  ec: ExecutionContext
) {
  val log = LoggerFactory.getLogger(this.getClass)

  implicit val timeout = Timeout(50.seconds)

  def refFor(id: PostId): EntityRef[PostMetricEntity.Command] =
    clusterSharding.entityRefFor(PostMetricEntity.typeKey, id)

  private def convertSuccess(confirmation: PostMetricEntity.Confirmation): Done =
    confirmation match {
      case PostMetricEntity.Success => Done
      case _                        => throw new RuntimeException("Match fail")
    }

  private def convertSuccessPostMetric(confirmation: PostMetricEntity.Confirmation): PostMetric =
    confirmation match {
      case PostMetricEntity.SuccessPostMetric(postMetric) => postMetric
      case _                                              => throw new RuntimeException("Match fail")
    }

  def viewPost(payload: ViewPostPayload): Future[Done] =
    refFor(payload.id)
      .ask[PostMetricEntity.Confirmation](replyTo =>
        payload
          .into[PostMetricEntity.ViewPost]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess _)

  def likePost(payload: LikePostPayload): Future[Done] =
    refFor(payload.id)
      .ask[PostMetricEntity.Confirmation](replyTo =>
        payload
          .into[PostMetricEntity.LikePost]
          .withFieldConst(_.replyTo, replyTo)
          .transform
      )
      .map(convertSuccess _)

  def deletePostMetric(id: PostId, deletedBy: AnnettePrincipal): Future[Done] =
    refFor(id)
      .ask[PostMetricEntity.Confirmation](PostMetricEntity.DeletePostMetric(id, deletedBy, _))
      .map(convertSuccess)

  def getPostMetric(id: PostId): Future[PostMetric] =
    refFor(id)
      .ask[PostMetricEntity.Confirmation](PostMetricEntity.GetPostMetric(id, _))
      .map(convertSuccessPostMetric)

  def getPostMetricById(id: PostId, fromReadSide: Boolean): Future[PostMetric] =
    if (fromReadSide)
      casRepository
        .getPostMetricById(id)
        .map(_.getOrElse(throw PostNotFound(id)))
    else
      getPostMetric(id)

  def getPostMetricsById(ids: Set[PostId], fromReadSide: Boolean): Future[Map[PostId, PostMetric]] =
    if (fromReadSide)
      casRepository.getPostMetricsById(ids)
    else
      Future
        .traverse(ids) { id =>
          refFor(id)
            .ask[PostMetricEntity.Confirmation](PostMetricEntity.GetPostMetric(id, _))
            .map {
              case PostMetricEntity.SuccessPostMetric(postMetric) => Some(postMetric)
              case _                                              => None
            }
        }
        .map(_.flatten.map(a => a.id -> a).toMap)

}
