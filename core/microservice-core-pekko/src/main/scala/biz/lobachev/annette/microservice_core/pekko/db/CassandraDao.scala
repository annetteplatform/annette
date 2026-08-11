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

import com.datastax.driver.core.{BoundStatement, ResultSet, Session}
import com.google.common.util.concurrent.{FutureCallback, Futures, MoreExecutors}
import org.apache.pekko.Done
import org.slf4j.Logger

import scala.concurrent.{ExecutionContext, Future, Promise}

/**
 * Replaces the Lagom-variant `microservice_core.db.CassandraDao`. Same method names so
 * service-side DAOs have a 1:1 migration: just change the import from
 * `microservice_core.db.CassandraDao` to `microservice_core.pekko.db.CassandraDao`.
 *
 * Key difference: the Lagom variant uses `Lagom's CassandraSession` (a wrapper that exposes
 * `selectWrite`, `selectAll`, etc.). This Pekko variant uses DataStax driver 3's
 * `com.datastax.driver.core.Session` directly — the same driver Quill 3.10 uses. Service
 * slices 004-012 obtain a driver-3 Session by connecting a `Cluster` (see
 * `dev/migration/003-core-recipe.md` §C for the wiring pattern).
 *
 * Driver-3 (`com.datastax.driver.core.*`) coexists on the classpath with Pekko Persistence
 * Cassandra's driver-4 (`com.datastax.oss.driver.api.core.*`); the package names do not
 * collide.
 */
trait CassandraDao {

  val session: Session
  implicit val ec: ExecutionContext

  protected val log: Logger

  /**
   * Execute the given statements in sequence. Returns `Done` when all complete. Failures
   * are logged but the returned Future fails on the first error (matching the Lagom variant's
   * `Sink.seq` semantics).
   *
   * Implementation note: DataStax driver 3's `ResultSetFuture` is a Guava `ListenableFuture`,
   * not a `java.util.concurrent.CompletionStage`, so `scala.compat.java8.FutureConverters`
   * cannot be used. We bridge via Guava's `Futures.addCallback` (Guava is on the classpath
   * transitively via driver 3).
   */
  protected def execute(statements: BoundStatement*): Future[Done] =
    Future
      .sequence(statements.map { statement =>
        val promise = Promise[Done]()
        Futures.addCallback(
          session.executeAsync(statement),
          new FutureCallback[ResultSet] {
            def onSuccess(result: ResultSet): Unit = promise.success(Done)
            def onFailure(t: Throwable): Unit = {
              log.error("Failed to process statement {}", statement, t)
              promise.failure(t)
            }
          },
          MoreExecutors.directExecutor()
        )
        promise.future
      })
      .map(_ => Done)

}
