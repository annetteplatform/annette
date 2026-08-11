# 010 — Application service

## Role

You are a senior Scala/Pekko engineer performing one vertical slice of the Annette Platform
migration. You execute against the working tree of the repository directly.

**This slice mirrors slice 004 (authorization) as the template.** Open `dev/migration/004-authorization-service.md`
side-by-side. This file lists only the application-specific deltas.

## Prerequisites

- Branch `feature/migrate-to-pekko` is checked out.
- Step 004 is already committed (`git log --oneline | grep -E "feat\(authorization\): migrate service"`).
- Repo state at start: `authorization` runs on Pekko. Other services still on Lagom.
- External services running: Cassandra 3.11 on `:9042`, OpenSearch 2.8 on `:9200`.

## Goal

Migrate `application` end-to-end. **4 entities**: `ApplicationEntity`, `LanguageEntity`,
`TranslationEntity`, `TranslationJsonEntity`. **7 processors** (Db + Index for the first 3;
`TranslationJson` has only a Db processor — no Index variant exists).

**Callout — TWO-TRAIT PATTERN** (same as slice 009 service-catalog). See slice 009's "Callout"
section: the `*ServiceLagomApi`, `*ServiceLagomApiImpl`, `*ServiceLagomImpl` trio is replaced
by the generated trait + a new thin gRPC client wrapper.

## Files in scope

**`application-api` module** (package root `biz.lobachev.annette.application`):
- `application/application-api/src/main/protobuf/application.proto` — **new**.
- `application/application-api/src/main/scala/biz/lobachev/annette/application/client/http/ApplicationServiceLagomApi.scala` — **delete** (27 `pathCall`s).
- `application/application-api/src/main/scala/biz/lobachev/annette/application/client/http/ApplicationServiceLagomImpl.scala` — **delete**.
- `application/application-api/src/main/scala/biz/lobachev/annette/application/api/ApplicationService.scala` — **keep** (plain trait).
- New: `application/application-api/src/main/scala/biz/lobachev/annette/application/client/grpc/ApplicationServiceGrpcImpl.scala` — **new** (adapts gRPC client → `ApplicationService`).
- `application/application-api/src/main/scala/biz/lobachev/annette/application/api/{application,language,translation}/Exceptions.scala` — **edit** (3 files).
- All other DTOs — **keep verbatim** (D1).

**`application` impl module:**
- `application/application/src/main/scala/biz/lobachev/annette/application/server/ApplicationServiceLoader.scala` — **rewrite**.
- `application/application/src/main/scala/biz/lobachev/annette/application/server/http/ApplicationServiceLagomApiImpl.scala` — **delete**.
- `application/application/src/main/scala/biz/lobachev/annette/application/impl/ApplicationServiceImpl.scala` — **light edit** (parent trait swap).
- `application/application/src/main/scala/biz/lobachev/annette/application/impl/application/ApplicationEntity.scala` — **edit** line 180 (tagger).
- `application/application/src/main/scala/biz/lobachev/annette/application/impl/language/LanguageEntity.scala` — **edit** line 112 (tagger).
- `application/application/src/main/scala/biz/lobachev/annette/application/impl/translation/TranslationEntity.scala` — **edit** line 111 (tagger).
- `application/application/src/main/scala/biz/lobachev/annette/application/impl/translation_json/TranslationJsonEntity.scala` — **edit** line 107 (tagger).
- 7 `*{Db,Index}EventProcessor.scala` files — **rewrite** as `ProjectionBase`. (Verify the 7 paths from the survey: ApplicationDb + ApplicationIndex, LanguageDb + LanguageIndex, TranslationDb + TranslationIndex, TranslationJsonDb only.)
- 4 `*SerializerRegistry.scala` files — **delete**.
- DAOs: 7 `*Dao.scala` files (4 Db + 3 Index; `TranslationJson` has only Db) — edit Db files; keep Index files.
- `application/application/src/main/scala/biz/lobachev/annette/application/impl/language/LanguageState.scala` — note: the survey flagged a likely **duplicate** of this file (one at `impl/language/LanguageState.scala`, another at `impl/language/model/LanguageState.scala`). **Resolve the duplicate** in this slice: delete the one that is not referenced by `LanguageEntity` / `LanguageSerializerRegistry` (now deleted). Document the choice in the commit message.

**`application` configs:**
- `application/application/conf/application.conf` — **edit** (indexing block is inline).
- `application/application/conf/application.dev.conf` — confirm port table.
- `application/application/conf/application.dc.conf` — **edit** (seed node `pekko://application@127.0.0.1:17361`).
- `application/application/conf/application.k8s.conf` — **edit** (service-name `"application"`, ports 17361/8510).
- `application/application/src/main/resources/reference.conf` — **edit**.

**`build.sbt`:**
- `application-api` — drop `enablePlugins(LagomScala)`.
- `application` — drop Lagom; add Pekko; `.dependsOn(microservice-core-pekko)`.

**Gateway:**
- `api-gateway/api-gateway/app/biz/lobachev/annette/api_gateway/AnnetteApiLoader.scala` — **edit** line 186.
- `api-gateway/api-gateway/conf/application.conf` — **edit**: `pekko.grpc.client.application { service-port = 8510 ... }`.
- All 4 gateway controllers under `api-gateway/application-api-gateway/src/main/scala/biz/lobachev/annette/application/gateway/*Controller.scala` (`ApplicationController`, `LanguageController`, `TranslationController`, `UserApplicationController`) — **keep unchanged**.

No other files.

## Locked assumptions

Same as slice 004. Specifically for `application`:
- Artery port: **17361**. HTTP/gRPC port: **8510**.
- Actor system name: `"application"`. (Note: this collides with the historical cluster name
  used under Lagom where every service was named `"application"`. Post-migration each service
  has a unique actor system name; this one happens to keep `"application"` legitimately.)
- 4 entities → 4 tagger replacements.
- **Two-trait pattern**.
- **No specs exist** in the impl module — verification relies on the smoke test.

## Steps

Follow slice 004 steps 1–10 with the two-trait adjustments from slice 009 step 1–3. Specific
notes:

- **Step 1 (proto):** 27 `pathCall`s.
- **Step 5 (projections):** 7 handlers. **Important:** `TranslationJsonEntity` has no Index
  processor — do not invent one.
- **`LanguageState` duplicate:** resolve before deleting `LanguageSerializerRegistry` (which
  may reference one of the two). Grep for both paths to confirm which is canonical.

## Verification

- `sbt 'project application-api' compile`
- `sbt 'project application' compile`
- `sbt 'project api-gateway' compile`
- `sbt compile` — whole repo compiles.
- **D5 cutover**: `TRUNCATE TABLE dev_application.snapshots;`.
- Smoke test:
  ```bash
  curl -X POST http://127.0.0.1:9000/api/application/v1/createApplication -H "Authorization: Bearer $TOKEN" -d '{...}'
  curl -X POST http://127.0.0.1:9000/api/application/v1/createLanguage     -H "Authorization: Bearer $TOKEN" -d '{...}'
  curl http://127.0.0.1:9000/api/application/v1/getApplication/<id>          -H "Authorization: Bearer $TOKEN"
  grpcurl -plaintext 127.0.0.1:8510 list
  ```

## Commit

Stage only "Files in scope". Message:
```
feat(application): migrate service to pekko gRPC [010]
```

## Out of scope

Same as slice 004 §Out of scope, applied to `application`.

## References

- `dev/migration/004-authorization-service.md` — the template.
- `dev/migration/009-service-catalog-service.md` — the two-trait pattern reference.
- `dev/migrate-to-pekko.md` §4, §5.2, §5.3, §5.7, §5.8, §8 Phase 3.
- `dev/migration/003-core-recipe.md`.
