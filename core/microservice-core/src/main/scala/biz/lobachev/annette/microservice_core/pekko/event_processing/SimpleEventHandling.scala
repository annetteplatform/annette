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

package biz.lobachev.annette.microservice_core.pekko.event_processing

import com.datastax.driver.core.BoundStatement

import scala.concurrent.{ExecutionContext, Future}
/**
 * Event-handling helpers for Pekko Projection handlers (see slice 003 §B).
 *
 * Handlers take a plain event of type `T` (not a Lagom EventStreamElement);
 * the projection wiring passes `envelope.event` from the
 * `org.apache.pekko.projection.eventsourced.EventEnvelope[T]`.
 */
trait SimpleEventHandling {

  /**
   * Adapt a side-effecting handler (one that returns `Future[_]` and produces no
   * BoundStatements) into the shape Pekko Projection's `Handler` expects. Used by handlers
   * that update OpenSearch indices, fire external side effects, etc.
   */
  def handle[T](
    eventHandler: T => Future[_]
  )(implicit ec: ExecutionContext): T => Future[List[BoundStatement]] =
    (event: T) => eventHandler(event).map(_ => List.empty[BoundStatement])

  /**
   * Adapt a statement-producing handler into the shape Pekko Projection's `Handler` expects.
   * Used by handlers that write to Cassandra.
   */
  def batchHandle[T](
    eventHandler: T => Future[List[BoundStatement]]
  ): T => Future[List[BoundStatement]] =
    (event: T) => eventHandler(event)

}
