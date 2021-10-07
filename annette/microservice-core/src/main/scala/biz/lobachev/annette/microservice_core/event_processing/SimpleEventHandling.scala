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

package biz.lobachev.annette.microservice_core.event_processing

import com.datastax.driver.core.BoundStatement
import com.lightbend.lagom.scaladsl.persistence.EventStreamElement

import scala.concurrent.{ExecutionContext, Future}

trait SimpleEventHandling {
  def handle[T](
    eventHandler: T => Future[_]
  )(implicit ec: ExecutionContext): EventStreamElement[T] => Future[List[BoundStatement]] =
    (element: EventStreamElement[T]) => eventHandler(element.event).map(_ => List.empty[BoundStatement])

  def batchHandle[T](
    eventHandler: T => Future[List[BoundStatement]]
  ): EventStreamElement[T] => Future[List[BoundStatement]] =
    (element: EventStreamElement[T]) => eventHandler(element.event)
}
