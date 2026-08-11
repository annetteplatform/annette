# 003 — Shared core rewrite (microservice-core)

## Role

You are a senior Scala/Pekko engineer performing one vertical slice of the Annette Platform
migration off Lagom/Akka to Apache Pekko + Pekko gRPC. You execute against the working tree
of the repository directly.

## Prerequisites

- Branch `feature/migrate-to-pekko` is checked out.
- Step 002 is already committed:
  ```bash
  git log --oneline | grep -E "build\(\*\): swap sbt plugins"
  ```
- Repo state at start of this step: build accepts both Lagom and Pekko plugins; gateway has
  `GrpcClientFactory` scaffold; dev-mode port table is in every `application.dev.conf`. No
  service has been migrated. All 9 microservices still boot under Lagom. All 20 specs pass.
- External services running: Cassandra 3.11 on `:9042`, OpenSearch 2.8 on `:9200`.

## Goal

Rewrite `core/microservice-core` so it compiles cleanly against Pekko, while introducing a
**parallel** `microservice-core-pekko` module that un-migrated services can depend on to keep
compiling against Lagom during the transition. This is the foundation every service slice
(004–012) consumes. The platform must still boot end-to-end on Lagom after this step.

## Files in scope

**New module:**
- `core/microservice-core-pekko/` — entire new sub-project. Add to `build.sbt` aggregate.
- `core/microservice-core-pekko/src/main/scala/biz/lobachev/annette/microservice_core/pekko/db/CassandraDao.scala`
- `core/microservice-core-pekko/src/main/scala/biz/lobachev/annette/microservice_core/pekko/db/CassandraQuillDao.scala`
- `core/microservice-core-pekko/src/main/scala/biz/lobachev/annette/microservice_core/pekko/db/QuillEncoders.scala`
- `core/microservice-core-pekko/src/main/scala/biz/lobachev/annette/microservice_core/pekko/db/CassandraTableBuilder.scala`
- `core/microservice-core-pekko/src/main/scala/biz/lobachev/annette/microservice_core/pekko/event_processing/SimpleEventHandling.scala`
- `core/microservice-core-pekko/src/main/scala/biz/lobachev/annette/microservice_core/pekko/event_processing/Tagger.scala`
- `core/microservice-core-pekko/src/main/scala/biz/lobachev/annette/microservice_core/pekko/projection/ProjectionBase.scala`
- `core/microservice-core-pekko/src/main/scala/biz/lobachev/annette/microservice_core/pekko/attribute/dao/CassandraQuillDaoWithAttributes.scala`
  (port of the existing `microservice-core` file at the same relative path)
- `core/microservice-core-pekko/src/test/scala/.../TaggerSpec.scala` — verifies the tagger
  format matches `001-decisions.md` §A.

**Existing module (untouched sources, build-only edits):**
- `build.sbt` — add `lazy val microservice-core-pekko = ...` project definition; aggregate it
  into root.

**Existing `core/` modules — mechanical package-rename only (no Lagom removal):**
- `core/api-gateway-core/src/main/scala/biz/lobachev/annette/api_gateway_core/authentication/keycloak/PublicKeyRequestor.scala`
- Every file under `core/api-gateway-core/src/main/scala/**` whose imports reference `akka.*`
  (rename to `pekko.*`). Run:
  ```bash
  rg -l "^import akka\." core/api-gateway-core/src/main/scala/
  ```
  and apply the mechanical rename to each file in that list.

**Documentation:**
- `dev/migration/003-core-recipe.md` — new. A 1-page recipe every service slice (004–012)
  copies: how to use `Tagger`, `ProjectionBase`, `CassandraDao` (Pekko variant),
  `CassandraQuillDao` (Pekko variant), and how to write a Projection handler.

No other files. In particular, **do not** modify any service's source — slices 004–012 do that.

## Locked assumptions (do not re-decide)

- **D4** Pekko Projection Cassandra offset store.
- **D5** Snapshots discarded at each service's cutover.
- The tagger format is locked in `dev/migration/001-decisions.md` §A. This step's `Tagger`
  must produce that exact format.
- **A4** One commit per step.
- Scala 2.13.x (do not migrate to Scala 3 — analysis §H anti-pattern).

## Steps

### 1. New module scaffold

In `build.sbt`, add the new sub-project per analysis §5.4 + §8 Phase 1:

```scala
lazy val `microservice-core-pekko` = (project in file("core/microservice-core-pekko"))
  .settings(annetteSettings: _*)
  .settings(scalacOptions += "-Wconf:cat=unused-nowarn:s")
  .settings(libraryDependencies ++= Dependencies.pekkoCore
    ++ Dependencies.pekkoPersistenceCassandra
    ++ Dependencies.pekkoProjection
    ++ Dependencies.quill
    ++ Dependencies.elastic
    ++ Dependencies.chimney
    ++ Dependencies.pureConfig
    ++ Dependencies.tests)
  .dependsOn(`core`)
```

Add `microservice-core-pekko` to the root `.aggregate(...)` list. Do **not** make any service
depend on it yet — slices 004–012 add `.dependsOn(microservice-core-pekko)` as they migrate.

### 2. `Tagger[T]` helper

Per analysis §4.3 and the format locked in `001-decisions.md` §A, author:

```scala
package biz.lobachev.annette.microservice_core.pekko.event_processing

object Tagger {
  def apply[E](entityName: String, numShards: Int = 10): E => Set[String] = { event =>
    val entityId = ??? // extract from event via a typeclass or pass entityId explicitly
    val shard = math.abs(entityId.hashCode % numShards)
    Set(s"$entityName|$shard")
  }
}
```

The exact format string from `001-decisions.md` §A wins. If the spike determined Lagom uses a
different separator or shard-zero-padding, follow that — do not improvise.

**Important:** the function signature must accept the **entityId** (not the event) so the
shard hash matches Lagom's behavior. Inspect an existing entity's
`PersistenceId.ofUniqueId(entityId)` extraction and mirror it. Document the chosen signature
in `dev/migration/003-core-recipe.md` §A.

### 3. `CassandraDao` (Pekko variant)

Per analysis §5.4 #1, author `core/microservice-core-pekko/.../db/CassandraDao.scala`:

- `val session: CqlSession` (DataStax driver — `com.datastax.oss.driver.api.core.CqlSession`).
- Replace `session.selectWrite(...)` / `session.composeStatement(...)` patterns with
  `session.executeAsync(...).asScala` / `session.prepareAsync(...).asScala`.
- Replace `Source.single(stmt).mapAsync(...)` patterns with `Future.sequence(stmts.map(session.executeAsync(_).asScala))`.
- Keep the same method names (`executeWrite`, `selectAll`, etc.) so service-side DAOs have a
  1:1 migration. The method bodies change; the call sites in slices 004–012 stay nearly
  identical.

### 4. `CassandraQuillDao` (Pekko variant)

Per analysis §3.5 and §5.4 #2:

- `lazy val ctx: CassandraAsyncContext[SnakeCase.type] = new CassandraAsyncContext(SnakeCase, session)`
  where `session: CqlSession`.
- Drop `quill-cassandra-lagom` from this module's dependencies (already done in step 1 via
  `Dependencies.quill` which still contains `quill-cassandra-lagom` — remove just the lagom
  artifact from the list passed to this module; slice 013 removes it from `Dependencies.quill`
  entirely).

### 5. `SimpleEventHandling` (Pekko variant)

Per analysis §5.4 #3, author `event_processing/SimpleEventHandling.scala`:

- Drop `import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, ...}`.
- The trait's `handle[T]` and `batchHandle[T]` now take a plain event of type `T` (or a
  `ProjectionContext[T]` carrying `event` + `eventMetadata`), returning
  `T => Future[List[BoundStatement]]`.
- Mirror the original's API surface so slice handlers (004–012) can swap
  `EventStreamElement[T].event` → `envelope.event` mechanically.

### 6. `ProjectionBase`

Per analysis §5.4 and §4.2, author `projection/ProjectionBase.scala`:

- A trait providing the boilerplate every Projection handler needs: `def groupId: String`,
  `def process(envelope: EventEnvelope[E]): Future[Done]`, an `actorSystem` and
  `projectionContext` in implicit scope, and a `def start(projectionId: ProjectionId,
  sourceProvider: SourceProvider[Offset, EventEnvelope[E]], handler: Handler[EventEnvelope[E]]): Future[Done]`
  that calls `Projection(...).withRestartBackoff(RestartSettings(...)).run(...)`.
- Document the restart-backoff defaults in `dev/migration/003-core-recipe.md` §B so service
  slices can override per-handler if needed.

### 7. Port `CassandraQuillDaoWithAttributes`

Copy `core/microservice-core/src/main/scala/biz/lobachev/annette/microservice_core/attribute/dao/CassandraQuillDaoWithAttributes.scala`
to `core/microservice-core-pekko/src/main/scala/biz/lobachev/annette/microservice_core/pekko/attribute/dao/CassandraQuillDaoWithAttributes.scala`,
adapting the `ctx` to the Pekko variant's `CassandraAsyncContext`. Used by services whose
entities carry attribute maps (persons, org-structure, principal-groups, application,
service-catalog, cms).

### 8. `core/api-gateway-core` mechanical rename

Per analysis §3.3 (the `akka.*` → `pekko.*` renames are config-only at the actor level but
source-level for imports):

- For every file under `core/api-gateway-core/src/main/scala/**` that imports `akka.*`,
  rename imports to `pekko.*`. The relevant files include (run the grep above to enumerate):
  - `authentication/AuthenticatedAction.scala`, `AuthenticatedRequest.scala`,
    `BearerAuthenticator.scala`, etc. (anything using `ActorSystem`, `Materializer`,
    `ExecutionContext` from `akka.*`).
  - `authentication/keycloak/PublicKeyRequestor.scala` — the one custom typed actor
    (analysis §3.3: 1 actor, 92 lines). Rename `akka.actor.typed.*` → `pekko.actor.typed.*`.
  - `exception/ApiGatewayErrorHandler.scala` if it imports `akka.*` (it imports Lagom's
    `TransportException` — keep that import; Lagom is still on the classpath in this step).
- Do **not** rename Lagom imports (`com.lightbend.lagom.*`) — slice 013 handles those.
- Do **not** touch the `core/core/` module in this step. Its exception hierarchy uses Lagom's
  `TransportErrorCode` and remains Lagom-coupled until slice 013.

### 9. Recipe document

Author `dev/migration/003-core-recipe.md` with sections:

- **§A. Tagger usage** — code example: how a service slice replaces
  `AkkaTaggerAdapter.fromLagom(ctx, Event.Tag)` with `Tagger("EntityName", numShards = 10)`.
  Note where to extract the entityId from the entity context.
- **§B. ProjectionBase usage** — code example: how to convert a `ReadSideProcessor[Event]`
  into a `ProjectionBase` subclass, including how to declare the `SourceProvider.eventsByTag`
  with the tag names from `Tagger`.
- **§C. CassandraDao migration** — the API diff between the Lagom and Pekko variants.
- **§D. CassandraQuillDao migration** — the API diff (just the `ctx` construction).
- **§E. Module dependency** — the one-line `.dependsOn(microservice-core-pekko)` addition
  every service slice makes.

## Verification

- `sbt clean compile` — all 31 aggregated projects compile, including the new
  `microservice-core-pekko`.
- `sbt 'project microservice-core-pekko' test` — the `TaggerSpec` passes, asserting the
  tagger output matches the format locked in `001-decisions.md` §A.
- `sbt test` — the existing 20 specs still pass. No service has been migrated yet, so all
  behavior is Lagom-side and unchanged.
- `cd deploy/docker && ./deploy.sh && ./run-local.sh` — the platform still boots under Lagom.
  Confirm one service starts cleanly (e.g. `authorization`).
- Commit only after all of the above pass.

## Commit

Stage only the files listed in "Files in scope". Commit message:

```
feat(microservice-core-pekko): add pekko-variant shared core [003]
```

Verify in later slices:
```bash
git log --oneline | grep -E "feat\(microservice-core-pekko\): add pekko-variant"
```

## Out of scope

- Do **not** migrate any service (slices 004–012).
- Do **not** modify `core/microservice-core/` (the existing Lagom variant). Both modules
  coexist until slice 013 deletes the Lagom one.
- Do **not** modify `core/core/` — its exception hierarchy remains Lagom-coupled. Slice 013
  rewrites it.
- Do **not** delete `quill-cassandra-lagom` from `Dependencies.quill` (slice 013).
- Do **not** change any service's `application*.conf` (slices 004–012).
- Do **not** swap any gateway `serviceClient.implement[T]` (slices 004–012).

## References

- `dev/migrate-to-pekko.md` §3.5 — the `quill-cassandra-lagom` coupling.
- `dev/migrate-to-pekko.md` §4.2 — read-side translation (Pekko Projection shape).
- `dev/migrate-to-pekko.md` §4.3 — entity translation (tagger replacement).
- `dev/migrate-to-pekko.md` §5.4 — full Cassandra/Quill layer plan.
- `dev/migrate-to-pekko.md` §6 — dependency substitution table.
- `dev/migrate-to-pekko.md` §8 Phase 1 — the shared-core phase.
- `dev/migrate-to-pekko.md` §11 — file-count reality check (`microservice-core` shared = ~8 files).
- `dev/migration/001-decisions.md` §A — locked tagger format string.
- `dev/migration/001-decisions.md` §B — locked offset-store schema.
