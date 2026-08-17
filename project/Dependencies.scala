import com.lightbend.lagom.core.LagomVersion
import sbt._

object Dependencies {
  object Version {
    val macwire                 = "2.5.0"
    val scalaTest               = "3.2.10"
    val scalaCheck              = "1.15.4"
    val commonsIO               = "2.11.0"
    val elastic4s               = "7.8.1"
    val playJsonExt             = "0.42.0"
    val jwtPlayJson             = "9.0.2"
    val akkaKubernetesDiscovery = "1.0.10"
    val chimney                 = "1.0.0"
    val pureConfig              = "0.17.1"
    val quill                   = "3.10.0"
    val alpakkaS3               = "3.0.4"
    val slick                   = "3.3.3"
    val slick_hikaricp          = "3.3.3"
    val postgresql              = "42.3.1"
    val playJson                = "2.8.2"
    val jacksonYaml             = "2.11.4"
    val slf4j                   = "1.7.36"
  }

  // ---------------------------------------------------------------------------
  // Pekko version matrix — locked by dev/migration/001-decisions.md §C.
  // Slices 003-012 consume these vals. Do not pin different versions.
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
    // Pekko Connectors S3 — NOT spiked; first user is CMS slice 011.
    val pekkoConnectorsS3   = "1.1.0"
    // Pekko Management — NOT spiked; first user is k8s slice 013.
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

  val playJsonExt: sbt.ModuleID = "ai.x" %% "play-json-extensions" % Version.playJsonExt

  val jwt: sbt.ModuleID = "com.github.jwt-scala" %% "jwt-play-json" % Version.jwtPlayJson

  val lagomAkkaDiscovery: Seq[sbt.ModuleID] = Seq(
    "com.lightbend.lagom"          %% "lagom-scaladsl-akka-discovery-service-locator" % LagomVersion.current,
    "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api"                 % Version.akkaKubernetesDiscovery
  )

  val pureConfig = "com.github.pureconfig" %% "pureconfig" % Version.pureConfig

  val chimney = "io.scalaland" %% "chimney" % Version.chimney

  val quillCore = "io.getquill" %% "quill-core" % Version.quill

  val quill: Seq[ModuleID] = Seq(
    quillCore,
    "io.getquill" %% "quill-cassandra"       % Version.quill,
    "io.getquill" %% "quill-cassandra-lagom" % Version.quill
  )

  // Pekko variant — drops quill-cassandra-lagom (which depends on Lagom's CassandraSession
  // and CassandraLagomAsyncContext). Pekko services construct a plain CassandraAsyncContext
  // from quill-cassandra. Slice 013 may consolidate this with `quill` once Lagom is gone.
  val quillPekko: Seq[ModuleID] = Seq(
    quillCore,
    "io.getquill" %% "quill-cassandra" % Version.quill,
    // quill-cassandra's driver 3 (3.7.2) initializes JMX metrics against the legacy
    // com.codahale namespace at Cluster.connect; nothing provides it transitively
    // (first observed booting cms in slice 011).
    "com.codahale.metrics" % "metrics-core" % "3.0.2"
  )

  val alpakkaS3: Seq[ModuleID] = Seq(
    "com.lightbend.akka" %% "akka-stream-alpakka-s3" % Version.alpakkaS3,
    "com.typesafe.akka"  %% "akka-stream"            % LagomVersion.akka,
    "com.typesafe.akka"  %% "akka-http"              % LagomVersion.akkaHttp,
    "com.typesafe.akka"  %% "akka-http-xml"          % LagomVersion.akkaHttp
  )

  val slick: Seq[ModuleID] = Seq(
    "com.typesafe.slick" %% "slick"          % Version.slick,
    "com.typesafe.slick" %% "slick-hikaricp" % Version.slick_hikaricp
  )

  val postgresql: ModuleID = "org.postgresql" % "postgresql" % Version.postgresql

  val playJson = "com.typesafe.play" %% "play-json" % Version.playJson

  val jacksonYaml = "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % Version.jacksonYaml

  val slf4j = Seq(
    "org.slf4j" % "slf4j-api"    % Version.slf4j,
    "org.slf4j" % "slf4j-simple" % Version.slf4j
  )

  // ---------------------------------------------------------------------------
  // Pekko dependency vals (not yet consumed by any project after slice 002;
  // slices 003-012 wire them in. Versions per PekkoVersion above.)
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
    // Java 8/11.
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

  val pekkoGrpcTestKit: Seq[ModuleID] = Seq(
    "org.apache.pekko" %% "pekko-grpc-testkit" % PekkoVersion.pekkoGrpc % Test
  )

}
