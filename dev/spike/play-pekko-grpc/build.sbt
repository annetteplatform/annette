scalaVersion := "2.13.18"

val pekkoVersion = "1.1.1"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, PekkoGrpcPlugin)
  .settings(
    name := "play-pekko-grpc-spike",
    libraryDependencies ++= Seq(
      guice,
      "org.apache.pekko" %% "pekko-actor-typed"          % pekkoVersion,
      "org.apache.pekko" %% "pekko-stream"                % pekkoVersion,
      "org.apache.pekko" %% "pekko-slf4j"                 % pekkoVersion,
      "org.apache.pekko" %% "pekko-serialization-jackson" % pekkoVersion,
      "org.apache.pekko" %% "pekko-grpc-runtime"          % "1.1.0"
    ),
    fork := true,
    javaOptions ++= Seq("-Xmx1g")
  )

// Play 3.x uses Pekko, not Akka. The plugin auto-configures the Pekko HTTP server.
