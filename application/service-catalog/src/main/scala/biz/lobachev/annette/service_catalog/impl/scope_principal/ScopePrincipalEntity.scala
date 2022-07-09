package biz.lobachev.annette.service_catalog.impl.scope_principal

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
import biz.lobachev.annette.service_catalog.api.scope_principal._

object ScopePrincipalEntity  {

  trait CommandSerializable
  sealed trait Command extends CommandSerializable
  final case class AssignScopePrincipal(scopeId: ScopeId, principal: String, updatedBy: AnnettePrincipal, replyTo: ActorRef[Confirmation]) extends Command {
    def apply(payload: AssignScopePrincipalPayload, replyTo: ActorRef[Confirmation]): AssignScopePrincipal =
      payload
      .into[AssignScopePrincipal]
      .withFieldConst(_.replyTo, replyTo)
      .transform
  }
  final case class UnassignScopePrincipal(scopeId: ScopeId, principal: String, updatedBy: AnnettePrincipal, replyTo: ActorRef[Confirmation]) extends Command {
    def apply(payload: UnassignScopePrincipalPayload, replyTo: ActorRef[Confirmation]): UnassignScopePrincipal =
      payload
      .into[UnassignScopePrincipal]
      .withFieldConst(_.replyTo, replyTo)
      .transform
  }
  final case class GetScopePrincipal(scopeId: ScopeId, principal: String, replyTo: ActorRef[Confirmation]) extends Command {
    def apply(payload: GetScopePrincipalPayload, replyTo: ActorRef[Confirmation]): GetScopePrincipal =
      payload
      .into[GetScopePrincipal]
      .withFieldConst(_.replyTo, replyTo)
      .transform
  }

  sealed trait Confirmation
  final case object Success extends Confirmation
  final case object ScopePrincipalNotFound extends Confirmation

  implicit val confirmationSuccessFormat: Format[Success.type] = Json.format
  implicit val confirmationScopePrincipalNotFoundFormat: Format[ScopePrincipalNotFound.type] = Json.format

  sealed trait Event extends AggregateEvent[Event] {
    override def aggregateTag: AggregateEventTagger[Event] = Event.Tag
  }

  object Event {
    val Tag: AggregateEventShards[Event] = AggregateEventTag.sharded[Event](numShards = 10)
  }

  final case class ScopePrincipalAssigned(scopeId: ScopeId, principal: String, updatedBy: AnnettePrincipal) extends Event
  final case class ScopePrincipalUnassigned(scopeId: ScopeId, principal: String, updatedBy: AnnettePrincipal) extends Event

  implicit val eventScopePrincipalAssignedFormat: Format[ScopePrincipalAssigned] = Json.format
  implicit val eventScopePrincipalUnassignedFormat: Format[ScopePrincipalUnassigned] = Json.format

  val empty = ScopePrincipalEntity()

  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("ServiceCatalog_ScopePrincipal")

  def apply(persistenceId: PersistenceId): EventSourcedBehavior[Command, Event, ScopePrincipalEntity] = {
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, ScopePrincipalEntity](
          persistenceId = persistenceId,
          emptyState = ScopePrincipalEntity.empty,
          commandHandler = (entity, cmd) => entity.applyCommand(cmd),
          eventHandler = (entity, evt) => entity.applyEvent(evt)
      )
  }

  def apply(entityContext: EntityContext[Command]): Behavior[Command] =
    apply(PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId))
      .withTagger(AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2))

  implicit val entityFormat: Format[ScopePrincipalEntity] = Json.format

}

final case class  ScopePrincipalEntity(maybeState: Option[ScopePrincipalState] = None ) {
  import ScopePrincipalEntity._

  val log = LoggerFactory.getLogger(this.getClass)

  def applyCommand(cmd: Command): ReplyEffect[Event, ScopePrincipalEntity] = {
    cmd match {
      case cmd: AssignScopePrincipal => assignScopePrincipal(cmd)
      case cmd: UnassignScopePrincipal => unassignScopePrincipal(cmd)
      case cmd: GetScopePrincipal => getScopePrincipal(cmd)
    }
  }

  def assignScopePrincipal(cmd: AssignScopePrincipal): ReplyEffect[Event, ScopePrincipalEntity] = ???

  def unassignScopePrincipal(cmd: UnassignScopePrincipal): ReplyEffect[Event, ScopePrincipalEntity] = ???

  def getScopePrincipal(cmd: GetScopePrincipal): ReplyEffect[Event, ScopePrincipalEntity] = ???

  def applyEvent(event: Event): ScopePrincipalEntity = {
    event match {
      case event: ScopePrincipalAssigned => onScopePrincipalAssigned(event)
      case event: ScopePrincipalUnassigned => onScopePrincipalUnassigned(event)
    }
  }

  def onScopePrincipalAssigned(event: ScopePrincipalAssigned): ScopePrincipalEntity = ???

  def onScopePrincipalUnassigned(event: ScopePrincipalUnassigned): ScopePrincipalEntity = ???

}
