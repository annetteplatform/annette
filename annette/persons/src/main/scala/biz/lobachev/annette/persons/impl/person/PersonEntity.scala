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

package biz.lobachev.annette.persons.impl.person

import java.time.OffsetDateTime
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl._
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import biz.lobachev.annette.core.model.PersonId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.category.CategoryId
import biz.lobachev.annette.persons.api.person._
import biz.lobachev.annette.persons.impl.person.model.PersonState
import com.lightbend.lagom.scaladsl.persistence._
import io.scalaland.chimney.dsl._
import play.api.libs.json.{Format, _}

object PersonEntity {

  trait CommandSerializable

  sealed trait Command extends CommandSerializable

  final case class CreatePerson(payload: CreatePersonPayload, replyTo: ActorRef[Confirmation]) extends Command
  final case class UpdatePerson(payload: UpdatePersonPayload, replyTo: ActorRef[Confirmation]) extends Command
  final case class DeletePerson(payload: DeletePersonPayload, replyTo: ActorRef[Confirmation]) extends Command
  final case class GetPerson(id: PersonId, replyTo: ActorRef[Confirmation])                    extends Command

  sealed trait Confirmation
  final case object Success                      extends Confirmation
  final case class SuccessPerson(entity: Person) extends Confirmation
  final case object NotFound                     extends Confirmation
  final case object AlreadyExist                 extends Confirmation

  implicit val confirmationSuccessFormat: Format[Success.type]           = Json.format
  implicit val confirmationSuccessPersonFormat: Format[SuccessPerson]    = Json.format
  implicit val confirmationNotFoundFormat: Format[NotFound.type]         = Json.format
  implicit val confirmationAlreadyExistFormat: Format[AlreadyExist.type] = Json.format
  implicit val confirmationFormat: Format[Confirmation]                  = Json.format[Confirmation]

  sealed trait Event extends AggregateEvent[Event] {
    override def aggregateTag: AggregateEventTagger[Event] = Event.Tag
  }

  object Event {
    val Tag: AggregateEventShards[Event] = AggregateEventTag.sharded[Event](numShards = 10)
  }

  final case class PersonCreated(
    id: PersonId,
    lastname: String,
    firstname: String,
    middlename: Option[String],
    categoryId: CategoryId,
    phone: Option[String],
    email: Option[String],
    createdBy: AnnettePrincipal,
    createdAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class PersonUpdated(
    id: PersonId,
    lastname: String,
    firstname: String,
    middlename: Option[String],
    categoryId: CategoryId,
    phone: Option[String],
    email: Option[String],
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event
  final case class PersonDeleted(
    id: PersonId,
    updatedBy: AnnettePrincipal,
    updatedAt: OffsetDateTime = OffsetDateTime.now
  ) extends Event

  implicit val personCreatedFormat: Format[PersonCreated]     = Json.format
  implicit val personUpdatedFormat: Format[PersonUpdated]     = Json.format
  implicit val personDeactivatedFormat: Format[PersonDeleted] = Json.format

  val empty                           = PersonEntity(None)
  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Person")

  def apply(persistenceId: PersistenceId): EventSourcedBehavior[Command, Event, PersonEntity] =
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, PersonEntity](
        persistenceId = persistenceId,
        emptyState = PersonEntity.empty,
        commandHandler = (entity, cmd) => entity.applyCommand(cmd),
        eventHandler = (entity, evt) => entity.applyEvent(evt)
      )

  def apply(entityContext: EntityContext[Command]): Behavior[Command] =
    apply(PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId))
      .withTagger(AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag))
      .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2))

  implicit val personEntityFormat: Format[PersonEntity] = Json.format
}

final case class PersonEntity(maybeState: Option[PersonState]) {

  import PersonEntity._

  def applyCommand(cmd: Command): ReplyEffect[Event, PersonEntity] =
    cmd match {
      case CreatePerson(payload, replyTo) => createPerson(payload, replyTo)
      case UpdatePerson(payload, replyTo) => updatePerson(payload, replyTo)
      case DeletePerson(payload, replyTo) => deletePerson(payload, replyTo)
      case GetPerson(_, replyTo)          => getPerson(replyTo)
    }

  def createPerson(payload: CreatePersonPayload, replyTo: ActorRef[Confirmation]): ReplyEffect[Event, PersonEntity] =
    maybeState match {
      case None    =>
        val event = payload.transformInto[PersonCreated]
        Effect
          .persist(event)
          .thenReply(replyTo)(_ => Success)
      case Some(_) => Effect.reply(replyTo)(AlreadyExist)
    }

  def updatePerson(payload: UpdatePersonPayload, replyTo: ActorRef[Confirmation]): ReplyEffect[Event, PersonEntity] =
    maybeState match {
      case None    => Effect.reply(replyTo)(NotFound)
      case Some(_) =>
        val event = payload.transformInto[PersonUpdated]
        Effect
          .persist(event)
          .thenReply(replyTo)(_ => Success)
    }

  def deletePerson(
    payload: DeletePersonPayload,
    replyTo: ActorRef[Confirmation]
  ): ReplyEffect[Event, PersonEntity] =
    maybeState match {
      case None    => Effect.reply(replyTo)(NotFound)
      case Some(_) =>
        val event = payload.transformInto[PersonDeleted]
        Effect
          .persist(event)
          .thenReply(replyTo)(_ => Success)
    }

  def getPerson(replyTo: ActorRef[Confirmation]): ReplyEffect[Event, PersonEntity] =
    maybeState match {
      case Some(state) => Effect.reply(replyTo)(SuccessPerson(state.toPerson))
      case None        => Effect.reply(replyTo)(NotFound)
    }

  def onPersonCreated(event: PersonCreated): PersonEntity =
    PersonEntity(
      Some(
        event
          .into[PersonState]
          .withFieldConst(_.updatedAt, event.createdAt)
          .withFieldConst(_.updatedBy, event.createdBy)
          .transform
      )
    )

  def onPersonUpdated(event: PersonUpdated): PersonEntity =
    PersonEntity(
      Some(
        event
          .into[PersonState]
          .withFieldConst(_.updatedAt, event.updatedAt)
          .transform
      )
    )

  def onPersonDeleted(): PersonEntity =
    PersonEntity(None)

  def applyEvent(event: Event): PersonEntity =
    event match {
      case event: PersonCreated => onPersonCreated(event)
      case event: PersonUpdated => onPersonUpdated(event)
      case _: PersonDeleted     => onPersonDeleted()
    }

}
