# 009 — Service-catalog service

## Role

You are a senior Scala/Pekko engineer performing one vertical slice of the Annette Platform
migration. You execute against the working tree of the repository directly.

**This slice mirrors slice 004 (authorization) as the template.** Open `dev/migration/004-authorization-service.md`
side-by-side. This file lists only the service-catalog-specific deltas.

## Prerequisites

- Branch `feature/migrate-to-pekko` is checked out.
- Step 004 is already committed (`git log --oneline | grep -E "feat\(authorization\): migrate service"`).
- Repo state at start: `authorization` runs on Pekko. Other services still on Lagom.
- External services running: Cassandra 3.11 on `:9042`, OpenSearch 2.8 on `:9200`.

## Goal

Migrate `service-catalog` end-to-end. **5 entities** (largest cluster-sharding footprint after
CMS): `CategoryEntity`, `ScopeEntity`, `ServiceItemEntity`, `ScopePrincipalEntity`,
`ServicePrincipalEntity`. 10 processors (Db + Index × 5).

**Callout — TWO-TRAIT PATTERN.** Unlike `authorization` (single-trait), `service-catalog`
uses the newer pattern:
- `ServiceCatalogServiceLagomApi extends Service` (the Lagom trait — at `application/service-catalog-api/src/main/scala/.../client/http/ServiceCatalogServiceLagomApi.scala`) → **becomes the `.proto`**.
- `ServiceCatalogService` (plain Scala trait at `application/service-catalog-api/src/main/scala/.../api/ServiceCatalogService.scala`) → **kept**; gateway injects this.
- `ServiceCatalogServiceLagomApiImpl` (server adapter at `application/service-catalog/src/main/scala/.../server/http/ServiceCatalogServiceLagomApiImpl.scala`) → **deleted** (no longer needed; the domain impl `ServiceCatalogServiceImpl` directly implements the generated trait).
- `ServiceCatalogServiceLagomImpl` (client wrapper at `application/service-catalog-api/src/main/scala/.../client/http/ServiceCatalogServiceLagomImpl.scala`) → **deleted** (replaced by a thin gRPC client wrapper that implements `ServiceCatalogService`).
- `ServiceCatalogServiceImpl` (domain impl at `application/service-catalog/src/main/scala/.../impl/ServiceCatalogServiceImpl.scala`) → **light edit**: change parent trait from `ServiceCatalogService` to the generated trait.

The gateway-side wiring line (`AnnetteApiLoader` line 189) currently references
`ServiceCatalogServiceLagomApi`. Adapt accordingly: it now constructs a gRPC client that the
new wrapper adapts back to `ServiceCatalogService`.

## Files in scope

**`service-catalog-api` module** (package root `biz.lobachev.annette.service_catalog`):
- `application/service-catalog-api/src/main/protobuf/service_catalog.proto` — **new**.
- `application/service-catalog-api/src/main/scala/biz/lobachev/annette/service_catalog/client/http/ServiceCatalogServiceLagomApi.scala` — **delete** (33 `pathCall`s).
- `application/service-catalog-api/src/main/scala/biz/lobachev/annette/service_catalog/client/http/ServiceCatalogServiceLagomImpl.scala` — **delete**.
- `application/service-catalog-api/src/main/scala/biz/lobachev/annette/service_catalog/api/ServiceCatalogService.scala` — **keep** (plain trait — gateway injects this).
- New file: `application/service-catalog-api/src/main/scala/biz/lobachev/annette/service_catalog/client/grpc/ServiceCatalogServiceGrpcImpl.scala` — **new** (adapts generated `ServiceCatalogServiceClient` → `ServiceCatalogService`).
- `application/service-catalog-api/src/main/scala/biz/lobachev/annette/service_catalog/api/{category,scope,item,scope_principal,service_principal}/Exceptions.scala` — **edit** (4 `Exceptions.scala` files per survey).
- `application/service-catalog-api/src/test/scala/biz/lobachev/annette/service_catalog/api/JsonSpec.scala` — **keep** (JSON-only unit spec, possibly stale per survey; do not modify).
- All other DTOs — **keep verbatim** (D1).

**`service-catalog` impl module:**
- `application/service-catalog/src/main/scala/biz/lobachev/annette/service_catalog/server/ServiceCatalogServiceLoader.scala` — **rewrite**.
- `application/service-catalog/src/main/scala/biz/lobachev/annette/service_catalog/server/http/ServiceCatalogServiceLagomApiImpl.scala` — **delete** (the Lagom adapter is no longer needed; the generated trait is implemented directly by `ServiceCatalogServiceImpl`).
- `application/service-catalog/src/main/scala/biz/lobachev/annette/service_catalog/impl/ServiceCatalogServiceImpl.scala` — **light edit** (parent trait swap: was `ServiceCatalogService`; now the generated `ServiceCatalogService` trait).
- `application/service-catalog/src/main/scala/biz/lobachev/annette/service_catalog/impl/category/CategoryEntity.scala` — **edit** line 113 (tagger `"ServiceCatalog_Category"` — verify).
- `application/service-catalog/src/main/scala/biz/lobachev/annette/service_catalog/impl/scope/ScopeEntity.scala` — **edit** line 122 (tagger `"ServiceCatalog_Scope"`).
- `application/service-catalog/src/main/scala/biz/lobachev/annette/service_catalog/impl/item/ServiceItemEntity.scala` — **edit** line 157 (tagger `"ServiceCatalog_Item"`).
- `application/service-catalog/src/main/scala/biz/lobachev/annette/service_catalog/impl/scope_principal/ScopePrincipalEntity.scala` — **edit** line 91 (tagger).
- `application/service-catalog/src/main/scala/biz/lobachev/annette/service_catalog/impl/service_principal/ServicePrincipalEntity.scala` — **edit** line 91 (tagger).
- 10 `*{Db,Index}EventProcessor.scala` files (one Db + one Index per entity, in each of the 5 packages) — **rewrite** as `ProjectionBase`.
- 5 `*SerializerRegistry.scala` files (one per entity package's `model/` subpackage) — **delete**.
- 5 `*DbDao.scala` files — **edit** (Pekko `CqlSession`).
- 5 `*IndexDao.scala` files — **keep**.

**`service-catalog` configs:**
- `application/service-catalog/conf/application.conf` — **edit**.
- `application/service-catalog/conf/application.dev.conf` — confirm port table.
- `application/service-catalog/conf/application.dc.conf` — **edit** (seed node `pekko://service-catalog@127.0.0.1:17362`).
- `application/service-catalog/conf/application.k8s.conf` — **edit** (service-name `"service-catalog"`, ports 17362/8511).
- `application/service-catalog/conf/indexing.conf` — **keep**.
- `application/service-catalog/src/main/resources/reference.conf` — **edit** (5 bindings → `pekko.jackson-json`).

**`build.sbt`:**
- `service-catalog-api` — drop `enablePlugins(LagomScala)`.
- `service-catalog` — drop Lagom; add Pekko; `.dependsOn(microservice-core-pekko)`.

**Gateway:**
- `api-gateway/api-gateway/app/biz/lobachev/annette/api_gateway/AnnetteApiLoader.scala` — **edit** line 189.
- `api-gateway/api-gateway/conf/application.conf` — **edit**: `pekko.grpc.client."service-catalog" { service-port = 8511 ... }`.
- All 6 gateway controllers under `api-gateway/service-catalog-api-gateway/src/main/scala/biz/lobachev/annette/service_catalog/gateway/*Controller.scala` (`CategoryController`, `ScopeController`, `ServiceItemController`, `ScopePrincipalController`, `ServicePrincipalController`, `UserServiceController`) — **keep unchanged**.

No other files.

## Locked assumptions

Same as slice 004. Specifically for `service-catalog`:
- Artery port: **17362**. HTTP/gRPC port: **8511**.
- Actor system name: `"service-catalog"`.
- **5 entities → 5 tagger replacements**.
- **Two-trait pattern** (see callout above).

## Steps

Follow slice 004 steps 1–10 with these pattern adjustments:

- **Step 1 (proto):** enumerate the 33 `pathCall`s in `ServiceCatalogServiceLagomApi.descriptor`
  (not `ServiceCatalogServiceApi` — that file does not exist for this service).
- **Step 2 (build/generate):** the generated trait is `ServiceCatalogService` (note: collides
  in name with the existing plain trait at `.../api/ServiceCatalogService.scala`). Resolve
  by renaming the plain trait to `ServiceCatalogServiceGateway` (or by moving the generated
  trait to a sub-package). Document the chosen approach in the commit message.
- **Step 3 (impl):** `ServiceCatalogServiceImpl` was already a clean domain impl separate
  from the Lagom adapter. The migration makes it implement the generated trait directly.
- **Step 5 (projections):** 10 handlers (Db + Index × 5 entities).
- **Step 7 (loader):** the file's location is `server/ServiceCatalogServiceLoader.scala` (not
  `impl/`).

## Verification

- `sbt 'project service-catalog-api' compile`
- `sbt 'project service-catalog' compile`
- `sbt 'project api-gateway' compile`
- `sbt 'service-catalog-api/testOnly biz.lobachev.annette.service_catalog.api.JsonSpec'`
  (the JSON unit spec — should still pass; if it references the now-deleted
  `api.group.Group` mentioned in the survey, mark it `pending` with a TODO and note in the
  commit message).
- **D5 cutover**: `TRUNCATE TABLE dev_service_catalog.snapshots;`.
- Smoke test (5 entities → 5 quick CRUD rounds; pick one each):
  ```bash
  curl -X POST http://127.0.0.1:9000/api/service-catalog/v1/createCategory -H "Authorization: Bearer $TOKEN" -d '{...}'
  curl -X POST http://127.0.0.1:9000/api/service-catalog/v1/createScope    -H "Authorization: Bearer $TOKEN" -d '{...}'
  curl -X POST http://127.0.0.1:9000/api/service-catalog/v1/createItem      -H "Authorization: Bearer $TOKEN" -d '{...}'
  # etc.
  grpcurl -plaintext 127.0.0.1:8511 list
  ```

## Commit

Stage only "Files in scope". Message:
```
feat(service-catalog): migrate service to pekko gRPC [009]
```

## Out of scope

Same as slice 004 §Out of scope, applied to `service-catalog`. Do not fix the
`api.group.Group` reference in `JsonSpec` if it is stale — note as TODO and `pending`.

## References

- `dev/migration/004-authorization-service.md` — the template.
- `dev/migrate-to-pekko.md` §4, §5.2, §5.3, §5.7, §5.8, §8 Phase 3.
- `dev/migration/003-core-recipe.md`.
