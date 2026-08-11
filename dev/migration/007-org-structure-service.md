# 007 — Org-structure service

## Role

You are a senior Scala/Pekko engineer performing one vertical slice of the Annette Platform
migration. You execute against the working tree of the repository directly.

**This slice mirrors slice 004 (authorization) as the template.** Open `dev/migration/004-authorization-service.md`
side-by-side. This file lists only the org-structure-specific deltas.

## Prerequisites

- Branch `feature/migrate-to-pekko` is checked out.
- Step 004 is already committed (`git log --oneline | grep -E "feat\(authorization\): migrate service"`).
- Repo state at start: `authorization` runs on Pekko. Other services still on Lagom.
- External services running: Cassandra 3.11 on `:9042`, OpenSearch 2.8 on `:9200`.

## Goal

Migrate `org-structure` end-to-end. **3 entities** (not 4 — verified by `EntityTypeKey`
occurrences and `reference.conf` bindings): `HierarchyEntity` (477 lines, the most complex
entity in the codebase), `CategoryEntity`, `OrgRoleEntity`. 6 processors (2 per entity).

**Callout — `HierarchyEntity` complexity.** Per the survey, `HierarchyEntity.scala` at
`principals/org-structure/src/main/scala/biz/lobachev/annette/org_structure/impl/hierarchy/entity/HierarchyEntity.scala`
is 477 lines and defines behavior inside `object HierarchyEntity` only (no `final case class`
wrapper) targeting `HierarchyState` directly. Its `AkkaTaggerAdapter.fromLagom` call is at
line 474. The migration edit is still one line (tagger replacement), but the entity should be
re-read carefully before editing — the surrounding `EventSourcedBehavior` is large and any
mistake will surface only at runtime.

## Files in scope

**`org-structure-api` module** (package root `biz.lobachev.annette.org_structure`):
- `principals/org-structure-api/src/main/protobuf/org_structure.proto` — **new**.
- `principals/org-structure-api/src/main/scala/biz/lobachev/annette/org_structure/api/OrgStructureServiceApi.scala` — **delete** (33 `pathCall`s).
- `principals/org-structure-api/src/main/scala/biz/lobachev/annette/org_structure/api/OrgStructureService.scala` — **keep** (plain trait).
- `principals/org-structure-api/src/main/scala/biz/lobachev/annette/org_structure/api/OrgStructureServiceImpl.scala` — **rewrite** (209 lines — the largest client wrapper; mechanical `.invoke()` removal).
- `principals/org-structure-api/src/main/scala/biz/lobachev/annette/org_structure/api/hierarchy/Exceptions.scala` — **edit**.
- `principals/org-structure-api/src/main/scala/biz/lobachev/annette/org_structure/api/category/Exceptions.scala` — **edit**.
- `principals/org-structure-api/src/main/scala/biz/lobachev/annette/org_structure/api/role/Exceptions.scala` — **edit**.
- All other DTOs under `.../org_structure/api/{hierarchy,category,role}/*.scala` — **keep verbatim** (D1).

**`org-structure` impl module:**
- `principals/org-structure/src/main/scala/biz/lobachev/annette/org_structure/impl/OrgStructureServiceLoader.scala` (117 lines) — **rewrite**.
- `principals/org-structure/src/main/scala/biz/lobachev/annette/org_structure/impl/OrgStructureServiceApiImpl.scala` — **light edit**.
- `principals/org-structure/src/main/scala/biz/lobachev/annette/org_structure/impl/hierarchy/entity/HierarchyEntity.scala` — **edit** line 474 (tagger `"Hierarchy"` — verify `typeKey.name = "Hierarchy"`). Rename `akka.*` → `pekko.*`.
- `principals/org-structure/src/main/scala/biz/lobachev/annette/org_structure/impl/category/CategoryEntity.scala` — **edit** line 107 (tagger).
- `principals/org-structure/src/main/scala/biz/lobachev/annette/org_structure/impl/role/OrgRoleEntity.scala` — **edit** line 102 (tagger).
- `principals/org-structure/src/main/scala/biz/lobachev/annette/org_structure/impl/hierarchy/HierarchyDbEventProcessor.scala` — **rewrite** as `ProjectionBase`.
- `principals/org-structure/src/main/scala/biz/lobachev/annette/org_structure/impl/hierarchy/HierarchyIndexEventProcessor.scala` — **rewrite** as `ProjectionBase`.
- `principals/org-structure/src/main/scala/biz/lobachev/annette/org_structure/impl/category/CategoryDbEventProcessor.scala` — **rewrite** as `ProjectionBase`.
- `principals/org-structure/src/main/scala/biz/lobachev/annette/org_structure/impl/category/CategoryIndexEventProcessor.scala` — **rewrite** as `ProjectionBase`.
- `principals/org-structure/src/main/scala/biz/lobachev/annette/org_structure/impl/role/OrgRoleDbEventProcessor.scala` — **rewrite** as `ProjectionBase`.
- `principals/org-structure/src/main/scala/biz/lobachev/annette/org_structure/impl/role/OrgRoleIndexEventProcessor.scala` — **rewrite** as `ProjectionBase`.
- `principals/org-structure/src/main/scala/biz/lobachev/annette/org_structure/impl/hierarchy/entity/HierarchySerializerRegistry.scala` — **delete** (note: lives under `entity/`, not `model/`).
- `principals/org-structure/src/main/scala/biz/lobachev/annette/org_structure/impl/category/model/CategorySerializerRegistry.scala` — **delete**.
- `principals/org-structure/src/main/scala/biz/lobachev/annette/org_structure/impl/role/model/OrgRoleSerializerRegistry.scala` — **delete**.
- `principals/org-structure/src/main/scala/biz/lobachev/annette/org_structure/impl/hierarchy/dao/HierarchyDbDao.scala` — **edit** (Pekko `CqlSession`).
- `principals/org-structure/src/main/scala/biz/lobachev/annette/org_structure/impl/category/dao/CategoryDbDao.scala` — **edit**.
- `principals/org-structure/src/main/scala/biz/lobachev/annette/org_structure/impl/role/dao/OrgRoleDbDao.scala` — **edit**.
- All three `*IndexDao.scala` files (`hierarchy/dao/`, `category/dao/`, `role/dao/`) — **keep**.

**`org-structure` configs:**
- `principals/org-structure/conf/application.conf` — **edit**.
- `principals/org-structure/conf/application.dev.conf` — confirm port table.
- `principals/org-structure/conf/application.dc.conf` — **edit** (seed node `pekko://org-structure@127.0.0.1:17367`).
- `principals/org-structure/conf/application.k8s.conf` — **edit** (service-name `"org-structure"`, ports 17367/8516).
- `principals/org-structure/conf/indexing.conf` — **keep**.
- `principals/org-structure/src/main/resources/reference.conf` — **edit** (3 bindings: `HierarchyEntity$CommandSerializable`, `CategoryEntity$CommandSerializable`, `OrgRoleEntity$CommandSerializable` → `pekko.jackson-json`).

**`build.sbt`:**
- `org-structure-api` — drop `enablePlugins(LagomScala)`.
- `org-structure` — drop Lagom; add Pekko; `.dependsOn(microservice-core-pekko)`. This project adds `Dependencies.pureConfig` (per `orgStructureProject`) — keep it.

**Gateway:**
- `api-gateway/api-gateway/app/biz/lobachev/annette/api_gateway/AnnetteApiLoader.scala` — **edit** line 180.
- `api-gateway/api-gateway/conf/application.conf` — **edit**: `pekko.grpc.client."org-structure" { service-port = 8516 ... }`.
- `api-gateway/org-structure-api-gateway/src/main/scala/biz/lobachev/annette/org_structure/gateway/OrgStructureController.scala` (610 lines — largest controller) — **keep unchanged**.

No other files.

## Locked assumptions

Same as slice 004. Specifically for `org-structure`:
- Artery port: **17367**. HTTP/gRPC port: **8516**.
- Actor system name: `"org-structure"`.
- **3 entities → 3 tagger replacements** (not 4 — verified; analysis's "4 entities" figure is wrong).
- Single-trait pattern.

## Steps

Follow slice 004 steps 1–10. Specific notes:

- **Step 1 (proto):** 33 `pathCall`s — the largest service contract after CMS. The hierarchy
  DTOs are intricate (tree operations, item moves, chief assignments). Translate carefully
  — these are the highest-risk DTOs in the migration.
- **Step 5 (projections):** 6 handlers.
- **Step 7 (loader):** the loader is 117 lines, larger than `authorization`'s. The three
  shards (`Hierarchy`, `Category`, `OrgRole`) all init.
- **`HierarchyEntity` tagger edit**: read the file fully before editing line 474. Its
  `EventSourcedBehavior` is non-trivial and the tagger line sits at the end of the
  behavior definition.

## Verification

- `sbt 'project org-structure-api' compile`
- `sbt 'project org-structure' compile`
- `sbt 'project api-gateway' compile`
- `sbt 'org-structure/testOnly biz.lobachev.annette.org_structure.items.test.OrgRoleEntitySpec'`
- `sbt 'org-structure/testOnly biz.lobachev.annette.org_structure.items.test.OrgRoleServiceSpec'`
  (only `OrgRole` has spec coverage — survey confirms `HierarchyEntity` and `CategoryEntity`
  have none; do not write new specs in this slice).
- **D5 cutover**: `TRUNCATE TABLE dev_org_structure.snapshots;`. Pay extra attention to the
  `HierarchyEntity` recovery logs — its 477-line behavior may surface replay issues.
- Smoke test (hierarchy is the critical path):
  ```bash
  curl -X POST http://127.0.0.1:9000/api/org-structure/v1/createItem \
       -H "Authorization: Bearer $TOKEN" -d '{"id":"smoke-root","name":"Smoke Root"}'
  curl http://127.0.0.1:9000/api/org-structure/v1/getItem/smoke-root -H "Authorization: Bearer $TOKEN"
  grpcurl -plaintext 127.0.0.1:8516 list
  ```

## Commit

Stage only "Files in scope". Message:
```
feat(org-structure): migrate service to pekko gRPC [007]
```

## Out of scope

Same as slice 004 §Out of scope, applied to `org-structure`. Do not write new tests for
`HierarchyEntity` or `CategoryEntity` — that is a follow-up outside this migration.

## References

- `dev/migration/004-authorization-service.md` — the template.
- `dev/migrate-to-pekko.md` §4, §5.2, §5.3, §5.7, §5.8, §8 Phase 3.
- `dev/migration/003-core-recipe.md`.
