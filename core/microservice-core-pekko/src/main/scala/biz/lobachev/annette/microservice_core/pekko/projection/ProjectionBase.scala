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

package biz.lobachev.annette.microservice_core.pekko.projection

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.persistence.query.Offset
import org.apache.pekko.projection.Projection
import org.apache.pekko.projection.ProjectionId
import org.apache.pekko.projection.eventsourced.EventEnvelope
import org.apache.pekko.projection.eventsourced.scaladsl.EventSourcedProvider
import org.apache.pekko.projection.scaladsl.{Handler, SourceProvider}
import org.apache.pekko.projection.cassandra.scaladsl.CassandraProjection
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
 * Boilerplate every Annette read-side / projection handler needs. Replaces Lagom's
 * `ReadSideProcessor[Event]` shape. Each service slice 004-012 implements a concrete subclass
 * that:
 *
 *  1. Declares the event tags via `Tagger.fromEventName[Event.Event](10).allTags` (one
 *     `SourceProvider` per tag).
 *  2. Implements `process(envelope)` with the per-event logic (adapted from the Lagom
 *     `aggregateEventTag` + `handle` pattern).
 *
 * Per `001-decisions.md` §B the Cassandra offset store keyspace is `pekko_projection` with
 * tables `offset_store` + `projection_management`. Slices 004-012 must call
 * `CassandraProjection.createTablesIfNotExists(actorSystem)` once at startup; this trait
 * exposes `init()` for that purpose but does not call it automatically (avoid duplicate calls
 * when multiple projections share a system).
 *
 * Restart backoff defaults are conservative; override `withRestartBackoff` on the returned
 * `Projection` if a particular handler needs different tuning. See
 * `dev/migration/003-core-recipe.md` §B for the full migration recipe.
 */
trait ProjectionBase[E] {

  protected implicit def system: ActorSystem[_]
  protected implicit def ec: ExecutionContext

  protected val log = LoggerFactory.getLogger(getClass)

  /** Name for this projection — appears in `pekko_projection.offset_store.projection_name`. */
  def projectionName: String

  /** Per-shard tag strings. Typically `Tagger.fromEventName[E](10).allTags`. */
  def tags: Seq[String]

  /** The event handler — one envelope at a time (at-least-once). */
  def process(envelope: EventEnvelope[E]): Future[org.apache.pekko.Done]

  /**
   * Build the SourceProvider for a single tag (one projection instance per tag). Per
   * `001-decisions.md` §B the offset store is shared with Pekko Persistence Cassandra.
   */
  def sourceProvider(tag: String): SourceProvider[Offset, EventEnvelope[E]] =
    EventSourcedProvider.eventsByTag[E](system, readJournalPluginId = "pekko.persistence.cassandra.query.journal", tag = tag)

  /**
   * Construct (but do not start) the at-least-once Cassandra projection for a single tag.
   * Default restart backoff: 3s min, 30s max, random factor 0.2 (matches Pekko docs example).
   * Override the returned `Projection` if a handler needs different tuning.
   */
  def projection(tag: String): Projection[EventEnvelope[E]] = {
    val handler = Handler[EventEnvelope[E]](envelope => process(envelope))
    CassandraProjection
      .atLeastOnce(
        ProjectionId(projectionName, tag),
        sourceProvider(tag),
        () => handler
      )
      .withRestartBackoff(minBackoff = 3.seconds, maxBackoff = 30.seconds, randomFactor = 0.2d)
  }

  /**
   * Idempotent table-creation helper. Call once at service startup before any projection
   * runs. Safe to call from multiple projections in the same ActorSystem. Uses the implicit
   * ActorSystem declared by the trait.
   */
  def init(): Future[org.apache.pekko.Done] =
    CassandraProjection.createTablesIfNotExists()

}
