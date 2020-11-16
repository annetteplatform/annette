import com.lightbend.lagom.core.LagomVersion
import sbt._

object Dependencies {
  object Version {
    val macwire                 = "2.3.7"
    val scalaTest               = "3.2.2"
    val scalaCheck              = "1.14.3"
    val commonsIO               = "2.7"
    val elastic4s               = "7.8.1"
    val playJsonExt             = "0.42.0"
    val jwtPlayJson             = "4.3.0"
    val akkaKubernetesDiscovery = "1.0.8"
    val logstashEncoder         = "6.4"
    val chimney                 = "0.6.1"
    val pureConfig              = "0.14.0"
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

  val jwt: sbt.ModuleID = "com.pauldijou" %% "jwt-play-json" % Version.jwtPlayJson

  val lagomAkkaDiscovery: Seq[sbt.ModuleID] = Seq(
    "com.lightbend.lagom"          %% "lagom-scaladsl-akka-discovery-service-locator" % LagomVersion.current,
    "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api"                 % Version.akkaKubernetesDiscovery
  )

  val logstashEncoder = "net.logstash.logback" % "logstash-logback-encoder" % Version.logstashEncoder

  val pureConfig = "com.github.pureconfig" %% "pureconfig" % Version.pureConfig

  val chimney = "io.scalaland" %% "chimney" % Version.chimney
}
