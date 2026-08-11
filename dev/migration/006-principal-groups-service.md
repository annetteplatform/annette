# 006 — Principal-groups service

## Role

You are a senior Scala/Pekko engineer performing one vertical slice of the Annette Platform
migration. You execute against the working tree of the repository directly.

**This slice mirrors slice 004 (authorization) as the template.** Open `dev/migration/004-authorization-service.md`
side-by-side. This file lists only the principal-groups-specific deltas.

## Prerequisites

- Branch `feature/migrate-to-pekko` is checked out.
- Step 004 is already committed (`git log --oneline | grep -E "feat\(authorization\): migrate service"`).
- Repo state at start: `authorization` runs on Pekko. The other 8 services still run on
  Lagom. All non-principal-groups specs pass.
- External services running: Cassandra 3.11 on `:9042`, OpenSearch 2.8 on `:9200`.

## Goal

Migrate `principal-groups` end-to-end following the slice-004 recipe. 2 entities, 4 processors,
no specs (per the survey).

## Files in scope

**`principal-groups-api` module** (package root `biz.lobachev.annette.principal_group` —
note the singular `principal_group`):
- `principals/principal-groups-api/src/main/protobuf/principal_group.proto` — **new**.
- `principals/principal-groups-api/src/main/scala/biz/lobachev/annette/principal_group/api/PrincipalGroupServiceApi.scala` — **delete** (19 `pathCall`s to mirror in the proto).
- `principals/principal-groups-api/src/main/scala/biz/lobachev/annette/principal_group/api/PrincipalGroupService.scala` — **keep** (plain trait).
- `principals/principal-groups-api/src/main/scala/biz/lobachev/annette/principal_group/api/PrincipalGroupServiceImpl.scala` — **rewrite**.
- `principals/principal-groups-api/src/main/scala/biz/lobachev/annette/principal_group/api/group/Exceptions.scala` — **edit** per 004 exception section.
- All other DTOs under `.../principal_group/api/{group,category}/*.scala` — **keep verbatim** (D1).

**`principal-groups` impl module:**
- `principals/principal-groups/src/main/scala/biz/lobachev/annette/principal_group/impl/PrincipalGroupServiceLoader.scala` — **rewrite**.
- `principals/principal-groups/src/main/scala/biz/lobachev/annette/principal_group/impl/PrincipalGroupServiceApiImpl.scala` — **light edit**.
- `principals/principal-groups/src/main/scala/biz/lobachev/annette/principal_group/impl/group/PrincipalGroupEntity.scala` — **edit** line 182 (tagger `"PrincipalGroups_PrincipalGroup"` — verify `typeKey.name`).
- `principals/principal-groups/src/main/scala/biz/lobachev/annette/principal_group/impl/category/CategoryEntity.scala` — **edit** line 113 (tagger `"PrincipalGroups_Category"`).
- `principals/principal-groups/src/main/scala/biz/lobachev/annette/principal_group/impl/group/PrincipalGroupDbEventProcessor.scala` — **rewrite** as `ProjectionBase`.
- `principals/principal-groups/src/main/scala/biz/lobachev/annette/principal_group/impl/group/PrincipalGroupIndexEventProcessor.scala` — **rewrite** as `ProjectionBase`.
- `principals/principal-groups/src/main/scala/biz/lobachev/annette/principal_group/impl/category/CategoryDbEventProcessor.scala` — **rewrite** as `ProjectionBase`.
- `principals/principal-groups/src/main/scala/biz/lobachev/annette/principal_group/impl/category/CategoryIndexEventProcessor.scala` — **rewrite** as `ProjectionBase`.
- `principals/principal-groups/src/main/scala/biz/lobachev/annette/principal_group/impl/group/model/GroupSerializerRegistry.scala` — **delete**.
- `principals/principal-groups/src/main/scala/biz/lobachev/annette/principal_group/impl/category/model/CategorySerializerRegistry.scala` — **delete**.
- `principals/principal-groups/src/main/scala/biz/lobachev/annette/principal_group/impl/group/dao/PrincipalGroupDbDao.scala` — **edit** (Pekko `CqlSession`).
- `principals/principal-groups/src/main/scala/biz/lobachev/annette/principal_group/impl/category/dao/CategoryDbDao.scala` — **edit** (same).
- `principals/principal-groups/src/main/scala/biz/lobachev/annette/principal_group/impl/group/dao/PrincipalGroupIndexDao.scala` — **keep**.
- `principals/principal-groups/src/main/scala/biz/lobachev/annette/principal_group/impl/category/dao/CategoryIndexDao.scala` — **keep**.

**`principal-groups` configs:**
- `principals/principal-groups/conf/application.conf` — **edit** (note: this file has the `indexing {}` block inline, not in a separate `indexing.conf`).
- `principals/principal-groups/conf/application.dev.conf` — confirm port table.
- `principals/principal-groups/conf/application.dc.conf` — **edit** (seed node `pekko://principal-groups@127.0.0.1:17369`).
- `principals/principal-groups/conf/application.k8s.conf` — **edit** (service-name `"principal-groups"`, ports 17369/8518).
- `principals/principal-groups/src/main/resources/reference.conf` — **edit** (`pekko.jackson-json` bindings).

**`build.sbt`:**
- `principal-groups-api` — drop `enablePlugins(LagomScala)`.
- `principal-groups` — drop Lagom plugins/deps; add Pekko; `.dependsOn(microservice-core-pekko)`.

**Gateway:**
- `api-gateway/api-gateway/app/biz/lobachev/annette/api_gateway/AnnetteApiLoader.scala` — **edit** line 192: `PrincipalGroupServiceClient(grpcClientFactory.clientFor("principal-groups"))`.
- `api-gateway/api-gateway/conf/application.conf` — **edit**: `pekko.grpc.client."principal-groups" { service-port = 8518 ... }`.
- `api-gateway/principal-groups-api-gateway/src/main/scala/biz/lobachev/annette/principal_group/gateway/PrincipalGroupController.scala` — **keep unchanged**.

No other files.

## Locked assumptions

Same as slice 004. Specifically for `principal-groups`:
- Artery port: **17369**. HTTP/gRPC port: **8518**.
- Actor system name: `"principal-groups"`.
- 2 entities → 2 tagger replacements.
- Single-trait pattern.
- **No specs exist** — the survey confirms `principals/principal-groups/src/test/` contains
  only `logback-test.xml`, no Scala sources. Verification must rely on the smoke test alone.

## Steps

Follow slice 004 steps 1–10. Specific notes:

- **Step 1 (proto):** 19 `pathCall`s. DTOs include group + category types and the assignment/
  principal records used in derived projections.
- **Step 5 (projections):** 4 handlers.

## Verification

- `sbt 'project principal-groups-api' compile`
- `sbt 'project principal-groups' compile`
- `sbt 'project api-gateway' compile`
- **D5 cutover**: `TRUNCATE TABLE dev_principal_groups.snapshots;` before first boot.
- Smoke test:
  ```bash
  curl -X POST http://127.0.0.1:9000/api/principal-groups/v1/createGroup \
       -H "Authorization: Bearer $TOKEN" -d '{"id":"smoke-group","name":"Smoke"}'
  curl http://127.0.0.1:9000/api/principal-groups/v1/getGroup/smoke-group -H "Authorization: Bearer $TOKEN"
  grpcurl -plaintext 127.0.0.1:8518 list
  ```

## Commit

Stage only "Files in scope". Message:
```
feat(principal-groups): migrate service to pekko gRPC [006]
```

## Out of scope

Same as slice 004 §Out of scope, applied to `principal-groups`.

## References

- `dev/migration/004-authorization-service.md` — the template.
- `dev/migrate-to-pekko.md` §4, §5.2, §5.3, §5.7, §5.8, §8 Phase 3.
- `dev/migration/003-core-recipe.md`.
