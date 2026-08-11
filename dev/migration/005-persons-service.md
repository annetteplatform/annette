# 005 — Persons service

## Role

You are a senior Scala/Pekko engineer performing one vertical slice of the Annette Platform
migration. You execute against the working tree of the repository directly.

**This slice mirrors slice 004 (authorization) as the template.** Open `dev/migration/004-authorization-service.md`
side-by-side. This file lists only the persons-specific deltas; everything else follows 004's
recipe step-for-step.

## Prerequisites

- Branch `feature/migrate-to-pekko` is checked out.
- Step 004 is already committed:
  ```bash
  git log --oneline | grep -E "feat\(authorization\): migrate service"
  ```
- Repo state at start: `authorization` runs on Pekko (HTTP :8512, Artery :17363). The other
  8 services (including `persons`) still run on Lagom. The gateway has the gRPC client for
  `authorization` only. All non-persons specs pass.
- External services running: Cassandra 3.11 on `:9042`, OpenSearch 2.8 on `:9200`.

## Goal

Migrate `persons` end-to-end following the slice-004 recipe: proto → impl → tagger → projection →
config → gateway swap → snapshot cutover → smoke test.

## Files in scope

**`persons-api` module:**
- `principals/persons-api/src/main/protobuf/person.proto` — **new**.
- `principals/persons-api/src/main/scala/biz/lobachev/annette/persons/api/PersonServiceApi.scala` — **delete** (replaced by generated `PersonService` trait).
- `principals/persons-api/src/main/scala/biz/lobachev/annette/persons/api/PersonService.scala` — **keep** (plain Scala trait).
- `principals/persons-api/src/main/scala/biz/lobachev/annette/persons/api/PersonServiceImpl.scala` — **rewrite** (gRPC client wrapper).
- `principals/persons-api/src/main/scala/biz/lobachev/annette/persons/api/person/Exceptions.scala` — **edit** per 004's exception section.
- All other DTOs under `.../persons/api/{person,category}/*.scala` — **keep verbatim** (D1).

**`persons` impl module:**
- `principals/persons/src/main/scala/biz/lobachev/annette/persons/impl/PersonServiceLoader.scala` — **rewrite** (loader pattern from `003-core-recipe.md` §F).
- `principals/persons/src/main/scala/biz/lobachev/annette/persons/impl/PersonServiceApiImpl.scala` — **light edit** (parent trait swap + `.invoke()` removal).
- `principals/persons/src/main/scala/biz/lobachev/annette/persons/impl/person/PersonEntity.scala` — **edit** line 131 (tagger `"Persons_Person"` — verify against `typeKey.name`).
- `principals/persons/src/main/scala/biz/lobachev/annette/persons/impl/category/CategoryEntity.scala` — **edit** line 113 (tagger `"Persons_Category"`).
- `principals/persons/src/main/scala/biz/lobachev/annette/persons/impl/person/PersonDbEventProcessor.scala` — **rewrite** as `ProjectionBase`.
- `principals/persons/src/main/scala/biz/lobachev/annette/persons/impl/person/PersonIndexEventProcessor.scala` — **rewrite** as `ProjectionBase`.
- `principals/persons/src/main/scala/biz/lobachev/annette/persons/impl/category/CategoryDbEventProcessor.scala` — **rewrite** as `ProjectionBase`.
- `principals/persons/src/main/scala/biz/lobachev/annette/persons/impl/category/CategoryIndexEventProcessor.scala` — **rewrite** as `ProjectionBase`.
- `principals/persons/src/main/scala/biz/lobachev/annette/persons/impl/person/model/PersonSerializerRegistry.scala` — **delete**.
- `principals/persons/src/main/scala/biz/lobachev/annette/persons/impl/category/model/CategorySerializerRegistry.scala` — **delete**.
- `principals/persons/src/main/scala/biz/lobachev/annette/persons/impl/person/dao/PersonDbDao.scala` — **edit** (Lagom `CassandraSession` → `CqlSession`; extend `microservice_core.pekko.db.CassandraQuillDao`). Note: this project depends on `Dependencies.quill` (per `build.sbt` `personsProject`).
- `principals/persons/src/main/scala/biz/lobachev/annette/persons/impl/category/dao/CategoryDbDao.scala` — **edit** (same).
- `principals/persons/src/main/scala/biz/lobachev/annette/persons/impl/person/dao/PersonIndexDao.scala` — **keep** (elastic4s only).
- `principals/persons/src/main/scala/biz/lobachev/annette/persons/impl/category/dao/CategoryIndexDao.scala` — **keep**.

**`persons` configs:**
- `principals/persons/conf/application.conf` — **edit** per 004 step 8.
- `principals/persons/conf/application.dev.conf` — confirm port table (no change unless missing).
- `principals/persons/conf/application.dc.conf` — **edit** (seed node `pekko://persons@127.0.0.1:17368`).
- `principals/persons/conf/application.k8s.conf` — **edit** (service-name `"persons"`, container ports 17368/8517).
- `principals/persons/conf/indexing.conf` — **keep** (no `akka.*`/`lagom.*` keys; pure indexing config).
- `principals/persons/src/main/resources/reference.conf` — **edit** per 004 step 6 (`pekko.actor.serialization-bindings` → `pekko.jackson-json`).

**`build.sbt`:**
- `persons-api` project — drop `enablePlugins(LagomScala)`.
- `persons` project — drop `enablePlugins(LagomScala)`; swap `lagomScaladsl*` deps for `Dependencies.pekkoCore` + `pekkoPersistenceCassandra` + `pekkoProjection`; add `.dependsOn(microservice-core-pekko)`. Keep `Dependencies.quill` (used by attribute DAOs).

**Gateway:**
- `api-gateway/api-gateway/app/biz/lobachev/annette/api_gateway/AnnetteApiLoader.scala` — **edit** line 183: replace `serviceClient.implement[PersonServiceApi]` with `PersonServiceClient(grpcClientFactory.clientFor("persons"))`.
- `api-gateway/api-gateway/conf/application.conf` — **edit**: populate `pekko.grpc.client.persons { service-port = 8517 ... }`.
- `api-gateway/persons-api-gateway/src/main/scala/biz/lobachev/annette/person/gateway/PersonController.scala` — **keep unchanged** (D7).

No other files.

## Locked assumptions

Same as slice 004. Specifically for `persons`:
- **D5** Discard `persons` snapshots at cutover; replay from journal.
- Artery port: **17368**. HTTP/gRPC port: **8517**.
- Actor system name: `"persons"` (was `"application"` under Lagom).
- 2 entities → 2 `AkkaTaggerAdapter.fromLagom` calls to replace.
- Single-trait pattern (`PersonServiceApi extends Service`).

## Steps

Follow slice 004 steps 1–10 substituting service paths, names, and ports. Specific notes:

- **Step 1 (proto):** enumerate the 16 `pathCall`s in `PersonServiceApi.descriptor`. The DTOs
  include `CreatePersonPayload`, `Person`, the attribute `Map[String, AttributeValue]` (use
  `map<string, AttributeValue>`), and the category DTOs.
- **Step 5 (projections):** 4 handlers (Db + Index × 2 entities).
- **Step 7 (loader):** `persons` uses pureConfig via `orgStructureProject`? No — only
  `org-structure` adds `Dependencies.pureConfig`. `personsProject` adds `Dependencies.quill`
  only. Do not add pureConfig.

## Verification

- `sbt 'project persons-api' compile`
- `sbt 'project persons' compile`
- `sbt 'project api-gateway' compile`
- `sbt 'persons/testOnly biz.lobachev.annette.person.test.PersonEntitySpec'` — the entity
  spec must still pass after the `pekko.*` rename.
- `sbt 'persons/testOnly biz.lobachev.annette.person.test.PersonServiceApiSpec'` — the
  integration spec; note it currently uses Lagom `ServiceTest` + live Cassandra per analysis
  §5.9. For this slice, **either** keep it running against Lagom (i.e. revert to before-slice
  state and run it pre-migration to capture baseline, then mark it `pending` post-migration
  with a TODO), **or** rewrite it to Pekko testkit per analysis §5.9. **Recommended for this
  slice**: rewrite it to use `PersonServiceHandler` bound to an in-process `Http().newServerAt`
  + generated `PersonServiceClient` over an in-process channel. Use Testcontainers Cassandra
  (`org.testcontainers:cassandra`) if available; otherwise document the external-Cassandra
  requirement in the spec's scaladoc.
- **D5 cutover**: `cqlsh -e "TRUNCATE TABLE dev_persons.snapshots;"` before first boot;
  confirm entity recovery in logs.
- Smoke test:
  ```bash
  curl -X POST http://127.0.0.1:9000/api/persons/v1/createPerson \
       -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
       -d '{"id":"smoke-person","firstname":"Smoke","lastname":"Test","categoryId":"cat1"}'
  curl http://127.0.0.1:9000/api/persons/v1/getPerson/smoke-person -H "Authorization: Bearer $TOKEN"
  grpcurl -plaintext 127.0.0.1:8517 list
  ```

## Commit

Stage only "Files in scope". Message:
```
feat(persons): migrate service to pekko gRPC [005]
```

## Out of scope

Same as slice 004 §Out of scope, applied to `persons` instead of `authorization`. Do not
touch any other service.

## References

- `dev/migration/004-authorization-service.md` — the template. Read it first.
- `dev/migrate-to-pekko.md` §4, §5.2, §5.3, §5.7, §5.8, §8 Phase 3.
- `dev/migration/003-core-recipe.md` — helper usage.
