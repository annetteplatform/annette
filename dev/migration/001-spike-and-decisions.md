# 001 — Spike & locked decisions

## Role

You are a senior Scala/Pekko engineer performing Phase 0 of the Annette Platform migration off
Lagom/Akka to Apache Pekko + Pekko gRPC. You execute against the working tree of the repository
directly. This step produces **design outputs only** — no production source is changed.

## Prerequisites

- Branch `feature/migrate-to-pekko` is checked out (the long-lived migration branch).
- Repo state at start of this step: clean tree, on the upstream `master`/`main`. No prior
  migration commits exist. Verify with:
  ```bash
  git status --porcelain                      # must be empty
  git log --oneline -1                        # baseline commit
  ```
- External services running: Cassandra 3.11 on `localhost:9042`, OpenSearch 2.8 on `:9200`.
  Bring them up if down: `cd deploy/docker && ./deploy.sh`.
- A scratch Cassandra keyspace you can destroy is available (e.g. `KEYSPACE_PREFIX=spike_`).

## Goal

De-risk the four highest-severity unknowns from the analysis by running focused code spikes in
a throwaway location, then write a decisions document that locks the Pekko/Play/Pekko-gRPC
version matrix and the gRPC exception-mapping design. Subsequent steps (002 and 004) cannot
proceed without these outputs. The deliverable is one document plus one self-contained Scala
PoC project; nothing in the main build changes.

## Files in scope

- `dev/spike/pekko-projection/` — new throwaway sbt sub-project (create). One self-contained
  PoC. Do **not** wire it into the root `build.sbt` aggregate.
- `dev/spike/play-pekko-grpc/` — new throwaway sbt sub-project (create). Minimal Play app
  binding one Pekko gRPC route.
- `dev/migration/001-decisions.md` — new (create). The locked decisions document.

No other files.

## Locked assumptions (do not re-decide)

- **A1** Public REST contract frozen. (Not exercised in this spike but re-state for completeness.)
- **D4** Read-side offset store = Pekko Projection Cassandra offset store.
- **D5** Snapshots discarded at each service's cutover; entities replay from the journal.
- **D6** gRPC reflection enabled; gRPC-Web disabled.
- Pekko target versions (per analysis §1 #6): Core 1.6.0, HTTP 1.4.0, gRPC 1.2.0, Persistence
  Cassandra 1.1.0, Projection 1.1.0, Connectors 1.3.0, Management 1.2.1.

## Steps

### Spike A — Pekko Projection at-least-once + offset recovery

Validates analysis §7 risk #1 (Pekko Projection parity with Lagom `ReadSideProcessor`).

1. Create `dev/spike/pekko-projection/build.sbt` — standalone sbt project, Scala 2.13.9, depends
   on `pekko-actor-typed`, `pekko-persistence-typed`, `pekko-persistence-cassandra`,
   `pekko-projection-cassandra`, all per analysis §6 versions. Set
   `Test / fork := true`.
2. Author a minimal `EventSourcedBehavior` with one command, one event, one state, persisted
   to a Cassandra journal under keyspace `spike_projection`.
3. Tag events with **10 shards** using a hand-rolled `Tagger[T]` that produces tag names
   byte-for-byte equal to what Lagom's `AggregateEventTag.sharded(numShards = 10)` produces
   (per analysis §4.3). Lagom's format is `"<entityName>|<n>"` where `n ∈ [0, numShards)`,
   with `n` derived from a stable hash of the entity id modulo `numShards`. Confirm the
   exact format empirically by inspecting an existing `messages` table row in a real dev
   keyspace (e.g. `dev_authorization`) — query `SELECT writetime(blobAsText(event))`,
   tag column from `messages` for a known entity. **Document the exact format string in
   `001-decisions.md` §A.**
4. Author a `Projection[EventEnvelope[Event]]` with `ProjectionManagement(system)` and run
   it against `spike_projection`. Verify:
   - **At-least-once:** inject a handler that throws once on a chosen event; observe redelivery.
   - **Offset recovery:** stop the projection, write 3 more events to the journal, restart,
     confirm it resumes from the stored offset (not from zero).
   - **Tag continuity:** insert a row into an existing dev keyspace's `messages` table
     (e.g. a real `dev_persons` event) and confirm the spike's `eventsByTag` query returns it
     under the same tag name. **This proves historical events remain queryable** — analysis
     §7 risk #3.
5. Record the offset-store schema (`pekko_projection_cassandra_offset`, or whatever the plugin
   names it) in `001-decisions.md` §B.

### Spike B — Play-on-Pekko gRPC PoC

Validates analysis §7 risk #4 (the single biggest version-locking risk).

1. Create `dev/spike/play-pekko-grpc/build.sbt` — Play sbt plugin at the version identified in
   your pre-flight check (see step 5 below), plus `pekko-grpc-sbt-plugin` 1.2.0.
2. Write `app/biz/lobachev/annette/spike/Routes` with one GET route.
3. Write one Pekko gRPC `.proto` (`app/protobuf/hello.proto`) with one unary `rpc`.
4. Write the impl + the route concat:
   ```scala
   Http().newServerAt("127.0.0.1", 9500).bind(
     PekkoGrpcServerHandlers.concat(
       HelloServiceHandler(new HelloServiceImpl(system), system),
       PlayRouterFromContext(...)  // your routes
     )
   )
   ```
   with `pekko.http.server.preview.enable-http2 = on` in `application.conf`.
5. Pre-flight: check which Play release runs on Pekko 1.x by inspecting Play's release notes
   or Pekko's compatibility docs. **Lock the exact Play version in `001-decisions.md` §C**.
   If no Play release runs on Pekko 1.x, halt and surface the blocker — the migration cannot
   proceed without it.
6. Verify `sbt run` boots both the Play route (HTTP/1.1) and the gRPC service (HTTP/2) on the
   same port via ALPN.

### Spike C — Exception → gRPC Status mapping design

Validates analysis §7 risk #6 and §5.6. No code spike — design only.

1. Read the `AnnetteTransportException` hierarchy in `core/core/src/main/scala/biz/lobachev/annette/core/exception/`
   (9 files). Enumerate every `TransportErrorCode` used across the codebase:
   ```bash
   rg -n "TransportErrorCode\." --type scala | sort -u
   ```
2. Read `AnnetteTransportExceptionCompanion*.scala` to understand the companion pattern
   (`BadRequest`, `NotFound`, etc.) used by the ~35 `Exceptions.scala` files.
3. Author `001-decisions.md` §D with a mapping table from each `TransportErrorCode` to a
   `io.grpc.Status.Code`. Cover at minimum: `BadRequest → INVALID_ARGUMENT`,
   `NotFound → NOT_FOUND`, `Forbidden → PERMISSION_DENIED`, `Unauthorized → UNAUTHENTICATED`,
   `Conflict → ALREADY_EXISTS`, any custom ones used.
4. Specify the server-side wiring: Pekko gRPC's
   `GreeterServiceHandler.partial(impl, system, exceptionHandler = ...)` pattern, with a single
   `GrpcExceptionMapper` trait. Specify the client-side rehydration at the gateway boundary:
   catch `io.grpc.StatusRuntimeException`, map back to the `AnnetteTransportException`
   hierarchy.
5. Confirm that `ApiGatewayErrorHandler` (`core/api-gateway-core/.../exception/`) is the only
   place that translates to public REST. The mapping there is unchanged: it converts
   `AnnetteTransportException` to JSON; the only difference post-migration is that the gateway
   side now rehydrates the exception from a gRPC status before this handler runs.

### Deliverable — `dev/migration/001-decisions.md`

Write the document with these sections:

- **§A. Tagger format.** The exact `String` format produced by Lagom's
  `AggregateEventTag.sharded(numShards = 10)`, with a one-line proof (the row you inspected).
  This locks the `Tagger[T]` implementation slice 003 must produce.
- **§B. Offset store schema.** Table name + DDL emitted by `pekko-projection-cassandra` 1.1.0.
  Confirms continuity with existing Cassandra cluster.
- **§C. Locked version matrix.** Table with one row per Pekko artifact + the pinned Play
  release that runs on Pekko 1.x. If spike B failed, write `BLOCKED` and explain.
- **§D. Exception → Status mapping.** Table + the server/client wiring design.
- **§E. Snapshot strategy confirmation.** State that snapshots will be discarded at each
  service slice's cutover (D5), with the verification step the slice must run.
- **§F. Go/no-go.** One of `GO` or `NO-GO` with one-paragraph rationale. If `NO-GO`, list the
  specific blockers; subsequent steps must not start.

## Verification

- `dev/spike/pekko-projection` — `sbt run` boots; the projection consumes events; offset
  recovery is observed; the tag-name continuity row is visible. Paste the console output
  snippets into `001-decisions.md` §A and §B.
- `dev/spike/play-pekko-grpc` — `sbt run` boots on `127.0.0.1:9500`; `curl http://127.0.0.1:9500/`
  returns 200 from the Play route; `grpcurl -plaintext 127.0.0.1:9500 hello.HelloService/SayHello`
  (or equivalent) returns the gRPC response. Paste both into `001-decisions.md` §C.
- `rg "TransportErrorCode\." --type scala | wc -l` — the count must agree with the rows in
  `001-decisions.md` §D.
- `001-decisions.md` §F reads `GO`. If it reads `NO-GO`, **stop and surface the blockers**;
  do not commit and do not start slice 002.

## Commit

Stage only the files listed in "Files in scope". Commit message:

```
docs(migration): add phase-0 spike + locked decisions [001]
```

Example SHA pattern to verify in later slices:
```bash
git log --oneline | grep -E "^\w+ docs\(migration\): add phase-0 spike"
```

## Out of scope

- Do **not** modify any production source (`core/`, `authorization/`, `api-gateway/`, etc.).
- Do **not** wire the spike sub-projects into the root `build.sbt` `aggregate(...)`.
- Do **not** decide the DTO representation (D1) — already locked: play-json at gateway,
  protobuf intra-cluster.
- Do **not** decide circuit-breaker strategy (D3) — already locked: drop.
- Do **not** start slice 002 even if §F reads `GO`. Slice 002 starts in a separate session.
- Do **not** delete the spike directories after the step — they are referenced by 002 and 004
  during their Prerequisites checks.

## References

- `dev/migrate-to-pekko.md` §1 #6 — Pekko target versions.
- `dev/migrate-to-pekko.md` §4.3 — entity translation (tagger replacement).
- `dev/migrate-to-pekko.md` §5.6 — exception model design.
- `dev/migrate-to-pekko.md` §7 risks #1, #3, #4, #6 — the four unknowns this spike resolves.
- `dev/migrate-to-pekko.md` §8 Phase 0 — phase definition.
