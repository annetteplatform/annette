import com.typesafe.sbt.SbtNativePackager.autoImport.NativePackagerHelper._
import com.typesafe.sbt.packager.docker.{Cmd, DockerChmodType}
import play.sbt.routes.RoutesKeys
import sbt.Keys.sources

scalaVersion := "2.13.3"
maintainer := "valery@lobachev.biz"

organization in ThisBuild := "biz.lobachev.annette"
version in ThisBuild := "0.1.2"
maintainer in ThisBuild := "valery@lobachev.biz"
scalaVersion in ThisBuild := "2.13.3"

// Use external Kafka
lagomKafkaEnabled in ThisBuild := false
// Use external Cassandra
lagomCassandraEnabled in ThisBuild := false

// Copyright settings
def annetteSettings: Seq[Setting[_]] =
  Seq(
    organizationName := "Valery Lobachev",
    startYear := Some(2013),
    licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt")),
    sources in (Compile, doc) := Seq.empty
  )

def confDirSettings: Seq[Setting[_]] =
  Seq(
    unmanagedClasspath in Runtime += baseDirectory.value / "conf",
    mappings in Universal ++= directory(baseDirectory.value / "conf"),
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
    `api-gateway-core`,
    `api-gateway`,
    // microservices API
    `application-api`,
    `attributes-api`,
    `authorization-api`,
    `org-structure-api`,
    `persons-api`,
    // microservices
    `application`,
    `attributes`,
    `authorization`,
    `org-structure`,
    `persons`,
    // initialization application
    `init-app`
  )

lazy val `core` = (project in file("annette/core"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      lagomScaladslPersistenceCassandra,
      lagomScaladslServer % Optional,
      lagomScaladslTestKit,
      Dependencies.playJsonExt,
      Dependencies.logstashEncoder,
      Dependencies.macwire
    ) ++ Dependencies.tests
      ++ Dependencies.elastic
      ++ Dependencies.lagomAkkaDiscovery
  )
  .settings(annetteSettings: _*)

lazy val `api-gateway-core` = (project in file("annette/api-gateway-core"))
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
    `application-api`,
    `authorization-api`,
    `org-structure-api`,
    `persons-api`
  )

lazy val `api-gateway` = (project in file("annette/api-gateway"))
  .enablePlugins(LagomPlay, LagomScala)
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
    `api-gateway-core`
  )

def initAppProject(pr: Project) =
  pr
    .enablePlugins(LagomPlay, LagomScala)
    .settings(
      // To disable Unused import error for routes
      RoutesKeys.routesImport := Seq.empty,
      libraryDependencies ++= Seq(
        lagomScaladslServer,
        ws,
        Dependencies.macwire,
        Dependencies.playJsonExt,
        Dependencies.pureConfig,
        Dependencies.chimney
      ) ++
        Dependencies.tests
    )
    .settings(confDirSettings: _*)
    .settings(annetteSettings: _*)
    .settings(dockerSettings: _*)
    .dependsOn(
      `core`,
      `application-api`,
      `authorization-api`,
      `org-structure-api`,
      `persons-api`
    )

lazy val `application-api` = (project in file("annette/application-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      lagomScaladslTestKit
    ) ++ Dependencies.tests
  )
  .settings(annetteSettings: _*)
  .dependsOn(`core`)

def applicationProject(pr: Project) =
  pr
    .enablePlugins(LagomScala)
    .settings(
      libraryDependencies ++= Seq(
        lagomScaladslPersistenceCassandra,
        lagomScaladslKafkaBroker,
        lagomScaladslTestKit,
        Dependencies.macwire,
        Dependencies.chimney
      ) ++ Dependencies.tests
    )
    .settings(lagomForkedTestSettings: _*)
    .settings(confDirSettings: _*)
    .settings(annetteSettings: _*)
    .settings(dockerSettings: _*)
    .dependsOn(`application-api`)

lazy val `attributes-api` = (project in file("annette/attributes-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      lagomScaladslTestKit,
      Dependencies.chimney
    ) ++ Dependencies.tests
  )
  .settings(annetteSettings: _*)
  .dependsOn(`core`)

def attributesProject(pr: Project) =
  pr
    .enablePlugins(LagomScala)
    .settings(
      libraryDependencies ++= Seq(
        lagomScaladslPersistenceCassandra,
        lagomScaladslKafkaBroker,
        lagomScaladslTestKit,
        Dependencies.macwire,
        Dependencies.chimney
      ) ++ Dependencies.tests
    )
    .settings(lagomForkedTestSettings: _*)
    .settings(confDirSettings: _*)
    .settings(annetteSettings: _*)
    .settings(dockerSettings: _*)
    .dependsOn(`attributes-api`)

lazy val `authorization-api` = (project in file("annette/authorization-api"))
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
    .enablePlugins(LagomScala)
    .settings(
      libraryDependencies ++= Seq(
        lagomScaladslPersistenceCassandra,
        lagomScaladslKafkaBroker,
        lagomScaladslTestKit,
        Dependencies.macwire,
        Dependencies.chimney
      ) ++ Dependencies.tests
    )
    .settings(lagomForkedTestSettings: _*)
    .settings(confDirSettings: _*)
    .settings(annetteSettings: _*)
    .settings(dockerSettings: _*)
    .dependsOn(`authorization-api`)

lazy val `org-structure-api` = (project in file("annette/org-structure-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      Dependencies.chimney
    )
  )
  .settings(annetteSettings: _*)
  .dependsOn(`core`, `attributes-api`)

def orgStructureProject(pr: Project) =
  pr
    .enablePlugins(LagomScala)
    .settings(
      libraryDependencies ++= Seq(
        lagomScaladslPersistenceCassandra,
        lagomScaladslKafkaBroker,
        lagomScaladslTestKit,
        Dependencies.macwire,
        Dependencies.chimney
      ) ++ Dependencies.tests
    )
    .settings(lagomForkedTestSettings: _*)
    .settings(confDirSettings: _*)
    .settings(annetteSettings: _*)
    .settings(dockerSettings: _*)
    .dependsOn(`org-structure-api`, `attributes-api`)

lazy val `persons-api` = (project in file("annette/persons-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      Dependencies.chimney
    )
  )
  .settings(annetteSettings: _*)
  .dependsOn(`core`, `attributes-api`)

def personsProject(pr: Project) =
  pr
    .enablePlugins(LagomScala)
    .settings(
      libraryDependencies ++= Seq(
        lagomScaladslPersistenceCassandra,
        lagomScaladslKafkaClient,
        lagomScaladslTestKit,
        Dependencies.macwire,
        Dependencies.chimney
      ) ++ Dependencies.tests
    )
    .settings(lagomForkedTestSettings: _*)
    .settings(confDirSettings: _*)
    .settings(annetteSettings: _*)
    .settings(dockerSettings: _*)
    .dependsOn(`persons-api`, `attributes-api`)

lazy val `init-app`      = initAppProject(project in file("annette/init-app"))
lazy val `application`   = applicationProject(project in file("annette/application"))
lazy val `attributes`    = attributesProject(project in file("annette/attributes"))
lazy val `authorization` = authorizationProject(project in file("annette/authorization"))
lazy val `org-structure` = orgStructureProject(project in file("annette/org-structure"))
lazy val `persons`       = personsProject(project in file("annette/persons"))
