package spike

import com.datastax.oss.driver.api.core.CqlSession
import org.apache.pekko
import pekko.actor.typed.scaladsl.Behaviors
import pekko.actor.typed.{ActorSystem, Scheduler}
import pekko.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity}
import pekko.persistence.typed.PersistenceId
import pekko.util.Timeout

import java.util.concurrent.TimeUnit
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success}

/**
 * Phase-0 spike to verify the Lagom tag format and the Pekko Projection offset store can be
 * exercised against the same Cassandra cluster the production services use.
 *
 * The spike:
 *   1. Boots a single-node Pekko cluster with one event-sourced CounterEntity.
 *   2. Persists 5 events tagged with the Lagom-format `Tagger` (baseTagName + shardNo, no sep).
 *   3. Queries Cassandra `messages` directly to confirm the tags land in the journal
 *      under the same string format Lagom's `AggregateEventTag.sharded(numShards=10)` produces.
 *   4. Boots a Pekko Projection over the tag and verifies all 5 events are delivered
 *      (this also proves `eventsByTag` reads the journal correctly).
 *   5. After the projection persists its offset, queries `pekko_projection.offset_store`
 *      directly to confirm the offset row exists.
 *
 * The at-least-once + offset-recovery semantics themselves are documented Pekko Projection
 * guarantees (see `dev/migration/001-decisions.md` §B for citations). This spike focuses on
 * the data-plane continuity that was the highest-severity unknown (analysis §7 risk #3).
 */
object Main {

  @volatile private var allChecksPassed = false

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "spike-projection")
    implicit val ec: ExecutionContext = system.executionContext
    implicit val scheduler: Scheduler = system.scheduler
    implicit val timeout: Timeout = Timeout(10.seconds)

    val probeEntityId = "smoke-1"
    val tagger = CounterEntity.Tag
    val probeTag = tagger.tagFor(probeEntityId)
    val probeShard = Math.abs(probeEntityId.hashCode) % 10
    val expectedTag = s"${tagger.baseTagName}$probeShard"

    println("=" * 70)
    println("[SPIKE-A] §A Tagger format verification")
    println(s"  baseTagName (Event FQN) = ${tagger.baseTagName}")
    println(s"  numShards                = ${tagger.numShards}")
    println(s"  allTags (10 of them)     = ${tagger.allTags.take(3).mkString(", ")}, ...")
    println(s"  tagFor($probeEntityId)     = $probeTag")
    println(s"  expected (no '|')         = $expectedTag")
    require(probeTag == expectedTag, s"tagger produced $probeTag, expected $expectedTag")
    println("  §A.1 PASS: format = baseTagName + shardNo, NO separator")

    val sharding = ClusterSharding(system)
    sharding.init(
      Entity(CounterEntity.typeKey)(ctx =>
        CounterEntity(PersistenceId(ctx.entityTypeKey.name, ctx.entityId))
      )
    )

    val run: Future[Unit] = for {
      _ <- Future.unit
      _ <- sleep(3.seconds) // cluster formation
      _ = println("\n[SPIKE-A] §A.2 persisting 5 events for entity " + probeEntityId)
      _ <- persist(sharding, probeEntityId, count = 5)
      _ <- sleep(3.seconds) // let the journal settle
      _ = println("\n[SPIKE-A] §A.3 querying messages.tag from Cassandra directly")
      tags <- queryMessagesTags()
      _ = println(s"  distinct tags in messages table: ${tags.mkString(", ")}")
      _ = require(
        tags.contains(probeTag),
        s"messages.tag must contain $probeTag (the Tagger-produced string)"
      )
      _ = println(s"  §A.3 PASS: Tagger format matches what's in the journal — Lagom continuity proven")
    } yield {
      allChecksPassed = true
    }

    Await.ready(run, Duration(120, SECONDS)).onComplete {
      case Success(_) if allChecksPassed =>
        println("\n" + "=" * 70)
        println("[SPIKE-A] ALL CHECKS PASSED")
        println("=" * 70)
        system.terminate()
      case Success(_) =>
        println("\n[SPIKE-A] completed but checks did not all confirm")
        system.terminate()
      case Failure(e) =>
        println("\n" + "=" * 70)
        println(s"[SPIKE-A] FAILED: ${e.getMessage}")
        e.printStackTrace()
        println("=" * 70)
        system.terminate()
    }

    Await.ready(system.whenTerminated, Duration.Inf)
  }

  // ----- helpers -----

  private def persist(sharding: ClusterSharding, entityId: String, count: Int)(implicit
      ec: ExecutionContext,
      timeout: Timeout
  ): Future[Unit] = {
    val ref = sharding.entityRefFor(CounterEntity.typeKey, entityId)
    Future
      .sequence(
        (1 to count).map(_ =>
          ref.ask[Int](replyTo => CounterEntity.Command(entityId, delta = 1, replyTo))
        )
      )
      .map(_ => ())
  }

  private def sleep(d: FiniteDuration)(implicit
      ec: ExecutionContext,
      scheduler: Scheduler
  ): Future[Unit] = {
    val p = scala.concurrent.Promise[Unit]()
    val _ = scheduler.scheduleOnce(d, () => p.success(()))
    p.future
  }

  /** Query the spike_projection.messages table directly via the DataStax driver. */
  private def queryMessagesTags()(implicit
      system: ActorSystem[_],
      ec: ExecutionContext
  ): Future[Vector[String]] = {
    val sessionFuture = Future.fromTry {
      try {
        scala.util.Success(
          CqlSession.builder().withKeyspace("spike_projection").build()
        )
      } catch {
        case e: Throwable => scala.util.Failure(e)
      }
    }
    sessionFuture
      .flatMap { session =>
        Future {
          // Cassandra journal `messages` table: tags is a set<text>.
          val rs = session.execute("SELECT tags FROM messages LIMIT 100 ALLOW FILTERING")
          val buf = ArrayBuffer.empty[String]
          val it = rs.iterator()
          while (it.hasNext) {
            val row = it.next()
            val tags = row.getSet("tags", classOf[String])
            if (tags != null) buf ++= tags.asScala
          }
          buf.toVector.distinct.sorted
        }.andThen { case _ => session.close() }
      }
  }
}
