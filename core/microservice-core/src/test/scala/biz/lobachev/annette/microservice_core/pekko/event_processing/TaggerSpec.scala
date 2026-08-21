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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * Verifies `Tagger` reproduces Lagom's `AggregateEventTag.sharded[Event](numShards)`
 * byte-for-byte, per `dev/migration/001-decisions.md` §A (the format Spike A verified
 * empirically against Cassandra 3.11).
 *
 * These assertions are a regression guard: if the format drifts, Pekko-based services would
 * stop finding events written by Lagom-based ones (and vice versa).
 */
class TaggerSpec extends AnyWordSpec with Matchers {

  "A Tagger constructed via fromEventName" should {

    "use the Event trait's runtime class FQN as the baseTagName" in {
      val tagger = Tagger.fromEventName[TaggerSpecFixture.Event](numShards = 10)
      // For a sealed trait nested in an object, the JVM FQN ends with `$Event`.
      tagger.baseTagName shouldEqual "biz.lobachev.annette.microservice_core.pekko.event_processing.TaggerSpecFixture$Event"
    }

    "produce 10 distinct tag strings for numShards = 10" in {
      val tagger = Tagger.fromEventName[TaggerSpecFixture.Event](numShards = 10)
      tagger.allTags should have size 10L
      tagger.allTags.toSet should have size 10L
    }

    "format tags as baseTagName + shardNo with NO separator (per §A)" in {
      val tagger = Tagger.fromEventName[TaggerSpecFixture.Event](numShards = 10)
      val base   = tagger.baseTagName
      tagger.allTags shouldEqual (0 until 10).toVector.map(i => s"$base$i")
      // Negative assertion: never use a separator like '|' or ':'.
      tagger.allTags.foreach { t =>
        t should not include "|"
        t should not include ":"
      }
    }

    "assign the same shard to entity IDs that hash to the same bucket (Lagom-compatible)" in {
      val tagger = Tagger.fromEventName[TaggerSpecFixture.Event](numShards = 10)
      val base   = tagger.baseTagName

      // Reproduce the Lagom shard-selection formula locally.
      def expectedShard(entityId: String): Int = Math.abs(entityId.hashCode) % 10
      def expectedTag(entityId: String): String = s"$base${expectedShard(entityId)}"

      // Cross-check against several known entity IDs (mirrors the smoke-1 case from Spike A).
      Seq("smoke-1", "role-admin", "person-P0001", "x", "an-arbitrary-long-id-12345").foreach { id =>
        tagger.tagFor(id) shouldEqual expectedTag(id)
      }
    }

    "reproduce the exact Spike A evidence (entity `smoke-1` → shard 3)" in {
      val tagger = Tagger.fromEventName[SpikeAFormat.Event](numShards = 10)
      val base   = tagger.baseTagName
      // The spike's CounterEntity.Event had base `spike.CounterEntity$Event`; entity `smoke-1`
      // hashed to shard 3 — verified by direct Cassandra SELECT in 001-decisions.md §A.
      // We can't use the spike's class here, but we can recompute what the spike's tagger would
      // have produced for `smoke-1` against this spec's own fixture Event class and confirm
      // the shard for `smoke-1` is the same shard Lagom's `Math.abs("smoke-1".hashCode) % 10`
      // produces — which is 3.
      val expectedShardForSmoke1 = Math.abs("smoke-1".hashCode) % 10
      expectedShardForSmoke1 shouldEqual 3
      tagger.tagFor("smoke-1") shouldEqual s"$base$expectedShardForSmoke1"
    }
  }

  "A Tagger constructed via fromBaseName" should {

    "use the supplied baseTagName verbatim" in {
      val tagger = Tagger.fromBaseName[TaggerSpecFixture.Event](
        baseTagName = "com.example.FooEntity$Event",
        numShards = 4
      )
      tagger.allTags shouldEqual Vector(
        "com.example.FooEntity$Event0",
        "com.example.FooEntity$Event1",
        "com.example.FooEntity$Event2",
        "com.example.FooEntity$Event3"
      )
    }
  }
}

/** Fixture sealed trait nested in an object — mirrors the universal Annette entity pattern. */
object TaggerSpecFixture {
  sealed trait Event
}

/** Mirror of the spike's CounterEntity.Event shape — for the smoke-1 shard-3 regression check. */
object SpikeAFormat {
  sealed trait Event
}
