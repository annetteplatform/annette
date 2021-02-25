package biz.lobachev.annette.ignition.core

import akka.stream.scaladsl.{Sink, Source}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.ignition.core.model.{BatchLoadResult, EntityLoadResult, LoadFailed, LoadOk}
import play.api.libs.json.Reads

import scala.concurrent.Future

protected trait FileBatchLoader[A] extends EntityLoader[A] with FileSourcing {

  def loadFromFiles(batchFilenames: Seq[String], principal: AnnettePrincipal)(implicit
    reads: Reads[Seq[A]]
  ): Future[EntityLoadResult] =
    Source(batchFilenames)
      .mapAsync(1) { batchFilename =>
        getData[Seq[A]](name, batchFilename) match {
          case Right(data) =>
            loadBatch(batchFilename, data, principal)
          case Left(th)    =>
            Future.successful(BatchLoadResult(batchFilename, LoadFailed(th.getMessage), Some(0)))
        }
      }
      .runWith(
        Sink.fold(EntityLoadResult(name, LoadOk, 0, Seq.empty)) {
          case (acc, res @ BatchLoadResult(_, LoadOk, Some(loaded)))        =>
            acc.copy(
              quantity = acc.quantity + loaded,
              batches = acc.batches :+ res
            )
          case (acc, res @ BatchLoadResult(_, LoadFailed(_), Some(loaded))) =>
            acc.copy(
              status = LoadFailed(""),
              quantity = acc.quantity + loaded,
              batches = acc.batches :+ res
            )
        }
      )

}
