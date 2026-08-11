# 011 — CMS service

## Role

You are a senior Scala/Pekko engineer performing one vertical slice of the Annette Platform
migration. You execute against the working tree of the repository directly.

**This slice mirrors slice 004 (authorization) as the template.** Open `dev/migration/004-authorization-service.md`
side-by-side. This file lists only the CMS-specific deltas.

## Prerequisites

- Branch `feature/migrate-to-pekko` is checked out.
- Step 004 is already committed (`git log --oneline | grep -E "feat\(authorization\): migrate service"`).
- Repo state at start: `authorization` runs on Pekko. Other services still on Lagom.
- External services running: Cassandra 3.11 on `:9042`, OpenSearch 2.8 on `:9200`, **MinIO on `:9000`** (S3 backend).

## Goal

Migrate `cms` end-to-end. **8 entities** (verified by survey — the analysis's "10 entities"
figure counts the 2 from `cms/subscriptions`, which is a separate service migrated in slice
008):

1. `SpaceEntity`
2. `SpaceCategoryEntity`
3. `PageEntity`
4. `BlogEntity`
5. `BlogCategoryEntity`
6. `PostEntity`
7. `FileEntity`
8. `HomePageEntity`

**15 read-side processors** in `cms/cms` (one per entity × Db, plus Index variants except
`FileEntity` which is Db-only). Single-trait pattern (`CmsServiceApi`).

**Callout — Alpakka S3 → Pekko Connectors S3** (analysis §3.3 + §5.3). Two files use
`akka.stream.alpakka.s3`:
- `cms/cms-api/src/main/scala/biz/lobachev/annette/cms/api/CmsStorage.scala`
- `api-gateway/cms-api-gateway/src/main/scala/biz/lobachev/annette/cms/gateway/s3/CmsS3Helper.scala`

Both get the import rename `akka.stream.alpakka.s3` → `org.apache.pekko.connectors.s3`. The
Pekko Connectors S3 API is API-compatible (the namespace was preserved per analysis §5.8 —
`alpakka.s3.*` HOCON keys are kept).

This is the **largest service slice** in the migration: 8 entities, 15 processors, ~115
pathCall's in `CmsServiceApi`, 12 gateway controllers (the most of any service).

## Files in scope

**`cms-api` module** (package root `biz.lobachev.annette.cms.api`):
- `cms/cms-api/src/main/protobuf/cms.proto` — **new** (~115 pathCall → rpc; the largest proto).
- `cms/cms-api/src/main/scala/biz/lobachev/annette/cms/api/CmsServiceApi.scala` — **delete**.
- `cms/cms-api/src/main/scala/biz/lobachev/annette/cms/api/CmsService.scala` — **keep** (plain trait).
- `cms/cms-api/src/main/scala/biz/lobachev/annette/cms/api/CmsServiceImpl.scala` — **rewrite** (gRPC client wrapper).
- `cms/cms-api/src/main/scala/biz/lobachev/annette/cms/api/CmsStorage.scala` — **edit**: rename `akka.stream.alpakka.s3.*` → `org.apache.pekko.connectors.s3.*`. Body unchanged.
- 6 `Exceptions.scala` files under `cms/cms-api/src/main/scala/biz/lobachev/annette/cms/api/{files,blogs/blog,blogs/post,pages/space,pages/page,home_pages}/Exceptions.scala` — **edit** per 004.
- All other DTOs — **keep verbatim** (D1).

**`cms` impl module:**
- `cms/cms/src/main/scala/biz/lobachev/annette/cms/impl/CmsServiceLoader.scala` — **rewrite** (note: this file also declares the aggregate `object ServiceSerializerRegistry` — remove that line; bindings move to `reference.conf`).
- `cms/cms/src/main/scala/biz/lobachev/annette/cms/impl/CmsServiceApiImpl.scala` — **light edit**.
- 8 `*Entity.scala` files (one per entity, in each of the 8 packages: `pages/space`, `pages/category`, `pages/page`, `blogs/blog`, `blogs/category`, `blogs/post`, `files`, `home_pages`) — **edit** tagger line + `akka.*` → `pekko.*`. Tagger names: `Space`, `SpaceCategory`, `Page`, `Blog`, `BlogCategory`, `Post`, `File`, `HomePage` (verify each against `typeKey.name`).
- 15 `*{Db,Index}EventProcessor.scala` files — **rewrite** as `ProjectionBase`. (Each of the 7 non-file entities has Db+Index = 14; `files` has only `FileDbEventProcessor` = 1; total 15. Verify by globbing `cms/cms/src/main/scala/**/*EventProcessor.scala`.)
- 8 `*SerializerRegistry.scala` files (one per entity's `model/` subpackage) — **delete**.
- 15 `*Dao.scala` files (Db + Index variants; `files/dao/` has only `FileDbDao`) — edit Db DAOs; keep Index DAOs.
- All `*Record.scala` row classes — **keep** (no Lagom coupling).

**`cms` configs:**
- `cms/cms/conf/application.conf` — **edit** (indexing block is inline — 7 OpenSearch indices).
- `cms/cms/conf/application.dev.conf` — confirm port table.
- `cms/cms/conf/application.dc.conf` — **edit** (seed node `pekko://cms@127.0.0.1:17365`).
- `cms/cms/conf/application.k8s.conf` — **edit** (service-name `"cms"`, ports 17365/8514).
- `cms/cms/src/main/resources/reference.conf` — **edit** (8 bindings → `pekko.jackson-json`).

**`build.sbt`:**
- `cms-api` — drop `enablePlugins(LagomScala)`.
- `cms` — drop Lagom (this project also pulls `lagomScaladslKafkaClient`; drop it — analysis §3.2 confirms zero actual Kafka usage); add `Dependencies.pekkoCore`, `Dependencies.pekkoPersistenceCassandra`, `Dependencies.pekkoProjection`, **`Dependencies.pekkoConnectorsS3`** (replaces `alpakkaS3`); `.dependsOn(microservice-core-pekko)`.

**Gateway:**
- `api-gateway/api-gateway/app/biz/lobachev/annette/api_gateway/AnnetteApiLoader.scala` — **edit** line 198.
- `api-gateway/api-gateway/conf/application.conf` — **edit**: `pekko.grpc.client.cms { service-port = 8514 ... }`.
- `api-gateway/cms-api-gateway/src/main/scala/biz/lobachev/annette/cms/gateway/s3/CmsS3Helper.scala` — **edit**: rename `akka.stream.alpakka.s3` → `org.apache.pekko.connectors.s3`. Body unchanged.
- All 12 gateway controllers under `api-gateway/cms-api-gateway/src/main/scala/biz/lobachev/annette/cms/gateway/**/*Controller.scala` — **keep unchanged**.

No other files.

## Locked assumptions

Same as slice 004. Specifically for `cms`:
- Artery port: **17365**. HTTP/gRPC port: **8514**.
- Actor system name: `"cms"`.
- **8 entities → 8 tagger replacements** (not 10 — verified).
- Single-trait pattern.
- **MinIO/S3 required** for the smoke test (bring up via `deploy/docker/deploy.sh`).
- **No specs exist** in `cms/cms` (no `src/test/` directory at all per survey). Verification
  relies on the smoke test.

## Steps

Follow slice 004 steps 1–10. Specific notes:

- **Step 1 (proto):** ~115 `pathCall`s — the largest proto file. Consider splitting into
  multiple `.proto` files (`cms_pages.proto`, `cms_blogs.proto`, `cms_files.proto`,
  `cms_home_pages.proto`) with a shared `cms_common.proto`. Document the chosen structure.
- **Step 5 (projections):** 15 handlers. The `files` package has only `FileDbEventProcessor`
  (files aren't indexed).
- **Step 7 (loader):** `cms` is the largest service — the loader body is significant.
- **S3 swap:** mechanical import rename in `CmsStorage.scala` and `CmsS3Helper.scala`.

## Verification

- `sbt 'project cms-api' compile`
- `sbt 'project cms' compile`
- `sbt 'project api-gateway' compile`
- `sbt compile` — whole repo compiles.
- `cms/cms-api/src/test/.../HomePageTest.scala` is a non-Spec unit test (per survey); run via
  `sbt 'cms-api/testOnly *HomePageTest'` if it exists. If not, skip.
- **D5 cutover**: `TRUNCATE TABLE dev_cms.snapshots;`. With 8 entities this is the largest
  replay surface — monitor first-boot recovery logs carefully.
- **S3 verification** (manual):
  ```bash
  curl -X POST http://127.0.0.1:9000/api/cms/v1/files/upload -H "Authorization: Bearer $TOKEN" ...
  curl http://127.0.0.1:9000/api/cms/v1/files/<id> -H "Authorization: Bearer $TOKEN"
  ```
- Smoke test (one round per entity type — pages, blogs, files, home_pages):
  ```bash
  curl -X POST http://127.0.0.1:9000/api/cms/v1/spaces      -H "Authorization: Bearer $TOKEN" -d '{...}'
  curl -X POST http://127.0.0.1:9000/api/cms/v1/blogs       -H "Authorization: Bearer $TOKEN" -d '{...}'
  curl -X POST http://127.0.0.1:9000/api/cms/v1/posts       -H "Authorization: Bearer $TOKEN" -d '{...}'
  curl -X POST http://127.0.0.1:9000/api/cms/v1/home_pages  -H "Authorization: Bearer $TOKEN" -d '{...}'
  grpcurl -plaintext 127.0.0.1:8514 list
  ```

## Commit

Stage only "Files in scope". Message:
```
feat(cms): migrate service to pekko gRPC with connectors s3 [011]
```

## Out of scope

Same as slice 004 §Out of scope, applied to `cms`. **Do not** migrate any `cms/subscriptions`
files (slice 008).

## References

- `dev/migration/004-authorization-service.md` — the template.
- `dev/migrate-to-pekko.md` §3.3 — the Alpakka S3 row (Pekko Connectors replacement).
- `dev/migrate-to-pekko.md` §4, §5.2, §5.3, §5.7, §5.8, §8 Phase 3.
- `dev/migration/003-core-recipe.md`.
