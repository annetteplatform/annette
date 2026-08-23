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

package biz.lobachev.annette.microservice_core.pekko.event_processing

import scala.reflect.ClassTag

/**
 * Mirrors Lagom's `AggregateEventTag.sharded[Event](numShards)` byte-for-byte so that events
 * written by Lagom-based Annette services remain queryable by Pekko-based ones after migration.
 *
 * Per `dev/migration/001-decisions.md` §A (verified empirically by Spike A against Cassandra
 * 3.11), Lagom's tagger format is:
 * {{{
 * def selectShard(numShards: Int, entityId: String): Int =
 *   Math.abs(entityId.hashCode) % numShards
 *
 * def shardTag(baseTagName: String, shardNo: Int): String =
 *   s"\$baseTagName\$shardNo"
 * }}}
 *
 * When `AggregateEventTag.sharded[Event](numShards)` is called WITHOUT an explicit baseTagName
 * (the universal pattern in all 28 Annette entities — verified by grep), the baseTagName is
 * `implicitly[ClassTag[Event]].runtimeClass.getName` — i.e. the JVM FQN of the Event trait.
 * For a sealed trait nested in an object companion, this is `"<pkg>.<Object>\$Event"`.
 *
 * Therefore for e.g. `RoleEntity.Event` (Lagom base tag):
 *   biz.lobachev.annette.authorization.impl.role.RoleEntity\$Event
 * the per-shard tag strings are:
 *   biz.lobachev.annette.authorization.impl.role.RoleEntity\$Event0
 *   biz.lobachev.annette.authorization.impl.role.RoleEntity\$Event1
 *   ...
 *   biz.lobachev.annette.authorization.impl.role.RoleEntity\$Event9
 *
 * NOTE: there is NO separator between baseTagName and shardNo — this corrects the
 * `"entityName|n"` guess in `dev/migrate-to-pekko.md` §4.3. See `001-decisions.md` §A for
 * the Cassandra evidence.
 *
 * Usage in service slices (004-012): replace
 *   .withTagger(AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag))
 * with
 *   .withTagger { case evt => Set(Tagger.fromEventName[Event.Event](numShards = 10).tagFor(entityIdFor(evt))) }
 * where `entityIdFor(evt)` extracts the entity id from the event (typically `evt.id` or
 * `evt.entityId`; varies per entity). See `dev/migration/003-core-recipe.md` §A for guidance.
 */
final class Tagger[E] private (val baseTagName: String, val numShards: Int) {

  /** All tag strings this tagger emits (one per shard). Use this to declare SourceProvider.eventsByTags. */
  lazy val allTags: Vector[String] =
    (0 until numShards).toVector.map(shardTag)

  /** The tag for a given entity id, matching `AggregateEventShards.forEntityId(_).tag`. */
  def tagFor(entityId: String): String =
    shardTag(selectShard(entityId))

  // --- mirror of Lagom's private helpers, byte-for-byte ---

  private def selectShard(entityId: String): Int =
    Math.abs(entityId.hashCode) % numShards

  private def shardTag(shardNo: Int): String =
    s"$baseTagName$shardNo"
}

object Tagger {

  /**
   * Single-arg form, matching `AggregateEventTag.sharded[Event](numShards)` (no explicit
   * baseTagName). The Event class's runtime FQN is used as the baseTagName — exactly what
   * Lagom does for the 28 Annette entities that all call this form.
   */
  def fromEventName[E: ClassTag](numShards: Int = 10): Tagger[E] = {
    val cls = implicitly[ClassTag[E]].runtimeClass
    new Tagger[E](cls.getName, numShards)
  }

  /**
   * Two-arg form, matching `AggregateEventTag.sharded[Event](baseTagName, numShards)`.
   * None of the 28 Annette entities use this form today, but the API is here for completeness.
   * No ClassTag context bound required — the caller supplies the baseTagName directly.
   */
  def fromBaseName[E](baseTagName: String, numShards: Int = 10): Tagger[E] =
    new Tagger[E](baseTagName, numShards)
}
