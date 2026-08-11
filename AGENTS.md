# AGENTS.md

High-signal notes for working in the Annette Platform CE repo. Read this before editing or running anything.

## Stack

Scala 2.13.9 + sbt 1.10.10 + **Lagom 1.6.7** (Scala DSL) microservices. Akka cluster sharding + Cassandra persistence,
OpenSearch/Elastic via elastic4s 7.8.1, Keycloak auth, external Kafka, Postgres (bpm-repository only), MinIO/S3 (CMS).
Published to Sonatype OSSRH under org `biz.lobachev.annette`; all Scala packages live under `biz.lobachev.annette.*`.

## Build prerequisites (easily missed)

- **JDK 11+ required.** `.jvmopts` uses `--add-opens=...` (Java 9+). Docker base image is `openjdk:11`.
- **Embedded Lagom Kafka and Cassandra are disabled** in `build.sbt` (`lagomKafkaEnabled := false`,
  `lagomCassandraEnabled := false`). You must run external services yourself.
- `.sbtopts` sets `-Xmx8G -Xss32M -XX:MaxMetaspaceSize=2G`. Don't shrink this for the full build.
- `sbt-tpolecat` is enabled repo-wide → strict scalac flags. `microservice-core` additionally sets
  `-Wconf:cat=unused-nowarn:s`. New code must compile clean under `-Xlint`/deprecation as fatal.

## External services required (dev + test)

`runAll`, the Play entrypoint, and most specs all need live backing services. Bring them all up with:

```bash
cd deploy/docker && ./deploy.sh        # Postgres, Cassandra 3.11, OpenSearch 2.8, MinIO, Keycloak, Camunda, Traefik, frontend
./demo-ignition.sh                      # after services are healthy — loads demo data
```

Tests spin up keyspaces via `ServiceTest.defaultSetup.withCassandra(true)` but **do not start a Cassandra server** — a
real Cassandra must already be on `localhost:9042`. Same for OpenSearch on `:9200`. Expect tests to fail/hang otherwise.

### Dev-mode service ports (post-Pekko migration)

After slice 002 the dev-mode port table is authoritative (D2). Microservices run as their own
processes (no Lagom `runAll`). Pekko Artery ports 17361–17369 are reserved; HTTP/gRPC ports
8510–8518 mirror the existing docker-compose host-port allocation.

| Service           | Artery port | HTTP/gRPC port |
|-------------------|-------------|----------------|
| application       | 17361       | 8510           |
| service-catalog   | 17362       | 8511           |
| authorization     | 17363       | 8512           |
| bpm-repository    | 17364       | 8513           |
| cms               | 17365       | 8514           |
| subscriptions     | 17366       | 8515           |
| org-structure     | 17367       | 8516           |
| persons           | 17368       | 8517           |
| principal-groups  | 17369       | 8518           |
| api-gateway       | 17355       | 9000           |

## Module layout (real boundaries from `build.sbt`, ~30 sub-projects)

Each business domain is split into three layers — keep this when adding code:

| Layer        | Suffix / location                                               | Role                                                       |
|--------------|-----------------------------------------------------------------|------------------------------------------------------------|
| Service API  | `*-api`                                                         | Lagom service trait + DTOs, depends only on `core`         |
| Service impl | `application/`, `authorization/`, `bpm/`, `cms/`, `principals/` | `LagomScala` + Cassandra persistence, forked tests, Docker |
| HTTP gateway | `*-api-gateway` (under `api-gateway/`)                          | Play/JWT facade delegating to service APIs                 |

- `core/` = shared libs: `core`, `microservice-core`, `api-gateway-core`. Everything depends on `core`; microservices
  depend on `microservice-core`; gateways on `api-gateway-core`.
- `api-gateway/api-gateway` is the **single runnable Play app** (`LagomPlay` + `LagomScala` enabled, port 9000). All
  gateway modules are aggregated into it.
- Microservice entrypoint convention: `biz.lobachev.annette.<service>.impl.<Service>Loader`, wired in each
  `conf/application.conf` via `play.application.loader`.
- `ignition/demo-ignition` is a standalone Java app (not a service) that seeds demo data; `run-ignition.sh` runs it.

## Config selection (per environment)

Each service ships four HOCON files in `conf/`; pick with `-Dconfig.resource=`:

- `application.conf` — base (always included)
- `application.dev.conf` — local dev (`run-local.sh`, `run-ignition.sh`, `test-local.sh`)
- `application.dc.conf` — docker-compose (referenced via `JAVA_OPTS` in `deploy/docker/env/*.env`)
- `application.k8s.conf` — Kubernetes (`deploy/k8s/`)

The `conf/` dir is added to runtime unmanaged classpath by `confDirSettings` in `build.sbt` — runtime configs are NOT
under `src/main/resources`.

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

# Run all services locally in dev mode (one JVM via Lagom runAll; needs external services + dev_ prefixes)
./run-local.sh

# Run ignition to seed demo data
./run-ignition.sh                       # sbt -Dconfig.resource=application.dev.conf demo-ignition/run

# All tests (forked per service via lagomForkedTestSettings)
sbt test

# Single spec / single service
./test-local.sh                         # example: runs PersonServiceApiSpec with test1_ prefixes
sbt 'persons/testOnly biz.lobachev.annette.person.test.PersonServiceApiSpec'
sbt persons/test                        # one service

# Build + publish Docker images locally
./build-local.sh                        # sbt clean docker:publishLocal
# NOTE: build-local.sh ALSO tags and pushes to reg.cloud.ambergate.ru (maintainer-private registry).
#       Do not run it unless you intend that push.
```

## Testing quirks

- Tests are **forked** (`lagomForkedTestSettings`) per service module — slow startup, expect ~tens of seconds per spec.
- Integration specs (`*ServiceApiSpec`) need live Cassandra + OpenSearch; entity specs (`*EntitySpec`) may not. Check
  `Abstract*ServiceApiSpec` before assuming.
- Use distinct prefixes (`KEYSPACE_PREFIX=test_`, `INDEX_PREFIX=test-`) when running tests against a shared cluster.

## Formatting

`.scalafmt.conf` (scalafmt 2.6.3, 120 cols, `rewrite.rules = [SortImports, RedundantBraces]`, `align = most`). **No
sbt-scalafmt plugin** is registered — format via the `scalafmt` CLI or IDE, not `sbt scalafmtAll`.

## Publishing

`publishTo` targets Sonatype OSSRH (`oss.sonatype.org`). `sbt publish` / `publishSigned` are for releases — do not
invoke casually. `version` is `0.5.1` (set in `build.sbt` and mirrored in `deploy/docker/deploy.sh`, `build-local.sh`).

## Operational notes

- No CI workflows, no pre-commit hooks, no OpenCode/Cursor/Claude instruction files exist yet — this is the canonical
  agent guide.
- `.bsp/`, `.idea/`, `target/`, `.DS_Store` are present but gitignored; don't commit them.
- Docker images are tagged `annetteplatform/<service>:<version>` by `dockerSettings`.
