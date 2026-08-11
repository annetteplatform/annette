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
 * Service slices 004-012 supply the config under a known prefix; the typical pattern is
 * to reuse the existing `cassandra.default` HOCON block (which has `contact-points`,
 * `keyspace`, `authentication`). See `dev/migration/003-core-recipe.md` §D.
 *
 * Quill 3.10.0 uses DataStax driver 3 (`com.datastax.driver.core.Session`/`Cluster`).
 * Pekko Persistence Cassandra uses driver 4 (`com.datastax.oss.driver.api.core.CqlSession`).
 * The two drivers coexist on the classpath (different Java packages); services obtain a
 * separate driver-3 Cluster for Quill and a driver-4 CqlSession for Pekko Persistence.
 */
trait CassandraQuillDao extends QuillEncoders {

  /**
   * HOCON path that configures the Quill Cassandra context. Service slices typically
   * return `CassandraContextConfig.fromPrefix("cassandra-quill")` (or similar) where
   * `cassandra-quill` is a HOCON block carrying `keyspace`, `contact-points`,
   * `authentication`, etc.
   */
  protected def cassandraContextConfig: CassandraContextConfig

  lazy val ctx: CassandraAsyncContext[SnakeCase.type] =
    new CassandraAsyncContext(SnakeCase, cassandraContextConfig)

  @annotation.nowarn
  def touch(obj: Any): Unit = ()

}
