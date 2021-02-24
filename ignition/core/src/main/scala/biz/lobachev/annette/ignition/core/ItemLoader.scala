package biz.lobachev.annette.ignition.core

import akka.Done
import akka.stream.scaladsl.RestartSource
//import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.ignition.core.model.{EntityLoadResult, LoadFailed, LoadOk}
import org.slf4j.Logger

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

trait ItemLoader[A] {

  implicit val materializer: Materializer
  implicit val executionContext: ExecutionContext

  protected val log: Logger

  val name: String

  def load(items: Seq[A], principal: AnnettePrincipal): Future[EntityLoadResult] =
    Source(items)
      .mapAsync(1) { category =>
        RestartSource
          .onFailuresWithBackoff(
            minBackoff = 3.seconds,
            maxBackoff = 20.seconds,
            randomFactor = 0.2,
            maxRestarts = 20
          ) { () =>
            Source.future(
              loadItem(category, principal)
            )
          }
          .runWith(Sink.last)
      }
      .runWith(
        Sink.fold(EntityLoadResult(name, LoadOk, 0, Seq.empty)) {
          case (acc, Right(Done))                                        => acc.copy(quantity = acc.quantity + 1)
          case (acc @ EntityLoadResult(_, LoadOk, _, _), Left(th))       => acc.copy(status = LoadFailed(th.getMessage))
          case (acc @ EntityLoadResult(_, LoadFailed(_), _, _), Left(_)) => acc
        }
      )

  def loadItem(
    item: A,
    principal: AnnettePrincipal
  ): Future[Either[Throwable, Done.type]]

}
