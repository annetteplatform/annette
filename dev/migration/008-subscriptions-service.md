# 008 — Subscriptions service

## Role

You are a senior Scala/Pekko engineer performing one vertical slice of the Annette Platform
migration. You execute against the working tree of the repository directly.

**This slice mirrors slice 004 (authorization) as the template.** Open `dev/migration/004-authorization-service.md`
side-by-side. This file lists only the subscriptions-specific deltas.

## Prerequisites

- Branch `feature/migrate-to-pekko` is checked out.
- Step 004 is already committed (`git log --oneline | grep -E "feat\(authorization\): migrate service"`).
- Repo state at start: `authorization` runs on Pekko. Other services still on Lagom.
- External services running: Cassandra 3.11 on `:9042`, OpenSearch 2.8 on `:9200`.

## Goal

Migrate `subscriptions` end-to-end. **2 entities** (`SubscriptionEntity`,
`SubscriptionTypeEntity`), 4 processors. This service lives under the `cms/` directory but is
a **separate microservice** (separate Lagom service descriptor, separate loader, separate
Cassandra keyspace `subscriptions`). Do not conflate it with slice 011 (`cms`).

**Callout — no dedicated gateway module.** The survey confirms there is no
`api-gateway/subscriptions-api-gateway/` sub-project. The subscriptions service is wired
**directly** into `AnnetteApiLoader` line 195 with no controller layer. The gateway swap is
one line (no controller file edits).

## Files in scope

**`subscriptions-api` module** (package root `biz.lobachev.annette.subscription` — singular):
- `cms/subscriptions-api/src/main/protobuf/subscription.proto` — **new**.
- `cms/subscriptions-api/src/main/scala/biz/lobachev/annette/subscription/api/SubscriptionServiceApi.scala` — **delete** (13 `pathCall`s).
- `cms/subscriptions-api/src/main/scala/biz/lobachev/annette/subscription/api/SubscriptionService.scala` — **keep** (plain trait).
- `cms/subscriptions-api/src/main/scala/biz/lobachev/annette/subscription/api/SubscriptionServiceImpl.scala` — **rewrite**.
- `cms/subscriptions-api/src/main/scala/biz/lobachev/annette/subscription/api/subscription/Exceptions.scala` — **edit**.
- `cms/subscriptions-api/src/main/scala/biz/lobachev/annette/subscription/api/subscription_type/Exceptions.scala` — **edit**.
- All other DTOs — **keep verbatim** (D1).

**`subscriptions` impl module:**
- `cms/subscriptions/src/main/scala/biz/lobachev/annette/subscription/impl/SubscriptionServiceLoader.scala` — **rewrite** (note: this file also declares the aggregate `SubscriptionRepositorySerializerRegistry` object — that line is removed; bindings move to `reference.conf`).
- `cms/subscriptions/src/main/scala/biz/lobachev/annette/subscription/impl/SubscriptionServiceApiImpl.scala` — **light edit**.
- `cms/subscriptions/src/main/scala/biz/lobachev/annette/subscription/impl/subscription/SubscriptionEntity.scala` — **edit** line 109 (tagger `"Subscriptions_Subscription"` — verify).
- `cms/subscriptions/src/main/scala/biz/lobachev/annette/subscription/impl/subscription_type/SubscriptionTypeEntity.scala` — **edit** line 103 (tagger `"Subscriptions_SubscriptionType"` — verify).
- `cms/subscriptions/src/main/scala/biz/lobachev/annette/subscription/impl/subscription/SubscriptionDbEventProcessor.scala` — **rewrite** as `ProjectionBase`.
- `cms/subscriptions/src/main/scala/biz/lobachev/annette/subscription/impl/subscription/SubscriptionIndexEventProcessor.scala` — **rewrite** as `ProjectionBase`.
- `cms/subscriptions/src/main/scala/biz/lobachev/annette/subscription/impl/subscription_type/SubscriptionTypeDbEventProcessor.scala` — **rewrite** as `ProjectionBase`.
- `cms/subscriptions/src/main/scala/biz/lobachev/annette/subscription/impl/subscription_type/SubscriptionTypeIndexEventProcessor.scala` — **rewrite** as `ProjectionBase`.
- `cms/subscriptions/src/main/scala/biz/lobachev/annette/subscription/impl/subscription/model/SubscriptionSerializerRegistry.scala` — **delete**.
- `cms/subscriptions/src/main/scala/biz/lobachev/annette/subscription/impl/subscription_type/model/SubscriptionTypeSerializerRegistry.scala` — **delete**.
- `cms/subscriptions/src/main/scala/biz/lobachev/annette/subscription/impl/subscription/dao/SubscriptionDbDao.scala` — **edit**.
- `cms/subscriptions/src/main/scala/biz/lobachev/annette/subscription/impl/subscription_type/dao/SubscriptionTypeDbDao.scala` — **edit**.
- `cms/subscriptions/src/main/scala/biz/lobachev/annette/subscription/impl/subscription/dao/SubscriptionIndexDao.scala` — **keep**.
- `cms/subscriptions/src/main/scala/biz/lobachev/annette/subscription/impl/subscription_type/dao/SubscriptionTypeIndexDao.scala` — **keep**.

**`subscriptions` configs:**
- `cms/subscriptions/conf/application.conf` — **edit** (indexing block is inline).
- `cms/subscriptions/conf/application.dev.conf` — confirm port table.
- `cms/subscriptions/conf/application.dc.conf` — **edit** (seed node `pekko://subscriptions@127.0.0.1:17366`).
- `cms/subscriptions/conf/application.k8s.conf` — **edit** (service-name `"subscriptions"`, ports 17366/8515).
- `cms/subscriptions/src/main/resources/reference.conf` — **edit**.

**`build.sbt`:**
- `subscriptions-api` — drop `enablePlugins(LagomScala)`.
- `subscriptions` — drop Lagom (this project also pulls `lagomScaladslKafkaClient` per build.sbt survey; drop it — analysis §3.2 confirms **zero actual Kafka usage**); add Pekko; `.dependsOn(microservice-core-pekko)`.

**Gateway:**
- `api-gateway/api-gateway/app/biz/lobachev/annette/api_gateway/AnnetteApiLoader.scala` — **edit** line 195.
- `api-gateway/api-gateway/conf/application.conf` — **edit**: `pekko.grpc.client.subscriptions { service-port = 8515 ... }`.

No controller files (none exist for subscriptions). No other files.

## Locked assumptions

Same as slice 004. Specifically for `subscriptions`:
- Artery port: **17366**. HTTP/gRPC port: **8515**.
- Actor system name: `"subscriptions"`.
- 2 entities → 2 tagger replacements.
- Single-trait pattern.
- **No specs exist** — verification relies on the smoke test alone.

## Steps

Follow slice 004 steps 1–10. Specific notes:

- **Step 1 (proto):** 13 `pathCall`s.
- **Step 5 (projections):** 4 handlers.

## Verification

- `sbt 'project subscriptions-api' compile`
- `sbt 'project subscriptions' compile`
- `sbt 'project api-gateway' compile`
- **D5 cutover**: `TRUNCATE TABLE dev_subscriptions.snapshots;`.
- Smoke test:
  ```bash
  curl -X POST http://127.0.0.1:9000/api/subscriptions/v1/createSubscriptionType \
       -H "Authorization: Bearer $TOKEN" -d '{"id":"smoke-type","name":"Smoke"}'
  curl http://127.0.0.1:9000/api/subscriptions/v1/getSubscriptionType/smoke-type -H "Authorization: Bearer $TOKEN"
  grpcurl -plaintext 127.0.0.1:8515 list
  ```
  Note: verify the exact route paths in `api-gateway/api-gateway/conf/routes` (search for
  `subscriptions`).

## Commit

Stage only "Files in scope". Message:
```
feat(subscriptions): migrate service to pekko gRPC [008]
```

## Out of scope

Same as slice 004 §Out of scope, applied to `subscriptions`. **Do not** migrate any `cms/cms`
files (slice 011).

## References

- `dev/migration/004-authorization-service.md` — the template.
- `dev/migrate-to-pekko.md` §3.2 — confirms zero Kafka usage (justify dropping `lagomScaladslKafkaClient`).
- `dev/migrate-to-pekko.md` §4, §5.2, §5.3, §5.7, §5.8, §8 Phase 3.
- `dev/migration/003-core-recipe.md`.
