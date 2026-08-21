// The scala-xml eviction conflict is in the sbt meta-build (Scala 2.12); the build.sbt
// scheme setting only applies to the main build. This must live in project/.
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % "always"

// Play 3.0.11 (Pekko-based; coordinates under org.playframework). Added in slice 013
// when the last Lagom dependency was removed — Lagom 1.6.7's sbt plugin (Play 2.8-based)
// could not coexist with Play 3.x in the same sbt meta-build (NoSuchMethodError on
// play.sbt.PlaySettings.manageClasspath). See dev/migration/013.
addSbtPlugin("org.playframework" % "sbt-plugin" % "3.0.11")

// Pekko gRPC codegen — version locked by dev/migration/001-decisions.md §C.
addSbtPlugin("org.apache.pekko" % "pekko-grpc-sbt-plugin" % "1.1.0")

// Copyright Headers
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.6.0")

// Universal/Docker packager
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.7")

// Compiler options
addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat" % "0.4.1")

addDependencyTreePlugin
