# 003-core-recipe — Service-slice migration recipe

This document is the canonical recipe every service slice (004–012) follows. It explains
how to use the Pekko-variant shared core introduced by slice 003 in
`core/microservice-core-pekko/`. Each slice copies the relevant section verbatim into its
own work, then adapts to its specific entity / DAO / projection.

References:
- `dev/migration/001-decisions.md` §A — locked Tagger format
- `dev/migration/001-decisions.md` §B — locked offset-store schema
- `dev/migration/001-decisions.md` §C — locked version matrix
- `core/microservice-core-pekko/src/main/scala/biz/lobachev/annette/microservice_core/pekko/`
  — source-of-truth for all the helpers below

---

## §A. Tagger usage

Replaces Lagom's `AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag)`.

### Before (Lagom)

```scala
import com.lightbend.lagom.scaladsl.persistenceAkka.AkkaTaggerAdapter

object RoleEntity {
  sealed trait Event extends AggregateEvent[Event]
  object Event {
    val Tag: AggregateEventShards[Event] = AggregateEventTag.sharded[Event](numShards = 10)
  }
  val tagger = AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag)
  // ...
  .withTagger(tagger)
}
```

### After (Pekko)

```scala
import biz.lobachev.annette.microservice_core.pekko.event_processing.Tagger

object RoleEntity {
  sealed trait Event
  // No Event.Tag needed; the Tagger carries the baseTagName implicitly via ClassTag[Event].

  // Construct ONCE per entity (cheaper than re-deriving the FQN on every event).
  private val tagger = Tagger.fromEventName[Event](numShards = 10)

  // ... inside the Behavior:
  .withTagger { case evt => Set(tagger.tagFor(entityIdFor(evt))) }
}
```

### Where to extract the entityId

Lagom's `AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag)` internally called
`Event.Tag.forEntityId(entityContext.entityId).tag` — i.e. the **persistence-id**, which
for Lagom's `PersistenceId.ofUniqueId(entityId)` is just the raw `entityId` string.

In Pekko, the equivalent is `entityContext.entityId` (the same string). For most Annette
entities the `entityId` is the first field of every `Event` case class — conventionally
called `id`. The tagger call should be:

```scala
.withTagger {
  case evt: RoleCreated   => Set(tagger.tagFor(evt.id))
  case evt: RoleUpdated   => Set(tagger.tagFor(evt.id))
  case evt: RoleActivated => Set(tagger.tagFor(evt.id))
  // ... one case per event variant
}
```

**Important**: the entityId must be the **raw id string** (e.g. `"role-admin"`), NOT the
full persistence id (`"RoleEntity|role-admin"`). The Tagger does `Math.abs(entityId.hashCode) % numShards`
— same formula Lagom uses. Verified byte-for-byte against Lagom by Spike A (see
`dev/migration/001-decisions.md` §A).

### All-tags list (for projections)

To declare the source-provider for a projection, you need ALL tag strings (one per shard):

```scala
val allTags: Vector[String] = Tagger.fromEventName[RoleEntity.Event](numShards = 10).allTags
// Vector(
//   "biz.lobachev.annette.authorization.impl.role.RoleEntity$Event0",
//   "biz.lobachev.annette.authorization.impl.role.RoleEntity$Event1",
//   ...
//   "biz.lobachev.annette.authorization.impl.role.RoleEntity$Event9"
// )
```

One `SourceProvider.eventsByTag(...)` per tag; one Projection per tag. See §B.

---

## §B. ProjectionBase usage

Replaces Lagom's `ReadSideProcessor[Event]`. Each service slice implements a concrete
`ProjectionBase[E]` subclass per read-side handler.

### Before (Lagom)

```scala
class RoleIndexProcessor(indexDao: RoleIndexDao)(implicit ec: ExecutionContext)
  extends ReadSideProcessor[RoleEntity.Event] {
  override def aggregateTags: AggregateEventShards[RoleEntity.Event] = RoleEntity.Event.Tag
  override def buildHandler: ReadSideProcessor.ReadSideHandler[RoleEntity.Event] =
    ReadSideProcessor
      .readSideHandler[RoleEntity.Event] {
        (element, _) => element.event match {
          case evt: RoleCreated => indexDao.create(evt).map(_ => Done)
          case evt: RoleUpdated => indexDao.update(evt).map(_ => Done)
          // ...
        }
      }
      .build()
}
```

### After (Pekko)

```scala
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.projection.eventsourced.EventEnvelope
import org.apache.pekko.projection.scaladsl.Handler
import org.apache.pekko.projection.cassandra.scaladsl.CassandraProjection
import org.apache.pekko.projection.{ProjectionId, Projection}
import biz.lobachev.annette.microservice_core.pekko.event_processing.Tagger
import biz.lobachev.annette.microservice_core.pekko.projection.ProjectionBase

class RoleIndexProjection(indexDao: RoleIndexDao)(implicit
  override val system: ActorSystem[_],
  override val ec: ExecutionContext
) extends ProjectionBase[RoleEntity.Event] {

  override val projectionName: String = "role-index-projection"
  override val tags: Seq[String] = Tagger.fromEventName[RoleEntity.Event](10).allTags

  override def process(envelope: EventEnvelope[RoleEntity.Event]): Future[Done] = {
    envelope.event match {
      case evt: RoleCreated => indexDao.create(evt)
      case evt: RoleUpdated => indexDao.update(evt)
      // ...
      case _                => Future.successful(Done)
    }
  }
}
```

### Wiring projections at startup

In the service's `*Loader` or `*Module` (per slice 004–012 wiring):

```scala
val roleIndexProjection = new RoleIndexProjection(roleIndexDao)
// Create offset_store tables ONCE per service (idempotent).
roleIndexProjection.init()
// Start one ProjectionBehavior per tag.
roleIndexProjection.tags.foreach { tag =>
  ClusterSharding(system).init(
    Entity(ProjectionBehavior.Type) { _ =>
      ProjectionBehavior(roleIndexProjection.projection(tag))
    }
  )
}
```

### Restart backoff defaults (override per-projection if needed)

The default backoff from `ProjectionBase.projection(tag)` is:
- min backoff: 3 seconds
- max backoff: 30 seconds
- random factor: 0.2

Override like this for a handler with different requirements:

```scala
override def projection(tag: String): Projection[EventEnvelope[RoleEntity.Event]] = {
  super
    .projection(tag)
    .withRestartBackoff(minBackoff = 5.seconds, maxBackoff = 2.minutes, randomFactor = 0.1)
}
```

### Per-slice verification (mandatory, per 001-decisions.md §E)

Before the first boot of the migrated service:
1. Stop the running Lagom version of the service.
2. Truncate the snapshots table for this keyspace:
   ```bash
   docker exec annette-cassandra-1 cqlsh -e \
     "TRUNCATE TABLE <keyspace>.snapshots;"
   ```
3. Start the migrated service. Monitor logs for `recovery-started` / `recovery-completed`.
4. Confirm `SELECT count(*) FROM <keyspace>.snapshots;` returns 0 immediately after start.

---

## §C. CassandraDao migration

Replaces Lagom's `microservice_core.db.CassandraDao`.

### Import change

```scala
// Before:
import biz.lobachev.annette.microservice_core.db.CassandraDao
// After:
import biz.lobachev.annette.microservice_core.pekko.db.CassandraDao
```

### API diff

| Aspect | Lagom variant | Pekko variant |
|--------|---------------|---------------|
| `session` type | `com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession` | `com.datastax.driver.core.Session` (driver 3) |
| `execute(stmts*)` returns | `Future[akka.Done]` | `Future[org.apache.pekko.Done]` |
| Async write | `session.executeWrite(stmt)` | `session.executeAsync(stmt)` (Guava → Scala bridge in `CassandraDao.execute`) |
| `selectWrite` / `selectAll` | Lagom wrappers | NOT ported — use `session.executeAsync(...)` directly or Quill `ctx.run` |

### Session wiring

Pekko Persistence Cassandra uses driver 4 (`com.datastax.oss.driver.api.core.CqlSession`).
Quill 3.10 uses driver 3 (`com.datastax.driver.core.Session`). The two drivers coexist on
the classpath (different Java packages). Each service slice obtains a driver-3 Session by
connecting a `Cluster` from the same contact-points:

```scala
import com.datastax.driver.core.Cluster

lazy val cluster: Cluster = Cluster.builder()
  .addContactPoints(config.getString("cassandra.default.contact-points"): _*)
  .withCredentials(
    config.getString("cassandra.default.authentication.username"),
    config.getString("cassandra.default.authentication.password")
  )
  .build()

lazy val session: Session = cluster.connect(config.getString("cassandra.default.keyspace"))
```

Pekko's driver-4 `CqlSession` is separate (initialized by Pekko Persistence Cassandra from
`pekko.persistence.cassandra.*` config keys).

---

## §D. CassandraQuillDao migration

Replaces Lagom's `microservice_core.db.CassandraQuillDao`.

### Import change

```scala
// Before:
import biz.lobachev.annette.microservice_core.db.CassandraQuillDao
// After:
import biz.lobachev.annette.microservice_core.pekko.db.CassandraQuillDao
```

### API diff

| Aspect | Lagom variant | Pekko variant |
|--------|---------------|---------------|
| `ctx` type | `CassandraLagomAsyncContext[SnakeCase.type]` | `CassandraAsyncContext[SnakeCase.type]` |
| `ctx` constructed from | `Lagom CassandraSession` | `CassandraContextConfig` (loaded from HOCON) |
| Lagom dep | `quill-cassandra-lagom` | NOT used (module has `quill-cassandra` only) |

### Wiring

```scala
import io.getquill.CassandraContextConfig

trait RoleCassandraQuillDao extends CassandraQuillDao {
  override protected def cassandraContextConfig: CassandraContextConfig =
    CassandraContextConfig.fromConfig(
      system.settings.config.getConfig("cassandra-quill")
    )
  // where `cassandra-quill` is a HOCON block mirroring `cassandra.default`
}
```

The HOCON block typically looks like:

```hocon
cassandra-quill {
  keyspace = ${?KEYSPACE_PREFIX}authorization
  contact-points = ["localhost:9042"]
  contact-points = ${?CASSANDRA_CONTACT_POINTS}
  username = ${?CASSANDRA_USERNAME}
  password = ${?CASSANDRA_PASSWORD}
}
```

(Service slice 004 onward adds this to the service's `application.conf`.)

### CassandraQuillDaoWithAttributes (services with attribute maps)

For persons / org-structure / principal-groups / application / service-catalog / cms — same
trait name, new package:

```scala
// Before:
import biz.lobachev.annette.microservice_core.attribute.dao.CassandraQuillDaoWithAttributes
// After:
import biz.lobachev.annette.microservice_core.pekko.attribute.dao.CassandraQuillDaoWithAttributes
```

The API is identical; the only difference is the Pekko-variant parent trait and Pekko
streams under the hood.

---

## §E. Module dependency

Every service slice 004–012 adds ONE line to its project definition in `build.sbt`:

```scala
// For authorization:
def authorizationProject(pr: Project) =
  pr
    .enablePlugins(LagomScala, PekkoGrpcPlugin)
    .settings(/* ... */)
    // ADD THIS:
    .dependsOn(`authorization-api`, `microservice-core`, `microservice-core-pekko`)
```

Until slice 013, services depend on **both** `microservice-core` (for un-migrated code paths)
and `microservice-core-pekko` (for migrated ones). Slice 013 removes the Lagom-variant dep.

**Caveat**: if the same Scala file imports from BOTH `microservice_core.db.CassandraDao`
(Lagom) and `microservice_core.pekko.db.CassandraDao` (Pekko), use a rename import to
avoid collisions:

```scala
import biz.lobachev.annette.microservice_core.db.{CassandraDao => LagomCassandraDao}
import biz.lobachev.annette.microservice_core.pekko.db.CassandraDao
```

In practice each service is fully migrated in one slice, so this is rarely needed.

---

## §F. Step 8 deferral — api-gateway-core akka→pekko rename

Slice 003 spec step 8 mandated renaming `akka.*` imports to `pekko.*` in
`core/api-gateway-core/src/main/scala/`. This step was **deferred** because the api-gateway
still runs on Play 2.8.x (slice 002 blocker: Play 3.0.11 + Lagom 1.6.7 binary incompat).

The two affected files:
- `authentication/keycloak/KeycloakAuthenticator.scala`
- `authentication/keycloak/PublicKeyRequestor.scala`

These use `akka.actor.{ActorSystem, typed.*}` types that are wired by the api-gateway's
`BuiltInComponentsFromContext.actorSystem` (still `akka.actor.ActorSystem` under Play 2.8.x).
Renaming the imports without upgrading the gateway would cause type-checking failures.

Slice 013 (Lagom removal + Play upgrade) re-attempts this rename once the gateway's
`actorSystem` is `org.apache.pekko.actor.ActorSystem`.
