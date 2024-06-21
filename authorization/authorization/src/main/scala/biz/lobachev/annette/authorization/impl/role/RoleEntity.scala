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

package biz.lobachev.annette.authorization.impl.role

import java.time.OffsetDateTime
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl.{EntityContext, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import biz.lobachev.annette.authorization.api.assignment.AuthSource
import biz.lobachev.annette.authorization.api.role._
import biz.lobachev.annette.authorization.impl.role.model.RoleState
import biz.lobachev.annette.core.model.auth.{AnnettePrincipal, Permission}
import com.lightbend.lagom.scaladsl.persistence._
import biz.lobachev.annette.core.utils.ChimneyCommons._
import io.scalaland.chimney.dsl._
import org.slf4j.LoggerFactory
import play.api.libs.json._

object RoleEntity {

  trait CommandSerializable
  sealed trait Command                                                                                   extends CommandSerializable
  final case class CreateRole(payload: CreateRolePayload, replyTo: ActorRef[Confirmation])               extends Command
  final case class UpdateRole(payload: UpdateRolePayload, replyTo: ActorRef[Confirmation])               extends Command
  final case class DeleteRole(payload: DeleteRolePayload, replyTo: ActorRef[Confirmation])               extends Command
  final case class AssignPrincipal(payload: AssignPrincipalPayload, replyTo: ActorRef[Confirmation])     extends Command
  final case class UnassignPrincipal(payload: UnassignPrincipalPayload, replyTo: ActorRef[Confirmation]) extends Command
  final case class GetRole(id: AuthRoleId, replyTo: ActorRef[Confirmation])                          extends Command
  final case class GetRolePrincipals(id: AuthRoleId, replyTo: ActorRef[Confirmation])                    extends Command

  sealed trait Confirmation
  final case class SuccessRole(role: AuthRole)                          extends Confirmation
  final case class SuccessPrincipals(principals: Set[AnnettePrincipal]) extends Confirmation
  final case object Success                                             extends Confirmation
  final case object RoleAlreadyExist                                    extends Confirmation
  final case object RoleNotFound                                        extends Confirmation

  implicit val confirmationSuccessRoleFormat: Format[SuccessRole]                = Json.format
  implicit val confirmationSuccessPrincipalsFormat: Format[SuccessPrincipals]    = Json.format
  implicit val confirmationSuccessFormat: Format[Success.type]                   = Json.format
  implicit val confirmationRoleAlreadyExistFormat: Format[RoleAlreadyExist.type] = Json.format
  implicit val confirmationRoleNotFoundFormat: Format[RoleNotFound.type]         = Json.format
  implicit val confirmationFormat: Format[Confirmation]                          = Json.format[Confirmation]

  sealed trait Event extends AggregateEvent[Event] {
    override def aggregateTag: AggregateEventTagger[Event] = Event.Tag
  }

  object Event {
    val Tag: AggregateEventShards[Event] = AggregateEventTag.sharded[Event](numShards = 10)
  }

  final case class RoleCreated(
    id: AuthRoleId,
    name: String,
    description: String,
    permissions: Set[Permission],
    createdBy: AnnettePrincipal,
    createdAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class RoleUpdated(
    id: AuthRoleId,
    name: Option[String],
    description: Option[String],
    addedPermissions: Set[Permission],
    removedPermissions: Set[Permission],
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class RoleDeleted(
    id: AuthRoleId,
    deletedBy: AnnettePrincipal,
    deletedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class PrincipalAssigned(
    roleId: AuthRoleId,
    principal: AnnettePrincipal,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class PrincipalUnassigned(
    roleId: AuthRoleId,
    principal: AnnettePrincipal,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class AssignmentCreated(
    principal: AnnettePrincipal,
    permission: Permission,
    source: AuthSource,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class AssignmentDeleted(
    principal: AnnettePrincipal,
    permission: Permission,
    source: AuthSource,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  implicit val eventRoleCreatedFormat: Format[RoleCreated]                 = Json.format
  implicit val eventRoleUpdatedFormat: Format[RoleUpdated]                 = Json.format
  implicit val eventRoleDeletedFormat: Format[RoleDeleted]                 = Json.format
  implicit val eventPrincipalAssignedFormat: Format[PrincipalAssigned]     = Json.format
  implicit val eventPrincipalUnassignedFormat: Format[PrincipalUnassigned] = Json.format
  implicit val eventAssignmentCreatedFormat: Format[AssignmentCreated]     = Json.format
  implicit val eventAssignmentDeletedFormat: Format[AssignmentDeleted]     = Json.format

  val empty = RoleEntity()

  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Authorization_Role")

  def apply(persistenceId: PersistenceId): EventSourcedBehavior[Command, Event, RoleEntity] =
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, RoleEntity](
        persistenceId = persistenceId,
        emptyState = RoleEntity.empty,
        commandHandler = (entity, cmd) => entity.applyCommand(cmd),
        eventHandler = (entity, evt) => entity.applyEvent(evt)
      )

  def apply(entityContext: EntityContext[Command]): Behavior[Command] =
    apply(PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId))
      .withTagger(AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2))

  implicit val entityFormat: Format[RoleEntity] = Json.format

}

final case class RoleEntity(maybeRole: Option[RoleState] = None) {
  import RoleEntity._

  val log = LoggerFactory.getLogger(this.getClass)

  def applyCommand(cmd: Command): ReplyEffect[Event, RoleEntity] =
    cmd match {
      case cmd: CreateRole        => createRole(cmd)
      case cmd: UpdateRole        => updateRole(cmd)
      case cmd: DeleteRole        => deleteRole(cmd)
      case cmd: AssignPrincipal   => assignPrincipal(cmd)
      case cmd: UnassignPrincipal => unassignPrincipal(cmd)
      case cmd: GetRole       => getRole(cmd)
      case cmd: GetRolePrincipals => getRolePrincipals(cmd)
    }

  def createRole(cmd: CreateRole): ReplyEffect[Event, RoleEntity] =
    maybeRole match {
      case None =>
        val event = cmd.payload.transformInto[RoleCreated]
        Effect
          .persist(event)
          .thenReply(cmd.replyTo)(_ => Success)
      case _    => Effect.reply(cmd.replyTo)(RoleAlreadyExist)
    }

  def updateRole(cmd: UpdateRole): ReplyEffect[Event, RoleEntity] =
    maybeRole match {
      case None       => Effect.reply(cmd.replyTo)(RoleNotFound)
      case Some(role) =>
        log.debug("updateRole payload permissions: {}", cmd.payload.permissions)
        log.debug("updateRole state permissions: {}", role.permissions)
        val addedPermissions   = cmd.payload.permissions -- role.permissions
        val removedPermissions = role.permissions -- cmd.payload.permissions
        log.debug("updateRole addedPermissions: {}", addedPermissions)
        log.debug("updateRole removedPermissions: {}", removedPermissions)

        val event                   = cmd.payload
          .into[RoleUpdated]
          .withFieldConst(_.name, if (cmd.payload.name != role.name) Some(cmd.payload.name) else None)
          .withFieldConst(
            _.description,
            if (cmd.payload.description != role.description) Some(cmd.payload.description)
            else None
          )
          .withFieldConst(_.addedPermissions, addedPermissions)
          .withFieldConst(_.removedPermissions, removedPermissions)
          .transform
        val createdAssignmentEvents = for {
          permission <- addedPermissions
          principal  <- role.principals
        } yield AssignmentCreated(
          principal = principal,
          permission = permission,
          source = AuthSource("role", role.id),
          updatedBy = cmd.payload.updatedBy,
          updatedAt = event.updatedAt
        )
        val removedAssignmentEvents = for {
          permission <- removedPermissions
          principal  <- role.principals
        } yield AssignmentDeleted(
          principal = principal,
          permission = permission,
          source = AuthSource("role", role.id),
          updatedBy = cmd.payload.updatedBy,
          updatedAt = event.updatedAt
        )

        log.debug("updateRole event: {}", event)
        if (
          event.name.isDefined || event.description.isDefined || addedPermissions.nonEmpty || removedPermissions.nonEmpty
        )
          Effect
            .persist(createdAssignmentEvents.toSeq ++ removedAssignmentEvents.toSeq :+ event)
            .thenReply(cmd.replyTo)(_ => Success)
        else
          Effect
            .reply(cmd.replyTo)(Success)
    }

  def deleteRole(cmd: DeleteRole): ReplyEffect[Event, RoleEntity] =
    maybeRole match {
      case None       => Effect.reply(cmd.replyTo)(RoleNotFound)
      case Some(role) =>
        val event                   = cmd.payload.transformInto[RoleDeleted]
        val removedAssignmentEvents = for {
          permission <- role.permissions
          principal  <- role.principals
        } yield AssignmentDeleted(
          principal = principal,
          permission = permission,
          source = AuthSource("role", role.id),
          updatedBy = cmd.payload.deletedBy,
          updatedAt = event.deletedAt
        )
        Effect
          .persist(removedAssignmentEvents.toSeq :+ event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def assignPrincipal(cmd: AssignPrincipal): ReplyEffect[Event, RoleEntity] =
    maybeRole match {
      case None       => Effect.reply(cmd.replyTo)(RoleNotFound)
      case Some(role) =>
        val event                   = cmd.payload.transformInto[PrincipalAssigned]
        val createdAssignmentEvents = for {
          permission <- role.permissions
        } yield AssignmentCreated(
          principal = cmd.payload.principal,
          permission = permission,
          source = AuthSource("role", role.id),
          updatedBy = cmd.payload.updatedBy,
          updatedAt = event.updatedAt
        )
        Effect
          .persist(createdAssignmentEvents.toSeq :+ event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def unassignPrincipal(cmd: UnassignPrincipal): ReplyEffect[Event, RoleEntity] =
    maybeRole match {
      case None       => Effect.reply(cmd.replyTo)(RoleNotFound)
      case Some(role) =>
        val event                   = cmd.payload.transformInto[PrincipalUnassigned]
        val removedAssignmentEvents = for {
          permission <- role.permissions
        } yield AssignmentDeleted(
          principal = cmd.payload.principal,
          permission = permission,
          source = AuthSource("role", role.id),
          updatedBy = cmd.payload.updatedBy,
          updatedAt = event.updatedAt
        )
        Effect
          .persist(removedAssignmentEvents.toSeq :+ event)
          .thenReply(cmd.replyTo)(_ => Success)
    }

  def getRole(cmd: GetRole): ReplyEffect[Event, RoleEntity] =
    maybeRole match {
      case Some(role) => Effect.reply(cmd.replyTo)(SuccessRole(role.transformInto[AuthRole]))
      case None       => Effect.reply(cmd.replyTo)(RoleNotFound)
    }

  def getRolePrincipals(cmd: GetRolePrincipals): ReplyEffect[Event, RoleEntity] =
    maybeRole match {
      case Some(role) => Effect.reply(cmd.replyTo)(SuccessPrincipals(role.principals))
      case None       => Effect.reply(cmd.replyTo)(RoleNotFound)
    }

  def applyEvent(event: Event): RoleEntity =
    event match {
      case event: RoleCreated         => onRoleCreated(event)
      case event: RoleUpdated         => onRoleUpdated(event)
      case _: RoleDeleted             => onRoleDeleted()
      case event: PrincipalAssigned   => onPrincipalAssigned(event)
      case event: PrincipalUnassigned => onPrincipalUnassigned(event)
      case _: AssignmentCreated       => this
      case _: AssignmentDeleted       => this
    }

  def onRoleCreated(event: RoleCreated): RoleEntity =
    RoleEntity(
      Some(
        event
          .into[RoleState]
          .withFieldConst(_.updatedBy, event.createdBy)
          .withFieldConst(_.updatedAt, event.createdAt)
          .transform
      )
    )

  def onRoleUpdated(event: RoleUpdated): RoleEntity =
    RoleEntity(
      maybeRole.map { role =>
        role.copy(
          name = event.name.getOrElse(role.name),
          description = event.description.getOrElse(role.description),
          permissions = role.permissions ++ event.addedPermissions -- event.removedPermissions,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      }
    )

  def onRoleDeleted(): RoleEntity =
    RoleEntity(None)

  def onPrincipalAssigned(event: PrincipalAssigned): RoleEntity =
    RoleEntity(
      maybeRole.map { role =>
        role.copy(
          principals = role.principals + event.principal,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      }
    )

  def onPrincipalUnassigned(event: PrincipalUnassigned): RoleEntity =
    RoleEntity(
      maybeRole.map { role =>
        role.copy(
          principals = role.principals - event.principal,
          updatedBy = event.updatedBy,
          updatedAt = event.updatedAt
        )
      }
    )

}
