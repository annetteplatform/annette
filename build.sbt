import com.typesafe.sbt.SbtNativePackager.autoImport.NativePackagerHelper._
import com.typesafe.sbt.packager.docker.DockerChmodType
import play.sbt.routes.RoutesKeys

scalaVersion := "2.13.3"
maintainer := "valery@lobachev.biz"

ThisBuild / version := "0.3.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.3"

ThisBuild / maintainer := "valery@lobachev.biz"

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

// Copyright settings
def annetteSettings: Seq[Setting[_]] =
  Seq(
    organizationName := "Valery Lobachev",
    startYear := Some(2013)
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
    `api-gateway-core`,
    `api-gateway`,
    // initialization application
    `ignition-core`,
//    `demo-ignition`,
    // API gateways
    `application-api-gateway`,
    `authorization-api-gateway`,
    `org-structure-api-gateway`,
    `persons-api-gateway`,
    `cms-api-gateway`,
    `principal-groups-api-gateway`,
    // microservices API
    `application-api`,
    `attributes-api`,
    `authorization-api`,
    `org-structure-api`,
    `persons-api`,
    `principal-groups-api`,
    `subscriptions-api`,
    `cms-api`,
    // microservices
    `application`,
//    `attributes`,
    `authorization`,
    `org-structure`,
    `persons`,
    `principal-groups`,
    `subscriptions`,
    `cms`
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

lazy val `microservice-core` = (project in file("annette/microservice-core"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      lagomScaladslPersistenceCassandra,
      lagomScaladslServer % Optional,
      lagomScaladslTestKit,
      Dependencies.chimney,
      Dependencies.pureConfig,
      Dependencies.playJsonExt,
      Dependencies.logstashEncoder,
      Dependencies.macwire
    ) ++ Dependencies.tests
      ++ Dependencies.elastic
      ++ Dependencies.lagomAkkaDiscovery
  )
  .settings(annetteSettings: _*)
  .dependsOn(
    `core`
  )

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
    `api-gateway-core`,
    `application-api-gateway`,
    `authorization-api-gateway`,
    `org-structure-api-gateway`,
    `persons-api-gateway`,
    `principal-groups-api-gateway`,
    `cms-api-gateway`
  )

lazy val `ignition-core` = (project in file("ignition/core"))
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
  .dependsOn(
    `api-gateway-core`,
    `application-api`,
    `authorization-api`,
    `org-structure-api`,
    `persons-api`,
    `cms-api`,
    `subscriptions-api`
  )

def demoIgnitionProject(pr: Project) =
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
      `ignition-core`
    )

lazy val `application-api` = (project in file("annette/application-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      lagomScaladslTestKit
    ) ++ Dependencies.tests
  )
  .settings(annetteSettings: _*)
  .dependsOn(`microservice-core`)

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
      ) ++ Dependencies.tests ++ Dependencies.lagomAkkaDiscovery
    )
    .settings(lagomForkedTestSettings: _*)
    .settings(confDirSettings: _*)
    .settings(annetteSettings: _*)
    .settings(dockerSettings: _*)
    .dependsOn(`application-api`)

lazy val `application-api-gateway` = (project in file("annette/application-api-gateway"))
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

lazy val `attributes-api` = (project in file("annette/attributes-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      lagomScaladslTestKit,
      Dependencies.chimney
    ) ++ Dependencies.tests
  )
  .settings(annetteSettings: _*)
  .dependsOn(`microservice-core`)

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
      ) ++ Dependencies.tests ++ Dependencies.lagomAkkaDiscovery
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
  .dependsOn(`microservice-core`)

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
      ) ++ Dependencies.tests ++ Dependencies.lagomAkkaDiscovery
    )
    .settings(lagomForkedTestSettings: _*)
    .settings(confDirSettings: _*)
    .settings(annetteSettings: _*)
    .settings(dockerSettings: _*)
    .dependsOn(`authorization-api`)

lazy val `authorization-api-gateway` = (project in file("annette/authorization-api-gateway"))
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

lazy val `org-structure-api` = (project in file("annette/org-structure-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      Dependencies.chimney
    )
  )
  .settings(annetteSettings: _*)
  .dependsOn(`microservice-core`, `attributes-api`)

def orgStructureProject(pr: Project) =
  pr
    .enablePlugins(LagomScala)
    .settings(
      libraryDependencies ++= Seq(
        lagomScaladslPersistenceCassandra,
        lagomScaladslKafkaBroker,
        lagomScaladslTestKit,
        Dependencies.macwire,
        Dependencies.chimney,
        Dependencies.pureConfig
      ) ++ Dependencies.tests ++ Dependencies.lagomAkkaDiscovery
    )
    .settings(lagomForkedTestSettings: _*)
    .settings(confDirSettings: _*)
    .settings(annetteSettings: _*)
    .settings(dockerSettings: _*)
    .dependsOn(`org-structure-api`, `attributes-api`)

lazy val `org-structure-api-gateway` = (project in file("annette/org-structure-api-gateway"))
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

lazy val `persons-api` = (project in file("annette/persons-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      Dependencies.chimney
    )
  )
  .settings(annetteSettings: _*)
  .dependsOn(`microservice-core`)

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
      ) ++ Dependencies.tests ++ Dependencies.lagomAkkaDiscovery
    )
    .settings(lagomForkedTestSettings: _*)
    .settings(confDirSettings: _*)
    .settings(annetteSettings: _*)
    .settings(dockerSettings: _*)
    .dependsOn(`persons-api`)

lazy val `persons-api-gateway` = (project in file("annette/persons-api-gateway"))
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

lazy val `principal-groups-api` = (project in file("annette/principal-groups-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      lagomScaladslTestKit,
      Dependencies.chimney
    ) ++ Dependencies.tests
  )
  .settings(annetteSettings: _*)
  .dependsOn(`microservice-core`)

def principalGroupsProject(pr: Project) =
  pr
    .enablePlugins(LagomScala)
    .settings(
      libraryDependencies ++= Seq(
        lagomScaladslPersistenceCassandra,
        lagomScaladslKafkaClient,
        lagomScaladslTestKit,
        Dependencies.macwire,
        Dependencies.chimney
      ) ++ Dependencies.tests ++ Dependencies.lagomAkkaDiscovery
    )
    .settings(lagomForkedTestSettings: _*)
    .settings(confDirSettings: _*)
    .settings(annetteSettings: _*)
    .settings(dockerSettings: _*)
    .dependsOn(`principal-groups-api`)

lazy val `principal-groups-api-gateway` = (project in file("annette/principal-groups-api-gateway"))
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

lazy val `subscriptions-api` = (project in file("annette/subscriptions-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      lagomScaladslTestKit,
      Dependencies.chimney
    ) ++ Dependencies.tests
  )
  .settings(annetteSettings: _*)
  .dependsOn(`microservice-core`)

def subscriptionsProject(pr: Project) =
  pr
    .enablePlugins(LagomScala)
    .settings(
      libraryDependencies ++= Seq(
        lagomScaladslPersistenceCassandra,
        lagomScaladslKafkaClient,
        lagomScaladslTestKit,
        Dependencies.macwire,
        Dependencies.chimney
      ) ++ Dependencies.tests ++ Dependencies.lagomAkkaDiscovery
    )
    .settings(lagomForkedTestSettings: _*)
    .settings(confDirSettings: _*)
    .settings(annetteSettings: _*)
    .settings(dockerSettings: _*)
    .dependsOn(`subscriptions-api`)

lazy val `cms-api` = (project in file("annette/cms-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      lagomScaladslTestKit,
      Dependencies.chimney
    ) ++ Dependencies.tests
  )
  .settings(annetteSettings: _*)
  .dependsOn(`microservice-core`)

def cmsProject(pr: Project) =
  pr
    .enablePlugins(LagomScala)
    .settings(
      libraryDependencies ++= Seq(
        lagomScaladslPersistenceCassandra,
        lagomScaladslKafkaClient,
        lagomScaladslTestKit,
        Dependencies.macwire,
        Dependencies.chimney
      ) ++ Dependencies.tests ++ Dependencies.lagomAkkaDiscovery
    )
    .settings(lagomForkedTestSettings: _*)
    .settings(confDirSettings: _*)
    .settings(annetteSettings: _*)
    .settings(dockerSettings: _*)
    .dependsOn(`cms-api`)

lazy val `cms-api-gateway` = (project in file("annette/cms-api-gateway"))
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

//lazy val `demo-ignition`    = demoIgnitionProject(project in file("ignition/demo"))
lazy val `application`      = applicationProject(project in file("annette/application"))
//lazy val `attributes`       = attributesProject(project in file("annette/attributes"))
lazy val `authorization`    = authorizationProject(project in file("annette/authorization"))
lazy val `org-structure`    = orgStructureProject(project in file("annette/org-structure"))
lazy val `persons`          = personsProject(project in file("annette/persons"))
lazy val `principal-groups` = principalGroupsProject(project in file("annette/principal-groups"))
lazy val `subscriptions`    = subscriptionsProject(project in file("annette/subscriptions"))
lazy val `cms`              = cmsProject(project in file("annette/cms"))
