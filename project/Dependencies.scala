import sbt._

object Dependencies {
  object Version {
    val macwire                 = "2.5.0"
    val scalaTest               = "3.2.10"
    val scalaCheck              = "1.15.4"
    val commonsIO               = "2.11.0"
    val elastic4s               = "7.8.1"
    val jwtPlayJson             = "10.0.0"
    val chimney                 = "1.0.0"
    val pureConfig              = "0.17.1"
    val quill                   = "3.10.0"
    val slick                   = "3.3.3"
    val slick_hikaricp          = "3.3.3"
    val postgresql              = "42.3.1"
    val playJson                = "3.0.1"
    val playJsonExt             = "0.42.0"
    val scalaXml                = "2.1.0"
    val jacksonYaml             = "2.11.4"
    val slf4j                   = "1.7.36"
  }

  // ---------------------------------------------------------------------------
  // Pekko version matrix — locked by dev/migration/001-decisions.md §C.
  // ---------------------------------------------------------------------------
  object PekkoVersion {
    // Play 3.0.11 is the Pekko-based Play release; coordinates moved to
    // org.playframework (not com.typesafe.play) in Play 3.x.
    val playFramework       = "3.0.11"
    // Pekko Core verified empirically by Spike A.
    val pekkoCore           = "1.1.3"
    // Pekko HTTP is pulled in transitively by Play 3.0.11 at 1.1.x; pinned
    // explicitly for direct dependencies in clients/tests.
    val pekkoHttp           = "1.1.0"
    // Pekko gRPC (runtime + sbt plugin) verified by Spike B.
    val pekkoGrpc           = "1.1.0"
    // Pekko Persistence Cassandra verified by Spike A.
    val pekkoPersistenceCassandra = "1.1.0"
    // Pekko Projection verified by Spike A (compiles; runtime documented).
    val pekkoProjection     = "1.1.0"
    // Pekko Connectors S3 — first user was CMS (slice 011); gateway follows in 013.
    val pekkoConnectorsS3   = "1.1.0"
    // Pekko Management — k8s bootstrap.
    val pekkoManagement     = "1.1.0"
  }

  val macwire = "com.softwaremill.macwire" %% "macros" % Version.macwire % "provided"

  val tests = Seq(
    "org.scalatest"  %% "scalatest"  % Version.scalaTest  % Test,
    "commons-io"      % "commons-io" % Version.commonsIO  % Test,
    "org.scalacheck" %% "scalacheck" % Version.scalaCheck % Test
  )

  val elastic: Seq[sbt.ModuleID] = Seq(
    "com.sksamuel.elastic4s" %% "elastic4s-core"          % Version.elastic4s,
    "com.sksamuel.elastic4s" %% "elastic4s-client-esjava" % Version.elastic4s,
    "com.sksamuel.elastic4s" %% "elastic4s-json-play"     % Version.elastic4s
  )

  val jwt: sbt.ModuleID = "com.github.jwt-scala" %% "jwt-play-json" % Version.jwtPlayJson

  val pureConfig = "com.github.pureconfig" %% "pureconfig" % Version.pureConfig

  val chimney = "io.scalaland" %% "chimney" % Version.chimney

  val quillCore = "io.getquill" %% "quill-core" % Version.quill

  // quill-cassandra-lagom was dropped in slice 013 together with the last Lagom
  // dependency (it needed Lagom's CassandraSession / CassandraLagomAsyncContext).
  val quill: Seq[ModuleID] = Seq(
    quillCore,
    "io.getquill" %% "quill-cassandra" % Version.quill,
    // quill-cassandra's driver 3 (3.7.2) initializes JMX metrics against the legacy
    // com.codahale namespace at Cluster.connect; nothing provides it transitively
    // (first observed booting cms in slice 011).
    "com.codahale.metrics" % "metrics-core" % "3.0.2"
  )

  val slick: Seq[ModuleID] = Seq(
    "com.typesafe.slick" %% "slick"          % Version.slick,
    "com.typesafe.slick" %% "slick-hikaricp" % Version.slick_hikaricp
  )

  val postgresql: ModuleID = "org.postgresql" % "postgresql" % Version.postgresql

  val playJson = "org.playframework" %% "play-json" % Version.playJson

  // play-json-extensions: >22-field case-class formats (TaskFindQuery). Latest release
  // (0.42.0) targets play-json 2.8 coordinates; the API surface it uses (JsPath/
  // Reads/Writes/functional) is stable across play-json 3 — its transitive play-json
  // 2.8 dep is evicted up to 3.0.x by our direct dependency. Verified at the gateway
  // runtime in slice 013.
  val playJsonExt: sbt.ModuleID = "ai.x" %% "play-json-extensions" % Version.playJsonExt

  // Play 3 framework artifacts (org.playframework) — for the gateway, ignition and camunda
  // library projects. The api-gateway app itself gets Play via the PlayScala sbt plugin.
  val play: Seq[ModuleID] = Seq(
    "org.playframework" %% "play"                 % PekkoVersion.playFramework,
    "org.playframework" %% "play-ahc-ws"          % PekkoVersion.playFramework,
    "org.playframework" %% "play-filters-helpers" % PekkoVersion.playFramework
  )

  val scalaXml: sbt.ModuleID = "org.scala-lang.modules" %% "scala-xml" % Version.scalaXml

  val jacksonYaml = "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % Version.jacksonYaml

  val slf4j = Seq(
    "org.slf4j" % "slf4j-api"    % Version.slf4j,
    "org.slf4j" % "slf4j-simple" % Version.slf4j
  )

  // ---------------------------------------------------------------------------
  // Pekko dependency vals. Versions per PekkoVersion above.
  // ---------------------------------------------------------------------------

  val pekkoCore: Seq[ModuleID] = Seq(
    "org.apache.pekko" %% "pekko-actor-typed"            % PekkoVersion.pekkoCore,
    "org.apache.pekko" %% "pekko-stream"                 % PekkoVersion.pekkoCore,
    "org.apache.pekko" %% "pekko-cluster-typed"          % PekkoVersion.pekkoCore,
    "org.apache.pekko" %% "pekko-cluster-sharding-typed" % PekkoVersion.pekkoCore,
    "org.apache.pekko" %% "pekko-persistence-typed"      % PekkoVersion.pekkoCore,
    "org.apache.pekko" %% "pekko-http"                   % PekkoVersion.pekkoHttp,
    // pekko-http 1.1.0 was built against Pekko 1.1.1 and drags pekko-discovery 1.1.1 in
    // transitively; without this explicit pin the runtime mixed-version detector aborts
    // startup (first observed booting cms in slice 011; latent in all Pekko services).
    "org.apache.pekko" %% "pekko-discovery"              % PekkoVersion.pekkoCore,
    // The `jackson-json` serializer alias used by every service's serialization-bindings
    // is declared in this artifact's reference.conf; nothing pulls it in transitively, so
    // without it actor-system startup fails with "key not found: jackson-json" (latent in
    // all Pekko services since slice 004; first observed booting cms in slice 011).
    "org.apache.pekko" %% "pekko-serialization-jackson"  % PekkoVersion.pekkoCore,
    // Every microservice ships conf/logback.xml (on the runtime classpath via
    // confDirSettings); Lagom used to supply the backend. Without it SLF4J falls back to
    // NOP and the services log nothing (same latent-gap class; slice 011). 1.3.x is the
    // slf4j-2.0-compatible line (Pekko 1.1.3 pulls slf4j-api 2.0.16) while still targeting
    // Java 8/11. Projects on Play 3 (the gateway) evict this up to logback 1.4.x.
    "ch.qos.logback"    % "logback-classic"              % "1.3.14"
  )

  val pekkoPersistenceCassandra: Seq[ModuleID] = Seq(
    "org.apache.pekko" %% "pekko-persistence-cassandra" % PekkoVersion.pekkoPersistenceCassandra
  )

  val pekkoProjection: Seq[ModuleID] = Seq(
    "org.apache.pekko" %% "pekko-projection-cassandra"   % PekkoVersion.pekkoProjection,
    // eventsourced adds EventEnvelope + EventSourcedProvider.eventsByTag — used by ProjectionBase.
    "org.apache.pekko" %% "pekko-projection-eventsourced" % PekkoVersion.pekkoProjection
  )

  val pekkoManagement: Seq[ModuleID] = Seq(
    "org.apache.pekko" %% "pekko-management-cluster-bootstrap" % PekkoVersion.pekkoManagement,
    "org.apache.pekko" %% "pekko-discovery-kubernetes-api"     % PekkoVersion.pekkoManagement
  )

  val pekkoConnectorsS3: Seq[ModuleID] = Seq(
    "org.apache.pekko" %% "pekko-connectors-s3" % PekkoVersion.pekkoConnectorsS3
  )

  val pekkoGrpcRuntime: Seq[ModuleID] = Seq(
    "org.apache.pekko" %% "pekko-grpc-runtime" % PekkoVersion.pekkoGrpc
  )

  val pekkoGrpcTestKit: Seq[ModuleID] = Seq(
    "org.apache.pekko" %% "pekko-grpc-testkit" % PekkoVersion.pekkoGrpc % Test
  )

  // Play 3.0.11 was built against Pekko 1.0.x and drags 1.0.3 artifacts transitively;
  // without overrides, projects that consume Play but don't take the full pekkoCore set
  // (e.g. camunda) end up with mixed versions — the runtime detector aborts startup
  // (first observed running camunda specs in slice 013).
  val pekkoDependencyOverrides: Seq[ModuleID] = Seq(
    "org.apache.pekko" %% "pekko-actor"                % PekkoVersion.pekkoCore,
    "org.apache.pekko" %% "pekko-actor-typed"          % PekkoVersion.pekkoCore,
    "org.apache.pekko" %% "pekko-stream"               % PekkoVersion.pekkoCore,
    "org.apache.pekko" %% "pekko-slf4j"                % PekkoVersion.pekkoCore,
    "org.apache.pekko" %% "pekko-serialization-jackson" % PekkoVersion.pekkoCore,
    "org.apache.pekko" %% "pekko-protobuf-v3"          % PekkoVersion.pekkoCore,
    "org.apache.pekko" %% "pekko-discovery"            % PekkoVersion.pekkoCore
  )

}
