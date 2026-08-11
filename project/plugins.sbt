// The scala-xml eviction conflict is in the sbt meta-build (Scala 2.12); the build.sbt
// scheme setting only applies to the main build. This must live in project/.
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % "always"

// The Lagom plugin (Akka-based Play 2.8.x transitively).
addSbtPlugin("com.lightbend.lagom" % "lagom-sbt-plugin" % "1.6.7")

// BLOCKER (slice 002 step 4): Play 3.0.11 (Pekko-based) and Lagom 1.6.7 (Play 2.8.x-based)
// CANNOT coexist in the same sbt meta-build. The Lagom sbt plugin references
// play.sbt.PlaySettings.manageClasspath(Configuration), which Play 3.0.x removed/renamed;
// at project-load time this surfaces as:
//   java.lang.NoSuchMethodError:
//     'sbt.internal.util.Init$Setting play.sbt.PlaySettings$.manageClasspath(...)'
//
// Resolution: each project picks ONE. The api-gateway stays on Lagom + Play 2.8.x
// until slice 013 removes Lagom. Slices 004-012 introduce standalone Pekko+Play 3.0
// services (each in its own sbt project, no Lagom). When the last Lagom dependency
// is gone (slice 013), the Play 3.0.11 plugin can be safely added here.
//
// The `org.playframework` % `sbt-plugin` % `3.0.11` line below is intentionally
// absent until slice 013. Pekko gRPC alone (added below) works fine alongside Lagom.

// Pekko gRPC codegen — version locked by dev/migration/001-decisions.md §C.
// Applied additively; .proto files arrive in slices 004-012.
addSbtPlugin("org.apache.pekko" % "pekko-grpc-sbt-plugin" % "1.1.0")

// Copyright headers
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.6.0")

// Universal/Docker packager
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.7")

// Compiler options
addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat" % "0.4.1")

addDependencyTreePlugin
