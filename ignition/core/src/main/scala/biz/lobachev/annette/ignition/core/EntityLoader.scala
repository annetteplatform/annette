package biz.lobachev.annette.ignition.core

import akka.stream.scaladsl.RestartSource
import akka.{Done, NotUsed}
import biz.lobachev.annette.ignition.core.model.BatchLoadResult
//import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.ignition.core.model.{EntityLoadResult, LoadFailed, LoadOk}
import org.slf4j.Logger

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

protected trait EntityLoader[A] {

  protected implicit val materializer: Materializer
  protected implicit val executionContext: ExecutionContext

  protected val log: Logger

  val name: String

  def loadEntity(items: Seq[A], principal: AnnettePrincipal): Future[EntityLoadResult] =
    loadItems(items, principal)
      .runWith(
        Sink.fold(EntityLoadResult(name, LoadOk, 0, Seq.empty)) {
          case (acc, Right(Done))                                        => acc.copy(quantity = acc.quantity + 1)
          case (acc @ EntityLoadResult(_, LoadOk, _, _), Left(th))       => acc.copy(status = LoadFailed(th.getMessage))
          case (acc @ EntityLoadResult(_, LoadFailed(_), _, _), Left(_)) => acc
        }
      )

  def loadBatch(batchFilename: String, items: Seq[A], principal: AnnettePrincipal): Future[BatchLoadResult] =
    loadItems(items, principal)
      .runWith(
        Sink.fold(BatchLoadResult(batchFilename, LoadOk, Some(0))) {
          case (acc, Right(Done))                                    => acc.copy(quantity = acc.quantity.map(_ + 1))
          case (acc @ BatchLoadResult(_, LoadOk, _), Left(th))       => acc.copy(status = LoadFailed(th.getMessage))
          case (acc @ BatchLoadResult(_, LoadFailed(_), _), Left(_)) => acc
        }
      )

  private def loadItems(items: Seq[A], principal: AnnettePrincipal): Source[Either[Throwable, Done.type], NotUsed] =
    Source(items)
      .mapAsync(1) { item =>
        RestartSource
          .onFailuresWithBackoff(
            minBackoff = 3.seconds,
            maxBackoff = 20.seconds,
            randomFactor = 0.2,
            maxRestarts = 20
          ) { () =>
            Source.future(
              loadItem(item, principal)
            )
          }
          .runWith(Sink.last)
      }

  protected def loadItem(
    item: A,
    principal: AnnettePrincipal
  ): Future[Either[Throwable, Done.type]]

}
