package biz.lobachev.annette.service_catalog.impl.group

import java.time.OffsetDateTime
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl.{EntityContext, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import com.lightbend.lagom.scaladsl.persistence._
import play.api.libs.json._
import org.slf4j.LoggerFactory
import io.scalaland.chimney.dsl._

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.service_catalog.api.group._

object GroupEntity  {

  trait CommandSerializable
  sealed trait Command extends CommandSerializable
  final case class CreateGroup(id: GroupId, name: String, description: String, icon: String, caption: Caption, captionDescription: Caption, services: Seq[ServiceId] = Seq.empty, createdBy: AnnettePrincipal, replyTo: ActorRef[Confirmation]) extends Command {
    def apply(payload: CreateGroupPayload, replyTo: ActorRef[Confirmation]): CreateGroup =
      payload
      .into[CreateGroup]
      .withFieldConst(_.replyTo, replyTo)
      .transform
  }
  final case class UpdateGroup(id: GroupId, name: Option[String], description: Option[String], icon: Option[String], caption: Option[Caption], captionDescription: Option[Caption], services: Seq[ServiceId] = Seq.empty, updatedBy: AnnettePrincipal, replyTo: ActorRef[Confirmation]) extends Command {
    def apply(payload: UpdateGroupPayload, replyTo: ActorRef[Confirmation]): UpdateGroup =
      payload
      .into[UpdateGroup]
      .withFieldConst(_.replyTo, replyTo)
      .transform
  }
  final case class ActivateGroup(id: GroupId, updatedBy: AnnettePrincipal, replyTo: ActorRef[Confirmation]) extends Command {
    def apply(payload: ActivateGroupPayload, replyTo: ActorRef[Confirmation]): ActivateGroup =
      payload
      .into[ActivateGroup]
      .withFieldConst(_.replyTo, replyTo)
      .transform
  }
  final case class DeactivateGroup(id: GroupId, updatedBy: AnnettePrincipal, replyTo: ActorRef[Confirmation]) extends Command {
    def apply(payload: DeactivateGroupPayload, replyTo: ActorRef[Confirmation]): DeactivateGroup =
      payload
      .into[DeactivateGroup]
      .withFieldConst(_.replyTo, replyTo)
      .transform
  }
  final case class DeleteGroup(id: GroupId, deletedBy: AnnettePrincipal, replyTo: ActorRef[Confirmation]) extends Command {
    def apply(payload: DeleteGroupPayload, replyTo: ActorRef[Confirmation]): DeleteGroup =
      payload
      .into[DeleteGroup]
      .withFieldConst(_.replyTo, replyTo)
      .transform
  }
  final case class GetGroup(id: GroupId, replyTo: ActorRef[Confirmation]) extends Command {
    def apply(payload: GetGroupPayload, replyTo: ActorRef[Confirmation]): GetGroup =
      payload
      .into[GetGroup]
      .withFieldConst(_.replyTo, replyTo)
      .transform
  }

  sealed trait Confirmation
  final case object Success extends Confirmation
  final case class SuccessGroup(group: Group) extends Confirmation
  final case object GroupAlreadyExist extends Confirmation
  final case object GroupNotFound extends Confirmation

  implicit val confirmationSuccessFormat: Format[Success.type] = Json.format
  implicit val confirmationSuccessGroupFormat: Format[SuccessGroup] = Json.format
  implicit val confirmationGroupAlreadyExistFormat: Format[GroupAlreadyExist.type] = Json.format
  implicit val confirmationGroupNotFoundFormat: Format[GroupNotFound.type] = Json.format

  sealed trait Event extends AggregateEvent[Event] {
    override def aggregateTag: AggregateEventTagger[Event] = Event.Tag
  }

  object Event {
    val Tag: AggregateEventShards[Event] = AggregateEventTag.sharded[Event](numShards = 10)
  }

  final case class GroupCreated(id: GroupId, name: String, description: String, categoryId: CategoryId, targets: Set[AnnettePrincipal] = Set.empty, createdBy: AnnettePrincipal, createdAt: OffsetDateTime = OffsetDateTime.now) extends Event
  final case class GroupUpdated(name: Option[String], description: Option[String], categoryId: Option[CategoryId], groups: Seq[GroupId] = Seq.empty, updatedBy: AnnettePrincipal, updatedAt: OffsetDateTime = OffsetDateTime.now) extends Event
  final case class GroupActivated(id: GroupId, updatedBy: AnnettePrincipal, updatedAt: OffsetDateTime = OffsetDateTime.now) extends Event
  final case class GroupDeactivated(id: GroupId, updatedBy: AnnettePrincipal, updatedAt: OffsetDateTime = OffsetDateTime.now) extends Event
  final case class GroupDeleted(id: GroupId, deleteBy: AnnettePrincipal, deleteAt: OffsetDateTime = OffsetDateTime.now) extends Event

  implicit val eventGroupCreatedFormat: Format[GroupCreated] = Json.format
  implicit val eventGroupUpdatedFormat: Format[GroupUpdated] = Json.format
  implicit val eventGroupActivatedFormat: Format[GroupActivated] = Json.format
  implicit val eventGroupDeactivatedFormat: Format[GroupDeactivated] = Json.format
  implicit val eventGroupDeletedFormat: Format[GroupDeleted] = Json.format

  val empty = GroupEntity()

  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("ServiceCatalog_Group")

  def apply(persistenceId: PersistenceId): EventSourcedBehavior[Command, Event, GroupEntity] = {
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, GroupEntity](
          persistenceId = persistenceId,
          emptyState = GroupEntity.empty,
          commandHandler = (entity, cmd) => entity.applyCommand(cmd),
          eventHandler = (entity, evt) => entity.applyEvent(evt)
      )
  }

  def apply(entityContext: EntityContext[Command]): Behavior[Command] =
    apply(PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId))
      .withTagger(AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2))

  implicit val entityFormat: Format[GroupEntity] = Json.format

}

final case class  GroupEntity(maybeState: Option[GroupState] = None ) {
  import GroupEntity._

  val log = LoggerFactory.getLogger(this.getClass)

  def applyCommand(cmd: Command): ReplyEffect[Event, GroupEntity] = {
    cmd match {
      case cmd: CreateGroup => createGroup(cmd)
      case cmd: UpdateGroup => updateGroup(cmd)
      case cmd: ActivateGroup => activateGroup(cmd)
      case cmd: DeactivateGroup => deactivateGroup(cmd)
      case cmd: DeleteGroup => deleteGroup(cmd)
      case cmd: GetGroup => getGroup(cmd)
    }
  }

  def createGroup(cmd: CreateGroup): ReplyEffect[Event, GroupEntity] = ???

  def updateGroup(cmd: UpdateGroup): ReplyEffect[Event, GroupEntity] = ???

  def activateGroup(cmd: ActivateGroup): ReplyEffect[Event, GroupEntity] = ???

  def deactivateGroup(cmd: DeactivateGroup): ReplyEffect[Event, GroupEntity] = ???

  def deleteGroup(cmd: DeleteGroup): ReplyEffect[Event, GroupEntity] = ???

  def getGroup(cmd: GetGroup): ReplyEffect[Event, GroupEntity] = ???

  def applyEvent(event: Event): GroupEntity = {
    event match {
      case event: GroupCreated => onGroupCreated(event)
      case event: GroupUpdated => onGroupUpdated(event)
      case event: GroupActivated => onGroupActivated(event)
      case event: GroupDeactivated => onGroupDeactivated(event)
      case event: GroupDeleted => onGroupDeleted(event)
    }
  }

  def onGroupCreated(event: GroupCreated): GroupEntity = ???

  def onGroupUpdated(event: GroupUpdated): GroupEntity = ???

  def onGroupActivated(event: GroupActivated): GroupEntity = ???

  def onGroupDeactivated(event: GroupDeactivated): GroupEntity = ???

  def onGroupDeleted(event: GroupDeleted): GroupEntity = ???

}
