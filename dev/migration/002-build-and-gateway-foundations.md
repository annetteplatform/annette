# 002 — Build & gateway foundations

## Role

You are a senior Scala/Pekko engineer performing one vertical slice of the Annette Platform
migration off Lagom/Akka to Apache Pekko + Pekko gRPC. You execute against the working tree
of the repository directly.

## Prerequisites

- Branch `feature/migrate-to-pekko` is checked out.
- Step 001 is already committed:
  ```bash
  git log --oneline | grep -E "docs\(migration\): add phase-0 spike"
  ```
- `dev/migration/001-decisions.md` §F reads `GO`. Halt if it reads `NO-GO`.
- `dev/migration/001-decisions.md` §C contains a concrete Play version (not `BLOCKED`).
- Repo state at start of this step: no production source has been touched. All services still
  compile against Lagom 1.6.7. The `dev/spike/*` directories exist (from step 001) but are
  not aggregated into the root build.
- External services running: Cassandra 3.11 on `:9042`, OpenSearch 2.8 on `:9200`.

## Goal

Convert the build and the gateway bootstrap to compile against **both** Lagom and Pekko
simultaneously, so that slice 003 onward can migrate one service at a time without breaking
the rest. Concretely: swap sbt plugins, add Pekko dependency artifacts, upgrade Play, rewrite
`AnnetteApiLoader` to introduce a gRPC-client-factory pattern (still wired with macwire), and
write a fixed-port dev-mode table into every service's `application.dev.conf`. **No microservice
is migrated in this step** — they all still serve via Lagom. The dual-protocol gateway lives
in `AnnetteApiLoader` as a wiring scaffold only.

## Files in scope

**Build:**
- `project/plugins.sbt`
- `project/Dependencies.scala`
- `build.sbt`

**Gateway:**
- `api-gateway/api-gateway/app/biz/lobachev/annette/api_gateway/AnnetteApiLoader.scala`
- `api-gateway/api-gateway/conf/application.conf`
- `api-gateway/api-gateway/conf/application.dev.conf`
- `api-gateway/api-gateway/conf/application.dc.conf`
- `api-gateway/api-gateway/conf/application.k8s.conf`

**Dev-mode port table (one-line addition to each):**
- `application/application/conf/application.dev.conf`
- `application/service-catalog/conf/application.dev.conf`
- `authorization/authorization/conf/application.dev.conf`
- `bpm/bpm-repository/conf/application.dev.conf`
- `cms/cms/conf/application.dev.conf`
- `cms/subscriptions/conf/application.dev.conf`
- `principals/org-structure/conf/application.dev.conf`
- `principals/persons/conf/application.dev.conf`
- `principals/principal-groups/conf/application.dev.conf`

**Documentation:**
- `AGENTS.md` — append a "Dev-mode port table" subsection under "External services required".

No other files. In particular, **do not** touch any service's `*ServiceLoader.scala`,
`*Entity.scala`, or `*ServiceApiImpl.scala` — those belong to slices 003–012.

## Locked assumptions (do not re-decide)

- **D1** play-json at the gateway; protobuf intra-cluster.
- **D2** Fixed ports per service in `application.dev.conf`. The table you write here is the
  authoritative allocation.
- **D3** Drop `lagom.circuit-breaker.*`. Do not add a replacement in this step.
- **D7** Public REST contract frozen. The `routes` file and the 30 controller bodies are
  untouched in this step.
- **A4** One commit per step.
- The Pekko version matrix is locked in `dev/migration/001-decisions.md` §C. Use those exact
  versions; do not pin different ones.

## Steps

### 1. sbt plugins

In `project/plugins.sbt`, per analysis §5.1:

- Keep `lagom-sbt-plugin` 1.6.7 for now (dual-compile requires it; slice 013 removes it).
- Add `addSbtPlugin("org.apache.pekko" % "pekko-grpc-sbt-plugin" % "1.2.0")`.
- Keep `sbt-header` 5.6.0, `sbt-native-packager` 1.9.7, `sbt-tpolecat` 0.4.1,
  `addDependencyTreePlugin`.

### 2. Dependencies

In `project/Dependencies.scala`, per analysis §6 dependency table:

- Add a new `object PekkoVersion` with the four locked version constants from
  `001-decisions.md` §C (Core, HTTP, gRPC, Persistence Cassandra, Projection, Connectors,
  Management).
- Add dependency `val`s:
  - `pekkoCore` = `Seq(pekko-actor-typed, pekko-stream, pekko-cluster-typed,
    pekko-cluster-sharding-typed, pekko-persistence-typed, pekko-http)` at Core/HTTP versions.
  - `pekkoPersistenceCassandra` = `pekko-persistence-cassandra` 1.1.0.
  - `pekkoProjection` = `pekko-projection-cassandra` 1.1.0.
  - `pekkoManagement` = `pekko-management-cluster-bootstrap` 1.2.1 +
    `pekko-discovery-kubernetes-api` 1.2.1.
  - `pekkoConnectorsS3` = `pekko-connectors-s3` 1.3.0.
  - `pekkoGrpcTestKit` = `pekko-grpc-testkit` 1.2.0 `% Test`.
- Keep all existing entries unchanged. Do **not** delete `quill`, `lagomAkkaDiscovery`, etc.
  Slice 003 onward consumes them.

### 3. build.sbt

In `build.sbt`, per analysis §5.1 and §5.2:

- Add `enablePlugins(PekkoGrpcPlugin)` to **each** `*-api` project (9 modules: `application-api`,
  `service-catalog-api`, `authorization-api`, `bpm-repository-api`, `cms-api`,
  `org-structure-api`, `persons-api`, `principal-groups-api`, `subscriptions-api`). This is
  additive — keep `enablePlugins(LagomScala)` for now so the existing Scala traits still
  compile. The `.proto` files themselves are added in slices 004–012, not here.
- Add the `PekkoGrpcPlugin` also to `api-gateway/api-gateway` (the gateway consumes generated
  client stubs).
- Do **not** remove `lagomKafkaEnabled := false`, `lagomCassandraEnabled := false`,
  `lagomForkedTestSettings`, `enablePlugins(LagomScala)`, `enablePlugins(LagomPlay)` — slice
  013 removes these.
- Append `Test / fork := true` to each microservice project helper (in addition to
  `lagomForkedTestSettings`). The two coexist fine; slice 013 removes the Lagom one.
- Add `import org.apache.pekko.grpc.sbt.PekkoGrpcPlugin` and the
  `import org.apache.pekko.grpc.sbt.PekkoGrpc` keys to the top of `build.sbt` if needed by the
  plugin's API.

### 4. Play upgrade

Per `001-decisions.md` §C, upgrade Play to the version that runs on Pekko 1.x. This is
expressed via the Play sbt plugin version (`addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "<version>")`).
If the Lagom plugin pulls in Play transitively at a conflicting version, override it
explicitly via `playPluginVersion` or the equivalent plugin key.

If Play and Lagom cannot coexist at the required versions, surface this as a blocker in the
commit message body and **stop** — slice 003 cannot proceed otherwise.

### 5. AnnetteApiLoader rewrite

In `api-gateway/api-gateway/app/biz/lobachev/annette/api_gateway/AnnetteApiLoader.scala`,
per analysis §5.5:

- Keep the `class ServiceGateway extends BuiltInComponentsFromContext with ... with
  LagomServiceClientComponents` cake for now. Slice 013 removes the LagomServiceClientComponents
  mix-in; in this step we keep it so un-migrated services still work.
- Introduce a `GrpcClientFactory` trait:
  ```scala
  trait GrpcClientFactory {
    def system: ActorSystem
    def ec: ExecutionContext
    def clientFor(name: String): GrpcClientSettings =
      GrpcClientSettings.fromConfig(s"pekko.grpc.client.$name")(system)
  }
  ```
- Add a `val grpcClientFactory: GrpcClientFactory` to `ServiceGateway`, wired from
  `implicit val system = actorSystem` (Play provides `actorSystem` via `BuiltInComponentsFromContext`).
- Leave the 9 `serviceClient.implement[T]` lines untouched. Slice 004–012 swap them one at a
  time; this step only introduces the scaffold.

### 6. Gateway config

In `api-gateway/api-gateway/conf/application*.conf`, apply the analysis §5.8 mechanical
renames **additively** (do not remove the `akka.*` keys yet — Play still reads some). Specifically:

- `application.conf`: add `pekko.http.server.preview.enable-http2 = on`. Add a
  `pekko.grpc.client` block stub (empty, populated by slice 004 onward as services migrate):
  ```hocon
  pekko.grpc.client {
    # populated per-service in slices 004-012
  }
  ```
- `application.dc.conf`: add `pekko.remote.artery.canonical.hostname = "127.0.0.1"` and
  `pekko.remote.artery.canonical.port = 17355` (the gateway gets the default Pekko Artery
  port; microservices get distinct ports per the table in step 7 below).
- `application.k8s.conf`: add the `pekko.management.cluster.bootstrap` and
  `pekko.discovery.method = kubernetes-api` keys alongside the existing `akka.*` ones.

### 7. Dev-mode fixed-port table (D2)

For each microservice `application.dev.conf` in "Files in scope", append a fixed-port
assignment block. Use this exact port allocation (one artery port per service so they can all
run on `127.0.0.1` without conflict; HTTP/gRPC port for the service's own server):

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

Append to each microservice `application.dev.conf`:
```hocon
pekko.remote.artery.canonical.port = <artery-port-from-table>
annette.http.port = <http-port-from-table>
```
Do **not** add HTTP server binding yet — slice 003 onward binds each service's own
`Http().newServerAt(...)`. The ports are reserved here to avoid collisions.

### 8. AGENTS.md update

Append to `AGENTS.md` under "External services required" a new subsection:

```markdown
### Dev-mode service ports (post-Pekko migration)

After slice 002 the dev-mode port table is authoritative (D2). Microservices run as their own
processes (no Lagom `runAll`). Pekko Artery ports 17361–17369 are reserved; HTTP/gRPC ports
8510–8518 mirror the existing docker-compose host-port allocation.
```

## Verification

- `sbt clean compile` — **must succeed for every aggregated project**. The dual-Lagom-and-Pekko
  state compiles. If Play/Lagom conflict at the version level, halt and surface the blocker.
- `sbt 'project api-gateway' compile` — the gateway with the new `GrpcClientFactory` trait
  compiles.
- `sbt 'project authorization-api' compile` — the `PekkoGrpcPlugin` applies cleanly even
  without any `.proto` files yet.
- `sbt 'project application-api' compile` — same.
- `sbt test` — the existing 20 specs still pass. No behavioral change yet.
- `cd deploy/docker && ./deploy.sh` — bring services up; confirm the existing Lagom-mode
  `./run-local.sh` still boots. (Slice 013 replaces `run-local.sh`; this step keeps it working.)
- Commit only after all of the above pass.

## Commit

Stage only the files listed in "Files in scope". Commit message:

```
build(*): swap sbt plugins, add pekko artifacts, scaffold grpc gateway [002]
```

Verify in later slices:
```bash
git log --oneline | grep -E "build\(\*\): swap sbt plugins"
```

## Out of scope

- Do **not** migrate any microservice's loader/entity/processor (slices 003–012).
- Do **not** remove any `lagomScaladsl*` dependency (slice 013).
- Do **not** remove `enablePlugins(LagomScala)` or `enablePlugins(LagomPlay)` (slice 013).
- Do **not** author any `.proto` file (slices 004–012).
- Do **not** swap any `serviceClient.implement[T]` call to a gRPC client (slices 004–012).
- Do **not** delete the `akka.*` config keys (slice 013). This step adds `pekko.*` keys
  alongside.
- Do **not** modify the `routes` file or any controller (D7).
- Do **not** modify `core/microservice-core` (slice 003).

## References

- `dev/migrate-to-pekko.md` §5.1 — build / sbt changes.
- `dev/migrate-to-pekko.md` §5.2 — service API module plugin changes.
- `dev/migrate-to-pekko.md` §5.5 — gateway rewrite.
- `dev/migrate-to-pekko.md` §5.8 — config key renames.
- `dev/migrate-to-pekko.md` §6 — dependency substitution table.
- `dev/migrate-to-pekko.md` §7 risk #4 — Play-Pekko version compatibility (resolved by spike 001).
- `dev/migration/001-decisions.md` §C — locked version matrix.
