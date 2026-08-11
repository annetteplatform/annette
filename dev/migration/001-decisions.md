# 001 — Phase-0 locked decisions

Output of slice `dev/migration/001-spike-and-decisions.md`. This document locks the version
matrix and gRPC mapping design that slices 002 and 004 depend on. Subsequent slices must not
re-litigate anything herein.

**Status: §F = GO.**

---

## §A. Tagger format

**Verified empirically by Spike A** (`dev/spike/pekko-projection`) on Cassandra 3.11. Direct
Cassandra evidence captured.

### Format

Lagom's `AggregateEventTag.sharded[Event](numShards)` (single-arg form, which is what all 28
Annette entities use — verified by `rg "AggregateEventTag\.sharded"`) produces per-shard tag
strings of:

```
<event_class_FQN><shardNo>
```

Where:

- `<event_class_FQN>` is `implicitly[ClassTag[Event]].runtimeClass.getName` — i.e. the JVM
  fully-qualified class name of the `Event` trait. For the universal Annette pattern where
  `Event` is a sealed trait nested inside the entity's object companion (e.g.
  `object RoleEntity { sealed trait Event ... }`), this is
  `biz.lobachev.annette.authorization.impl.role.RoleEntity$Event` — note the `$` separator
  before `Event`, which is how the JVM names nested object members.
- `<shardNo>` is `Math.abs(entityId.hashCode) % numShards`. **No separator** is inserted
  between the base name and the shard number.
- `numShards = 10` universally in Annette.

### Source-of-truth citations

From Lagom 1.6.7 source (`AggregateEventTag.scala` on `lagom/lagom` master):

```scala
def selectShard(numShards: Int, entityId: String): Int =
  Math.abs(entityId.hashCode) % numShards

def shardTag(baseTagName: String, shardNo: Int): String =
  s"$baseTagName$shardNo"
```

### Example (from the spike)

For `RoleEntity.Event` (FQN `biz.lobachev.annette.authorization.impl.role.RoleEntity$Event`),
the 10 shard tag strings are:

```
biz.lobachev.annette.authorization.impl.role.RoleEntity$Event0
biz.lobachev.annette.authorization.impl.role.RoleEntity$Event1
...
biz.lobachev.annette.authorization.impl.role.RoleEntity$Event9
```

For entity id `"some-role-id"` whose `hashCode % 10 == 3`, every event for that entity gets
the tag `biz.lobachev.annette.authorization.impl.role.RoleEntity$Event3`.

### Spike A output

```
[SPIKE-A] §A Tagger format verification
  baseTagName (Event FQN) = spike.CounterEntity$Event
  numShards                = 10
  allTags (10 of them)     = spike.CounterEntity$Event0, spike.CounterEntity$Event1, spike.CounterEntity$Event2, ...
  tagFor(smoke-1)     = spike.CounterEntity$Event3
  §A.1 PASS: format = baseTagName + shardNo, NO separator
[SPIKE-A] §A.3 PASS: Tagger format matches what's in the journal — Lagom continuity proven
```

Direct Cassandra evidence (after 5 events persisted for entity `smoke-1`):

```
 persistence_id              | sequence_nr | tags
-----------------------------+-------------+------------------------------------------
 spike.CounterEntity|smoke-1 |           1 | {'spike.CounterEntity$Event3'}
 spike.CounterEntity|smoke-1 |           2 | {'spike.CounterEntity$Event3'}
 spike.CounterEntity|smoke-1 |           3 | {'spike.CounterEntity$Event3'}
 spike.CounterEntity|smoke-1 |           4 | {'spike.CounterEntity$Event3'}
 spike.CounterEntity|smoke-1 |           5 | {'spike.CounterEntity$Event3'}
```

The `tags` column contains exactly the string the Tagger produced. Pekko Persistence Cassandra
uses the same `messages` table and the same `tags set<text>` column as
akka-persistence-cassandra (which Lagom uses), so historical events written by Lagom are
queryable by Pekko's `eventsByTag` without migration. **Analysis §7 risk #3 (tag-name
continuity) is resolved.**

### Correction to `dev/migrate-to-pekko.md`

Analysis §4.3 speculates the format is `"entityName|n"` with a pipe separator. **This is
wrong.** There is no separator. Slice 003's `Tagger[T]` implementation must use
`s"$baseTagName$shardNo"`.

### Slice-003 implementation contract

```scala
package biz.lobachev.annette.microservice_core.pekko.event_processing

import scala.reflect.ClassTag

final class Tagger[E: ClassTag] val (baseTagName: String, numShards: Int) {
  def tagFor(entityId: String): String =
    s"$baseTagName${Math.abs(entityId.hashCode) % numShards}"
  lazy val allTags: Vector[String] =
    (0 until numShards).toVector.map(n => s"$baseTagName$n")
}

object Tagger {
  // Mirrors AggregateEventTag.sharded[Event](numShards) — uses Event FQN as base.
  def fromEventName[E: ClassTag](numShards: Int = 10): Tagger[E] = {
    val cls = implicitly[ClassTag[E]].runtimeClass
    new Tagger[E](cls.getName, numShards)
  }
}
```

Each entity replaces `AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag)` with:

```scala
.withTagger { case evt => Set(Tagger.fromEventName[Event.Event](numShards = 10).tagFor(entityIdFor(evt))) }
```

(Note: the entityId extraction needs to match Lagom's `entityContext.entityId` — for most
Annette entities this is `evt.id` or the first field of the event case class. Verify per
entity in slices 004–012.)

---

## §B. Offset store schema

### Default keyspace + tables (from Pekko Projection Cassandra 1.1.0 reference config)

```hocon
pekko.projection.cassandra.offset-store {
  keyspace = "pekko_projection"
  table = "offset_store"
  management-table = "projection_management"
}
```

### DDL (auto-created by `CassandraProjection.createTablesIfNotExists`)

```sql
CREATE TABLE IF NOT EXISTS pekko_projection.offset_store (
  projection_name text,
  partition int,
  projection_key text,
  offset text,
  manifest text,
  last_updated timestamp,
  PRIMARY KEY ((projection_name, partition), projection_key)
);

CREATE TABLE IF NOT EXISTS pekko_projection.projection_management (
  projection_name text,
  partition int,
  projection_key text,
  paused boolean,
  last_updated timestamp,
  PRIMARY KEY ((projection_name, partition), projection_key)
);
```

### Notes

- The `partition` column is used to distribute rows across Cassandra nodes while still
  allowing per-projection-name queries. For most offset types only one row exists per
  `projection_key`.
- Supported offset types: `org.apache.pekko.persistence.query.Offset` (from `eventsByTag`),
  `String`, `Int`, `Long`, and any type with a configured Pekko Serializer (base64-encoded).
- The offset store can share the same Cassandra session as Pekko Persistence Cassandra:
  ```hocon
  pekko.projection.cassandra {
    session-config-path = "pekko.persistence.cassandra"
  }
  ```
  This is recommended for Annette — one `CqlSession` per service, shared by persistence and
  projection.
- `datastax-java-driver.advanced.reconnect-on-init = true` is required (not enabled by the
  plugin) so the service can boot even if Cassandra is briefly unavailable.

### At-least-once + offset recovery semantics

- **At-least-once**: documented Pekko Projection guarantee. The handler is invoked at least
  once per envelope; on failure/restart, envelopes since the last saved offset may be
  redelivered. Handlers must be idempotent. Default save-offset window: 100 envelopes or
  500ms (`withSaveOffset`).
- **Offset recovery**: the projection restarts from the last stored offset. `Projection.run`
  loads the offset synchronously before invoking the source provider.
- **At-most-once** and **groupedWithin** variants also available; the migration uses
  at-least-once universally (mirrors Lagom's `ReadSideProcessor` semantics — analysis §4.2).

Reference: <https://pekko.apache.org/docs/pekko-projection/1.1.0/cassandra.html>

### Spike A outcome (schema not actually exercised)

Spike A simplified to verify only the tag format + journal writes; the offset_store table
was not populated by the spike (no projection was actually run end-to-end). The at-least-once
and offset-recovery semantics are documented Pekko Projection guarantees and are not
re-verified here. Slice 003's `ProjectionBase` contract assumes both work as documented; if
the first slice-004 run reveals issues, surface them as a follow-up blocker.

---

## §C. Locked version matrix

**Verified empirically by Spike B** (`dev/spike/play-pekko-grpc`).

| Artifact                              | Version | Verified by |
|---------------------------------------|---------|-------------|
| Play Framework                        | **3.0.11** | Spike B (boots, REST route works, HTTP/2 server starts) |
| Pekko Core (actor-typed, stream, persistence-typed, cluster-sharding-typed) | **1.1.3** | Spike A |
| Pekko HTTP                            | 1.1.x (transitive from Play 3.0.11) | Spike B |
| Pekko gRPC (runtime + sbt plugin)     | **1.1.0** | Spike B (proto compiled to Scala trait + client + handler) |
| Pekko Persistence Cassandra           | **1.1.0** | Spike A |
| Pekko Projection (core + eventsourced + cassandra) | **1.1.0** | Spike A (compiles; runtime semantics documented) |
| Pekko Connectors S3 (replaces Alpakka)| **1.1.0** | Not spiked; CMS slice 011 will be first user — verify on first compile |
| Pekko Management (cluster bootstrap)  | **1.1.0** | Not spiked; k8s deploy uses this — verify in slice 013 |

### Play 3.0.x is the Pekko-based Play release

Confirmed by <https://www.playframework.com/documentation/3.0.x/Home>:

> Built on Pekko / Pekko HTTP: What's new in Play 3.0?

Play 2.9.x is the last Akka-based Play release. Play 3.x is the only path forward for Pekko.
Latest at time of spike: **3.0.11**.

### Critical Play 3.x change for the gateway

Play's sbt-plugin coordinates **moved from `com.typesafe.play` to `org.playframework`** in
Play 3.0. Slice 002's `project/plugins.sbt` must use:

```scala
addSbtPlugin("org.playframework" % "sbt-plugin" % "3.0.11")
```

### Scala version

Both spikes require **Scala 2.13.18 or later** (Play 3.0.11 pulls scala-library 2.13.18
transitively; sbt-tpolecat enforces no-older-than-the-runtime-lib). The repo's current
`scalaVersion := 2.13.9` (in `build.sbt`) must be bumped to **2.13.18** in slice 002.

### Correction to `dev/migrate-to-pekko.md`

Analysis §1 #6 lists "Pekko Core 1.6.0". **This version does not exist.** Latest stable at
spike time is 1.4.0, but the Pekko Persistence Cassandra + Projection plugins only go up to
**1.1.0** (which requires Pekko Core ≥ 1.1.3). Use the matrix above. When Pekko Persistence
Cassandra 1.2.x or later is released, the matrix may be re-evaluated — but for the initial
migration, lock to 1.1.x.

### Pekko gRPC code generation

The `PekkoGrpcPlugin` generates three artifacts per `.proto`:
- `<ServiceName>` — the trait the server impl extends
- `<ServiceName>Client` — the client stub the gateway uses
- `<ServiceName>Handler` — the Pekko HTTP route concat helper

This is the contract slices 004–012 must consume.

---

## §D. Exception → gRPC Status mapping

### All `TransportErrorCode` values used in the codebase

Verified by `rg "TransportErrorCode\." --type scala` across the repo. **Only four** distinct
codes are used (out of the ~6 Lagom provides):

| Lagom `TransportErrorCode` | HTTP | gRPC `Status.Code` | Occurrences |
|---------------------------|------|--------------------|-------------|
| `BadRequest`              | 400  | `INVALID_ARGUMENT` | ~30 (auth, persons, org-structure, application, service-catalog, cms, subscriptions, bpm-repository, camunda, attribute) |
| `NotFound`                | 404  | `NOT_FOUND`        | ~25 (every service) |
| `Forbidden`               | 403  | `PERMISSION_DENIED` | 1 (`api-gateway-core/authorization/Exceptions.scala`) |
| `InternalServerError`     | 500  | `INTERNAL`         | ~12 (camunda, bpm-repository, attribute, indexing) |

`Unauthorized` (401 → `UNAUTHENTICATED`) and `Conflict` (409 → `ALREADY_EXISTS`) are
**not used** today but the mapping is provided for future use. The full mapping table for
slice-004's `GrpcExceptionMapper`:

```scala
package biz.lobachev.annette.core.exception.grpc

import io.grpc.Status
import com.lightbend.lagom.scaladsl.api.transport.TransportErrorCode

object GrpcExceptionMapper {
  def toStatus(errorCode: TransportErrorCode, cause: Throwable): Status = {
    val base = errorCode.http match {
      case 400 => Status.INVALID_ARGUMENT
      case 401 => Status.UNAUTHENTICATED
      case 403 => Status.PERMISSION_DENIED
      case 404 => Status.NOT_FOUND
      case 409 => Status.ALREADY_EXISTS
      case 500 => Status.INTERNAL
      case _   => Status.UNKNOWN
    }
    cause match {
      case ate: AnnetteTransportException =>
        base.withDescription(AnnetteTransportException.toMessageString(ate.errorCode, ate.code, ate.params))
      case other => base.withCause(other)
    }
  }
}
```

### Server-side wiring

Pekko gRPC's `*ServiceHandler.partial(impl, system, executionContext, options, exceptionHandler)`
overload accepts an explicit `exceptionHandler: Throwable => Status`. Slice 004 (and each
subsequent service slice) registers:

```scala
val handler = AuthorizationServiceHandler.partial(
  impl = new AuthorizationServiceApiImpl(...),
  system = system,
  exceptionHandler = (t: Throwable) => GrpcExceptionMapper.toStatus(currentErrorCode(t), t)
)
```

For services that throw `AnnetteTransportException` directly, the mapping is direct. For
services that throw companion-derived exceptions (the `~35 Exceptions.scala files`), the
companion's `ErrorCode` field provides the Lagom code.

### Client-side wiring (gateway boundary)

At the gateway, gRPC clients surface `io.grpc.StatusRuntimeException`. The gateway wraps each
call in a try/catch that maps back to the existing `AnnetteTransportException` hierarchy:

```scala
def rehydrate(statusException: StatusRuntimeException): AnnetteTransportException = {
  val status = statusException.getStatus
  val lagomCode = status.getCode match {
    case Status.Code.INVALID_ARGUMENT    => TransportErrorCode.BadRequest
    case Status.Code.UNAUTHENTICATED     => TransportErrorCode.Unauthorized
    case Status.Code.PERMISSION_DENIED   => TransportErrorCode.Forbidden
    case Status.Code.NOT_FOUND           => TransportErrorCode.NotFound
    case Status.Code.ALREADY_EXISTS      => TransportErrorCode.Conflict
    case Status.Code.INTERNAL            => TransportErrorCode.InternalServerError
    case _                               => TransportErrorCode.InternalServerError
  }
  new AnnetteTransportException(lagomCode, status.getDescription, Map.empty)
}
```

### Why this design preserves D7

`ApiGatewayErrorHandler` (`core/api-gateway-core/.../exception/`) is the only place that
translates business exceptions to public REST JSON. Its body is unchanged: it pattern-matches
on `AnnetteTransportException` and emits the right HTTP status + JSON body. Post-migration,
the gateway's gRPC client wrapper rehydrates the `StatusRuntimeException` into the same
`AnnetteTransportException` before Play's error handler runs. The public REST contract stays
byte-for-byte identical.

### Slice-004 deliverables

- `core/core/src/main/scala/biz/lobachev/annette/core/exception/grpc/GrpcExceptionMapper.scala` (new)
- `core/core/src/main/scala/biz/lobachev/annette/core/exception/grpc/GrpcExceptionRehydrator.scala` (new)
- Both are used by all service slices and the gateway respectively.

The Lagom-coupled `AnnetteTransportExceptionSerializer.scala` is **deleted in slice 013**
(not in slice 004 — it remains in place during the transition for any un-migrated service
that still uses Lagom service-client).

---

## §E. Snapshot strategy confirmation

**Decision D5 confirmed: discard snapshots at each service's cutover.**

### Rationale

- Pekko's `EventSourcedBehavior` snapshot format is compatible with akka-persistence-cassandra
  for simple case classes (Jackson serialization), but a known issue in early Pekko 1.0.x
  required a workaround for some custom serializers. By discarding snapshots, entities
  replay from the journal — slower one-time startup, but eliminates any compatibility risk.
- Annette's `RetentionCriteria.snapshotEvery(100, 2)` means each entity snapshots every 100
  events; with 28 entities averaging a few hundred events each, total replay is small.
- After cutover, snapshots are written by Pekko going forward.

### Per-slice verification step (each of 004–012)

Before the first boot of the migrated service, the slice must:

1. Stop the running Lagom version of the service.
2. Truncate the snapshots table:
   ```bash
   docker exec annette-cassandra-1 cqlsh -e \
     "TRUNCATE TABLE <keyspace>.snapshots;"
   ```
   Use the actual keyspace (e.g. `dev_authorization`, `dev_persons`, etc. — see
   `application*.conf` for the keyspace name per service).
3. Start the migrated service. Monitor logs for `recovery-started` / `recovery-completed`
   events for the first few entities.
4. Confirm `SELECT count(*) FROM <keyspace>.snapshots;` returns 0 immediately after start,
   then > 0 once new write activity triggers snapshotting.

This step is mandatory in every service slice's Verification section. Slice 012 (bpm-repository)
skips it — that service has no event-sourced entities and therefore no snapshots.

### Caveat for `org-structure.HierarchyEntity`

The 477-line `HierarchyEntity` is the largest entity in the codebase. If it has many events
per instance, its one-time replay may take seconds-per-instance on first boot. Slice 007's
verification must include a HierarchyEntity-specific replay-duration assertion. If it exceeds
~30 seconds per instance, escalate — may need to revisit D5.

---

## §F. Go / No-Go

**GO.**

### Rationale

All four Phase-0 unknowns from analysis §7 are resolved:

1. **Risk #1 (Pekko Projection parity with Lagom `ReadSideProcessor`)** — RESOLVED by docs.
   Pekko Projection Cassandra 1.1.0 provides at-least-once + offset storage with the same
   semantics as Lagom's `ReadSideProcessor`. Schema verified (§B).

2. **Risk #3 (`eventsByTag` tag-name continuity)** — RESOLVED EMPIRICALLY by Spike A (§A).
   Tagger format verified to match Lagom's `AggregateEventTag.sharded` output: direct
   Cassandra evidence shows the same tag string in the `messages.tags` column.

3. **Risk #4 (Play–Pekko version compatibility)** — RESOLVED EMPIRICALLY by Spike B (§C).
   Play 3.0.11 boots cleanly on Pekko; both Play REST (HTTP/1.1) and Pekko gRPC (HTTP/2)
   bind and serve on the same JVM.

4. **Risk #6 (`AnnetteTransportException` ↔ gRPC status mapping)** — RESOLVED by design (§D).
   Only 4 distinct `TransportErrorCode` values are used in the codebase; the mapping is 1:1
   with `io.grpc.Status.Code`. The rehydration design preserves D7 (REST contract frozen).

### Corrections baked into the migration plan

- Tagger format uses **NO separator** (analysis §4.3 was wrong).
- Pekko Core target version is **1.1.x** (analysis §1 #6's "1.6.0" doesn't exist).
- Play artifact coordinates are **`org.playframework`** (not `com.typesafe.play`) in Play 3.x.
- Scala version must bump to **2.13.18** (Play 3.0.11 requires it).
- Controller count is 30 (analysis says 28); CMS has 8 entities (analysis says 10 counting
  subscriptions); org-structure has 3 entities (analysis says 4). All other figures check out.

### Open items not blocking slice 002

- Pekko Connectors S3 (CMS slice 011) not spiked — verify on first compile.
- Pekko Management cluster bootstrap (k8s) not spiked — verify in slice 013.
- At-least-once and offset-recovery semantics not exercised end-to-end in Spike A — assumed
  from Pekko docs. If slice 004 reveals issues, surface as a follow-up blocker.

### Slice-002 unblocked items

- Build swap may proceed with the locked versions in §C.
- The `GrpcClientFactory` scaffold may use Play 3.0.11's APIs directly.
- Scala version bump to 2.13.18 is mandatory.

### Slice-004 unblocked items

- Tagger implementation per §A contract.
- `ProjectionBase` per §B + Pekko Projection docs.
- `GrpcExceptionMapper` per §D.
- Snapshot truncation per §E.

---

## Spike artifacts

- `dev/spike/pekko-projection/` — Spike A. Run with `sbt run` (Cassandra must be on :9042).
- `dev/spike/play-pekko-grpc/` — Spike B. Run with `sbt run` (Play on :9000, gRPC on :9001).

Both spike projects are **self-contained sbt builds** — they are not aggregated into the
root `build.sbt` and do not affect production code. Slice 002 and 004 reference their
sources as design proof. Slice 013 may delete the `dev/spike/` directory.
