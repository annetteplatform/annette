package biz.lobachev.annette.persons.impl

import com.lightbend.lagom.scaladsl.persistence.EventStreamElement
import slick.dbio.{DBIOAction, NoStream}

import scala.concurrent.Future

trait PgEventHandler {

  def globalPrepare(func: () => Future[_]): DBIOAction[Any, _, _] =
    DBIOAction.from(func())

  def handle[T](
    eventHandler: T => Future[_]
  ): EventStreamElement[T] => DBIOAction[Any, NoStream, Nothing] =
    (element: EventStreamElement[T]) => DBIOAction.from(eventHandler(element.event))
}
