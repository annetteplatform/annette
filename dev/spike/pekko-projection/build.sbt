scalaVersion := "2.13.16"

val pekkoVersion = "1.1.3"

libraryDependencies ++= Seq(
  "org.apache.pekko" %% "pekko-actor-typed"          % pekkoVersion,
  "org.apache.pekko" %% "pekko-stream"                % pekkoVersion,
  "org.apache.pekko" %% "pekko-cluster-typed"         % pekkoVersion,
  "org.apache.pekko" %% "pekko-cluster-sharding-typed" % pekkoVersion,
  "org.apache.pekko" %% "pekko-serialization-jackson"  % pekkoVersion,
  "org.apache.pekko" %% "pekko-persistence-typed"     % pekkoVersion,
  "org.apache.pekko" %% "pekko-persistence-cassandra" % "1.1.0",
  "org.apache.pekko" %% "pekko-projection-core"        % "1.1.0",
  "org.apache.pekko" %% "pekko-projection-eventsourced" % "1.1.0",
  "org.apache.pekko" %% "pekko-projection-cassandra"   % "1.1.0",
  "com.typesafe.play" %% "play-json"                  % "2.9.4",
  "org.slf4j" % "slf4j-simple"                        % "2.0.9"
)

Test / fork := true
fork := true
javaOptions ++= Seq("-Xmx1g")
