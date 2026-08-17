/*
 * Copyright 2013 Valery Lobachev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package biz.lobachev.annette.microservice_core.pekko.db

import io.getquill.{CassandraAsyncContext, CassandraContextConfig, SnakeCase}

/**
 * Replaces the Lagom-variant `microservice_core.db.CassandraQuillDao`.
 *
 * The Lagom variant constructs a `CassandraLagomAsyncContext[SnakeCase.type]` from a Lagom
 * `CassandraSession`. That context type lives in `quill-cassandra-lagom` and is tightly
 * coupled to Lagom. This Pekko variant uses Quill's plain `CassandraAsyncContext` from
 * `quill-cassandra`, which is constructed from a `CassandraContextConfig` (loaded from HOCON).
 *
 * Quill 3.10's config-driven `ClusterBuilder` maps HOCON keys onto `Cluster.Builder` methods
 * via reflection and cannot express contact points with ports (`host:port`) or auth
 * credentials (no multi-argument method mapping) — it throws `Invalid config key` on both.
 * This trait therefore builds the driver-3 `Cluster` itself from a small, deterministic
 * config surface (first verified at runtime in slice 011):
 *
 * {{{
 * cassandra-quill {
 *   session {
 *     contact-points = ["localhost:9042"]        # host[:port]; port defaults to 9042
 *     credentials {                              # optional; PlainText auth
 *       username = "cassandra"
 *       password = "cassandra"
 *     }
 *   }
 *   keyspace = ${?KEYSPACE_PREFIX}cms
 *   keyspace-autocreate = true                   # optional (dev); SimpleStrategy
 *   replication-factor = 1                       # optional; used by autocreate
 * }
 * }}}
 *
 * Quill 3.10 uses DataStax driver 3 (`com.datastax.driver.core.Session`/`Cluster`).
 * Pekko Persistence Cassandra uses driver 4 (`com.datastax.oss.driver.api.core.CqlSession`).
 * The two drivers coexist on the classpath (different Java packages); services obtain a
 * separate driver-3 Cluster for Quill and a driver-4 CqlSession for Pekko Persistence.
 */
trait CassandraQuillDao extends QuillEncoders {

  /**
   * HOCON path that configures the Quill Cassandra context. Service slices typically
   * return `CassandraContextConfig(config.getConfig("cassandra-quill"))` where
   * `cassandra-quill` is a HOCON block carrying `session.contact-points`,
   * `session.credentials` and `keyspace` (see the example above).
   */
  protected def cassandraContextConfig: CassandraContextConfig

  lazy val ctx: CassandraAsyncContext[SnakeCase.type] = {
    val cfg     = cassandraContextConfig
    val session = cfg.config.getConfig("session")
    val builder = {
      val b = com.datastax.driver.core.Cluster.builder()
      import scala.jdk.CollectionConverters._
      val contactPoints = session.getStringList("contact-points").asScala
        .map { entry =>
          val parts = entry.split(":")
          new java.net.InetSocketAddress(parts(0), if (parts.length > 1) parts(1).toInt else 9042)
        }
      b.addContactPointsWithPorts(contactPoints.asJavaCollection)
      if (session.hasPath("credentials")) {
        val credentials = session.getConfig("credentials")
        b.withCredentials(credentials.getString("username"), credentials.getString("password"))
      }
      b
    }
    val cluster = builder.build()
    // CassandraAsyncContext connects to the keyspace eagerly; create it first when asked
    // (mirrors pekko-persistence-cassandra's keyspace-autocreate).
    if (cfg.config.hasPath("keyspace-autocreate") && cfg.config.getBoolean("keyspace-autocreate")) {
      val replicationFactor =
        if (cfg.config.hasPath("replication-factor")) cfg.config.getInt("replication-factor") else 1
      val bootstrap = cluster.connect()
      bootstrap.execute(
        s"CREATE KEYSPACE IF NOT EXISTS ${cfg.keyspace} " +
          s"WITH replication = {'class':'SimpleStrategy','replication_factor':$replicationFactor}"
      )
      bootstrap.close()
    }
    new CassandraAsyncContext(SnakeCase, cluster, cfg.keyspace, cfg.preparedStatementCacheSize)
  }

  @annotation.nowarn
  def touch(obj: Any): Unit = ()

}
