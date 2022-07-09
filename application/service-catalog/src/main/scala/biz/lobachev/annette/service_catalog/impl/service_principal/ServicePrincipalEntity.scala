package biz.lobachev.annette.service_catalog.impl.service_principal

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
import biz.lobachev.annette.service_catalog.api.service_principal._

object ServicePrincipalEntity  {

  trait CommandSerializable
  sealed trait Command extends CommandSerializable
  final case class AssignServicePrincipal(scopeId: ScopeId, principal: String, updatedBy: AnnettePrincipal, replyTo: ActorRef[Confirmation]) extends Command {
    def apply(payload: AssignServicePrincipalPayload, replyTo: ActorRef[Confirmation]): AssignServicePrincipal =
      payload
      .into[AssignServicePrincipal]
      .withFieldConst(_.replyTo, replyTo)
      .transform
  }
  final case class UnassignServicePrincipal(scopeId: ScopeId, principal: String, updatedBy: AnnettePrincipal, replyTo: ActorRef[Confirmation]) extends Command {
    def apply(payload: UnassignServicePrincipalPayload, replyTo: ActorRef[Confirmation]): UnassignServicePrincipal =
      payload
      .into[UnassignServicePrincipal]
      .withFieldConst(_.replyTo, replyTo)
      .transform
  }
  final case class GetServicePrincipal(scopeId: ScopeId, principal: String, replyTo: ActorRef[Confirmation]) extends Command {
    def apply(payload: GetServicePrincipalPayload, replyTo: ActorRef[Confirmation]): GetServicePrincipal =
      payload
      .into[GetServicePrincipal]
      .withFieldConst(_.replyTo, replyTo)
      .transform
  }

  sealed trait Confirmation
  final case object Success extends Confirmation
  final case object ServicePrincipalNotFound extends Confirmation

  implicit val confirmationSuccessFormat: Format[Success.type] = Json.format
  implicit val confirmationServicePrincipalNotFoundFormat: Format[ServicePrincipalNotFound.type] = Json.format

  sealed trait Event extends AggregateEvent[Event] {
    override def aggregateTag: AggregateEventTagger[Event] = Event.Tag
  }

  object Event {
    val Tag: AggregateEventShards[Event] = AggregateEventTag.sharded[Event](numShards = 10)
  }

  final case class ServicePrincipalAssigned(scopeId: ScopeId, principal: String, updatedBy: AnnettePrincipal) extends Event
  final case class ServicePrincipalUnassigned(scopeId: ScopeId, principal: String, updatedBy: AnnettePrincipal) extends Event

  implicit val eventServicePrincipalAssignedFormat: Format[ServicePrincipalAssigned] = Json.format
  implicit val eventServicePrincipalUnassignedFormat: Format[ServicePrincipalUnassigned] = Json.format

  val empty = ServicePrincipalEntity()

  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("ServiceCatalog_ServicePrincipal")

  def apply(persistenceId: PersistenceId): EventSourcedBehavior[Command, Event, ServicePrincipalEntity] = {
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, ServicePrincipalEntity](
          persistenceId = persistenceId,
          emptyState = ServicePrincipalEntity.empty,
          commandHandler = (entity, cmd) => entity.applyCommand(cmd),
          eventHandler = (entity, evt) => entity.applyEvent(evt)
      )
  }

  def apply(entityContext: EntityContext[Command]): Behavior[Command] =
    apply(PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId))
      .withTagger(AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2))

  implicit val entityFormat: Format[ServicePrincipalEntity] = Json.format

}

final case class  ServicePrincipalEntity(maybeState: Option[ServicePrincipalState] = None ) {
  import ServicePrincipalEntity._

  val log = LoggerFactory.getLogger(this.getClass)

  def applyCommand(cmd: Command): ReplyEffect[Event, ServicePrincipalEntity] = {
    cmd match {
      case cmd: AssignServicePrincipal => assignServicePrincipal(cmd)
      case cmd: UnassignServicePrincipal => unassignServicePrincipal(cmd)
      case cmd: GetServicePrincipal => getServicePrincipal(cmd)
    }
  }

  def assignServicePrincipal(cmd: AssignServicePrincipal): ReplyEffect[Event, ServicePrincipalEntity] = ???

  def unassignServicePrincipal(cmd: UnassignServicePrincipal): ReplyEffect[Event, ServicePrincipalEntity] = ???

  def getServicePrincipal(cmd: GetServicePrincipal): ReplyEffect[Event, ServicePrincipalEntity] = ???

  def applyEvent(event: Event): ServicePrincipalEntity = {
    event match {
      case event: ServicePrincipalAssigned => onServicePrincipalAssigned(event)
      case event: ServicePrincipalUnassigned => onServicePrincipalUnassigned(event)
    }
  }

  def onServicePrincipalAssigned(event: ServicePrincipalAssigned): ServicePrincipalEntity = ???

  def onServicePrincipalUnassigned(event: ServicePrincipalUnassigned): ServicePrincipalEntity = ???

}
