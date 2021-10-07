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

package biz.lobachev.annette.microservice_core.db

import akka.Done
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import com.datastax.driver.core.BoundStatement
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import org.slf4j.Logger

import scala.concurrent.{ExecutionContext, Future}

trait CassandraDao {

  val session: CassandraSession
  implicit val ec: ExecutionContext
  implicit val materializer: Materializer

  protected val log: Logger

  protected def execute(statements: BoundStatement*): Future[Done] =
    for (
      _ <- Source(statements)
             .mapAsync(1) { statement =>
               val future = session.executeWrite(statement)
               future.failed.foreach(th => log.error("Failed to process statement {}", statement, th))
               future
             }
             .runWith(Sink.seq)
    ) yield Done

}
