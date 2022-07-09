package biz.lobachev.annette.service_catalog.impl.service

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
import biz.lobachev.annette.service_catalog.api.service._

object ServiceEntity  {

  trait CommandSerializable
  sealed trait Command extends CommandSerializable
  final case class CreateService(id: ServiceId, name: String, description: String, icon: String, caption: Caption, captionDescription: Caption, link: ServiceLink, createdBy: AnnettePrincipal, replyTo: ActorRef[Confirmation]) extends Command {
    def apply(payload: CreateServicePayload, replyTo: ActorRef[Confirmation]): CreateService =
      payload
      .into[CreateService]
      .withFieldConst(_.replyTo, replyTo)
      .transform
  }
  final case class UpdateService(id: ServiceId, name: Option[String], description: Option[String], icon: Option[String], caption: Option[Caption], captionDescription: Option[Caption], link: Option[ServiceLink], updatedBy: AnnettePrincipal, replyTo: ActorRef[Confirmation]) extends Command {
    def apply(payload: UpdateServicePayload, replyTo: ActorRef[Confirmation]): UpdateService =
      payload
      .into[UpdateService]
      .withFieldConst(_.replyTo, replyTo)
      .transform
  }
  final case class ActivateService(id: ServiceId, updatedBy: AnnettePrincipal, replyTo: ActorRef[Confirmation]) extends Command {
    def apply(payload: ActivateServicePayload, replyTo: ActorRef[Confirmation]): ActivateService =
      payload
      .into[ActivateService]
      .withFieldConst(_.replyTo, replyTo)
      .transform
  }
  final case class DeactivateService(id: ServiceId, updatedBy: AnnettePrincipal, replyTo: ActorRef[Confirmation]) extends Command {
    def apply(payload: DeactivateServicePayload, replyTo: ActorRef[Confirmation]): DeactivateService =
      payload
      .into[DeactivateService]
      .withFieldConst(_.replyTo, replyTo)
      .transform
  }
  final case class DeleteService(id: ServiceId, deletedBy: AnnettePrincipal, replyTo: ActorRef[Confirmation]) extends Command {
    def apply(payload: DeleteServicePayload, replyTo: ActorRef[Confirmation]): DeleteService =
      payload
      .into[DeleteService]
      .withFieldConst(_.replyTo, replyTo)
      .transform
  }
  final case class GetService(id: ServiceId, replyTo: ActorRef[Confirmation]) extends Command {
    def apply(payload: GetServicePayload, replyTo: ActorRef[Confirmation]): GetService =
      payload
      .into[GetService]
      .withFieldConst(_.replyTo, replyTo)
      .transform
  }

  sealed trait Confirmation
  final case object Success extends Confirmation
  final case class SuccessService(service: Service) extends Confirmation
  final case object ServiceAlreadyExist extends Confirmation
  final case object ServiceNotFound extends Confirmation

  implicit val confirmationSuccessFormat: Format[Success.type] = Json.format
  implicit val confirmationSuccessServiceFormat: Format[SuccessService] = Json.format
  implicit val confirmationServiceAlreadyExistFormat: Format[ServiceAlreadyExist.type] = Json.format
  implicit val confirmationServiceNotFoundFormat: Format[ServiceNotFound.type] = Json.format

  sealed trait Event extends AggregateEvent[Event] {
    override def aggregateTag: AggregateEventTagger[Event] = Event.Tag
  }

  object Event {
    val Tag: AggregateEventShards[Event] = AggregateEventTag.sharded[Event](numShards = 10)
  }

  final case class ServiceCreated(id: ServiceId, name: String, description: String, categoryId: CategoryId, targets: Set[AnnettePrincipal] = Set.empty, createdBy: AnnettePrincipal, createdAt: OffsetDateTime = OffsetDateTime.now) extends Event
  final case class ServiceUpdated(name: Option[String], description: Option[String], categoryId: Option[CategoryId], services: Seq[ServiceId] = Seq.empty, updatedBy: AnnettePrincipal, updatedAt: OffsetDateTime = OffsetDateTime.now) extends Event
  final case class ServiceActivated(id: ServiceId, updatedBy: AnnettePrincipal, updatedAt: OffsetDateTime = OffsetDateTime.now) extends Event
  final case class ServiceDeactivated(id: ServiceId, updatedBy: AnnettePrincipal, updatedAt: OffsetDateTime = OffsetDateTime.now) extends Event
  final case class ServiceDeleted(id: ServiceId, deleteBy: AnnettePrincipal, deleteAt: OffsetDateTime = OffsetDateTime.now) extends Event

  implicit val eventServiceCreatedFormat: Format[ServiceCreated] = Json.format
  implicit val eventServiceUpdatedFormat: Format[ServiceUpdated] = Json.format
  implicit val eventServiceActivatedFormat: Format[ServiceActivated] = Json.format
  implicit val eventServiceDeactivatedFormat: Format[ServiceDeactivated] = Json.format
  implicit val eventServiceDeletedFormat: Format[ServiceDeleted] = Json.format

  val empty = ServiceEntity()

  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("ServiceCatalog_Service")

  def apply(persistenceId: PersistenceId): EventSourcedBehavior[Command, Event, ServiceEntity] = {
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, ServiceEntity](
          persistenceId = persistenceId,
          emptyState = ServiceEntity.empty,
          commandHandler = (entity, cmd) => entity.applyCommand(cmd),
          eventHandler = (entity, evt) => entity.applyEvent(evt)
      )
  }

  def apply(entityContext: EntityContext[Command]): Behavior[Command] =
    apply(PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId))
      .withTagger(AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2))

  implicit val entityFormat: Format[ServiceEntity] = Json.format

}

final case class  ServiceEntity(maybeState: Option[ServiceState] = None ) {
  import ServiceEntity._

  val log = LoggerFactory.getLogger(this.getClass)

  def applyCommand(cmd: Command): ReplyEffect[Event, ServiceEntity] = {
    cmd match {
      case cmd: CreateService => createService(cmd)
      case cmd: UpdateService => updateService(cmd)
      case cmd: ActivateService => activateService(cmd)
      case cmd: DeactivateService => deactivateService(cmd)
      case cmd: DeleteService => deleteService(cmd)
      case cmd: GetService => getService(cmd)
    }
  }

  def createService(cmd: CreateService): ReplyEffect[Event, ServiceEntity] = ???

  def updateService(cmd: UpdateService): ReplyEffect[Event, ServiceEntity] = ???

  def activateService(cmd: ActivateService): ReplyEffect[Event, ServiceEntity] = ???

  def deactivateService(cmd: DeactivateService): ReplyEffect[Event, ServiceEntity] = ???

  def deleteService(cmd: DeleteService): ReplyEffect[Event, ServiceEntity] = ???

  def getService(cmd: GetService): ReplyEffect[Event, ServiceEntity] = ???

  def applyEvent(event: Event): ServiceEntity = {
    event match {
      case event: ServiceCreated => onServiceCreated(event)
      case event: ServiceUpdated => onServiceUpdated(event)
      case event: ServiceActivated => onServiceActivated(event)
      case event: ServiceDeactivated => onServiceDeactivated(event)
      case event: ServiceDeleted => onServiceDeleted(event)
    }
  }

  def onServiceCreated(event: ServiceCreated): ServiceEntity = ???

  def onServiceUpdated(event: ServiceUpdated): ServiceEntity = ???

  def onServiceActivated(event: ServiceActivated): ServiceEntity = ???

  def onServiceDeactivated(event: ServiceDeactivated): ServiceEntity = ???

  def onServiceDeleted(event: ServiceDeleted): ServiceEntity = ???

}
