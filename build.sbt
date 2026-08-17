import com.typesafe.sbt.SbtNativePackager.autoImport.NativePackagerHelper._
import com.typesafe.sbt.packager.docker.DockerChmodType
import org.apache.pekko.grpc.sbt.PekkoGrpcPlugin
import play.sbt.routes.RoutesKeys

scalaVersion := "2.13.18"

ThisBuild / version := "0.6.0"
ThisBuild / scalaVersion := "2.13.18"

ThisBuild / organization := "biz.lobachev.annette"
ThisBuild / organizationName := "Valery Lobachev"
ThisBuild / organizationHomepage := Some(url("https://lobachev.biz/"))

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/annetteplatform/annette"),
    "scm:git@github.com:annetteplatform/annette.git"
  )
)
ThisBuild / developers := List(
  Developer(
    id = "valerylobachev",
    name = "Valery Lobachev",
    email = "valery@lobachev.biz",
    url = url("https://lobachev.biz/")
  )
)

ThisBuild / description := "Annette Platform Community Edition"
ThisBuild / licenses := List("Apache-2.0" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt"))
ThisBuild / homepage := Some(url("https://github.com/annetteplatform/annette"))

// Remove all additional repository other than Maven Central from POM
ThisBuild / pomIncludeRepository := { _ => false }
ThisBuild / publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
ThisBuild / publishMavenStyle := true

// Use external Kafka
ThisBuild / lagomKafkaEnabled := false
// Use external Cassandra
ThisBuild / lagomCassandraEnabled := false

ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-java8-compat" % "always"
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml"          % "always"

// Copyright settings
def annetteSettings: Seq[Setting[_]] =
  Seq(
    organizationName := "Valery Lobachev",
    startYear := Some(2013),
    scalaVersion := "2.13.18",
    // Scala 2.13.18 tightened implicit-explicit-type checks (new since 2.13.9).
    // Pre-existing codebase has ~700 `implicit val format = Json.format[X]` without
    // explicit types; fixing them is out of scope for slice 002. Silent for now;
    // the silence is per-message (not per-category) so other lint remains fatal.
    scalacOptions += "-Wconf:msg=Implicit definition should have explicit type:s"
  )

def confDirSettings: Seq[Setting[_]] =
  Seq(
    Runtime / unmanagedClasspath += baseDirectory.value / "conf",
    Universal / mappings ++= directory(baseDirectory.value / "conf"),
    scriptClasspath := "../conf/" +: scriptClasspath.value
  )

def dockerSettings: Seq[Setting[_]] =
  Seq(
    dockerBaseImage := "openjdk:11",
    dockerEntrypoint += "-Dpidfile.path=/dev/null", // в common settings
    dockerExposedPorts += 9000,
    dockerChmodType := DockerChmodType.UserGroupWriteExecute,
    dockerUsername := Some("annetteplatform")
  )

lazy val root = (project in file("."))
  .settings(name := "annette")
  .settings(annetteSettings: _*)
  .aggregate(
    `core`,
    `microservice-core`,
    `microservice-core-pekko`,
    `api-gateway-core`,
    `api-gateway`,
    // initialization application
    `demo-ignition`,
    `camunda`,
    // API gateways
    `application-api-gateway`,
    `service-catalog-api-gateway`,
    `authorization-api-gateway`,
    `bpm-api-gateway`,
    `cms-api-gateway`,
    `org-structure-api-gateway`,
    `persons-api-gateway`,
    `principal-groups-api-gateway`,
    // microservices API
    `application-api`,
    `service-catalog-api`,
    `authorization-api`,
    `bpm-repository-api`,
    `cms-api`,
    `org-structure-api`,
    `persons-api`,
    `principal-groups-api`,
    `subscriptions-api`,
    // microservices
    `application`,
    `service-catalog`,
    `authorization`,
    `bpm-repository`,
    `cms`,
    `org-structure`,
    `persons`,
    `principal-groups`,
    `subscriptions`
  )

lazy val `core` = (project in file("core/core"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      lagomScaladslPersistenceCassandra,
      lagomScaladslServer % Optional,
      lagomScaladslTestKit,
      Dependencies.chimney,
      Dependencies.playJsonExt,
      Dependencies.macwire
    ) ++ Dependencies.tests
      ++ Dependencies.elastic
      ++ Dependencies.lagomAkkaDiscovery
  )
  .settings(annetteSettings: _*)

lazy val `microservice-core` = (project in file("core/microservice-core"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      lagomScaladslPersistenceCassandra,
      lagomScaladslServer % Optional,
      lagomScaladslTestKit,
      Dependencies.chimney,
      Dependencies.pureConfig,
      Dependencies.playJsonExt,
//      Dependencies.logstashEncoder,
      Dependencies.macwire
    ) ++ Dependencies.tests
      ++ Dependencies.elastic
      ++ Dependencies.lagomAkkaDiscovery
      ++ Dependencies.quill
  )
  .settings(annetteSettings: _*)
  .settings(
    scalacOptions += "-Wconf:cat=unused-nowarn:s",
    scalacOptions += "-Wconf:msg=evidence parameter.*never used:s"
  )
  .dependsOn(
    `core`
  )

// Pekko-variant shared core (slice 003). Coexists with `microservice-core` (Lagom variant)
// during the migration. Slices 004-012 add `.dependsOn(microservice-core-pekko)` as they
// migrate. Slice 013 deletes the Lagom variant and renames this back to `microservice-core`.
//
// NOTE: depends on `core` only, NOT on `microservice-core`. The two share no source.
// Quill uses driver-3 (`com.datastax.driver.core.*`); Pekko Persistence Cassandra uses
// driver-4 (`com.datastax.oss.driver.api.core.*`). Both jars coexist on the classpath.
// Service slices obtain separate sessions for each driver.
lazy val `microservice-core-pekko` = (project in file("core/microservice-core-pekko"))
  .settings(
    libraryDependencies ++= Dependencies.pekkoCore
      ++ Dependencies.pekkoPersistenceCassandra
      ++ Dependencies.pekkoProjection
      ++ Dependencies.quillPekko
      ++ Dependencies.elastic
      ++ Seq(Dependencies.chimney, Dependencies.pureConfig)
      ++ Dependencies.tests
  )
  .settings(annetteSettings: _*)
  .settings(
    scalacOptions += "-Wconf:cat=unused-nowarn:s"
  )
  .dependsOn(
    `core`
  )

lazy val `api-gateway-core` = (project in file("core/api-gateway-core"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslServer % Optional,
      ws,
      Dependencies.macwire,
      Dependencies.playJsonExt,
      Dependencies.jwt,
      Dependencies.pureConfig,
      Dependencies.chimney
    ) ++
      Dependencies.tests
  )
  .settings(annetteSettings: _*)
  .dependsOn(
    `core`,
    `authorization-api`,
    `org-structure-api`,
    `persons-api`,
    `principal-groups-api`
  )

lazy val `api-gateway` = (project in file("api-gateway/api-gateway"))
  .enablePlugins(LagomPlay, LagomScala, PekkoGrpcPlugin)
  .settings(
    // To disable Unused import error for routes
    RoutesKeys.routesImport := Seq.empty,
    libraryDependencies ++= Seq(
      lagomScaladslServer,
      ws,
      Dependencies.macwire
    ) ++
      Dependencies.tests
  )
  .settings(confDirSettings: _*)
  .settings(annetteSettings: _*)
  .settings(dockerSettings: _*)
  .dependsOn(
    `api-gateway-core`,
    `application-api-gateway`,
    `service-catalog-api-gateway`,
    `authorization-api-gateway`,
    `org-structure-api-gateway`,
    `persons-api-gateway`,
    `principal-groups-api-gateway`,
    `cms-api-gateway`,
    `bpm-api-gateway`
  )

def ignitionDemoProject(pr: Project) =
  pr
    .enablePlugins(UniversalPlugin)
    .enablePlugins(JavaAppPackaging)
    .settings(
      // To disable Unused import error for routes
      RoutesKeys.routesImport := Seq.empty,
      libraryDependencies ++= Seq(
        ws,
        Dependencies.macwire,
        Dependencies.playJsonExt,
        Dependencies.pureConfig,
        Dependencies.chimney,
        Dependencies.jacksonYaml
      ) ++
        Dependencies.tests ++
        Dependencies.slf4j
    )
    .settings(confDirSettings: _*)
    .settings(annetteSettings: _*)
    .settings(
      dockerBaseImage := "openjdk:11",
      dockerChmodType := DockerChmodType.UserGroupWriteExecute,
      dockerUsername := Some("annetteplatform")
    )
    .dependsOn(
      `service-catalog-api`,
      `application-api`,
      `authorization-api`,
      `org-structure-api`,
      `persons-api`,
      `cms-api`,
      `principal-groups-api`,
      `subscriptions-api`
    )

lazy val `application-api` = (project in file("application/application-api"))
  .enablePlugins(PekkoGrpcPlugin)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    ) ++ Dependencies.tests
  )
  .settings(annetteSettings: _*)
  .dependsOn(`core`)

def applicationProject(pr: Project) =
  pr
    .enablePlugins(JavaAppPackaging)
    .settings(
      libraryDependencies ++= Dependencies.pekkoCore
        ++ Dependencies.pekkoPersistenceCassandra
        ++ Dependencies.pekkoProjection
        ++ Seq(Dependencies.macwire, Dependencies.chimney)
        ++ Dependencies.quillPekko
        ++ Dependencies.tests
    )
    .settings(Test / fork := true)
    .settings(confDirSettings: _*)
    .settings(annetteSettings: _*)
    .settings(dockerSettings: _*)
    .dependsOn(`application-api`, `microservice-core-pekko`, `microservice-core`)

lazy val `service-catalog-api` = (project in file("application/service-catalog-api"))
  .enablePlugins(PekkoGrpcPlugin)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    ) ++ Dependencies.tests
  )
  .settings(annetteSettings: _*)
  .dependsOn(`core`)

def serviceCatalogProject(pr: Project) =
  pr
    .enablePlugins(JavaAppPackaging)
    .settings(
      libraryDependencies ++= Dependencies.pekkoCore
        ++ Dependencies.pekkoPersistenceCassandra
        ++ Dependencies.pekkoProjection
        ++ Seq(Dependencies.macwire, Dependencies.chimney)
        ++ Dependencies.quillPekko
        ++ Dependencies.tests
    )
    .settings(Test / fork := true)
    .settings(confDirSettings: _*)
    .settings(annetteSettings: _*)
    .settings(dockerSettings: _*)
    .dependsOn(`service-catalog-api`, `microservice-core-pekko`, `microservice-core`)

lazy val `application-api-gateway` = (project in file("api-gateway/application-api-gateway"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslServer % Optional,
      ws,
      Dependencies.macwire,
      Dependencies.playJsonExt,
      Dependencies.jwt,
      Dependencies.pureConfig,
      Dependencies.chimney
    ) ++
      Dependencies.tests
  )
  .settings(annetteSettings: _*)
  .dependsOn(
    `api-gateway-core`,
    `application-api`
  )

lazy val `service-catalog-api-gateway` = (project in file("api-gateway/service-catalog-api-gateway"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslServer % Optional,
      ws,
      Dependencies.macwire,
      Dependencies.playJsonExt,
      Dependencies.jwt,
      Dependencies.pureConfig,
      Dependencies.chimney
    ) ++
      Dependencies.tests
  )
  .settings(annetteSettings: _*)
  .dependsOn(
    `api-gateway-core`,
    `application-api`,
    `service-catalog-api`
  )

lazy val `authorization-api` = (project in file("authorization/authorization-api"))
  .enablePlugins(PekkoGrpcPlugin)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      Dependencies.chimney
    )
  )
  .settings(annetteSettings: _*)
  .dependsOn(`core`)

def authorizationProject(pr: Project) =
  pr
    // Slice 004: dropped LagomScala + lagomScaladsl* deps; replaced with Pekko.
    // JavaAppPackaging provides the Universal/docker/scriptClasspath keys that confDirSettings
    // and dockerSettings reference (previously inherited from LagomScala).
    .enablePlugins(JavaAppPackaging)
    .settings(
      libraryDependencies ++= Dependencies.pekkoCore
        ++ Dependencies.pekkoPersistenceCassandra
        ++ Dependencies.pekkoProjection
        ++ Seq(Dependencies.macwire, Dependencies.chimney)
        ++ Dependencies.tests
    )
    .settings(Test / fork := true)
    .settings(confDirSettings: _*)
    .settings(annetteSettings: _*)
    .settings(dockerSettings: _*)
    // Note: also depends on `microservice-core` (Lagom variant) for the indexing module
    // (IndexingModule, AbstractIndexDao, RoleIndexDao, AssignmentIndexDao). That module uses
    // akka.Done + Lagom's TransportErrorCode; not yet ported to microservice-core-pekko.
    // Lagom deps come transitively but are unused by authorization sources. Slice 005+ should
    // either port indexing to microservice-core-pekko OR add the same dep line.
    .dependsOn(`authorization-api`, `microservice-core-pekko`, `microservice-core`)

lazy val `authorization-api-gateway` = (project in file("api-gateway/authorization-api-gateway"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslServer % Optional,
      ws,
      Dependencies.macwire,
      Dependencies.playJsonExt,
      Dependencies.jwt,
      Dependencies.pureConfig,
      Dependencies.chimney
    ) ++
      Dependencies.tests
  )
  .settings(annetteSettings: _*)
  .dependsOn(
    `api-gateway-core`,
    `authorization-api`
  )

lazy val `camunda` = (project in file("bpm/camunda"))
  .settings(
    libraryDependencies ++= Seq(
      ws,
      Dependencies.macwire,
      Dependencies.chimney,
      Dependencies.playJson,
      Dependencies.playJsonExt
    ) ++
      Dependencies.tests
  )
  .settings(annetteSettings: _*)
  .dependsOn(`core`)

lazy val `bpm-repository-api` = (project in file("bpm/bpm-repository-api"))
  .enablePlugins(PekkoGrpcPlugin)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      Dependencies.chimney
    )
  )
  .settings(annetteSettings: _*)
  .dependsOn(`core`)

def bpmRepositoryProject(pr: Project) =
  pr
    .enablePlugins(JavaAppPackaging)
    .settings(
      libraryDependencies ++= Dependencies.pekkoCore // HTTP server + gRPC; no persistence/projection (Postgres-only service)
        ++ Seq(
          Dependencies.chimney,
          Dependencies.postgresql
        ) ++ Dependencies.tests ++
        Dependencies.slick
    )
    .settings(Test / fork := true) // survives slice 013 (which removes lagomForkedTestSettings)
    .settings(confDirSettings: _*)
    .settings(annetteSettings: _*)
    .settings(dockerSettings: _*)
    .dependsOn(`bpm-repository-api`, `microservice-core-pekko`)

lazy val `bpm-api-gateway` = (project in file("api-gateway/bpm-api-gateway"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslServer % Optional,
      ws,
      Dependencies.macwire,
      Dependencies.playJsonExt,
      Dependencies.jwt,
      Dependencies.pureConfig,
      Dependencies.chimney
    ) ++
      Dependencies.tests
  )
  .settings(annetteSettings: _*)
  .dependsOn(
    `api-gateway-core`,
    `bpm-repository-api`,
    `camunda`
  )

lazy val `cms-api` = (project in file("cms/cms-api"))
  .enablePlugins(PekkoGrpcPlugin)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      Dependencies.chimney
    ) ++ Dependencies.tests ++
      Dependencies.alpakkaS3 // gateway still wires the akka/alpakka CmsStorage (until slice 013)
  )
  .settings(annetteSettings: _*)
  .dependsOn(`core`)

def cmsProject(pr: Project) =
  pr
    .enablePlugins(JavaAppPackaging)
    .settings(
      libraryDependencies ++= Dependencies.pekkoCore
        ++ Dependencies.pekkoPersistenceCassandra
        ++ Dependencies.pekkoProjection
        ++ Dependencies.pekkoConnectorsS3 // replaces alpakkaS3 (slice 011)
        ++ Seq(Dependencies.macwire, Dependencies.chimney)
        ++ Dependencies.quillPekko
        ++ Dependencies.tests
    )
    .settings(Test / fork := true)
    .settings(confDirSettings: _*)
    .settings(annetteSettings: _*)
    .settings(dockerSettings: _*)
    .dependsOn(`cms-api`, `microservice-core-pekko`, `microservice-core`)

lazy val `cms-api-gateway` = (project in file("api-gateway/cms-api-gateway"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslServer % Optional,
      ws,
      Dependencies.macwire,
      Dependencies.playJsonExt,
      Dependencies.jwt,
      Dependencies.pureConfig,
      Dependencies.chimney
    ) ++
      Dependencies.tests
  )
  .settings(annetteSettings: _*)
  .dependsOn(
    `api-gateway-core`,
    `cms-api`,
    `subscriptions-api`
  )

lazy val `org-structure-api` = (project in file("principals/org-structure-api"))
  .enablePlugins(PekkoGrpcPlugin)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      Dependencies.chimney
    )
  )
  .settings(annetteSettings: _*)
  .dependsOn(`core`)

def orgStructureProject(pr: Project) =
  pr
    .enablePlugins(JavaAppPackaging)
    .settings(
      libraryDependencies ++= Dependencies.pekkoCore
        ++ Dependencies.pekkoPersistenceCassandra
        ++ Dependencies.pekkoProjection
        ++ Seq(Dependencies.macwire, Dependencies.chimney, Dependencies.pureConfig)
        ++ Dependencies.quillPekko
        ++ Dependencies.tests
        ++ Seq(
          "org.apache.pekko" %% "pekko-actor-testkit-typed" % Dependencies.PekkoVersion.pekkoCore % Test,
          "org.apache.pekko" %% "pekko-persistence-testkit" % Dependencies.PekkoVersion.pekkoCore % Test
        )
    )
    .settings(Test / fork := true)
    .settings(confDirSettings: _*)
    .settings(annetteSettings: _*)
    .settings(dockerSettings: _*)
    .dependsOn(`org-structure-api`, `microservice-core-pekko`, `microservice-core`)

lazy val `org-structure-api-gateway` = (project in file("api-gateway/org-structure-api-gateway"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslServer % Optional,
      ws,
      Dependencies.macwire,
      Dependencies.playJsonExt,
      Dependencies.jwt,
      Dependencies.pureConfig,
      Dependencies.chimney
    ) ++
      Dependencies.tests
  )
  .settings(annetteSettings: _*)
  .dependsOn(
    `api-gateway-core`,
    `org-structure-api`
  )

lazy val `persons-api` = (project in file("principals/persons-api"))
  .enablePlugins(PekkoGrpcPlugin)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      Dependencies.chimney
    )
  )
  .settings(annetteSettings: _*)
  .dependsOn(`core`)

def personsProject(pr: Project) =
  pr
    .enablePlugins(JavaAppPackaging)
    .settings(
      libraryDependencies ++= Dependencies.pekkoCore
        ++ Dependencies.pekkoPersistenceCassandra
        ++ Dependencies.pekkoProjection
        ++ Seq(Dependencies.macwire, Dependencies.chimney)
        ++ Dependencies.quillPekko
        ++ Dependencies.tests
        ++ Seq(
          "org.apache.pekko" %% "pekko-actor-testkit-typed" % Dependencies.PekkoVersion.pekkoCore % Test,
          "org.apache.pekko" %% "pekko-persistence-testkit" % Dependencies.PekkoVersion.pekkoCore % Test
        )
    )
    .settings(Test / fork := true)
    .settings(confDirSettings: _*)
    .settings(annetteSettings: _*)
    .settings(dockerSettings: _*)
    .dependsOn(`persons-api`, `microservice-core-pekko`, `microservice-core`)

lazy val `persons-api-gateway` = (project in file("api-gateway/persons-api-gateway"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslServer % Optional,
      ws,
      Dependencies.macwire,
      Dependencies.playJsonExt,
      Dependencies.jwt,
      Dependencies.pureConfig,
      Dependencies.chimney
    ) ++
      Dependencies.tests
  )
  .settings(annetteSettings: _*)
  .dependsOn(
    `api-gateway-core`,
    `persons-api`
  )

lazy val `principal-groups-api` = (project in file("principals/principal-groups-api"))
  .enablePlugins(PekkoGrpcPlugin)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      Dependencies.chimney
    )
  )
  .settings(annetteSettings: _*)
  .dependsOn(`core`)

def principalGroupsProject(pr: Project) =
  pr
    .enablePlugins(JavaAppPackaging)
    .settings(
      libraryDependencies ++= Dependencies.pekkoCore
        ++ Dependencies.pekkoPersistenceCassandra
        ++ Dependencies.pekkoProjection
        ++ Seq(Dependencies.macwire, Dependencies.chimney)
        ++ Dependencies.quillPekko
        ++ Dependencies.tests
    )
    .settings(Test / fork := true)
    .settings(confDirSettings: _*)
    .settings(annetteSettings: _*)
    .settings(dockerSettings: _*)
    .dependsOn(`principal-groups-api`, `microservice-core-pekko`, `microservice-core`)

lazy val `principal-groups-api-gateway` = (project in file("api-gateway/principal-groups-api-gateway"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslServer % Optional,
      ws,
      Dependencies.macwire,
      Dependencies.playJsonExt,
      Dependencies.jwt,
      Dependencies.pureConfig,
      Dependencies.chimney
    ) ++
      Dependencies.tests
  )
  .settings(annetteSettings: _*)
  .dependsOn(
    `api-gateway-core`,
    `principal-groups-api`
  )

lazy val `subscriptions-api` = (project in file("cms/subscriptions-api"))
  .enablePlugins(PekkoGrpcPlugin)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      Dependencies.chimney
    ) ++ Dependencies.tests
  )
  .settings(annetteSettings: _*)
  .dependsOn(`core`)

def subscriptionsProject(pr: Project) =
  pr
    .enablePlugins(JavaAppPackaging)
    .settings(
      libraryDependencies ++= Dependencies.pekkoCore
        ++ Dependencies.pekkoPersistenceCassandra
        ++ Dependencies.pekkoProjection
        ++ Seq(Dependencies.macwire, Dependencies.chimney)
        ++ Dependencies.quillPekko
        ++ Dependencies.tests
    )
    .settings(Test / fork := true)
    .settings(confDirSettings: _*)
    .settings(annetteSettings: _*)
    .settings(dockerSettings: _*)
    .dependsOn(`subscriptions-api`, `microservice-core-pekko`, `microservice-core`)

lazy val `demo-ignition`    = ignitionDemoProject(project in file("ignition/demo-ignition"))
lazy val `application`      = applicationProject(project in file("application/application"))
lazy val `service-catalog`  = serviceCatalogProject(project in file("application/service-catalog"))
lazy val `authorization`    = authorizationProject(project in file("authorization/authorization"))
lazy val `bpm-repository`   = bpmRepositoryProject(project in file("bpm/bpm-repository"))
lazy val `cms`              = cmsProject(project in file("cms/cms"))
lazy val `org-structure`    = orgStructureProject(project in file("principals/org-structure"))
lazy val `persons`          = personsProject(project in file("principals/persons"))
lazy val `principal-groups` = principalGroupsProject(project in file("principals/principal-groups"))
lazy val `subscriptions`    = subscriptionsProject(project in file("cms/subscriptions"))
