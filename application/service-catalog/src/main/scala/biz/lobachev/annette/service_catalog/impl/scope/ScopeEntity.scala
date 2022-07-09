package biz.lobachev.annette.service_catalog.impl.scope

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
import biz.lobachev.annette.service_catalog.api.scope._

object ScopeEntity  {

  trait CommandSerializable
  sealed trait Command extends CommandSerializable
  final case class CreateScope(id: ScopeId, name: String, description: String, categoryId: CategoryId, groups: Seq[GroupId] = Seq.empty, createdBy: AnnettePrincipal, replyTo: ActorRef[Confirmation]) extends Command {
    def apply(payload: CreateScopePayload, replyTo: ActorRef[Confirmation]): CreateScope =
      payload
      .into[CreateScope]
      .withFieldConst(_.replyTo, replyTo)
      .transform
  }
  final case class UpdateScope(name: Option[String], description: Option[String], categoryId: Option[CategoryId], groups: Seq[GroupId] = Seq.empty, updatedBy: AnnettePrincipal, replyTo: ActorRef[Confirmation]) extends Command {
    def apply(payload: UpdateScopePayload, replyTo: ActorRef[Confirmation]): UpdateScope =
      payload
      .into[UpdateScope]
      .withFieldConst(_.replyTo, replyTo)
      .transform
  }
  final case class ActivateScope(id: ScopeId, updatedBy: AnnettePrincipal, replyTo: ActorRef[Confirmation]) extends Command {
    def apply(payload: ActivateScopePayload, replyTo: ActorRef[Confirmation]): ActivateScope =
      payload
      .into[ActivateScope]
      .withFieldConst(_.replyTo, replyTo)
      .transform
  }
  final case class DeactivateScope(id: ScopeId, updatedBy: AnnettePrincipal, replyTo: ActorRef[Confirmation]) extends Command {
    def apply(payload: DeactivateScopePayload, replyTo: ActorRef[Confirmation]): DeactivateScope =
      payload
      .into[DeactivateScope]
      .withFieldConst(_.replyTo, replyTo)
      .transform
  }
  final case class DeleteScope(id: ScopeId, deletedBy: AnnettePrincipal, replyTo: ActorRef[Confirmation]) extends Command {
    def apply(payload: DeleteScopePayload, replyTo: ActorRef[Confirmation]): DeleteScope =
      payload
      .into[DeleteScope]
      .withFieldConst(_.replyTo, replyTo)
      .transform
  }
  final case class GetScope(id: ScopeId, replyTo: ActorRef[Confirmation]) extends Command {
    def apply(payload: GetScopePayload, replyTo: ActorRef[Confirmation]): GetScope =
      payload
      .into[GetScope]
      .withFieldConst(_.replyTo, replyTo)
      .transform
  }

  sealed trait Confirmation
  final case object Success extends Confirmation
  final case class SuccessScope(scope: Scope) extends Confirmation
  final case object ScopeAlreadyExist extends Confirmation
  final case object ScopeNotFound extends Confirmation

  implicit val confirmationSuccessFormat: Format[Success.type] = Json.format
  implicit val confirmationSuccessScopeFormat: Format[SuccessScope] = Json.format
  implicit val confirmationScopeAlreadyExistFormat: Format[ScopeAlreadyExist.type] = Json.format
  implicit val confirmationScopeNotFoundFormat: Format[ScopeNotFound.type] = Json.format

  sealed trait Event extends AggregateEvent[Event] {
    override def aggregateTag: AggregateEventTagger[Event] = Event.Tag
  }

  object Event {
    val Tag: AggregateEventShards[Event] = AggregateEventTag.sharded[Event](numShards = 10)
  }

  final case class ScopeCreated(id: ScopeId, name: String, description: String, categoryId: CategoryId, targets: Set[AnnettePrincipal] = Set.empty, createdBy: AnnettePrincipal, createdAt: OffsetDateTime = OffsetDateTime.now) extends Event
  final case class ScopeUpdated(name: Option[String], description: Option[String], categoryId: Option[CategoryId], groups: Seq[GroupId] = Seq.empty, updatedBy: AnnettePrincipal, updatedAt: OffsetDateTime = OffsetDateTime.now) extends Event
  final case class ScopeActivated(id: ScopeId, updatedBy: AnnettePrincipal, updatedAt: OffsetDateTime = OffsetDateTime.now) extends Event
  final case class ScopeDeactivated(id: ScopeId, updatedBy: AnnettePrincipal, updatedAt: OffsetDateTime = OffsetDateTime.now) extends Event
  final case class ScopeDeleted(id: ScopeId, deleteBy: AnnettePrincipal, deleteAt: OffsetDateTime = OffsetDateTime.now) extends Event

  implicit val eventScopeCreatedFormat: Format[ScopeCreated] = Json.format
  implicit val eventScopeUpdatedFormat: Format[ScopeUpdated] = Json.format
  implicit val eventScopeActivatedFormat: Format[ScopeActivated] = Json.format
  implicit val eventScopeDeactivatedFormat: Format[ScopeDeactivated] = Json.format
  implicit val eventScopeDeletedFormat: Format[ScopeDeleted] = Json.format

  val empty = ScopeEntity()

  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("ServiceCatalog_Scope")

  def apply(persistenceId: PersistenceId): EventSourcedBehavior[Command, Event, ScopeEntity] = {
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, ScopeEntity](
          persistenceId = persistenceId,
          emptyState = ScopeEntity.empty,
          commandHandler = (entity, cmd) => entity.applyCommand(cmd),
          eventHandler = (entity, evt) => entity.applyEvent(evt)
      )
  }

  def apply(entityContext: EntityContext[Command]): Behavior[Command] =
    apply(PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId))
      .withTagger(AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2))

  implicit val entityFormat: Format[ScopeEntity] = Json.format

}

final case class  ScopeEntity(maybeState: Option[ScopeState] = None ) {
  import ScopeEntity._

  val log = LoggerFactory.getLogger(this.getClass)

  def applyCommand(cmd: Command): ReplyEffect[Event, ScopeEntity] = {
    cmd match {
      case cmd: CreateScope => createScope(cmd)
      case cmd: UpdateScope => updateScope(cmd)
      case cmd: ActivateScope => activateScope(cmd)
      case cmd: DeactivateScope => deactivateScope(cmd)
      case cmd: DeleteScope => deleteScope(cmd)
      case cmd: GetScope => getScope(cmd)
    }
  }

  def createScope(cmd: CreateScope): ReplyEffect[Event, ScopeEntity] = ???

  def updateScope(cmd: UpdateScope): ReplyEffect[Event, ScopeEntity] = ???

  def activateScope(cmd: ActivateScope): ReplyEffect[Event, ScopeEntity] = ???

  def deactivateScope(cmd: DeactivateScope): ReplyEffect[Event, ScopeEntity] = ???

  def deleteScope(cmd: DeleteScope): ReplyEffect[Event, ScopeEntity] = ???

  def getScope(cmd: GetScope): ReplyEffect[Event, ScopeEntity] = ???

  def applyEvent(event: Event): ScopeEntity = {
    event match {
      case event: ScopeCreated => onScopeCreated(event)
      case event: ScopeUpdated => onScopeUpdated(event)
      case event: ScopeActivated => onScopeActivated(event)
      case event: ScopeDeactivated => onScopeDeactivated(event)
      case event: ScopeDeleted => onScopeDeleted(event)
    }
  }

  def onScopeCreated(event: ScopeCreated): ScopeEntity = ???

  def onScopeUpdated(event: ScopeUpdated): ScopeEntity = ???

  def onScopeActivated(event: ScopeActivated): ScopeEntity = ???

  def onScopeDeactivated(event: ScopeDeactivated): ScopeEntity = ???

  def onScopeDeleted(event: ScopeDeleted): ScopeEntity = ???

}
