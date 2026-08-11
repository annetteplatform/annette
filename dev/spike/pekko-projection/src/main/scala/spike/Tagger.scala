package spike

import scala.reflect.ClassTag

/**
 * Mirrors Lagom's `AggregateEventTag.sharded` format byte-for-byte so that events written by
 * Lagom-based Annette services remain queryable by Pekko-based ones after migration.
 *
 * Lagom source (com.lightbend.lagom.scaladsl.persistence.AggregateEventTag):
 *   def selectShard(numShards: Int, entityId: String): Int =
 *     Math.abs(entityId.hashCode) % numShards
 *   def shardTag(baseTagName: String, shardNo: Int): String =
 *     s"$baseTagName$shardNo"
 *
 * When `AggregateEventTag.sharded[Event](numShards = 10)` is called without an explicit
 * baseTagName, the baseTagName is `implicitly[ClassTag[Event]].runtimeClass.getName` — i.e.
 * the JVM FQN of the Event trait's class. For a sealed trait nested in an object companion
 * (the universal pattern in Annette entities), this is `"<pkg>.<Object>$Event"`.
 *
 * Therefore for e.g. `RoleEntity.Event` (Lagom tag base):
 *   biz.lobachev.annette.authorization.impl.role.RoleEntity$Event
 * the per-shard tag strings are:
 *   biz.lobachev.annette.authorization.impl.role.RoleEntity$Event0
 *   biz.lobachev.annette.authorization.impl.role.RoleEntity$Event1
 *   ...
 *   biz.lobachev.annette.authorization.impl.role.RoleEntity$Event9
 *
 * NOTE: there is NO separator between baseTagName and shardNo — this corrects the
 * `"entityName|n"` guess in `dev/migrate-to-pekko.md` §4.3.
 */
final class Tagger[E: ClassTag] private (val baseTagName: String, val numShards: Int) {

  /** All tag strings this tagger emits (one per shard). */
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
   * Single-arg form, matching `AggregateEventTag.sharded[Event](numShards)`.
   * Uses the Event class's runtime FQN as the baseTagName.
   */
  def fromEventName[E: ClassTag](numShards: Int = 10): Tagger[E] = {
    val cls = implicitly[ClassTag[E]].runtimeClass
    new Tagger[E](cls.getName, numShards)
  }

  /**
   * Two-arg form, matching `AggregateEventTag.sharded[Event](baseTagName, numShards)`.
   * None of the 28 Annette entities use this form today (verified by grep — all 28 call
   * the single-arg form), but the API is here for completeness.
   */
  def fromBaseName[E: ClassTag](baseTagName: String, numShards: Int = 10): Tagger[E] =
    new Tagger[E](baseTagName, numShards)
}
