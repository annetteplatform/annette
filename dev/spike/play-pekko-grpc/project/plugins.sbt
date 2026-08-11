// Play 3.0.x is built on Pekko / Pekko HTTP (vs Play 2.9.x which is Akka-based).
// Note: Play 3.x moved from `com.typesafe.play` to `org.playframework`.
// https://www.playframework.com/documentation/3.0.x/Highlights30
addSbtPlugin("org.playframework" % "sbt-plugin" % "3.0.11")

// Pekko gRPC sbt plugin — generates Scala service traits + client stubs from .proto.
addSbtPlugin("org.apache.pekko" % "pekko-grpc-sbt-plugin" % "1.1.0")
