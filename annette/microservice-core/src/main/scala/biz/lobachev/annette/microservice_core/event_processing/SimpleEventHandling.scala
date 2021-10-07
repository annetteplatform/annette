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
