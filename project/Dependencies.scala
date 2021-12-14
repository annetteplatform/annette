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
    val chimney                 = "0.6.1"
    val pureConfig              = "0.17.1"
    val quill                   = "3.10.0"
    val alpakkaS3               = "3.0.4"
//    val akkaHttp                = "10.2.0"
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

  val alpakkaS3: Seq[ModuleID] = Seq(
    "com.lightbend.akka" %% "akka-stream-alpakka-s3" % Version.alpakkaS3,
    "com.typesafe.akka"  %% "akka-stream"            % LagomVersion.akka,
    "com.typesafe.akka"  %% "akka-http"              % LagomVersion.akkaHttp,
    "com.typesafe.akka"  %% "akka-http-xml"          % LagomVersion.akkaHttp
  )

}
