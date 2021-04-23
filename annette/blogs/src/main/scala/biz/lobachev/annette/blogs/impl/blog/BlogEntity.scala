package biz.lobachev.annette.blogs.impl.blog

import java.time.OffsetDateTime
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl.{EntityContext, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import com.lightbend.lagom.scaladsl.persistence._
import play.api.libs.json._
import org.slf4j.LoggerFactory
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.blogs.api.blog._
import biz.lobachev.annette.blogs.api.category.CategoryId
import biz.lobachev.annette.blogs.impl.blog.model.BlogState

object BlogEntity {

  trait CommandSerializable
  sealed trait Command                                                                                  extends CommandSerializable
  final case class CreateBlog(
    id: BlogId,
    name: String,
    description: String,
    categoryId: CategoryId,
    targets: Set[AnnettePrincipal] = Set.empty,
    createdBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  )                                                                                                     extends Command
  final case class UpdateBlogName(
    id: BlogId,
    name: String,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  )                                                                                                     extends Command
  final case class UpdateBlogDescription(
    id: BlogId,
    description: String,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  )                                                                                                     extends Command
  final case class UpdateBlogCategory(
    id: BlogId,
    categoryId: CategoryId,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  )                                                                                                     extends Command
  final case class AssignBlogTargetPrincipal(
    id: BlogId,
    principal: AnnettePrincipal,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  )                                                                                                     extends Command
  final case class UnassignBlogTargetPrincipal(
    id: BlogId,
    principal: AnnettePrincipal,
    updatedBy: AnnettePrincipal,
    replyTo: ActorRef[Confirmation]
  )                                                                                                     extends Command
  final case class ActivateBlog(id: BlogId, updatedBy: AnnettePrincipal, replyTo: ActorRef[Confirmation])
      extends Command
  final case class DeactivateBlog(id: BlogId, updatedBy: AnnettePrincipal, replyTo: ActorRef[Confirmation])
      extends Command
  final case class DeleteBlog(id: BlogId, deletedBy: AnnettePrincipal, replyTo: ActorRef[Confirmation]) extends Command
  final case class GetBlog(id: BlogId, replyTo: ActorRef[Confirmation])                                 extends Command
  final case class GetBlogAnnotation(id: BlogId, replyTo: ActorRef[Confirmation])                       extends Command

  sealed trait Confirmation
  final case object Success                                              extends Confirmation
  final case class SuccessBlog(blog: Blog)                               extends Confirmation
  final case class SuccessBlogAnnotation(blogAnnotation: BlogAnnotation) extends Confirmation
  final case object BlogAlreadyExist                                     extends Confirmation
  final case object BlogNotFound                                         extends Confirmation

  implicit val confirmationSuccessFormat: Format[Success.type]                        = Json.format
  implicit val confirmationSuccessBlogFormat: Format[SuccessBlog]                     = Json.format
  implicit val confirmationSuccessBlogAnnotationFormat: Format[SuccessBlogAnnotation] = Json.format
  implicit val confirmationBlogAlreadyExistFormat: Format[BlogAlreadyExist.type]      = Json.format
  implicit val confirmationBlogNotFoundFormat: Format[BlogNotFound.type]              = Json.format

  sealed trait Event extends AggregateEvent[Event] {
    override def aggregateTag: AggregateEventTagger[Event] = Event.Tag
  }

  object Event {
    val Tag: AggregateEventShards[Event] = AggregateEventTag.sharded[Event](numShards = 10)
  }

  final case class BlogCreated(
    id: BlogId,
    name: String,
    description: String,
    categoryId: CategoryId,
    targets: Set[AnnettePrincipal] = Set.empty,
    createdBy: AnnettePrincipal,
    createdAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class BlogNameUpdated(
    id: BlogId,
    name: String,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class BlogDescriptionUpdated(
    id: BlogId,
    description: String,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class BlogCategoryUpdated(
    id: BlogId,
    categoryId: CategoryId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class BlogTargetPrincipalAssigned(
    id: BlogId,
    principal: AnnettePrincipal,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class BlogTargetPrincipalUnassigned(
    id: BlogId,
    principal: AnnettePrincipal,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class BlogActivated(
    id: BlogId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class BlogDeactivated(
    id: BlogId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class BlogDeleted(id: BlogId, deleteBy: AnnettePrincipal, deleteAt: OffsetDateTime = OffsetDateTime.now)
      extends Event

  implicit val eventBlogCreatedFormat: Format[BlogCreated]                                     = Json.format
  implicit val eventBlogNameUpdatedFormat: Format[BlogNameUpdated]                             = Json.format
  implicit val eventBlogDescriptionUpdatedFormat: Format[BlogDescriptionUpdated]               = Json.format
  implicit val eventBlogCategoryUpdatedFormat: Format[BlogCategoryUpdated]                     = Json.format
  implicit val eventBlogTargetPrincipalAssignedFormat: Format[BlogTargetPrincipalAssigned]     = Json.format
  implicit val eventBlogTargetPrincipalUnassignedFormat: Format[BlogTargetPrincipalUnassigned] = Json.format
  implicit val eventBlogActivatedFormat: Format[BlogActivated]                                 = Json.format
  implicit val eventBlogDeactivatedFormat: Format[BlogDeactivated]                             = Json.format
  implicit val eventBlogDeletedFormat: Format[BlogDeleted]                                     = Json.format

  val empty = BlogEntity()

  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Blogs_Blog")

  def apply(persistenceId: PersistenceId): EventSourcedBehavior[Command, Event, BlogEntity] =
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, BlogEntity](
        persistenceId = persistenceId,
        emptyState = BlogEntity.empty,
        commandHandler = (entity, cmd) => entity.applyCommand(cmd),
        eventHandler = (entity, evt) => entity.applyEvent(evt)
      )

  def apply(entityContext: EntityContext[Command]): Behavior[Command] =
    apply(PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId))
      .withTagger(AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2))

  implicit val entityFormat: Format[BlogEntity] = Json.format

}

final case class BlogEntity(maybeState: Option[BlogState] = None) {
  import BlogEntity._

  val log = LoggerFactory.getLogger(this.getClass)

  def applyCommand(cmd: Command): ReplyEffect[Event, BlogEntity] =
    cmd match {
      case cmd: CreateBlog                  => createBlog(cmd)
      case cmd: UpdateBlogName              => updateBlogName(cmd)
      case cmd: UpdateBlogDescription       => updateBlogDescription(cmd)
      case cmd: UpdateBlogCategory          => updateBlogCategory(cmd)
      case cmd: AssignBlogTargetPrincipal   => assignBlogTargetPrincipal(cmd)
      case cmd: UnassignBlogTargetPrincipal => unassignBlogTargetPrincipal(cmd)
      case cmd: ActivateBlog                => activateBlog(cmd)
      case cmd: DeactivateBlog              => deactivateBlog(cmd)
      case cmd: DeleteBlog                  => deleteBlog(cmd)
      case cmd: GetBlog                     => getBlog(cmd)
      case cmd: GetBlogAnnotation           => getBlogAnnotation(cmd)
    }

  def createBlog(cmd: CreateBlog): ReplyEffect[Event, BlogEntity] = ???

  def updateBlogName(cmd: UpdateBlogName): ReplyEffect[Event, BlogEntity] = ???

  def updateBlogDescription(cmd: UpdateBlogDescription): ReplyEffect[Event, BlogEntity] = ???

  def updateBlogCategory(cmd: UpdateBlogCategory): ReplyEffect[Event, BlogEntity] = ???

  def assignBlogTargetPrincipal(cmd: AssignBlogTargetPrincipal): ReplyEffect[Event, BlogEntity] = ???

  def unassignBlogTargetPrincipal(cmd: UnassignBlogTargetPrincipal): ReplyEffect[Event, BlogEntity] = ???

  def activateBlog(cmd: ActivateBlog): ReplyEffect[Event, BlogEntity] = ???

  def deactivateBlog(cmd: DeactivateBlog): ReplyEffect[Event, BlogEntity] = ???

  def deleteBlog(cmd: DeleteBlog): ReplyEffect[Event, BlogEntity] = ???

  def getBlog(cmd: GetBlog): ReplyEffect[Event, BlogEntity] = ???

  def getBlogAnnotation(cmd: GetBlogAnnotation): ReplyEffect[Event, BlogEntity] = ???

  def applyEvent(event: Event): BlogEntity =
    event match {
      case event: BlogCreated                   => onBlogCreated(event)
      case event: BlogNameUpdated               => onBlogNameUpdated(event)
      case event: BlogDescriptionUpdated        => onBlogDescriptionUpdated(event)
      case event: BlogCategoryUpdated           => onBlogCategoryUpdated(event)
      case event: BlogTargetPrincipalAssigned   => onBlogTargetPrincipalAssigned(event)
      case event: BlogTargetPrincipalUnassigned => onBlogTargetPrincipalUnassigned(event)
      case event: BlogActivated                 => onBlogActivated(event)
      case event: BlogDeactivated               => onBlogDeactivated(event)
      case event: BlogDeleted                   => onBlogDeleted(event)
    }

  def onBlogCreated(event: BlogCreated): BlogEntity = ???

  def onBlogNameUpdated(event: BlogNameUpdated): BlogEntity = ???

  def onBlogDescriptionUpdated(event: BlogDescriptionUpdated): BlogEntity = ???

  def onBlogCategoryUpdated(event: BlogCategoryUpdated): BlogEntity = ???

  def onBlogTargetPrincipalAssigned(event: BlogTargetPrincipalAssigned): BlogEntity = ???

  def onBlogTargetPrincipalUnassigned(event: BlogTargetPrincipalUnassigned): BlogEntity = ???

  def onBlogActivated(event: BlogActivated): BlogEntity = ???

  def onBlogDeactivated(event: BlogDeactivated): BlogEntity = ???

  def onBlogDeleted(event: BlogDeleted): BlogEntity = ???

}
