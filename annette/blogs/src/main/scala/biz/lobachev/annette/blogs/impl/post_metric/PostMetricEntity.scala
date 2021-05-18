package biz.lobachev.annette.blogs.impl.post_metric

import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl.{EntityContext, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import biz.lobachev.annette.blogs.api.post.PostId
import biz.lobachev.annette.blogs.impl.post_metric.model.PostMetricState
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import com.lightbend.lagom.scaladsl.persistence._
import org.slf4j.LoggerFactory
import play.api.libs.json._

import java.time.OffsetDateTime

object PostMetricEntity {

  trait CommandSerializable
  sealed trait Command                                                                                extends CommandSerializable
  final case class ViewPost(id: PostId, updatedBy: AnnettePrincipal, replyTo: ActorRef[Confirmation]) extends Command
  final case class LikePost(id: PostId, updatedBy: AnnettePrincipal, replyTo: ActorRef[Confirmation]) extends Command
  final case class DeletePostMetric(id: PostId, deletedBy: AnnettePrincipal, replyTo: ActorRef[Confirmation])
      extends Command

  sealed trait Confirmation
  final case object Success extends Confirmation

  implicit val confirmationSuccessFormat: Format[Success.type] = Json.format

  sealed trait Event extends AggregateEvent[Event] {
    override def aggregateTag: AggregateEventTagger[Event] = Event.Tag
  }

  object Event {
    val Tag: AggregateEventShards[Event] = AggregateEventTag.sharded[Event](numShards = 10)
  }

  final case class PostViewed(
    id: PostId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class PostLiked(
    id: PostId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class PostMetricDeleted(
    id: PostId,
    deleteBy: AnnettePrincipal,
    deleteAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  implicit val eventPostViewedFormat: Format[PostViewed]               = Json.format
  implicit val eventPostLikedFormat: Format[PostLiked]                 = Json.format
  implicit val eventPostMetricDeletedFormat: Format[PostMetricDeleted] = Json.format

  val empty = PostMetricEntity()

  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Blogs_PostMetric")

  def apply(persistenceId: PersistenceId): EventSourcedBehavior[Command, Event, PostMetricEntity] =
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, PostMetricEntity](
        persistenceId = persistenceId,
        emptyState = PostMetricEntity.empty,
        commandHandler = (entity, cmd) => entity.applyCommand(cmd),
        eventHandler = (entity, evt) => entity.applyEvent(evt)
      )

  def apply(entityContext: EntityContext[Command]): Behavior[Command] =
    apply(PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId))
      .withTagger(AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2))

  implicit val entityFormat: Format[PostMetricEntity] = Json.format

}

final case class PostMetricEntity(maybeState: Option[PostMetricState] = None) {
  import PostMetricEntity._

  val log = LoggerFactory.getLogger(this.getClass)

  def applyCommand(cmd: Command): ReplyEffect[Event, PostMetricEntity] =
    cmd match {
      case cmd: ViewPost         => viewPost(cmd)
      case cmd: LikePost         => likePost(cmd)
      case cmd: DeletePostMetric => deletePostMetric(cmd)
    }

  def viewPost(cmd: ViewPost): ReplyEffect[Event, PostMetricEntity] = ???

  def likePost(cmd: LikePost): ReplyEffect[Event, PostMetricEntity] = ???

  def deletePostMetric(cmd: DeletePostMetric): ReplyEffect[Event, PostMetricEntity] = ???

  def applyEvent(event: Event): PostMetricEntity =
    event match {
      case event: PostViewed        => onPostViewed(event)
      case event: PostLiked         => onPostLiked(event)
      case event: PostMetricDeleted => onPostMetricDeleted(event)
    }

  def onPostViewed(event: PostViewed): PostMetricEntity = ???

  def onPostLiked(event: PostLiked): PostMetricEntity = ???

  def onPostMetricDeleted(event: PostMetricDeleted): PostMetricEntity = ???

}
