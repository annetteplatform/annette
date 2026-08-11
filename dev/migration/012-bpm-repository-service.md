# 012 — BPM repository service

## Role

You are a senior Scala/Pekko engineer performing one vertical slice of the Annette Platform
migration. You execute against the working tree of the repository directly.

**This slice mirrors slice 004 (authorization) as the template.** Open `dev/migration/004-authorization-service.md`
side-by-side. This file lists only the bpm-repository-specific deltas.

## Prerequisites

- Branch `feature/migrate-to-pekko` is checked out.
- Step 004 is already committed (`git log --oneline | grep -E "feat\(authorization\): migrate service"`).
- Repo state at start: `authorization` runs on Pekko. Other services still on Lagom.
- External services running: **Postgres on `:5432`** (this is the only service using Postgres;
  bring it up via `deploy/docker/deploy.sh`). Cassandra / OpenSearch are **not** required
  for this slice.

## Goal

Migrate `bpm-repository` end-to-end. **Postgres-only, zero event-sourced entities, zero
read-side processors.** This is the easiest service slice: the Slick query bodies and DB layer
are unchanged; only the service contract (proto + gRPC) and the loader change.

**Callouts:**
- The service has **4 specs** (the most of any service). Verification is genuine.
- The gateway module is named `api-gateway/bpm-api-gateway/` (not `bpm-repository-api-gateway`)
  and aggregates 4 controllers including `CamundaRepositoryController`.
- **Pre-existing bug** (survey): `bpm/bpm-repository/conf/application.k8s.conf` sets
  `service-name = "subscriptions"` (copy-paste from the subscriptions service). **Fix this
  bug** in this slice: set `service-name = "bpm-repository"`.

## Files in scope

**`bpm-repository-api` module** (package root `biz.lobachev.annette.bpm_repository.api`):
- `bpm/bpm-repository-api/src/main/protobuf/bpm_repository.proto` — **new** (35 pathCall → rpc).
- `bpm/bpm-repository-api/src/main/scala/biz/lobachev/annette/bpm_repository/api/BpmRepositoryServiceApi.scala` — **delete**.
- `bpm/bpm-repository-api/src/main/scala/biz/lobachev/annette/bpm_repository/api/BpmRepositoryService.scala` — **keep** (plain trait).
- `bpm/bpm-repository-api/src/main/scala/biz/lobachev/annette/bpm_repository/api/BpmRepositoryServiceImpl.scala` — **rewrite**.
- 3 `Exceptions.scala` files (`api/model/Exceptions.scala`, `api/schema/Exceptions.scala`, `api/bp/Exceptions.scala`) — **edit** per 004.
- `bpm/bpm-repository-api/src/main/scala/biz/lobachev/annette/bpm_repository/api/rdb/serializers/DeserializationExceptions.scala` — **edit** if it references Lagom (else keep).
- All other DTOs — **keep verbatim** (D1).

**`bpm-repository` impl module:**
- `bpm/bpm-repository/src/main/scala/biz/lobachev/annette/bpm_repository/impl/BpmRepositoryServiceLoader.scala` — **rewrite** (no Cassandra, no projections; just Postgres via Slick, cluster sharding not used, bind HTTP/gRPC).
- `bpm/bpm-repository/src/main/scala/biz/lobachev/annette/bpm_repository/impl/BpmRepositoryServiceApiImpl.scala` — **light edit** (parent trait swap + `.invoke()` removal; the Slick bodies are unchanged).
- `bpm/bpm-repository/src/main/scala/biz/lobachev/annette/bpm_repository/impl/DBProvider.scala` — **keep** (`Database.forConfig(...)` — pure Slick, no Lagom coupling).
- All Slick `db/*.scala`, `model/*.scala`, `schema/*.scala`, `bp/*.scala` files — **keep unchanged**. The survey confirms these use `slick.jdbc.PostgresProfile` and have zero Lagom/Akka imports.
- **No `*Entity.scala`, no `*EventProcessor.scala`, no `*SerializerRegistry.scala`** to touch — the survey confirms none exist.

**`bpm-repository` configs:**
- `bpm/bpm-repository/conf/application.conf` — **edit** (mechanical `akka.*` → `pekko.*`; no Cassandra/persistence config to remove — it was never present; drop `lagom.circuit-breaker` block if present).
- `bpm/bpm-repository/conf/application.dev.conf` — confirm port table.
- `bpm/bpm-repository/conf/application.dc.conf` — **edit** (seed node `pekko://bpm-repository@127.0.0.1:17364`; or consider whether this service even needs a Pekko cluster — analysis §3.3 shows bpm-repository has no entities, no sharding; if so, drop the cluster config entirely and run standalone).
- `bpm/bpm-repository/conf/application.k8s.conf` — **edit** + **bug fix**: set `service-name = "bpm-repository"` (was `"subscriptions"`).
- `bpm/bpm-repository/src/main/resources/reference.conf` — **edit** (the 3 generic `akka.Done`/`akka.actor.Address`/`akka.remote.UniqueAddress` → `pekko-misc` bindings).

**`build.sbt`:**
- `bpm-repository-api` — drop `enablePlugins(LagomScala)`.
- `bpm-repository` — drop Lagom (this project already does **not** depend on `lagomScaladslPersistenceCassandra` per build.sbt survey; just drop `enablePlugins(LagomScala)` and `lagomScaladslServer`); add `Dependencies.pekkoCore` (for HTTP server + gRPC) — `pekkoPersistenceCassandra` and `pekkoProjection` are **not needed** here; `.dependsOn(microservice-core-pekko)`. Keep `Dependencies.slick` and `Dependencies.postgresql`.

**Gateway:**
- `api-gateway/api-gateway/app/biz/lobachev/annette/api_gateway/AnnetteApiLoader.scala` — **edit** line 201.
- `api-gateway/api-gateway/conf/application.conf` — **edit**: `pekko.grpc.client."bpm-repository" { service-port = 8513 ... }`.
- All 4 gateway controllers under `api-gateway/bpm-api-gateway/src/main/scala/biz/lobachev/annette/bpm/gateway/*Controller.scala` (`BpmModelController`, `DataSchemaController`, `BusinessProcessController`, `CamundaRepositoryController`) — **keep unchanged**.

No other files.

## Locked assumptions

Same as slice 004, with these adjustments for `bpm-repository`:
- Artery port: **17364**. HTTP/gRPC port: **8513**.
- Actor system name: `"bpm-repository"`.
- **0 entities → 0 tagger replacements.**
- **0 read-side processors → 0 ProjectionBase ports.**
- **D5 (snapshot discard) does NOT apply** — no snapshots exist.
- **D4 does not apply** — no projections.
- Single-trait pattern.

## Steps

Follow slice 004 steps 1–10 with significant simplifications:

- **Step 1 (proto):** 35 `pathCall`s.
- **Steps 4–6 (tagger, projections, serializer registries): SKIP.** None exist for this service.
- **Step 7 (loader):** simpler than `authorization` — no `ClusterSharding.init`, no projection
  startup. Just construct `Database.forConfig(...)`, the Slick-backed services, the gRPC
  handler, and bind HTTP.
- **Step 10 (snapshot cutover): SKIP.** No snapshots.

## Verification

- `sbt 'project bpm-repository-api' compile`
- `sbt 'project bpm-repository' compile`
- `sbt 'project api-gateway' compile`
- `sbt 'bpm-repository/testOnly biz.lobachev.annette.bpm_repository.test.BpmModelServiceSpec'`
- `sbt 'bpm-repository/testOnly biz.lobachev.annette.bpm_repository.test.DataSchemaServiceSpec'`
- `sbt 'bpm-repository/testOnly biz.lobachev.annette.bpm_repository.test.BusinessProcessServiceSpec'`
- `sbt 'bpm-repository/testOnly biz.lobachev.annette.bpm_repository.test.ExtractCodeSpec'`
  (this is the one slice with substantial spec coverage — all 4 must pass post-migration. If
  any fails, the Slick bodies are unchanged so the cause is in the loader/wiring; debug
  before committing.)
- **No D5 cutover.**
- Smoke test:
  ```bash
  curl -X POST http://127.0.0.1:9000/api/bpm-repository/v1/createBpmModel -H "Authorization: Bearer $TOKEN" -d '{...}'
  curl http://127.0.0.1:9000/api/bpm-repository/v1/getBpmModel/<id> -H "Authorization: Bearer $TOKEN"
  grpcurl -plaintext 127.0.0.1:8513 list
  ```

## Commit

Stage only "Files in scope". Message:
```
feat(bpm-repository): migrate service to pekko gRPC [012]
```

## Out of scope

Same as slice 004 §Out of scope, applied to `bpm-repository`. Do not migrate the `bpm/camunda`
module — it is a Camunda WSClient wrapper (Play-only, no Lagom coupling per analysis §3.1)
and is unaffected by this migration; no slice touches it.

## References

- `dev/migration/004-authorization-service.md` — the template.
- `dev/migrate-to-pekko.md` §3.1 — confirms `bpm-repository` Postgres-only, no Cassandra.
- `dev/migrate-to-pekko.md` §4, §5.2, §5.3, §5.8, §8 Phase 3.
- `dev/migration/003-core-recipe.md`.
