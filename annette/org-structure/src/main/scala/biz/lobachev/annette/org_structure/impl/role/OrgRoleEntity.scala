/*
 * Copyright 2013 Valery Lobachev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package biz.lobachev.annette.org_structure.impl.role

import java.time.OffsetDateTime

import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl._
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import biz.lobachev.annette.core.model.AnnettePrincipal
import biz.lobachev.annette.org_structure.api.role._
import biz.lobachev.annette.org_structure.impl.role.model.OrgRoleState
import com.lightbend.lagom.scaladsl.persistence._
import io.scalaland.chimney.dsl._
import play.api.libs.json.{Format, _}

object OrgRoleEntity {

  trait CommandSerializable

  sealed trait Command extends CommandSerializable

  final case class CreateOrgRole(payload: CreateOrgRolePayload, replyTo: ActorRef[Confirmation]) extends Command
  final case class UpdateOrgRole(payload: UpdateOrgRolePayload, replyTo: ActorRef[Confirmation]) extends Command
  final case class DeleteOrgRole(payload: DeleteOrgRolePayload, replyTo: ActorRef[Confirmation]) extends Command
  final case class GetOrgRole(id: OrgRoleId, replyTo: ActorRef[Confirmation])                    extends Command

  sealed trait Confirmation
  final case object Success                        extends Confirmation
  final case class SuccessOrgRole(entity: OrgRole) extends Confirmation
  final case object NotFound                       extends Confirmation
  final case object AlreadyExist                   extends Confirmation

  implicit val confirmationSuccessFormat: Format[Success.type]           = Json.format
  implicit val confirmationSuccessOrgRoleFormat: Format[SuccessOrgRole]  = Json.format
  implicit val confirmationNotFoundFormat: Format[NotFound.type]         = Json.format
  implicit val confirmationAlreadyExistFormat: Format[AlreadyExist.type] = Json.format

  implicit val confirmationFormat: Format[Confirmation] = Json.format

  sealed trait Event extends AggregateEvent[Event] {
    override def aggregateTag: AggregateEventTagger[Event] = Event.Tag
  }

  object Event {
    val Tag: AggregateEventShards[Event] = AggregateEventTag.sharded[Event](numShards = 10)
  }

  final case class OrgRoleCreated(
    id: OrgRoleId,
    name: String,
    description: String,
    createdBy: AnnettePrincipal,
    createdAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class OrgRoleUpdated(
    id: OrgRoleId,
    name: String,
    description: String,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class OrgRoleDeleted(
    id: OrgRoleId, // orgRole id
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  implicit val orgRoleCreatedFormat: Format[OrgRoleCreated]     = Json.format
  implicit val orgRoleUpdatedFormat: Format[OrgRoleUpdated]     = Json.format
  implicit val orgRoleDeactivatedFormat: Format[OrgRoleDeleted] = Json.format

  val empty                           = OrgRoleEntity(None)
  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("OrgRole")

  def apply(persistenceId: PersistenceId): EventSourcedBehavior[Command, Event, OrgRoleEntity] =
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, OrgRoleEntity](
        persistenceId = persistenceId,
        emptyState = OrgRoleEntity.empty,
        commandHandler = (entity, cmd) => entity.applyCommand(cmd),
        eventHandler = (entity, evt) => entity.applyEvent(evt)
      )

  def apply(entityContext: EntityContext[Command]): Behavior[Command] =
    apply(PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId))
      .withTagger(AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2))

  implicit val orgRoleEntityFormat: Format[OrgRoleEntity] = Json.format
}

final case class OrgRoleEntity(maybeState: Option[OrgRoleState]) {

  import OrgRoleEntity._

  def applyCommand(cmd: Command): ReplyEffect[Event, OrgRoleEntity] =
    cmd match {
      case CreateOrgRole(payload, replyTo) => createOrgRole(payload, replyTo)
      case UpdateOrgRole(payload, replyTo) => updateOrgRole(payload, replyTo)
      case DeleteOrgRole(payload, replyTo) => deleteOrgRole(payload, replyTo)
      case GetOrgRole(_, replyTo)          => getOrgRole(replyTo)
    }

  def createOrgRole(payload: CreateOrgRolePayload, replyTo: ActorRef[Confirmation]): ReplyEffect[Event, OrgRoleEntity] =
    maybeState match {
      case None    =>
        val event = payload.transformInto[OrgRoleCreated]
        Effect
          .persist(event)
          .thenReply(replyTo)(_ => Success)
      case Some(_) => Effect.reply(replyTo)(AlreadyExist)
    }

  def updateOrgRole(payload: UpdateOrgRolePayload, replyTo: ActorRef[Confirmation]): ReplyEffect[Event, OrgRoleEntity] =
    maybeState match {
      case None    => Effect.reply(replyTo)(NotFound)
      case Some(_) =>
        val event = payload.transformInto[OrgRoleUpdated]
        Effect
          .persist(event)
          .thenReply(replyTo)(_ => Success)
    }

  def deleteOrgRole(
    payload: DeleteOrgRolePayload,
    replyTo: ActorRef[Confirmation]
  ): ReplyEffect[Event, OrgRoleEntity] =
    maybeState match {
      case None    => Effect.reply(replyTo)(NotFound)
      case Some(_) =>
        val event = payload.transformInto[OrgRoleDeleted]
        Effect
          .persist(event)
          .thenReply(replyTo)(_ => Success)
    }

  def getOrgRole(replyTo: ActorRef[Confirmation]): ReplyEffect[Event, OrgRoleEntity] =
    maybeState match {
      case Some(state) => Effect.reply(replyTo)(SuccessOrgRole(state.toOrgRole))
      case None        => Effect.reply(replyTo)(NotFound)
    }

  def applyEvent(evt: Event): OrgRoleEntity =
    evt match {
      case event: OrgRoleCreated => onOrgRoleCreated(event)
      case event: OrgRoleUpdated => onOrgRoleUpdated(event)
      case _: OrgRoleDeleted     => onOrgRoleDeleted()
    }

  def onOrgRoleCreated(event: OrgRoleCreated): OrgRoleEntity =
    OrgRoleEntity(
      Some(
        event
          .into[OrgRoleState]
          .withFieldConst(_.updatedAt, event.createdAt)
          .withFieldConst(_.updatedBy, event.createdBy)
          .transform
      )
    )

  def onOrgRoleUpdated(event: OrgRoleUpdated): OrgRoleEntity =
    OrgRoleEntity(
      Some(
        event
          .into[OrgRoleState]
          .withFieldConst(_.updatedAt, event.updatedAt)
          .transform
      )
    )

  def onOrgRoleDeleted(): OrgRoleEntity =
    OrgRoleEntity(None)

}
