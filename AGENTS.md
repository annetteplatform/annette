# AGENTS.md

High-signal notes for working in the Annette Platform CE repo. Read this before editing or running anything.

## Stack

Scala 2.13 + sbt + **Apache Pekko 1.1.3** microservices (Pekko gRPC for inter-service calls, Pekko Persistence
Cassandra + Pekko Projection for event-sourced services, Slick/Postgres for bpm-repository, Pekko Connectors S3
for CMS files). The api-gateway is a **Play 3.0.11** app (org.playframework) exposing the public REST API and
delegating to the services over gRPC. OpenSearch via elastic4s 7.8.1, Keycloak auth, Camunda (BPM engine REST).
Published to Sonatype OSSRH under org `biz.lobachev.annette`; all Scala packages live under `biz.lobachev.annette.*`.
Zero Lagom/Akka dependencies remain (removed in migration slice 013 — see `dev/migration/`).

## Build prerequisites (easily missed)

- **JDK 11+ required.** `.jvmopts` uses `--add-opens=...` (Java 9+). Docker base image is `openjdk:11`.
- External services (Cassandra, OpenSearch, Postgres, Keycloak, Camunda, MinIO) are **never embedded** — bring them
  up yourself (`deploy/docker/deploy.sh`).
- `.sbtopts` sets `-Xmx8G -Xss32M -XX:MaxMetaspaceSize=2G`. Don't shrink this for the full build.
- `sbt-tpolecat` is enabled repo-wide → strict scalac flags. `microservice-core` additionally sets
  `-Wconf:cat=unused-nowarn:s`. New code must compile clean under `-Xlint`/deprecation as fatal.
- Play 3.0.11 drags Pekko 1.0.3 transitively; `dependencyOverrides` in `build.sbt` pin every module to 1.1.3.
  Don't remove them — the Pekko runtime aborts on mixed versions.

## External services required (dev + test)

Services, specs, and the gateway all need live backing services:

```bash
cd deploy/docker && ./deploy.sh        # Postgres, Cassandra 3.11, OpenSearch 2.8, MinIO, Keycloak, Camunda, Traefik, frontend
./demo-ignition.sh                      # after services are healthy — loads demo data
```

Most integration specs need a real Cassandra on `localhost:9042`, OpenSearch on `:9200`, Postgres on `:5432`,
Camunda on `:3090`. Expect tests to fail/hang otherwise. The four camunda specs share one engine and run
sequentially (`Test / parallelExecution := false`).

- OpenSearch 2.8 speaks **HTTPS** on `:9200` with a self-signed cert — `curl -k -u admin:admin ...`. Its disk
  flood-stage watermark **re-arms on container restart** and silently blocks index writes (symptom: projections
  retrying with `cluster_block_exception ... read-only-allow-delete`). Fix: raise the watermarks and clear
  `index.blocks.read_only_allow_delete` on affected indices.
- Keycloak serves on `:8080` **without** the legacy `/auth` prefix (`/realms/AnnetteDemo/...`, not
  `/auth/realms/...`). Credentials: master realm `admin`/`admin`; demo user `kristina.fisher`/`abc` (realm
  `AnnetteDemo`, public client `annette-console`).

## Dev mode (per-process, tmux)

There is **no `sbt runAll`** — each microservice is its own process. `run-local.sh` opens a tmux session with one
window per service plus the gateway (dev_ prefixes; gRPC ports from the table below):

```bash
./run-local.sh                 # all 9 services + api-gateway (tmux session "annette-dev")
./run-local.sh cms api-gateway # subset
tmux attach -t annette-dev
```

### Dev-mode port table (authoritative, migration decision D2)

| Service           | Artery port | HTTP/gRPC port |
|-------------------|-------------|----------------|
| application       | 17361       | 8510           |
| service-catalog   | 17362       | 8511           |
| authorization     | 17363       | 8512           |
| bpm-repository    | — (standalone, no cluster) | 8513 |
| cms               | 17365       | 8514           |
| subscriptions     | 17366       | 8515           |
| org-structure     | 17367       | 8516           |
| persons           | 17368       | 8517           |
| principal-groups  | 17369       | 8518           |
| api-gateway       | 17355       | 9000 (REST)    |

The gateway's gRPC client endpoints are `pekko.grpc.client.<service>` blocks in `api-gateway/api-gateway/conf/application.conf`.

## Module layout (real boundaries from `build.sbt`, ~30 sub-projects)

Each business domain is split into three layers — keep this when adding code:

| Layer        | Suffix / location                                               | Role                                                       |
|--------------|-----------------------------------------------------------------|------------------------------------------------------------|
| Service API  | `*-api`                                                         | proto-generated gRPC traits + DTOs + `*ServiceGrpcImpl` client adapters, depends only on `core` |
| Service impl | `application/`, `authorization/`, `bpm/`, `cms/`, `principals/` | `JavaAppPackaging` + Pekko persistence/projections, Docker |
| HTTP gateway | `*-api-gateway` (under `api-gateway/`)                          | Play controllers delegating to the plain `*Service` traits |

- `core/` = shared libs: `core`, `microservice-core`, `api-gateway-core`. Everything depends on `core`; microservices
  depend on `microservice-core`; gateways on `api-gateway-core`.
- `api-gateway/api-gateway` is the **single runnable Play 3 app** (port 9000). All gateway modules are aggregated
  into it. Controllers get the plain `*Service` traits, implemented by `*ServiceGrpcImpl` (proto↔domain conversion +
  `AnnetteGrpcExceptionMapping` error recovery).
- Microservice entrypoint convention: `biz.lobachev.annette.<service>.impl.<Service>Main` (plain `main()`, constructs
  the ActorSystem, shards entities, starts projections, binds gRPC on `annette.http.port`).
- **Startup contract (all three required; verified at runtime in slices 011/013):**
  1. `main()` must end with `Await.ready(system.whenTerminated, Duration.Inf)` — sbt (and the packaged app) kill the
     JVM once `main` returns.
  2. Loaders must call `Await.result(ProjectionBase.initAll(), 30.seconds)` before binding — it creates the projection
     offset tables AND runs `CassandraReadJournal.initialize()` (journal/snapshot keyspaces, `tag_views`). On a fresh
     keyspace, skipping it fails every projection with "Keyspace ... does not exist" / "unconfigured table tag_views".
  3. Every DbDao needs its `createTables()` awaited at startup (idempotent) — otherwise read-side queries fail with
     "unconfigured table <name>".
- `ignition/demo-ignition` is a standalone app (not a service) that seeds demo data over gRPC; `run-ignition.sh` runs it.

## gRPC wiring & exception mapping

- `GrpcClientSettings.fromConfig("<client-name>")` takes the client **name only** (`"persons"`, not
  `"pekko.grpc.client.persons"`) — the prefix is applied internally. Passing the full path doubles it and fails only
  at first call (`Config path 'pekko.grpc.client.pekko.grpc.client.<name>' does not exist`).
- Server side: bind handlers via `XxxServiceHandler(api, AnnetteGrpcExceptionMapping.serverHandlerOrDefault _)`
  (eta-expanded).
- Client side: every generated-client call goes through `AnnetteGrpcExceptionMapping.recoverAnnette`, which decodes
  the `annette-error-bin` trailer back into `AnnetteTransportException` with a **canonical** `TransportErrorCode`
  (400→BadRequest etc.). Non-canonical codes silently break every typed `case XxxAlreadyExist(...)` unapply match
  (companion `unapply` compares `errorCode` for equality) — this broke demo-ignition upserts until fixed.
- HTTP→gRPC status mapping: 400→INVALID_ARGUMENT, 403→PERMISSION_DENIED, 404→NOT_FOUND, else INTERNAL.

## Config selection (per environment)

Each service ships four HOCON files in `conf/`; pick with `-Dconfig.resource=`:

- `application.conf` — base (always included)
- `application.dev.conf` — local dev (`run-local.sh`, `run-ignition.sh`, `test-local.sh`)
- `application.dc.conf` — docker-compose (referenced via `JAVA_OPTS` in `deploy/docker/env/*.env`)
- `application.k8s.conf` — Kubernetes (`deploy/k8s/`)

The `conf/` dir is added to runtime unmanaged classpath by `confDirSettings` in `build.sbt` — runtime configs are NOT
under `src/main/resources`.

### Cassandra service config checklist

Every Cassandra-backed service's `application.conf` must carry all of the following (verified at runtime in slices
011/013; `cms/cms/conf/application.conf` is the reference implementation):

- `pekko.actor.provider = cluster` and `pekko.http.server.preview.enable-http2 = on` (gRPC needs h2c; without it
  clients die with `Http2Exception: First received frame was not SETTINGS`).
- A **nested** `cassandra-quill { session { contact-points, credentials } keyspace, keyspace-autocreate }` block —
  a flat `cassandra-quill = ${cassandra.default}` alias breaks the Quill context ("No configuration setting found
  for key 'session'"; the Pekko-variant `CassandraQuillDao` reads `session.*` directly).
- A `datastax-java-driver` block (contact points + `load-balancing-policy.local-datacenter` + auth) — both Pekko
  Persistence Cassandra and Pekko Projection resolve their session against it.
- Explicit `pekko.persistence.journal.plugin` / `pekko.persistence.snapshot-store.plugin` ids plus
  `keyspace-autocreate = true` / `tables-autocreate = true` in the journal/snapshot blocks.
- `events-by-tag` keys live **directly** under `pekko.persistence.cassandra.events-by-tag.*` — the Lagom-era
  `journal.events-by-tag` / `query.events-by-tag` paths are silently ignored (defaults = 5s EC delay, 2015
  first-time-bucket → projections stall scanning thousands of empty day-buckets).
- Dev: single-member self-join `pekko.cluster.seed-nodes = ["pekko://<system-name>@127.0.0.1:<artery-port>"]` in
  `application.dev.conf` — without it the cluster never forms and ClusterSharding asks time out.

## Env-var namespacing (don't cross-contaminate)

These prefixes isolate environments and are read at service startup. Setting them wrong will read/write another
environment's data:

- `KEYSPACE_PREFIX` (Cassandra), `INDEX_PREFIX` (OpenSearch), `CMS_STORAGE_BUCKET_PREFIX`, `MINIO_PREFIX`,
  `POSTGRES_PREFIX`
- Auth: `KEYCLOAK_URL`, `KEYCLOAK_REALM`, `KEYCLOAK_CLIENT`
- See `run-local.sh` (dev_*), `run-cm.sh` (cm_*), `test-local.sh` (test*), `deploy/docker/env/ms.env` (annette_) for the
  per-environment conventions.

## Common commands

```bash
# Compile everything
sbt compile

# Run all services locally in dev mode (tmux, one window per process; needs external services + dev_ prefixes)
./run-local.sh

# Run ignition to seed demo data
./run-ignition.sh                       # sbt -Dconfig.resource=application.dev.conf demo-ignition/run

# All tests
sbt test

# Single spec / single service
./test-local.sh                         # example: runs BpmModelServiceSpec with test1_ prefixes
sbt 'bpm-repository/testOnly biz.lobachev.annette.bpm_repository.test.BpmModelServiceSpec'
sbt persons/test                        # one service

# Build + publish Docker images locally
./build-local.sh                        # sbt clean docker:publishLocal
# NOTE: build-local.sh ALSO tags and pushes to reg.cloud.ambergate.ru (maintainer-private registry).
#       Do not run it unless you intend that push.

# E2E smoke through the gateway (needs seeded demo data)
TOKEN=$(curl -s -X POST "http://localhost:8080/realms/AnnetteDemo/protocol/openid-connect/token" \
  -d "grant_type=password&client_id=annette-console&username=kristina.fisher&password=abc" \
  -H "Content-Type: application/x-www-form-urlencoded" | jq -r .access_token)
curl -s -X POST localhost:9000/api/annette/v1/person/findPersons \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"offset":0,"size":2,"filter":""}'       # expect 200, {"total":1000,...}
# Sweep one find* call per REST-exposed category: application, authorization, serviceCatalog,
# principalGroup, person, orgStructure, bpm, cms. subscriptions is internal-only (no public routes).
```

## Testing quirks

- Service tests are forked (`Test / fork := true`) — slow startup, expect ~tens of seconds per spec.
- The service-level `*ServiceApiSpec`s of the Cassandra services were disabled during the migration (`.disabled`
  suffix); they need a rewrite onto the Pekko gRPC testkit. bpm-repository (Slick/Postgres) and camunda specs are
  live and green.
- Camunda specs share one engine → sequential execution is enforced.
- Use distinct prefixes (`KEYSPACE_PREFIX=test_`, `INDEX_PREFIX=test-`) when running tests against a shared cluster.

## Formatting

`.scalafmt.conf` (scalafmt 2.6.3, 120 cols, `rewrite.rules = [SortImports, RedundantBraces]`, `align = most`). **No
sbt-scalafmt plugin** is registered — format via the `scalafmt` CLI or IDE, not `sbt scalafmtAll`.

## Publishing

`publishTo` targets Sonatype OSSRH (`oss.sonatype.org`). `sbt publish` / `publishSigned` are for releases — do not
invoke casually. `version` is `0.6.0` (set in `build.sbt` and mirrored in `deploy/docker/deploy.sh`,
`deploy/docker/demo-ignition.sh`, `build-local.sh`).

## Operational notes

- No CI workflows, no pre-commit hooks — this is the canonical agent guide.
- `.bsp/`, `.idea/`, `target/`, `.DS_Store` are present but gitignored; don't commit them.
- Docker images are tagged `annetteplatform/<service>:<version>` by `dockerSettings`.
- The Pekko migration (slices 001–013) is documented in `dev/migration/`; decisions (tagging, exception mapping,
  port table) are locked in `dev/migration/001-decisions.md`.
