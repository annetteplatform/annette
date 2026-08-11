# Migration Analysis: Akka/Lagom → Apache Pekko / Pekko gRPC

**Scope:** Annette Platform CE (current: Scala 2.13.9, Lagom 1.6.7, Akka 2.6.x transitively, ~30 sbt sub-projects, 1,040+ Scala files).
**Target:** Apache Pekko 1.x + Pekko gRPC + Pekko Persistence Cassandra + Pekko Projection, with a Pekko-based Play gateway retaining the public REST surface.
**Status of this document:** Analysis + recommended plan. Not yet started.

---

## 1. TL;DR — read this first

1. **The blocker is Lagom, not Akka.** Application code uses Akka Typed APIs uniformly (`EventSourcedBehavior`, typed cluster sharding, `Behaviors`). Migrating that to Pekko is mechanical (import renames + config renames). Lagom 1.6.7, however, has **no Pekko port and no known fork** (GitHub search `lagom pekko` → 0 projects). Lagom is end-of-life (depends on Akka 2.6.x, which itself is EOL). Therefore "migrate to Pekko" here means **"migrate off Lagom entirely."**

2. **Migrating off Lagom = rewrite four abstractions:**
   - `Service`/`ServiceCall`/`pathCall` descriptors → **Pekko gRPC `.proto` + generated traits** (8 service traits, ~250 `pathCall` declarations).
   - `LagomApplication`/`LagomApplicationLoader`/`serverFor`/`CassandraPersistenceComponents`/`ClusterComponents` → **plain `ActorSystem` bootstrap + Pekko HTTP/gRPC server + `ClusterSharding.init`** (9 loaders).
   - `CassandraSession`/`CassandraReadSide`/`ReadSideProcessor` → **`CqlSession` (DataStax driver) + Pekko Projection over `eventsByTag`** (98 files, 55 read-side processors).
   - `JsonSerializerRegistry` → **Pekko Jackson serializers** or **protobuf messages** (35 registries, 38 files).

3. **What stays essentially unchanged:** All 28 event-sourced entity *behaviors* (already written against `akka.persistence.typed` directly — only the `AkkaTaggerAdapter.fromLagom` call at line ~130 of each needs replacement), all 30 Cassandra DAO query bodies, all Quill queries (only the context construction changes), the entire API-gateway Play layer (28 controllers, routes file — only `serviceClient.implement[T]` → gRPC stubs), `elastic4s`, `chimney`, `pureconfig`, `macwire`, `slick`/Postgres (bpm-repository), JWT.

4. **Cassandra data survives.** Pekko Persistence reads the same journal format as akka-persistence-cassandra. No data migration is required for the event store or read-side tables (subject to verifying the snapshot workaround noted in §7).

5. **Scale:** This is a multi-month, large-surface migration (estimated ~250–400 file changes, see §8). It must be done in phases, one vertical slice at a time, with the rest of the platform kept running on Lagom during the transition via a short-lived dual-protocol bridge.

6. **Recommended target runtime:** Pekko Core 1.6.x, Pekko HTTP 1.4.x, **Pekko gRPC 1.2.0** (the user-requested component), Pekko Persistence Cassandra 1.1.0, Pekko Projection 1.1.0, Pekko Connectors 1.3.0 (replaces Alpakka S3), Pekko Management 1.2.1. Pin Scala to 2.13.x for now (Pekko 2.0 is milestone-only as of writing; do not adopt yet).

---

## 2. Why migrate — the actual pressure

| Concern | Status | Implication |
|---|---|---|
| Akka license | Akka ≥ 2.7 is **BSL** (Sept 2022). 2.6.x is Apache 2.0 but **end-of-life**. | Lagom 1.6.7 pins Akka 2.6.14. You are running an EOL actor stack with no security backports. |
| Lagom project | No releases since 1.6.7 (2021). No Pekko port announced. Effectively abandoned by Lightbend. | No upgrade path. Bugs in Lagom's Cassandra/cluster integration will not be fixed. |
| Cluster stability | akka-persistence-cassandra 1.x under Lagom has known issues with `events-by-tag` backpressure and delayed events. | Operational risk grows as data volume grows. |
| Future-proofing | Pekko is actively developed under ASF, binary-compatible with Akka 2.6, supports Scala 3. | Migrating now unblocks future upgrades (JDK 17/21, Scala 3). |

**Counter-argument to keep in mind:** the platform works today. If there are no operational incidents and no BSL-audit pressure, the migration is a strategic bet, not an emergency. Phasing matters.

---

## 3. Current-state inventory (verified from the codebase)

### 3.1 Sub-project layout (from `build.sbt`, ~30 modules)

| Layer | Modules | Lagom dependency |
|---|---|---|
| **Core libs** | `core`, `microservice-core`, `api-gateway-core` | `lagomScaladslApi`, `lagomScaladslPersistenceCassandra`, `lagomScaladslServer`, `lagomScaladslTestKit` |
| **Service APIs** (×9) | `*-api` under `application/`, `authorization/`, `bpm/`, `cms/`, `principals/` | `lagomScaladslApi` only |
| **Microservices** (×9) | `application`, `service-catalog`, `authorization`, `bpm-repository`, `cms`, `subscriptions`, `org-structure`, `persons`, `principal-groups` | `enablePlugins(LagomScala)`, `lagomScaladslPersistenceCassandra`, `lagomScaladslServer % Optional`, `lagomScaladslTestKit`, `lagomForkedTestSettings`, `Dependencies.lagomAkkaDiscovery` |
| **HTTP gateways** (×8) | `*-api-gateway` under `api-gateway/` | None (pure Play/macwire) |
| **Runnable Play app** | `api-gateway/api-gateway` | `enablePlugins(LagomPlay, LagomScala)`, `lagomScaladslServer`, `ws` |
| **Ignition** | `demo-ignition` | `StandaloneLagomClientFactory` |
| **Camunda client** | `bpm/camunda` | None (Play `WSClient` only) |

### 3.2 Lagom API surface used (file counts verified by grep)

| Lagom abstraction | Files | Migration target |
|---|---|---|
| `Service` / `ServiceCall` / `pathCall` / descriptor DSL | 67 | Pekko gRPC `.proto` |
| `LagomApplication` / `LagomApplicationLoader` / `serverFor` / `LagomServerComponents` | 12 | Plain Pekko bootstrap |
| `CassandraPersistenceComponents` (provides `clusterSharding`, `readSide`, `cassandraSession`) | 8 loaders | Manual `ActorSystem` + `ClusterSharding.init` + own `CqlSession` |
| `CassandraSession` (DAOs) | ~30 | DataStax `CqlSession` directly |
| `CassandraReadSide` + `ReadSideProcessor[Event]` | 55 processors across ~98 files | Pekko Projection (`SourceProvider.eventsByTag` + `Handler`) |
| `AggregateEvent` / `AggregateEventTag` / `AggregateEventShards` | 27 entities + 55 processors | Small in-house tagger helper (see §5.3) |
| `AkkaTaggerAdapter.fromLagom(ctx, Event.Tag)` | **28 call sites** (one per entity) | Direct `EventSourcedBehavior.withTagger(...)` |
| `LagomKafkaClientComponents` | 3 loaders (**no actual Kafka usage** — `Topic`/`Producer`/`Consumer`/`TopicProducer` count = 0) | **Drop entirely.** No code to rewrite. |
| `JsonSerializerRegistry` / `JsonSerializer` | 38 files, 35 registries | Pekko Jackson serializers (HOCON) or Protobuf |
| `LagomServiceClientComponents` / `serviceClient.implement[T]` | 1 file (gateway, ~17 sites) + 1 ignition (`StandaloneLagomClientFactory`) | Pekko gRPC generated client stubs |
| `AnnetteDiscoveryServiceLocator extends AkkaDiscoveryServiceLocator` | 2 files (`core/core/.../discovery/`) | Pekko gRPC native `PekkoDiscovery` for client side; server side addressed by service mesh / k8s |
| `LagomConfigComponent` / `LagomDevModeComponents` | 9 + 9 | Replace with plain `ConfigFactory.load(env)` + (optional) `DevModeServiceLocator`-equivalent or k8s DNS |
| `ServiceTest` / `ReadSideTestDriver` / `LocalServiceLocator` | 3 test specs | Pekko testkit + Testcontainers Cassandra |
| `lagomForkedTestSettings` | 8 modules | `Test / fork := true` (direct sbt) |
| `AnnetteTransportExceptionSerializer` extends `DefaultExceptionSerializer` | 1 class, registered in 9 descriptors | gRPC status mappers (server) + a `GrpcException` mapper (client) |
| `EventStreamElement[T]` | 1 helper (`microservice-core/.../SimpleEventHandling.scala`) | Pekko Projection's `Envelope` |

### 3.3 Akka API surface (file counts verified by grep)

| Akka abstraction | Files | Notes |
|---|---|---|
| `akka.persistence.typed.scaladsl.{EventSourcedBehavior, Effect, ReplyEffect, RetentionCriteria}` + `PersistenceId` | 32 | All typed. **Already Pekko-portable** (mechanical rename). |
| `akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityRef, EntityTypeKey, EntityContext}` | 69 | Typed only — no classic `ClusterSharding`/`ShardRegion`. **Mechanical rename.** |
| `akka.actor.typed.{ActorRef, Behavior}` + `scaladsl.{ActorContext, Behaviors}` | 30 | One custom actor (`PublicKeyRequestor`, 92 lines). |
| `akka.actor.ActorSystem` (classic) | 9 | Injected by Lagom cake; replaced when loaders are rewritten. |
| `akka.stream.{Materializer, RestartSettings}` + `scaladsl.{Source, Sink, FileIO, RestartSource}` | 85 | Standard usage. `Materializer` is currently injected implicitly from `LagomApplication` — must be supplied manually after migration (use `SystemMaterializer(system)`). |
| `akka.stream.alpakka.s3` | 2 (`CmsStorage.scala`, `CmsS3Helper.scala`) | Move to `org.apache.pekko.connectors.s3` (Pekko Connectors 1.3.0). API rename only. |
| `akka.http.scaladsl.model.{ContentType, DateTime, headers.ByteRange}` | 3 | Pure value types. Trivial rename. **No Akka HTTP *server* usage** — the HTTP server is Play. |
| `akka.persistence.cassandra.*` | 0 Scala files; 8 `application.conf` reference the plugin | Config-only. Switch to `pekko.persistence.cassandra` keys + `org.apache.pekko.persistence.cassandra.ConfigSessionProvider`. |
| `akka.discovery.*` | 2 | Replace `akka-discovery-kubernetes-api` 1.0.10 → `org.apache.pekko:pekko-discovery-kubernetes-api`. |
| `akka.serialization` / `CborSerializable` / `Serializer` | 0 Scala files; 9 `reference.conf` declare `serialization-bindings` + use `jackson-json` / `akka-misc` | Config-only. Keys become `pekko.actor.serialization-bindings`, `pekko.jackson-json`, `pekko-misc`. |
| `akka.{Done, NotUsed}` | 106 | `org.apache.pekko.{Done, NotUsed}`. |
| `akka.util.{Timeout, ByteString}` | ~7 | Rename. |
| `akka.pattern.CircuitBreakerOpenException` | 1 (ignition `EntityLoader`) | Rename. Consider replacing with a typed-resilience4j equivalent. |
| `akka.actor.testkit.typed.scaladsl.{LogCapturing, ScalaTestWithActorTestKit}` | 2 entity specs | Rename. |
| `akka.management.cluster.bootstrap.*` | 0 Scala; 9 `application.k8s.conf` + 2 ConfigMaps | Config-only. Move to `pekko.management.cluster.bootstrap` + `pekko-management-cluster-bootstrap` artifact. |
| `akka.cluster.sharding.state-store-mode = ddata` | 9 `application.conf` | Key rename. |
| `akka.remote.artery.*` + `akka.cluster.seed-nodes` | 9 `application.dc.conf` | Key rename + **port change** (Artery default 25520 → 17355 in Pekko; explicit port assignments needed to keep existing cluster topology). |

### 3.4 What is independent of Akka/Lagom (retain as-is)

| Library | Version | Why it survives |
|---|---|---|
| Play Framework | 2.8.x (via Lagom) | The gateway is a Play app. Play ≥ 2.8 (community) / Play 2.9+ depends on Pekko 1.x officially. Upgrade Play to a Pekko-based build. |
| `play-json` | 2.8.2 | Pure JSON, no Akka coupling. Used in 585 files. Retain. |
| `elastic4s` (core/client-esjava/json-play) | 7.8.1 | Transport is Apache HttpClient, **not** akka-http. Zero coupling. (Consider upgrading to 8.x later — orthogonal.) |
| `chimney` | 1.0.0 | Compile-time. No coupling. |
| `pureconfig` | 0.17.1 | Reads `ConfigSource.default`. No coupling. |
| `macwire` | 2.5.0 | Compile-time DI macro. No coupling. |
| `jwt-play-json` | 9.0.2 | Only play-json coupling. |
| `slick` / `slick-hikaricp` | 3.3.3 | Postgres only (bpm-repository). No Akka. |
| `postgresql` driver | 42.3.1 | — |

### 3.5 The hardest coupling: `quill-cassandra-lagom`

`core/microservice-core/src/main/scala/biz/lobachev/annette/microservice_core/db/CassandraQuillDao.scala` constructs a `CassandraLagomAsyncContext(SnakeCase, session)` where `session: com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession`. This is the only Lagom-aware Quill context.

**Fix:** swap to vanilla `io.getquill.CassandraAsyncContext(SnakeCase, cqlSession)` where `cqlSession: com.datastax.oss.driver.api.core.CqlSession`. The Quill query DSL (`ctx.run(...)`, `lift(...)`) is unchanged. Touches **6 files** total — `CassandraQuillDao`, `QuillEncoders`, `CassandraTableBuilder`, `CassandraQuillDaoWithAttributes`, plus two concrete DAOs.

---

## 4. Target architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    Browser / external REST clients                       │
└─────────────────────────────────────────────────────────────────────────┘
                                       │  HTTPS / REST (JSON)
                                       ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  api-gateway  (single Play app, Pekko-based Play 2.9+)                   │
│  - conf/routes (kept, ~330 routes)                                       │
│  - 28 controllers (kept verbatim except for service-client injection)    │
│  - macwire DI, JWT auth, CORS, gzip filters (kept)                       │
│  - For each downstream service: generated Pekko gRPC *client stub*       │
│    (replaces `serviceClient.implement[PersonServiceApi]`)                │
│  - Keycloak / Camunda via Play WSClient (kept)                           │
└─────────────────────────────────────────────────────────────────────────┘
                                       │  gRPC over HTTP/2 (intra-cluster)
                                       ▼
┌──────────────┬──────────────┬──────────────┬──────────────┬─────────────┐
│  persons     │ org-structure│ authorization│ application  │  cms / ...  │  (8 + 1)
│  Pekko gRPC  │  Pekko gRPC  │  Pekko gRPC  │  Pekko gRPC  │  Pekko gRPC │   microservices
│  server      │  server      │  server      │  server      │  server     │
│  (Pekko HTTP)│              │              │              │             │
├──────────────┴──────────────┴──────────────┴──────────────┴─────────────┤
│  Each microservice:                                                      │
│  - `ActorSystem` + Cluster Sharding (typed) + Pekko Persistence Typed    │
│  - Pekko Persistence Cassandra 1.1.0 (journal + snapshots + query)       │
│  - Pekko Projection 1.1.0 for read-side (replaces ReadSideProcessor)     │
│  - CqlSession (DataStax driver) shared by Quill + Projection handlers    │
│  - elastic4s for OpenSearch index writes (unchanged)                     │
│  - Alpakka S3 → Pekko Connectors S3 (cms only)                           │
│  - Pekko Management + k8s bootstrap (cluster formation)                  │
└──────────────────────────────────────────────────────────────────────────┘
                                       │
                  ┌────────────────────┼────────────────────┐
                  ▼                    ▼                    ▼
            Cassandra 3.11       OpenSearch 2.8         Postgres (bpm-repo only)
```

### 4.1 Service contract translation

**Today (Lagom):** each `*-api` module declares

```scala
trait PersonServiceApi extends Service {
  def createPerson: ServiceCall[CreatePersonPayload, Done]
  def getPerson(id: String, source: Option[String], attributes: Option[String]): ServiceCall[NotUsed, Person]
  override def descriptor =
    named("persons")
      .withCalls(
        pathCall("/api/persons/v1/createPerson", createPerson),
        pathCall("/api/persons/v1/getPerson/:id", getPerson _)
      )
      .withExceptionSerializer(new AnnetteTransportExceptionSerializer())
}
```

The same trait is implemented server-side (`PersonServiceApiImpl`) and used to generate a client proxy in the gateway (`serviceClient.implement[PersonServiceApi]`).

**Target (Pekko gRPC):** the trait is generated from a `.proto`:

```proto
service PersonService {
  rpc CreatePerson(CreatePersonPayload) returns (google.protobuf.Empty);
  rpc GetPerson(GetPersonRequest)       returns (Person);
}
message GetPersonRequest { string id = 1; string source = 2; string attributes = 3; }
```

`sbt compile` (with `PekkoGrpcPlugin`) generates:
- `trait PersonService` — implemented by `PersonServiceImpl` (server side).
- `PersonServiceClient` — used by the gateway.
- `PersonServiceHandler` — wires the impl into a Pekko HTTP route.

A unary `rpc M(Req) returns (Res)` maps directly to `def m(in: Req): Future[Res]`, which is structurally identical to a `ServiceCall[Req, Res].invoke`. The translation is 1:1 for the ~250 existing `pathCall`s. Streaming RPCs (`Source[…]`) cover any future need; none exist today.

### 4.2 Read-side translation

**Today:**

```scala
class PersonDbEventProcessor(readSide: CassandraReadSide, session: CassandraSession, ...)
  extends ReadSideProcessor[PersonEntity.Event] {
  def buildHandler() = readSide.builder[PersonEntity.Event]("PersonDbEventProcessor")
    .setEventHandler(handleEvent _)
    .build()
  def aggregateTags = PersonEntity.Event.Tag.allTags
}
// registered: readSide.register(wire[PersonDbEventProcessor])
```

**Target (Pekko Projection):**

```scala
val projection =
  ShardedSourceProvider(eventsByTag[Event](tag, numShards = 10))(
    system, eventEnvelope[Event], PersonEntity.Event.Tag.allTags.toSeq)
Projection(personDbProjectionId)
  .withRestartBackoff(RestartSettings(...))
  .run(projection, handler)(system, projectionContext)
```

where `handler` is `Handler[EventEnvelope[Event]]` doing the same `session.executeWrite(...)` calls. Pekko Projection handles offset storage (a `OffsetStore` backed by Cassandra), at-least-once semantics, and sharded parallelism — the same guarantees Lagom's `ReadSideProcessor` provides.

### 4.3 Entity translation (smallest change in the migration)

The only Lagom-specific line in each of the 28 entities is:

```scala
.withTagger(AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag))
```

becomes (with a tiny in-house helper or inline):

```scala
.withTagger(EventSourcedType.tagger(Event.Tag))   // tagger = e => Set(tagFor(e, Event.Tag))
```

where `Event.Tag: AggregateEventShards[Event]` is replaced by a plain `numShards` constant + a hashing function. Everything else (`EventSourcedBehavior.withEnforcedReplies`, `Effect.persist(...).thenReply(...)`, `RetentionCriteria.snapshotEvery(100, 2)`) is unchanged modulo the package rename.

---

## 5. Detailed layer-by-layer plan

### 5.1 Build / sbt

**Before** (`project/plugins.sbt`):

```scala
addSbtPlugin("com.lightbend.lagom" % "lagom-sbt-plugin" % "1.6.7")
```

**After:**
```scala
addSbtPlugin("org.apache.pekko"      % "pekko-grpc-sbt-plugin" % "1.2.0")
addSbtPlugin("com.typesafe.sbt"      % "sbt-native-packager"  % "1.9.7")   // keep
addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat"      % "0.4.1")   // keep
// drop: lagom-sbt-plugin
```

**`build.sbt` changes:**
- Remove `lagomKafkaEnabled := false`, `lagomCassandraEnabled := false`, `lagomForkedTestSettings`, `enablePlugins(LagomScala)`, `enablePlugins(LagomPlay)`.
- Replace every `lagomScaladsl*` dependency with the Pekko equivalent (see table in §6.1).
- `microservice-core` keeps `-Wconf:cat=unused-nowarn:s`.
- Add `enablePlugins(PekkoGrpcPlugin)` to every `*-api` module so `.proto` files compile to Scala service traits + client stubs.
- `Test / fork := true` per microservice (replaces `lagomForkedTestSettings`).
- `.sbtopts` / `.jvmopts` unchanged (still need `-Xmx8G`, `--add-opens`).

### 5.2 Service API modules (`*-api`, 9 modules)

For each of the 9 `*-api` modules:

1. Add `enablePlugins(PekkoGrpcPlugin)`.
2. Author a `.proto` file under `src/main/protobuf/<Service>.proto` mirroring the existing `trait … extends Service`. One `rpc` per `pathCall`. DTOs (`CreatePersonPayload`, `Person`, etc.) become `message` definitions — translate them mechanically from the existing case classes. Use `optional`/`google.protobuf wrapper` types where the Scala field is `Option[T]`.
3. Delete the Scala `trait … extends Service`, the `descriptor` method, and the `Exceptions.scala`'s Lagom `TransportException` base (replace with a `GrpcException` carrying status codes — see §5.6).
4. Delete `AnnetteTransportExceptionSerializer` registration.
5. Keep the existing play-json `Format[T]` instances for the gateway's REST↔DTO conversion if the gateway keeps using play-json to talk to browsers; protobuf is only for the gateway→microservice hop.

**Effort estimate:** ~3 days per `*-api` module × 9 = ~4 person-weeks. The DTO translation is the bulk; it can be scripted partially.

### 5.3 Microservice modules (9 services)

For each `*ServiceLoader.scala` (e.g. `PersonServiceLoader`):

1. Replace `LagomApplicationLoader` with a plain main object:

   ```scala
   object PersonServiceMain {
     def main(args: Array[String]): Unit = {
       val config = ConfigFactory.load()
       implicit val system = ActorSystem("PersonService", config)
       new PersonServiceApp(system).start()
       CoordinatedShutdown(system).addCancellableHook(...)
     }
   }
   ```
   
2. Replace `LagomApplication` body with a plain class holding macwire-`wire[...]` instances, an `ActorSystem`, a `Materializer = SystemMaterializer(system).materializer`, a `CqlSession` (built once from config), and a `ClusterSharding(system).init(Entity(...))` call per entity.
3. Replace `serverFor[T](impl)` with:

   ```scala
   val handler = PersonServiceHandler(new PersonServiceImpl(...), system)
   Http().newServerAt(host, port).bind(handler)
   ```
   
   enable HTTP/2 via `pekko.http.server.preview.enable-http2 = on`.
4. Replace `readSide.register(wire[PersonDbEventProcessor])` with Pekko Projection initialization (one `Projection(...).run(...)` per tag shard set).
5. Replace `ClusterComponents`, `CassandraPersistenceComponents`, `LagomKafkaClientComponents`, `AhcWSComponents` mix-ins. For WS (Camunda/Keycloak only), construct an `AhcWSClient` explicitly.
6. Drop `LagomConfigComponent` / `LagomDevModeComponents` / `AnnetteDiscoveryComponents`. For service discovery use the Pekko gRPC client's built-in `PekkoDiscovery` lookup, or fall back to k8s DNS in dev mode.
7. Replace the 28 `AkkaTaggerAdapter.fromLagom(entityContext, Event.Tag)` calls.

**Effort estimate:** ~5 days per microservice × 9 = ~9 person-weeks. CMS is the largest (10 entities, 17 read-side processors); bpm-repository is the smallest (Postgres only, no Cassandra entities).

### 5.4 Cassandra / Quill layer (`microservice-core`)

1. **`CassandraDao.scala`**: change `val session: CassandraSession` to `val session: CqlSession`. Rewrite the helpers (`selectAll`, `executeWrite`, etc.) using `session.executeAsync(...).asScala` / `session.prepareAsync(...).asScala`. This is the largest single mechanical change in the migration — touches ~30 DAO files transitively, but the bodies of the DAOs (the CQL strings) are unchanged.
2. **`CassandraQuillDao.scala`**: swap `CassandraLagomAsyncContext` for `CassandraAsyncContext`. Drop the `quill-cassandra-lagom` dependency, keep `quill-cassandra`. The query DSL is identical.
3. **`SimpleEventHandling.scala`**: drop the `EventStreamElement` import; rewrite the helper to take a plain `Event` (Pekko Projection's `Envelope.event`).
4. Replace `lagom.persistence.read-side.cassandra` config keys with nothing — Pekko Projection has its own keyspace config (`pekko.projection.cassandra`).

**Effort estimate:** ~2 weeks for the shared DAO rework + propagating to all 30 concrete DAOs.

### 5.5 API gateway (`api-gateway/api-gateway`)

This is the largest single artifact but the **smallest code change** because the Play/REST surface stays put.

1. Upgrade Play to a Pekko-based release (Play 2.9.x community builds run on Pekko 1.0.x; verify exact Play–Pekko compatibility matrix before locking versions — see §7 risks). Drop `LagomPlay` plugin.
2. `AnnetteApiLoader` keeps its shape (`extends ApplicationLoader`, `BuiltInComponentsFromContext`, `HttpFiltersComponents`, `CORSComponents`, `GzipFilterComponents`, `AssetsComponents`, `AhcWSComponents`). Remove `LagomConfigComponent` and `LagomServiceClientComponents`.
3. Replace the 17 `serviceClient.implement[T]` lines with macwire'd generated gRPC clients:

   ```scala
    lazy val personService = PersonServiceClient(
      ServiceLoader
        .load(classOf[GrpcClientSettings])
        .fromConfig("pekko.grpc.client.persons")
    )(system, ec)
   ```
   
   (or whatever explicit channel construction the gateway uses).
4. The 28 controllers call e.g. `personService.createPerson().invoke(payload)` today; the new code calls `personService.createPerson(payload)` (one fewer `invoke`). This is a 1-character-per-call mechanical edit across ~330 route handlers.
5. Keep `routes` file, `AuthenticatedAction`, `Authorizer`, `KeycloakAuthenticator`, error handler, CORS, gzip — all Play-only.
6. The `PublicKeyRequestor` typed actor (in `api-gateway-core`) survives the migration unchanged modulo the package rename.

**Effort estimate:** ~2 weeks. The bulk is upgrading Play to a Pekko-compatible release and validating that filters/WS/auth continue to work.

### 5.6 Exception model

Today, ~35 `Exceptions.scala` files extend `AnnetteTransportException(TransportErrorCode, …)` which is serialized by `AnnetteTransportExceptionSerializer` over the Lagom wire. The wire format is HTTP-status + JSON body.

**Target:** map business exceptions to gRPC status codes server-side and rehydrate client-side:
- Define a small `GrpcExceptionMapper` trait with `def toStatus(e: Throwable): Status`.
- Server: register an exception handler via Pekko gRPC's `GreeterServiceHandler.partial(impl, system, exceptionHandler = ...)`.
- Client: gRPC clients surface `io.grpc.StatusRuntimeException` — wrap it back into the existing exception hierarchy at the gateway boundary.
- For client-facing REST, the gateway already translates via `ApiGatewayErrorHandler`; the inner exception is now a gRPC status, but the public REST contract is preserved.

This is the area most likely to need iteration. Suggest a 1-day design spike on the exception → status mapping before coding it across 35 files.

### 5.7 Serialization (`JsonSerializerRegistry` removal)

The 35 `*SerializerRegistry` objects exist solely because Lagom needs Akka cluster messages tagged for play-json serialization via a custom `Serializer`. After migration:

- **Option A (lowest risk, recommended):** switch to Pekko Jackson serialization. For each `Command`/`Event`/`State` trait currently in a registry, add a `pekko.actor.serialization-bindings` entry in `reference.conf` pointing to Jackson:
  ```hocon
  pekko.actor.serialization-bindings {
    "biz.lobachev.annette.persons.impl.person.model.PersonSerializable" = jackson-json
  }
  ```
  Delete the registry classes. Remove the `lazy val jsonSerializerRegistry = …` line from every loader.
- **Option B (deeper alignment with gRPC):** move `Command`/`Event`/`State` to protobuf messages (generated by the same `PekkoGrpcPlugin`). Then serialization uses `protobuf` natively. Larger change; defer until after the initial migration succeeds.

**Effort estimate (Option A):** ~1 week for all 35 registries + 9 loaders + 9 `reference.conf` files + 2 test specs that call `JsonSerializerRegistry.actorSystemSetupFor(...)`.

### 5.8 Config files (44 files: `application.conf`, `*.dev.conf`, `*.dc.conf`, `*.k8s.conf`, `reference.conf`, `indexing.conf`)

Mechanical renames applied with search-and-replace (verify each file after):

| From | To |
|---|---|
| `akka.actor` | `pekko.actor` |
| `akka.cluster.sharding` | `pekko.cluster.sharding` |
| `akka.cluster.seed-nodes` | `pekko.cluster.seed-nodes` |
| `akka.cluster.min-nr-of-members` | `pekko.cluster.min-nr-of-members` |
| `akka.remote.artery` | `pekko.remote.artery` |
| `akka.management` | `pekko.management` |
| `akka.persistence.cassandra.ConfigSessionProvider` | `org.apache.pekko.persistence.cassandra.ConfigSessionProvider` |
| `akka.http.server.parsing.max-content-length` | `pekko.http.server.parsing.max-content-length` |
| `akka.persistence.journal.inmem` (test) | `pekko.persistence.journal.inmem` |
| `akka://` | `pekko://` |
| `akka.tcp://` | `pekko.tcp://` |
| `lagom.persistence.read-side.cassandra` | **remove** (Pekko Projection uses `pekko.projection.cassandra.*`) |
| `lagom.circuit-breaker.default.*` | **remove** (replace with `pekko.circuit-breaker.*` or resilience4j if circuit-breaking is still needed; the gateway today *enables* it, the microservices *disable* it — most can be dropped) |
| `lagom.cluster.join-self` / `lagom.cluster.bootstrap.enabled` (in `deploy/docker/env/ms.env`) | `pekko.cluster.joins-self` is not a thing; use `pekko.management.cluster.bootstrap.form-single-member-cluster = on` or explicit seed-nodes |
| `akka.discovery.method` | `pekko.discovery.method` |
| `alpakka.s3.*` | `alpakka.s3.*` (Pekko Connectors preserves this key namespace) |

**Critical operational detail:** Artery's default port changes from `25520` (Akka) to `17355` (Pekko). Every `application.dc.conf` and `application.k8s.conf` that references `akka.remote.artery.canonical.port` or k8s container ports must be updated to keep the existing cluster topology. The `deploy/k8s/config.yml` ConfigMaps also embed `akka.management { … }` blocks literally — update them too.

**Effort estimate:** ~3 days for the sweep, +2 days for verifying cluster bootstrap actually forms in dev + k8s.

### 5.9 Tests

| Spec type | Count | Migration |
|---|---|---|
| `*EntitySpec` (typed testkit) | 2 (will grow) | Rename imports; `pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit`. The `inmem` journal + `snapshot-store.local` config keys rename. |
| `*ServiceApiSpec` (Lagom `ServiceTest` + live Cassandra) | 2 | Replace `ServiceTest.startServer` with a Pekko gRPC in-process server (`PersonServiceHandler` bound to a `Http().newServerAt`) + the generated `PersonServiceClient` over an in-process channel. Use **Testcontainers Cassandra** (`org.testcontainers:cassandra`) instead of requiring a real Cassandra on `localhost:9042`. This *improves* DX — tests no longer need an external server. |
| Camunda `*Spec` (Play WSClient) | 5 | Construct standalone `AhcWSClient` explicitly; otherwise unchanged. |
| Ignition | 1 client | Replace `StandaloneLagomClientFactory` with generated Pekko gRPC clients. |

**Effort estimate:** ~2 weeks for test infrastructure + rewriting the ~10 specs that exist.

### 5.10 Ignition

`ignition/demo-ignition` is a standalone seed app. It uses `StandaloneLagomClientFactory` to construct Lagom service clients and drives them via `RestartSource.withBackoff`. Migration: replace the factory with Pekko gRPC clients built from config. The seeding logic (ElasticSearch writes, Cassandra inserts via the service APIs) is unchanged.

**Effort estimate:** ~1 week.

### 5.11 Docker / deploy / k8s

- `deploy/docker/env/ms.env`: drop `-Dlagom.cluster.join-self=on -Dlagom.cluster.bootstrap.enabled=false`; replace with `-Dpekko.management.cluster.bootstrap.form-single-member-cluster=on`. Keep `KEYSPACE_PREFIX`, `INDEX_PREFIX` env-var convention unchanged.
- `deploy/docker/deploy.sh`, `demo-ignition.sh`: version bump only.
- `deploy/k8s/*.yml`, `config.yml`: update embedded `akka.*` keys → `pekko.*`, update container ports for Artery.
- `build-local.sh`: drop the `reg.cloud.ambergate.ru` push steps (or keep — out of scope here).
- Docker base image `openjdk:11` is fine for Pekko 1.x; consider `eclipse-temurin:17-jre` after the migration stabilizes.

---

## 6. Dependency substitution table

| Current (Lagom/Akka) | Target (Pekko) | Version |
|---|---|---|
| `com.lightbend.lagom %% lagomScaladslApi` | (deleted — replaced by generated gRPC traits) | — |
| `com.lightbend.lagom %% lagomScaladslPersistenceCassandra` | `org.apache.pekko %% pekko-actor-typed` + `org.apache.pekko %% pekko-persistence-typed` + `org.apache.pekko %% pekko-persistence-cassandra` | 1.6.0 / 1.6.0 / 1.1.0 |
| `com.lightbend.lagom %% lagomScaladslServer` | (deleted — replaced by `PekkoGrpcPlugin` + `org.apache.pekko %% pekko-http`) | 1.4.0 |
| `com.lightbend.lagom %% lagomScaladslTestKit` | `org.apache.pekko %% pekko-actor-testkit-typed` + `org.apache.pekko %% pekko-persistence-testkit` | 1.6.0 |
| `com.lightbend.lagom %% lagomScaladslKafkaClient` | (deleted — no usage) | — |
| `com.lightbend.lagom %% lagom-scaladsl-akka-discovery-service-locator` | (deleted — gRPC client uses `org.apache.pekko:pekko-discovery` natively) | — |
| `com.lightbend.akka.discovery %% akka-discovery-kubernetes-api` | `org.apache.pekko %% pekko-discovery-kubernetes-api` | 1.2.1 |
| (transitive) `akka-stream` | `org.apache.pekko %% pekko-stream` | 1.6.0 |
| `com.lightbend.akka %% akka-stream-alpakka-s3` | `org.apache.pekko %% pekko-connectors-s3` | 1.3.0 |
| `com.typesafe.akka %% akka-http` / `akka-http-xml` | `org.apache.pekko %% pekko-http` / `pekko-http-xml` | 1.4.0 |
| (transitive) `akka-cluster-sharding-typed` | `org.apache.pekko %% pekko-cluster-sharding-typed` | 1.6.0 |
| (transitive) `akka-cluster-tools` (for singleton) | `org.apache.pekko %% pekko-cluster-tools` (if/when needed) | 1.6.0 |
| (transitive) `akka-management-cluster-bootstrap` | `org.apache.pekko %% pekko-management-cluster-bootstrap` | 1.2.1 |
| (none) — Lagom `ReadSideProcessor` | `org.apache.pekko %% pekko-projection-cassandra` | 1.1.0 |
| `io.getquill %% quill-cassandra-lagom` | (deleted) | — |
| `io.getquill %% quill-cassandra` | `io.getquill %% quill-cassandra` (kept) | 3.10.0 |
| `play-json`, `play-json-extensions`, `chimney`, `pureconfig`, `macwire`, `jwt-play-json`, `slick`, `slick-hikaricp`, `postgresql`, `elastic4s-*`, `jackson-dataformat-yaml`, `slf4j-*` | (unchanged) | — |

**Net:** the `lagom-sbt-plugin` disappears from `project/plugins.sbt`; `pekko-grpc-sbt-plugin` 1.2.0 is added. No more `lagomScaladsl*` artifacts at all.

---

## 7. Risks and open questions

| # | Risk / unknown | Severity | Mitigation / spike |
|---|---|---|---|
| 1 | **Lagom's `ReadSideProcessor` semantics vs Pekko Projection's.** Lagom gives at-least-once + automatic offset storage per tag shard "for free." Pekko Projection gives the same, but the offset table format differs. | High | Do a 2-day spike: port one `*DbEventProcessor` to a Pekko Projection against a copy of the dev keyspace. Verify offset recovery + at-least-once behavior. |
| 2 | **Snapshot compatibility.** Pekko docs note one snapshot issue fixed in Pekko 1.0.3 with a known workaround. Existing snapshots written by akka-persistence-cassandra may need a migration script. | Medium | Inspect a sample `messages` table row. Test loading an existing snapshot with Pekko in dev. |
| 3 | **`eventsByTag` shard count continuity.** Each entity uses `AggregateEventTag.sharded[Event](numShards = 10)`. The shard → tag-name mapping must remain byte-identical so historical events continue to be queryable. | High | Verify the replacement tagger produces the same `String` tag names as Lagom's `AggregateEventTag.sharded` (Lagom uses `"entityName|n"` style — match it). |
| 4 | **Play–Pekko version compatibility.** The gateway depends on Play. Need to confirm which Play release runs on Pekko 1.x (not Akka). The "community" Play fork or Play 2.9.x (whichever moved to Pekko) must be identified. | High | Spend 1 day validating that a minimal Play app with `pekko.http.server.preview.enable-http2 = on` and Pekko gRPC routes works. This is the single biggest version-locking risk. |
| 5 | **`elastic4s` 7.8.1 transport.** Uses Apache HttpClient — not coupled — but pinned to a 2020 release. OpenSearch 2.8 wire-compatibility has been fine so far, but check during the migration window. | Low | Out of scope for this migration; treat as a separate upgrade. |
| 6 | **`AnnetteTransportException` ↔ gRPC status mapping.** ~35 exception classes must map to a gRPC `Status` (and the gateway must rehydrate them). | Medium | 1-day design spike before implementation. |
| 7 | **Service discovery in dev mode.** Lagom's `LagomDevModeComponents` provides a `ServiceLocator` that lets `runAll` bind services on dynamic ports. Pekko gRPC has no equivalent single-JVM runAll. | Medium | Either fix ports per service in `application.dev.conf`, or implement a tiny dev-mode service registry, or run each microservice as its own process (closer to prod). |
| 8 | **Circuit breaker.** Lagom's `lagom.circuit-breaker.default` wraps every service-client call. After migration, the gateway's gRPC clients have no built-in circuit breaker. | Low | Either add `org.apache.pekko:pekko-circuit-breaker` explicitly, or accept gRPC's built-in retry/backoff (`withBackoff`), or use resilience4j. Decision deferred to spike #6. |
| 9 | **`Materializer` injection.** ~80 files take `implicit val materializer: Materializer` from the Lagom cake. After migration, every one of those call sites needs an implicit in scope. | Low | Standardize on `implicit val mat: Materializer = SystemMaterializer(system).materializer` at the bootstrap level; the existing implicit chains already flow from one source. |
| 10 | **Akka + Pekko side-by-side during transition.** Pekko has experimental support for mixed Akka/Pekko clusters, but Lagom services will not interop with Pekko gRPC services at the *service-call* layer. | Medium | During the transition, the API gateway must speak both Lagom service-client *and* Pekko gRPC. Plan a single cutover per vertical slice (see §8). |
| 11 | **`sbt-tpolecat` strictness.** The migration will surface many new warnings (unused imports in generated code, etc.). | Low | Keep `-Wconf:cat=unused-nowarn:s` (already on `microservice-core`); extend to all modules if needed. |
| 12 | **License audit.** Confirm Pekko 1.x and all listed modules remain Apache 2.0 (they are, as of writing). Re-verify at execution time. | Low | Check `NOTICE` for each Pekko artifact. |

---

## 8. Phased execution plan

The migration cannot be a big-bang: the platform must keep serving traffic. The principle is **vertical slices**: pick one microservice, take it through the whole pipeline (proto → impl → projection → gateway client → k8s), prove it end-to-end, then replicate. Recommended first slice: **`authorization`** (2 entities, ~3 read-side processors, simplest DTO set, no CMS/S3 complexity).

### Phase 0 — Spike & decisions (2 weeks)

- Resolve risks #1, #3, #4, #6 (projections, tags, Play version, exception mapping) with code spikes in a throwaway branch.
- Lock the Pekko / Play / Pekko gRPC version matrix.
- Decide: Protobuf DTOs end-to-end (Option B §5.7) or play-json retained at the gateway only (Option A).
- Decide: per-process microservices in dev, or implement a dev-mode registry.

### Phase 1 — Shared core rewrite (3 weeks)

- `core/core`: replace `AnnetteDiscoveryComponents`/`AnnetteDiscoveryServiceLocator` with Pekko-native equivalents (or delete if service discovery moves to k8s DNS).
- `core/microservice-core`:
  - `CassandraDao` → `CqlSession`-based.
  - `CassandraQuillDao` → `CassandraAsyncContext`.
  - Delete `SimpleEventHandling`'s Lagom dependency; introduce a small `Tagger[T]` helper to replace `AggregateEventTagger`.
  - Add a `ProjectionBase` trait that Pekko Projection handlers extend.
- `core/api-gateway-core`: rename `akka.*` imports to `pekko.*` (mechanical).
- This phase ships a `microservice-core` that compiles against **both** Lagom (current services) and Pekko (the first migrated service). Use a feature trait or a parallel `microservice-core-pekko` module if needed to avoid forking the world.

### Phase 2 — First vertical slice: `authorization` (3 weeks)

- Author `authorization-api/src/main/protobuf/authorization.proto`.
- Rewrite `AuthorizationServiceImpl` against the generated trait (the body — `entityRef.ask[Confirmation]` — is unchanged).
- Replace 2 `AkkaTaggerAdapter.fromLagom` calls.
- Port the ~3 `*EventProcessor`s to Pekko Projection.
- Build the gateway's `AuthorizationServiceClient` (Pekko gRPC), swap it in at the gateway.
- Update `application.conf` / `application.dc.conf` / `application.k8s.conf` for `authorization`.
- Run the full integration spec suite against a real Cassandra + OpenSearch.
- **Cutover:** deploy `authorization` + new gateway simultaneously. Other services stay on Lagom.

### Phase 3 — Replicate across remaining services (10–12 weeks)

In order of estimated complexity: `persons`, `principal-groups`, `org-structure` (4 entities incl. `HierarchyEntity`), `subscriptions`, `service-catalog` (5 entities — biggest cluster-sharding footprint), `application` (4 entities), `cms` (10 entities, S3 — largest surface), `bpm-repository` (Postgres only — easiest, but no event sourcing; just REST).

Each follows the Phase 2 recipe. Plan one slice per developer per ~1.5 weeks.

### Phase 4 — Ignition + final cleanup (2 weeks)

- Migrate `demo-ignition` off `StandaloneLagomClientFactory`.
- Remove `lagom-sbt-plugin` from `project/plugins.sbt`.
- Remove all remaining `lagom.*` config keys.
- Delete `microservice-core` compatibility shims from Phase 1.
- Update `AGENTS.md`, `README.md`, `deploy/docker/README.md` to reflect the new commands (`./run-local.sh` may need to start processes differently since `sbt runAll` is Lagom-specific).

### Phase 5 — Hardening (ongoing)

- Migrate entity specs to Testcontainers.
- Adopt Pekko 2.0 when it goes GA (Scala 3, JDK 17+ baseline).
- Consider Pekko Persistence R2DBC as an alternative to Cassandra long-term (orthogonal decision).

### Total estimate

**~20–25 person-weeks of focused work** (one developer) plus spikes. A two-developer team can compress to ~12–14 calendar weeks. Add ~30% contingency for the unknowns in §7.

---

## 9. Decision register (fill in during Phase 0)

| Decision | Options | Status |
|---|---|---|
| DTO representation | (A) play-json at gateway, protobuf intra-cluster · (B) protobuf end-to-end | Open |
| Dev-mode service discovery | (A) fixed ports · (B) dev registry · (C) one-process-per-service | Open |
| Circuit breaker | (A) drop · (B) pekko circuit-breaker · (C) resilience4j | Open |
| Read-side | (A) Pekko Projection + Cassandra offset store · (B) Pekko Projection + JDBC offset store | Open |
| Snapshot migration | (A) discard snapshots (replay from journal) · (B) migrate via script | Open |
| gRPC reflection / gRPC-Web | enable? | Open |
| Public REST contract | (A) keep current paths/status codes byte-for-byte · (B) allow minor cleanups | Open (recommend A) |

---

## 10. References

- Apache Pekko — <https://pekko.apache.org/>
- Pekko gRPC walkthrough — <https://pekko.apache.org/docs/pekko-grpc/current/server/walkthrough.html>
- Pekko gRPC sbt plugin — <https://pekko.apache.org/docs/pekko-grpc/current/buildtools/sbt.html>
- Pekko Persistence Cassandra — <https://pekko.apache.org/docs/pekko-persistence-cassandra/current/>
- Pekko Projection — <https://pekko.apache.org/docs/pekko-projection/current/>
- Pekko Connectors (Alpakka successor) — <https://pekko.apache.org/docs/pekko-connectors/current/>
- Pekko Management — <https://pekko.apache.org/docs/pekko-management/current/>
- Migration: Akka → Pekko 1.0.x — <https://pekko.apache.org/docs/pekko/current/migration/migration-guide-akka-1.0.x.html>
- Pekko version support policy — <https://pekko.apache.org/version-support.html>
- DataStax Java driver (`CqlSession`) — <https://github.com/datastax/java-driver>
- Quill `CassandraAsyncContext` — <https://getquill.io/#contexts-cassandra>

---

## 11. Appendix — file-count reality check (what actually changes)

| Category | Files touched | Nature |
|---|---|---|
| `.proto` definitions (new) | ~9 new files | Authoring |
| Service API traits (`*-api`) | ~67 files | Rewrite (replace `Service`/`ServiceCall`/`pathCall`) |
| Microservice loaders (`*ServiceLoader.scala`) | 9 files | Rewrite |
| Microservice impls (`*ServiceImpl.scala`) | ~9 files | Light edit (drop `ServiceCall` wrapping) |
| Entity files (`*Entity.scala`) | 28 files | One-line edit each (drop `AkkaTaggerAdapter`) |
| Read-side processors (`*EventProcessor.scala`) | 55 files | Rewrite as Pekko Projection handlers |
| Cassandra DAOs | ~30 files | Edit session helpers |
| `microservice-core` shared | ~8 files | Rewrite (DAO/Quill base, event helpers) |
| Serializer registries | 35 files | **Delete** (replaced by HOCON Jackson bindings) |
| `reference.conf` (serialization bindings) | 9 files | Edit |
| `application*.conf` (akka → pekko keys) | 44 files | Mechanical rename |
| API gateway controllers | 28 files | 1-character edit per service call (`invoke` removal) |
| `AnnetteApiLoader.scala` + service clients | 1 file (~17 sites) | Rewrite client construction |
| Exception hierarchy | ~35 files | Add gRPC status mapping |
| Test specs | ~10 files | Rewrite infra (Testcontainers) |
| Ignition client | 1 file | Rewrite client construction |
| `build.sbt`, `project/plugins.sbt`, `project/Dependencies.scala` | 3 files | Rewrite dependencies |
| `deploy/` (docker env, k8s config) | ~10 files | Config key renames + port changes |

**Total: ~250–400 file edits**, the majority mechanical (config renames, import renames, `invoke` removal). The genuinely cognitive work is concentrated in: `.proto` authoring (~9 files), the read-side rewrites (~55 files), the `microservice-core` DAO base (~8 files), and the gateway client wiring (~1 file).
