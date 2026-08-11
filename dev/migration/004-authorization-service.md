# 004 — Authorization service (template slice)

## Role

You are a senior Scala/Pekko engineer performing one vertical slice of the Annette Platform
migration off Lagom/Akka to Apache Pekko + Pekko gRPC. You execute against the working tree
of the repository directly.

This slice is the **template**: slices 005–012 mirror its structure. Read
`dev/migration/003-core-recipe.md` for the helper APIs you consume.

## Prerequisites

- Branch `feature/migrate-to-pekko` is checked out.
- Step 003 is already committed:
  ```bash
  git log --oneline | grep -E "feat\(microservice-core-pekko\): add pekko-variant"
  ```
- `dev/migration/003-core-recipe.md` exists with §A (Tagger usage), §B (ProjectionBase usage),
  §C (CassandraDao diff), §D (CassandraQuillDao diff), §E (module dependency).
- Repo state at start of this step: `microservice-core-pekko` is available as a sub-project
  with `Tagger`, `ProjectionBase`, the Pekko `CassandraDao` and `CassandraQuillDao`. All 9
  microservices still serve via Lagom. The gateway has `GrpcClientFactory` scaffold but no
  client yet. All 20 specs pass.
- External services running: Cassandra 3.11 on `:9042`, OpenSearch 2.8 on `:9200`.

## Goal

Migrate the `authorization` service end-to-end: author the gRPC `.proto`, rewrite the service
impl against the generated trait, replace `AkkaTaggerAdapter.fromLagom` calls (2), port the
five `ReadSideProcessor`s to Pekko Projection handlers, swap `JsonSerializerRegistry` for
Pekko Jackson HOCON bindings, swap the gateway's `AuthorizationServiceApi` client for a
generated Pekko gRPC client. At the end of this step, `authorization` is the only service
running on Pekko; the other 8 still run on Lagom. The gateway speaks both.

## Files in scope

**`authorization-api` module (rewrite):**
- `authorization/authorization-api/src/main/protobuf/authorization.proto` — **new file**.
- `authorization/authorization-api/src/main/scala/biz/lobachev/annette/authorization/api/AuthorizationServiceApi.scala` — **delete** (replaced by generated trait).
- `authorization/authorization-api/src/main/scala/biz/lobachev/annette/authorization/api/AuthorizationService.scala` — **keep** (plain Scala trait; the gateway still consumes this).
- `authorization/authorization-api/src/main/scala/biz/lobachev/annette/authorization/api/AuthorizationServiceImpl.scala` — **rewrite** (was the Lagom client wrapper; becomes the gRPC client wrapper).
- `authorization/authorization-api/src/main/scala/biz/lobachev/annette/authorization/api/role/Exceptions.scala` — **edit**: drop `AnnetteTransportExceptionCompanion`'s Lagom dependency where it conflicts with the gRPC mapping in `001-decisions.md` §D. The exception classes themselves stay (gateway rehydrates them).
- All other `authorization/authorization-api/src/main/scala/biz/lobachev/annette/authorization/api/{role,assignment}/*.scala` DTO files — **keep verbatim** (play-json `Format[T]` instances stay; the gateway still uses them per D1). These DTOs are *not* the proto messages; the gateway translates JSON ↔ protobuf.

**`authorization` impl module (rewrite):**
- `authorization/authorization/src/main/scala/biz/lobachev/annette/authorization/impl/AuthorizationServiceLoader.scala` — **rewrite**: drop `LagomApplicationLoader`/`LagomApplication`/`CassandraPersistenceComponents`; become plain `main(args: Array[String])` that constructs `ActorSystem`, `CqlSession`, `ClusterSharding`, and binds `AuthorizationServiceHandler` via `Http().newServerAt("127.0.0.1", 8512)`. Add `.dependsOn(microservice-core-pekko)` via `build.sbt`.
- `authorization/authorization/src/main/scala/biz/lobachev/annette/authorization/impl/AuthorizationServiceApiImpl.scala` — **light edit**: drop `extends AuthorizationServiceApi` (Lagom); become `extends AuthorizationService` (generated). The `entityRef.ask[Confirmation]` bodies are unchanged (analysis §1 #3).
- `authorization/authorization/src/main/scala/biz/lobachev/annette/authorization/impl/role/RoleEntity.scala` — **edit**: replace `AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag)` at line 140 with `Tagger("Authorization_Role", numShards = 10)` per `003-core-recipe.md` §A. Rename `akka.*` imports to `pekko.*`.
- `authorization/authorization/src/main/scala/biz/lobachev/annette/authorization/impl/assignment/AssignmentEntity.scala` — **edit**: same, line 87, tagger name `"Authorization_Assignment"`.
- `authorization/authorization/src/main/scala/biz/lobachev/annette/authorization/impl/role/RoleEntityDbEventProcessor.scala` — **rewrite** as `ProjectionBase` subclass.
- `authorization/authorization/src/main/scala/biz/lobachev/annette/authorization/impl/role/RoleEntityIndexEventProcessor.scala` — **rewrite** as `ProjectionBase` subclass.
- `authorization/authorization/src/main/scala/biz/lobachev/annette/authorization/impl/role/RoleEntityAssigmentEventProcessor.scala` — **rewrite** as `ProjectionBase` subclass (note filename typo "Assigment" preserved).
- `authorization/authorization/src/main/scala/biz/lobachev/annette/authorization/impl/assignment/AssignmentEntityDbEventProcessor.scala` — **rewrite** as `ProjectionBase` subclass.
- `authorization/authorization/src/main/scala/biz/lobachev/annette/authorization/impl/assignment/AssignmentEntityIndexEventProcessor.scala` — **rewrite** as `ProjectionBase` subclass.
- `authorization/authorization/src/main/scala/biz/lobachev/annette/authorization/impl/role/model/RoleSerializerRegistry.scala` — **delete** (replaced by `reference.conf` HOCON bindings).
- `authorization/authorization/src/main/scala/biz/lobachev/annette/authorization/impl/assignment/model/AssignmentSerializerRegistry.scala` — **delete**.
- `authorization/authorization/src/main/scala/biz/lobachev/annette/authorization/impl/role/dao/RoleDbDao.scala` — **edit**: extend `microservice_core.pekko.db.CassandraQuillDao` instead of the Lagom variant; change `override val session: CassandraSession` → `override val session: CqlSession`. Query bodies unchanged.
- `authorization/authorization/src/main/scala/biz/lobachev/annette/authorization/impl/assignment/dao/AssignmentDbDao.scala` — **edit**: same as RoleDbDao.
- `authorization/authorization/src/main/scala/biz/lobachev/annette/authorization/impl/role/dao/RoleIndexDao.scala` — **keep** (elastic4s-only, no Lagom coupling).
- `authorization/authorization/src/main/scala/biz/lobachev/annette/authorization/impl/assignment/dao/AssignmentIndexDao.scala` — **keep**.

**`authorization` configs:**
- `authorization/authorization/conf/application.conf` — **edit**: rename `akka.*` keys to `pekko.*`; set `pekko.remote.artery.canonical.port = 17363`; drop `lagom.persistence.read-side.cassandra` block; drop `lagom.circuit-breaker.default` block; keep `play.application.loader` pointing at the new `main`-style entrypoint (or convert to `play.application.loader` removed + sbt-native `Universal / javaOptions` — follow `003-core-recipe.md` §F for loader pattern).
- `authorization/authorization/conf/application.dev.conf` — **edit**: confirm the port table from slice 002 is present (no change unless missing).
- `authorization/authorization/conf/application.dc.conf` — **edit**: rename `akka.remote.artery` → `pekko.remote.artery`; rename `akka.cluster.seed-nodes` → `pekko.cluster.seed-nodes`; change seed-node string `akka://application@127.0.0.1:25520` → `pekko://authorization@127.0.0.1:17363`; set `pekko.management.cluster.bootstrap.form-single-member-cluster = on` (replaces `-Dlagom.cluster.join-self=on`).
- `authorization/authorization/conf/application.k8s.conf` — **edit**: rename `akka.management` → `pekko.management`; set `pekko.discovery.method = kubernetes-api`; `service-name = "authorization"`; container port `17363` for Artery, `8512` for HTTP/gRPC.
- `authorization/authorization/src/main/resources/reference.conf` — **edit**: rename `akka.actor.serialization-bindings` → `pekko.actor.serialization-bindings`; change serializer id references from `jackson-json`/`akka-misc` to `pekko.jackson-json`/`pekko-misc`. Bindings: `RoleEntity$CommandSerializable`, `AssignmentEntity$CommandSerializable`.

**`build.sbt` (one-line edits for this slice):**
- `authorization-api` project — confirm `enablePlugins(PekkoGrpcPlugin)` is present (added in slice 002); keep `enablePlugins(LagomScala)` removed from this point — drop it now that the trait is generated. Actually: keep `enablePlugins(LagomScala)` until slice 013 for the other un-migrated APIs; only **remove** it from `authorization-api` here.
- `authorization` project — drop `enablePlugins(LagomScala)`; drop `lagomScaladslPersistenceCassandra` and `lagomScaladslServer` from its `libraryDependencies`; keep `lagomScaladslTestKit` for now if a spec depends on it (no spec exists for authorization, so drop it); add `Dependencies.pekkoCore`, `Dependencies.pekkoPersistenceCassandra`, `Dependencies.pekkoProjection`; add `.dependsOn(microservice-core-pekko)`.

**Gateway:**
- `api-gateway/api-gateway/app/biz/lobachev/annette/api_gateway/AnnetteApiLoader.scala` — **edit** lines 177–178: replace
  ```scala
  lazy val authorizationServiceApi = serviceClient.implement[AuthorizationServiceApi]
  lazy val authorizationService    = wire[AuthorizationServiceImpl]
  ```
  with
  ```scala
  lazy val authorizationServiceGrpc = AuthorizationServiceClient(grpcClientFactory.clientFor("authorization"))(actorSystem, executionContext)
  lazy val authorizationService     = wire[AuthorizationServiceImpl]
  ```
  Rewrite `AuthorizationServiceImpl` (in the `authorization-api` module) so it wraps the gRPC client instead of the Lagom client. Each method drops the `.invoke()` indirection (analysis §5.5 #4: `service.createPerson().invoke(payload)` → `service.createPerson(payload)`).
- `api-gateway/api-gateway/conf/application.conf` — **edit**: populate the `pekko.grpc.client.authorization` block:
  ```hocon
  pekko.grpc.client.authorization {
    service-name = "127.0.0.1"
    service-port = 8512
    use-tls = false
  }
  ```

**Gateway controller:**
- `api-gateway/authorization-api-gateway/src/main/scala/biz/lobachev/annette/authorization/gateway/AuthorizationController.scala` — **keep unchanged**. It still injects `authorizationService: AuthorizationService` (the plain Scala trait). The trait's impl has been rewritten underneath; the controller is none the wiser (D7 in action).

No other files. In particular, do **not** touch `api-gateway/api-gateway/conf/routes` (D7) or
any other service's source.

## Locked assumptions (do not re-decide)

- **A1 / D7** Public REST contract frozen. The `routes` file and controller body are unchanged.
- **A3** Dual-protocol gateway during transition — only `authorization` swaps to gRPC; the
  other 8 services keep their `serviceClient.implement[T]` lines untouched.
- **D1** play-json at the gateway; protobuf intra-cluster. The `*-api` DTOs keep their
  play-json `Format[T]` instances. The `.proto` messages are new and separate.
- **D3** Drop `lagom.circuit-breaker.*`. Do not add a replacement.
- **D4** Pekko Projection Cassandra offset store. Use `ProjectionBase` from `microservice-core-pekko`.
- **D5** Discard `authorization`'s snapshots at cutover; entities replay from the journal.
  See Verification section.
- **D6** gRPC reflection enabled. Concatenate `PekkoGrpcServerHandlers.concat(...,
  ServerReflection.create(...))` (or the partial-handler equivalent) when binding.
- Tagger format: locked in `dev/migration/001-decisions.md` §A.
- Artery port for `authorization`: **17363** (from slice 002 dev-mode table). HTTP/gRPC port:
  **8512**.

## Steps

### 1. Author the proto

Create `authorization/authorization-api/src/main/protobuf/authorization.proto`. Enumerate every
`pathCall` in `AuthorizationServiceApi.scala`'s `descriptor` method — each becomes one `rpc`.
Pattern (per analysis §4.1):

```proto
syntax = "proto3";
package biz.lobachev.annette.authorization.api;
option java_package = "biz.lobachev.annette.authorization.api.grpc";

import "scalapb/scalapb.proto";

service AuthorizationService {
  // one rpc per pathCall
  rpc CreateRole(CreateRolePayload) returns (google.protobuf.Empty);
  // ... etc.
}
message CreateRolePayload {
  // mirror the existing case class field-for-field
  string id = 1;
  string name = 2;
  // ...
}
// one message per DTO used as a ServiceCall[A, B] argument or return type
```

**Rules:**
- `rpc Name(Arg) returns (Res)` — `Name` = camelCase of the pathCall's method name; `Arg` =
  the `ServiceCall`'s first type parameter; `Res` = second.
- For `ServiceCall[NotUsed, X]`, use `google.protobuf.Empty` as the argument; the generated
  client method takes no `Arg` parameter.
- For `Option[T]` fields, use `google.protobuf.*Value` wrappers or `optional T field = N;`
  (proto3 optional).
- Tag each `AggregateEventShards[Event]` reference's serialization: events are **not** in the
  proto (they're internal; Jackson-serialized per D1 + §5.7 Option A). Only request/response
  DTOs go in the proto.

### 2. Build, generate, fix imports

`sbt 'project authorization-api' compile` — this generates `AuthorizationService` (the trait
the impl extends), `AuthorizationServiceClient` (used by the gateway), and
`AuthorizationServiceHandler` (the route concat). Delete the old
`AuthorizationServiceApi.scala` once the generated trait compiles cleanly.

### 3. Rewrite the service impl

In `AuthorizationServiceApiImpl.scala`, change the parent trait from `AuthorizationServiceApi`
(Lagom) to `AuthorizationService` (generated). Each method signature changes from
`def createRole: ServiceCall[CreateRolePayload, Done]` to
`def createRole(in: CreateRolePayload): Future[Done]`. The body —
`entityRef.ask[Confirmation](...)` — is **unchanged** (analysis §1 #3). Drop
`ServiceCall[Req, Res].invoke { req => ... }` wrapping; just take `in: Req` and return
`Future[Res]`.

### 4. Replace `AkkaTaggerAdapter.fromLagom`

In `RoleEntity.scala` line 140 and `AssignmentEntity.scala` line 87, replace
`.withTagger(AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag))` with the `Tagger` helper
per `003-core-recipe.md` §A. The tagger name is the **entity typeKey name** to match the
existing tag: `RoleEntity.typeKey.name = "Authorization_Role"`,
`AssignmentEntity.typeKey.name = "Authorization_Assignment"`. Verify against the format locked
in `001-decisions.md` §A.

Rename all `akka.*` imports in both entity files to `pekko.*` (analysis §3.3 — mechanical).

### 5. Port read-side processors

For each of the five `*EventProcessor.scala` files in scope, convert from
`extends ReadSideProcessor[Event]` to `extends ProjectionBase` per `003-core-recipe.md` §B.
The handler body — `session.executeWrite(cql, args...)` — is unchanged in shape; only the
envelope extraction changes (`envelope.event` instead of `element.event`).

For each processor:
- `*DbEventProcessor` writes to Cassandra via the (Pekko-variant) DAO. Same CQL strings.
- `*IndexEventProcessor` writes to OpenSearch via the (unchanged) elastic4s IndexDao.
- The `RoleEntityAssigmentEventProcessor` (note filename typo) maintains the
  role→assignment derived projection.

Register each projection in the rewritten `AuthorizationServiceLoader` (or in a dedicated
`ProjectionBootstrapper` class) — call `projection.start(...)` for every (processor, tag) pair.

### 6. Drop serializer registries

Delete `RoleSerializerRegistry.scala` and `AssignmentSerializerRegistry.scala`. Update
`reference.conf` per analysis §5.7 Option A: add `pekko.actor.serialization-bindings` entries
for `RoleEntity$CommandSerializable` → `pekko.jackson-json` and
`AssignmentEntity$CommandSerializable` → `pekko.jackson-json`.

Delete the `lazy val jsonSerializerRegistry = ...` aggregator from the loader (analysis §5.7:
remove the registry aggregator line from every loader).

### 7. Rewrite the loader

Convert `AuthorizationServiceLoader.scala` from `extends LagomApplicationLoader` to a plain
entrypoint per analysis §5.3:

```scala
object AuthorizationServiceMain {
  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
    implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "authorization", config)
    new AuthorizationServiceApp(system).start()
  }
}

class AuthorizationServiceApp(implicit val system: ActorSystem[Nothing]) {
  implicit val ec: ExecutionContext = system.executionContext
  implicit val mat: Materializer = SystemMaterializer(system).materializer
  val session: CqlSession = CqlSession.builder().withConfig(...).build()
  val sharding = ClusterSharding(system)
  // init RoleEntity + AssignmentEntity shards
  // start the 5 projections
  // bind HTTP/gRPC
  val handler = AuthorizationServiceHandler(
    new AuthorizationServiceApiImpl(...),
    system,
    exceptionHandler = Some(GrpcExceptionMapper.fromDecisionsDoc)
  )
  Http().newServerAt("127.0.0.1", 8512).bind(handler)
}
```

Add gRPC reflection per D6: concat `ServerReflection.create(List(AuthorizationService))(system)`
to the bound route.

Update `conf/application.conf`'s `play.application.loader` to the new entrypoint, or convert
to plain `sbt run` invocation — follow whichever pattern `003-core-recipe.md` §F prescribes.

### 8. Update configs

Apply the mechanical renames per analysis §5.8 to the four `authorization/authorization/conf/*.conf`
files and `authorization/authorization/src/main/resources/reference.conf`. Specifically:

- `akka.*` keys → `pekko.*` keys (full mapping table in analysis §5.8).
- `lagom.persistence.read-side.cassandra` block → **remove** (replaced by Pekko Projection
  config under `pekko.projection.cassandra`).
- `lagom.circuit-breaker.default` block → **remove** (D3).
- Artery port 25520 (implicit) → `pekko.remote.artery.canonical.port = 17363` (explicit).
- Seed-node string `akka://application@127.0.0.1:25520` → `pekko://authorization@127.0.0.1:17363`.
- Actor system name change: `"application"` → `"authorization"` (so each service has a unique
  system name; cluster identity is per-service now that they no longer share a JVM).
- `application.k8s.conf`: also update the embedded ConfigMap-style keys and any
  `deploy/k8s/backend.yml` Artery container port for `authorization` (17363). Add a separate
  container port entry alongside the existing 9000.

### 9. Gateway client swap

Edit `api-gateway/api-gateway/app/biz/lobachev/annette/api_gateway/AnnetteApiLoader.scala`
lines 177–178 as specified in "Files in scope" above.

Rewrite `AuthorizationServiceImpl.scala` (in `authorization-api`): instead of wrapping a
Lagom `AuthorizationServiceApi`, wrap the generated `AuthorizationServiceClient`. Each method
drops the `.invoke()` indirection. The plain Scala trait `AuthorizationService` it implements
is unchanged — so the gateway's `AuthorizationController` is unaware of the swap (D7 preserved).

Populate `api-gateway/api-gateway/conf/application.conf`:
```hocon
pekko.grpc.client.authorization {
  service-name = "127.0.0.1"
  service-port = 8512
  use-tls = false
}
```

### 10. Snapshot cutover (D5)

Before the first boot of the migrated service:

1. Stop the running Lagom `authorization` process.
2. Truncate its snapshot table:
   ```cql
   cqlsh -e "TRUNCATE TABLE dev_authorization.snapshots;"
   ```
   (Use the actual keyspace; for production, follow `001-decisions.md` §E.)
3. Start the migrated service. The first entity accessions replay from the journal (slower
   one-time startup).

This is the cutover. **Confirm in the Verification section that entities rehydrated without
errors** — check the log for `recovery-started` / `recovery-completed` events.

## Verification

- `sbt 'project authorization-api' compile` — the `.proto` generates cleanly; the deleted
  `AuthorizationServiceApi.scala` is not referenced anywhere in the codebase.
- `sbt 'project authorization' compile` — all sources compile against Pekko.
- `sbt 'project api-gateway' compile` — the gateway with the new gRPC client compiles.
- `sbt compile` — the whole repo still compiles (the other 8 services untouched).
- **D5 cutover performed**: `cqlsh -e "SELECT count(*) FROM dev_authorization.snapshots;"`
  returns 0 *after* the migrated service's first successful boot; entity recovery log lines
  present.
- Smoke test (no spec exists for `authorization` — the survey confirms zero `*Spec.scala`):
  ```bash
  cd deploy/docker && ./deploy.sh        # ensure backing services
  sbt 'project authorization' run        # boots on 127.0.0.1:8512
  # in another shell:
  sbt 'project api-gateway' run          # boots on 127.0.0.1:9000
  curl -X POST http://127.0.0.1:9000/api/annette/v1/auth/keycloak/signin \
       -H 'Content-Type: application/json' -d '{...}'   # obtain a JWT (follow AGENTS.md)
  curl -X POST http://127.0.0.1:9000/api/authorization/v1/createRole \
       -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
       -d '{"id":"smoke-test","name":"Smoke"}'
  # expect: 200 OK with the same JSON shape the Lagom version returned (D7)
  curl http://127.0.0.1:9000/api/authorization/v1/getRole/smoke-test \
       -H "Authorization: Bearer $TOKEN"
  # expect: 200 OK with the role JSON
  grpcurl -plaintext 127.0.0.1:8512 list                 # D6: reflection works
  grpcurl -plaintext 127.0.0.1:8512 authorization.AuthorizationService/GetRole ...
  ```
- Verify the OpenSearch index was populated by the `*IndexEventProcessor`:
  ```bash
  curl 'http://localhost:9200/dev-authorization-role/_search?q=id:smoke-test'
  # expect: hits
  ```
- Commit only after all of the above pass.

## Commit

Stage only the files listed in "Files in scope". Commit message:

```
feat(authorization): migrate service to pekko gRPC [004]
```

Verify in later slices:
```bash
git log --oneline | grep -E "feat\(authorization\): migrate service"
```

## Out of scope

- Do **not** migrate any other service (slices 005–012).
- Do **not** touch the `routes` file or any controller body (D7).
- Do **not** modify the Lagom service-impl path in `AnnetteApiLoader.scala` for services other
  than `authorization`.
- Do **not** delete the `microservice-core` Lagom variant (slice 013).
- Do **not** remove `enablePlugins(LagomScala)` from any `*-api` module other than
  `authorization-api`.
- Do **not** migrate `core/core/.../exception/` (slice 013).
- Do **not** migrate `AnnetteTransportExceptionSerializer` (slice 013).
- Do **not** change the public JSON shapes of any DTO (D7).

## References

- `dev/migrate-to-pekko.md` §1 #3 — what stays unchanged (entity bodies, DAO queries).
- `dev/migrate-to-pekko.md` §4.1 — service contract translation (pathCall → rpc).
- `dev/migrate-to-pekko.md` §4.2 — read-side translation (Pekko Projection shape).
- `dev/migrate-to-pekko.md` §4.3 — entity translation (tagger replacement).
- `dev/migrate-to-pekko.md` §5.2 — service API module changes.
- `dev/migrate-to-pekko.md` §5.3 — microservice module changes (loader rewrite).
- `dev/migrate-to-pekko.md` §5.5 — gateway client swap.
- `dev/migrate-to-pekko.md` §5.6 — exception model (mapping in `001-decisions.md` §D).
- `dev/migrate-to-pekko.md` §5.7 — serializer registry removal.
- `dev/migrate-to-pekko.md` §5.8 — config file renames.
- `dev/migrate-to-pekko.md` §8 Phase 2 — first vertical slice definition.
- `dev/migration/001-decisions.md` §A — tagger format.
- `dev/migration/001-decisions.md` §D — exception → Status mapping.
- `dev/migration/001-decisions.md` §E — snapshot strategy.
- `dev/migration/003-core-recipe.md` — helper usage recipes.
