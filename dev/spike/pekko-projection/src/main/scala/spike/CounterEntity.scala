package spike

import org.apache.pekko
import pekko.actor.typed.{ActorRef, Behavior}
import pekko.cluster.sharding.typed.scaladsl.EntityTypeKey
import pekko.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import pekko.persistence.typed.PersistenceId

/**
 * Minimal event-sourced counter, structured to mirror how Annette entities are organized:
 * the Event trait lives inside the object companion, so eventType.getName produces
 * the same "<pkg>.<Object>$Event" string Lagom's `AggregateEventTag.sharded[Event]` sees.
 *
 * Serialization marker is a plain trait — no external dependency needed for the spike.
 */
object CounterEntity {

  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("spike.CounterEntity")

  final case class Command(id: String, delta: Int, replyTo: ActorRef[Int])

  sealed trait Event {
    def id: String
  }
  final case class Incremented(id: String, delta: Int) extends Event
  final case class Decremented(id: String, delta: Int) extends Event

  /** The tag, using a Tagger that matches Lagom's `AggregateEventTag.sharded[Event](10)` format. */
  val Tag: Tagger[Event] = Tagger.fromEventName[Event](numShards = 10)

  def apply(persistenceId: PersistenceId): Behavior[Command] =
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, Map[String, Int]](
        persistenceId = persistenceId,
        emptyState = Map.empty,
        commandHandler = (_, cmd) =>
          cmd match {
            case Command(id, delta, replyTo) =>
              val evt = if (delta >= 0) Incremented(id, delta) else Decremented(id, delta)
              Effect.persist(evt).thenReply(replyTo)(_ => delta)
          },
        eventHandler = (state, evt) =>
          evt match {
            case Incremented(id, delta) => state.updated(id, state.getOrElse(id, 0) + delta)
            case Decremented(id, delta) => state.updated(id, state.getOrElse(id, 0) + delta)
          }
      )
      // The tagger is wired here — equivalent to Lagom's withTagger(AkkaTaggerAdapter.fromLagom(ctx, Event.Tag))
      .withTagger { case evt => Set(Tag.tagFor(evt.id)) }
}
