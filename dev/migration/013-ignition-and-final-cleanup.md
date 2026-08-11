# 013 — Ignition + final cleanup

## Role

You are a senior Scala/Pekko engineer performing the final slice of the Annette Platform
migration. You execute against the working tree of the repository directly.

## Prerequisites

- Branch `feature/migrate-to-pekko` is checked out.
- **Slices 005–012 are ALL committed.** Verify with:
  ```bash
  git log --oneline | grep -E "feat\((persons|principal-groups|org-structure|subscriptions|service-catalog|application|cms|bpm-repository)\): migrate"
  # expect 8 commits (slice 004 was authorization, also present)
  ```
- Repo state at start of this step: **all 9 microservices run on Pekko.** The gateway speaks
  gRPC to every service. The Lagom plugin and Lagom service-client path are still in place
  but **unused** by any service or any gateway line — they are dead code. The
  `microservice-core-pekko` shim (slice 003) coexists with the old `microservice-core` Lagom
  variant; nothing references the Lagom variant any more.
- External services running: full `deploy/docker/deploy.sh` stack (Cassandra, OpenSearch,
  Postgres, MinIO, Keycloak, Camunda).

## Goal

Remove every remaining Lagom artifact: drop the `lagom-sbt-plugin`, delete the
`microservice-core` Lagom variant (fold its distinct contents into `microservice-core-pekko`
or just delete if all contents have been ported), sweep all `lagom.*` config keys, rewrite
`demo-ignition`'s `IgnitionLagomClient` off `StandaloneLagomClientFactory`, remove the
dual-protocol gateway dead code, update documentation. At the end of this step the codebase
has **zero `com.lightbend.lagom` imports, zero `lagom.*` config keys, zero Lagom sbt
plugins**. The migration feature branch is eligible for PR.

## Files in scope

**Build:**
- `project/plugins.sbt` — **remove** `addSbtPlugin("com.lightbend.lagom" % "lagom-sbt-plugin" % "1.6.7")`.
- `project/Dependencies.scala` — **remove** from `Dependencies.quill` the `quill-cassandra-lagom` artifact (keep `quill-core` + `quill-cassandra`). **Remove** `Dependencies.lagomAkkaDiscovery` (the entire `val`); replace any remaining usage with `Dependencies.pekkoManagement`. **Remove** `Dependencies.alpakkaS3` (replaced by `Dependencies.pekkoConnectorsS3` in slice 011).
- `build.sbt` — **remove** every occurrence of: `lagomKafkaEnabled`, `lagomCassandraEnabled`, `lagomForkedTestSettings`, `enablePlugins(LagomScala)`, `enablePlugins(LagomPlay)`, `lagomScaladslPersistenceCassandra`, `lagomScaladslServer`, `lagomScaladslTestKit`, `lagomScaladslKafkaClient`, `lagomScaladslApi`, `ws` (if sourced from Lagom; verify whether `com.typesafe.play %% ws` is needed standalone). Confirm via:
  ```bash
  rg -n "lagom|Lagom" build.sbt
  # after the edits, this must return zero matches
  ```

**Ignition:**
- `ignition/demo-ignition/src/main/scala/biz/lobachev/annette/ignition/core/IgnitionLagomClient.scala` — **rename to `IgnitionGrpcClient.scala`** + **rewrite**: replace `extends StandaloneLagomClientFactory("IgnitionServiceClient") with AhcWSComponents with AnnetteDiscoveryComponents` with a class that constructs a `GrpcClientSettings` per service and instantiates the generated `*ServiceClient` classes. The seeding logic in the `EntityLoader` classes that consume this client is **unchanged in shape** — each `loader.xxx` call drops the `.invoke()` indirection (analysis §5.5 #4 applies here too).
- `ignition/demo-ignition/src/main/scala/biz/lobachev/annette/ignition/core/EntityLoader.scala` — **edit** if it references `akka.pattern.CircuitBreakerOpenException` (analysis §3.3); rename to `pekko.pattern.CircuitBreakerOpenException` or drop if unused.
- All other ignition files under `ignition/demo-ignition/src/main/scala/**` (77 files per survey) — mechanical `.invoke()` removal in service-call sites + `akka.*` → `pekko.*` import renames where present. Run:
  ```bash
  rg -l "com.lightbend.lagom|import akka\." ignition/demo-ignition/src/main/scala/
  ```
  and edit each file in the resulting list. The Keycloak/Camunda seed data files are unchanged.

**Core libs:**
- `core/microservice-core/` — **delete the entire directory** if its contents have been fully ported to `core/microservice-core-pekko/` (which should be the case after slices 004–012). Verify with:
  ```bash
  rg -l "microservice_core\." --type scala | xargs -I{} grep -L "microservice_core.pekko" {}
  ```
  Any file that still references the un-Pekko'd `microservice_core` package must be updated
  first. If anything remains, port it before deleting.
- `core/microservice-core-pekko/` — **rename to `core/microservice-core/`** (drop the `-pekko` suffix; the Pekko variant is now the only variant). Update `build.sbt`'s project name and every `.dependsOn(microservice-core-pekko)` line to `.dependsOn(microservice-core)`.
- `core/core/src/main/scala/biz/lobachev/annette/core/exception/AnnetteTransportException.scala` — **edit**: drop the Lagom `TransportErrorCode` import; the field stays a plain case-class field. Update `AnnetteTransportExceptionCompanion*.scala` (8 files) to drop `TransportErrorCode` references in favor of the mapping in `001-decisions.md` §D.
- `core/core/src/main/scala/biz/lobachev/annette/core/exception/AnnetteTransportExceptionSerializer.scala` — **delete** (Lagom-only; replaced by the gRPC exception handler introduced in slice 004).
- `core/core/src/main/scala/biz/lobachev/annette/core/discovery/AnnetteDiscoveryComponents.scala` — **delete** (Lagom-only).
- `core/core/src/main/scala/biz/lobachev/annette/core/discovery/AnnetteDiscoveryServiceLocator.scala` — **delete** (Lagom-only; replaced by `PekkoDiscovery` for gRPC clients).
- `core/core/src/test/scala/biz/lobachev/annette/core/test/exception/SerializerSpec.scala` — **rewrite or delete** (it tests the deleted serializer). Replace with a spec for the new `GrpcExceptionMapper` if appropriate.

**Gateway:**
- `api-gateway/api-gateway/app/biz/lobachev/annette/api_gateway/AnnetteApiLoader.scala` — **edit**: drop the `LagomServiceClientComponents` mix-in from `ServiceGateway`; drop the `lagomServiceClient` field; drop any remaining `serviceClient.implement[...]` lines (there should be zero after slices 004–012 — verify with `rg "serviceClient.implement" api-gateway/`). Confirm all 9 clients are now `*ServiceClient(grpcClientFactory.clientFor(...))`.
- `api-gateway/api-gateway/conf/application.conf` — **sweep** for any remaining `lagom.*` or `akka.*` keys. The `pekko.*` keys (added in slices 002, 004–012) are now authoritative.
- `api-gateway/api-gateway/conf/application.{dev,dc,k8s}.conf` — **sweep** (same).

**Per-service configs (sweep all 9 services + api-gateway):**
For each of `application/application`, `application/service-catalog`, `authorization/authorization`,
`bpm/bpm-repository`, `cms/cms`, `cms/subscriptions`, `principals/org-structure`,
`principals/persons`, `principals/principal-groups`, and `api-gateway/api-gateway`:
- All `conf/application*.conf` files and `src/main/resources/reference.conf` — **remove** any remaining `akka.*` keys (now redundant since `pekko.*` keys are in place from slices 002/004–012) and any `lagom.*` keys.
- Verify each file with:
  ```bash
  rg -n "akka\.|lagom\." <path-to-conf>
  # expect zero matches
  ```

**Deploy:**
- `deploy/docker/env/ms.env` — **edit**: drop `-Dlagom.cluster.join-self=on -Dlagom.cluster.bootstrap.enabled=false`; replace with `-Dpekko.management.cluster.bootstrap.form-single-member-cluster=on` for each service. Keep `KEYSPACE_PREFIX`, `INDEX_PREFIX`, `CMS_STORAGE_BUCKET_PREFIX`, `MINIO_PREFIX`, `POSTGRES_PREFIX` env-var conventions unchanged.
- `deploy/docker/deploy.sh` — **edit**: version bump (per `build-local.sh` convention; update the `version=...` line).
- `deploy/docker/demo-ignition.sh` — **edit**: version bump.
- `deploy/docker/README.md` — **edit**: document the new per-process dev mode (no `sbt runAll`; see slice 002's port table in `AGENTS.md`).
- `deploy/k8s/config.yml` — **edit**: rename every embedded `akka.*` key in ConfigMap blocks to `pekko.*`; update Artery container ports 25520 → 17355–17369 per the service port table; fix `service-name` for bpm-repository (the slice 012 fix is also reflected here if k8s config embeds it).
- `deploy/k8s/backend.yml` — **edit**: update each Deployment's container ports (Artery 1736X, HTTP/gRPC 851X); drop any `-Dlagom.*` from `JAVA_OPTS`-equivalent env.

**Root scripts:**
- `run-local.sh` — **rewrite**: this currently runs `sbt runAll` (Lagom-specific). Replace with one-process-per-service: e.g. an `xterm -e sbt 'project authorization' run` per service, or a `tmux`-based script, or a `docker-compose`-only dev flow. Document the chosen approach in `AGENTS.md`. Reference the dev-mode port table.
- `run-ignition.sh` — **edit**: works unchanged (already `sbt -Dconfig.resource=application.dev.conf demo-ignition/run`); just verify it still boots post-cleanup.
- `test-local.sh` — **edit**: update the example spec invocation to use a Pekko-migrated module.
- `build-local.sh` — **edit**: per AGENTS.md this script also tags and pushes to `reg.cloud.ambergate.ru`. **Do not change that behavior** — only the version line if needed.
- `run-cm.sh` — **edit** if it references `sbt runAll` or `lagom.*`; otherwise keep.

**Documentation:**
- `AGENTS.md` — **rewrite** the "Stack", "Build prerequisites", "Common commands", and "External services required" sections to reflect Pekko (not Lagom), the new dev-mode commands (no `runAll`), the port table from slice 002, and the per-process model. Drop the references to `lagomKafkaEnabled`/`lagomCassandraEnabled`/`lagomForkedTestSettings`.
- `README.md` — **edit** (if it references Lagom or `sbt runAll`).
- `deploy/docker/README.md` — **edit** per above.

No other files. In particular, do **not** change the public `routes` file (D7) or any
controller body.

## Locked assumptions

- **A1 / D7** Public REST contract frozen — still applies. No route changes.
- **D6** gRPC reflection stays enabled.
- All 9 services + gateway are on Pekko. This step removes the dead Lagom path.
- **A4** One commit per step (this slice). No multi-commit cleanup.

## Steps

### 1. Confirm dual-protocol gateway is fully dead

```bash
rg "serviceClient\.implement" api-gateway/         # expect zero matches
rg "LagomServiceClientComponents" api-gateway/     # expect the one definition; will be removed
rg "com.lightbend.lagom" --type scala | wc -l      # baseline count; must be zero after this step
```

If any `serviceClient.implement` remains, a service slice (004–012) was incomplete — halt and
return to that slice.

### 2. Remove `lagom-sbt-plugin`

`project/plugins.sbt`: delete the Lagom plugin line. `sbt reload` must succeed. If any
project still has `enablePlugins(LagomScala)` or `enablePlugins(LagomPlay)`, sbt will fail to
reload — fix the remaining `build.sbt` occurrences first.

### 3. Sweep `build.sbt` and `Dependencies.scala`

Apply all build removals listed in "Files in scope". Run `sbt clean compile` after each
removal batch to catch dangling references.

### 4. Delete the Lagom `microservice-core` variant

```bash
git rm -r core/microservice-core/
mv core/microservice-core-pekko core/microservice-core
# update build.sbt project name + every .dependsOn(microservice-core-pekko) reference
```

### 5. Delete Lagom-only core files

`AnnetteTransportExceptionSerializer.scala`, `AnnetteDiscoveryComponents.scala`,
`AnnetteDiscoveryServiceLocator.scala` — `git rm`. Rewrite `SerializerSpec.scala` (or delete).

### 6. Rewrite `IgnitionLagomClient` → `IgnitionGrpcClient`

Per analysis §5.10: replace `StandaloneLagomClientFactory` with `GrpcClientSettings`-based
construction. The 77 ignition files (per survey) need a mechanical `.invoke()` removal pass
where they call service APIs — but their seeding *logic* (ElasticSearch writes, service API
calls) is unchanged.

### 7. Sweep remaining `lagom.*` config

For every `application*.conf` and `reference.conf` in the repo:
```bash
rg -l "lagom\." --type-add 'conf:*.conf' --type conf
```
Edit each file to remove `lagom.*` blocks. Most were already cleaned in slices 004–012; this
catches anything missed.

### 8. Sweep remaining `akka.*` config and source

For source:
```bash
rg -l "^import akka\." --type scala
```
Edit each file (rename to `pekko`). Most were already renamed in slices 002–012; this catches
stragglers.

For config:
```bash
rg -l "^akka\." --type-add 'conf:*.conf' --type conf
```
Remove the now-redundant `akka.*` keys (the `pekko.*` keys are authoritative).

### 9. Update deploy artifacts

Per "Files in scope" §Deploy — `ms.env`, `deploy.sh`, `demo-ignition.sh`, k8s YAMLs. Apply
the Artery port change 25520 → 1736X per service in `deploy/k8s/backend.yml`.

### 10. Rewrite `run-local.sh`

Replace `sbt runAll` with the per-process dev flow. Document in `AGENTS.md`. The simplest
viable approach: a bash script that spawns one terminal per service via `xterm -e` or
`tmux new-window`. Reference the port table from slice 002.

### 11. Update documentation

`AGENTS.md`, `README.md`, `deploy/docker/README.md` — rewrite the Lagom references per
"Files in scope" §Documentation.

## Verification

- `rg "com.lightbend.lagom" --type scala | wc -l` → **must be 0**.
- `rg "lagom-sbt-plugin|LagomScala|LagomPlay|lagomScaladsl" build.sbt project/` → **must be 0**.
- `rg "lagom\." --type-add 'conf:*.conf' --type conf` → **must be 0**.
- `rg "serviceClient\.implement" api-gateway/` → **must be 0**.
- `rg "AkkaTaggerAdapter" --type scala` → **must be 0**.
- `rg "StandaloneLagomClientFactory" --type scala` → **must be 0**.
- `rg "ReadSideProcessor" --type scala` → **must be 0**.
- `rg "JsonSerializerRegistry" --type scala` → **must be 0**.
- `rg "^import akka\." --type scala | wc -l` → **must be 0** (every source file renamed).
- `sbt clean compile` → succeeds.
- `sbt test` → all 20 specs pass.
- **Full end-to-end smoke test** through the gateway: run one REST call per service category
  (authorization, persons, principal-groups, org-structure, subscriptions, service-catalog,
  application, cms, bpm-repository). All must return 200 with the same JSON shape as the
  pre-migration baseline (D7).
- Run `demo-ignition` via `./run-ignition.sh` against a fresh keyspace (`KEYSPACE_PREFIX=verify_`);
  confirm it seeds successfully end-to-end (proves the gRPC client rewrite works).
- `./build-local.sh` — builds Docker images cleanly. **Note**: this script also pushes to
  `reg.cloud.ambergate.ru` per AGENTS.md — only run if that push is intended.

## Commit

Stage only the files listed in "Files in scope". Commit message:
```
chore(*): remove lagom artifacts; finalize pekko migration [013]
```

This is the merge-candidate commit. After it lands, the `feature/migrate-to-pekko` branch
is ready for PR review.

## Out of scope

- Do **not** change the public `routes` file or any controller body (D7).
- Do **not** migrate to Scala 3 or Pekko 2.0 milestones (analysis §H anti-pattern).
- Do **not** add resilience4j or any library not in the analysis §6 table.
- Do **not** upgrade `elastic4s` to 8.x (analysis §7 risk #5 — out of scope).
- Do **not** adopt Pekko Persistence R2DBC (analysis §8 Phase 5 — future work).
- Do **not** change the `reg.cloud.ambergate.ru` push behavior of `build-local.sh`.
- Do **not** upgrade the Docker base image to `eclipse-temurin:17-jre` in this slice (analysis
  §5.11 — separate concern).

## References

- `dev/migrate-to-pekko.md` §5.1 — build plugin removal.
- `dev/migrate-to-pekko.md` §5.8 — config key sweep.
- `dev/migrate-to-pekko.md` §5.10 — ignition migration.
- `dev/migrate-to-pekko.md` §5.11 — docker/deploy/k8s changes.
- `dev/migrate-to-pekko.md` §8 Phase 4 — the cleanup phase definition.
- `dev/migrate-to-pekko.md` §11 — file-count appendix (sanity check the scope).
- All prior slice files (`dev/migration/001`–`012`).
