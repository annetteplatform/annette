package biz.lobachev.annette.ignition.core.authorization

import akka.Done
import akka.stream.Materializer
import akka.stream.scaladsl.{RestartSource, Sink, Source}
import biz.lobachev.annette.authorization.api.AuthorizationService
import biz.lobachev.annette.authorization.api.role.CreateRolePayload
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.ignition.core.model.{BatchLoadResult, EntityLoadResult, LoadFailed, LoadOk}
import io.scalaland.chimney.dsl.TransformerOps
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.Json

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.io.{Source => IOSource}
import scala.util.{Failure, Success, Try}

class RoleLoader(
  authorizationService: AuthorizationService,
  implicit val materializer: Materializer,
  implicit val executionContext: ExecutionContext
) {

  protected val log: Logger = LoggerFactory.getLogger(this.getClass)

  val name = "AuthRole"

  def load(batches: Seq[String], principal: AnnettePrincipal): Future[EntityLoadResult] =
    Source(batches)
      .mapAsync(1) { batch =>
        loadBatch(batch, principal)
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

  def loadBatch(batch: String, principal: AnnettePrincipal): Future[BatchLoadResult] =
    getData(batch) match {
      case Right(data) =>
        Source(data)
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
          .runWith(
            Sink.fold(BatchLoadResult(batch, LoadOk, Some(0))) {
              case (acc, Right(Done))                                    => acc.copy(quantity = acc.quantity.map(_ + 1))
              case (acc @ BatchLoadResult(_, LoadOk, _), Left(th))       => acc.copy(status = LoadFailed(th.getMessage))
              case (acc @ BatchLoadResult(_, LoadFailed(_), _), Left(_)) => acc
            }
          )
      case Left(th)    =>
        Future.successful(BatchLoadResult(batch, LoadFailed(th.getMessage), Some(0)))
    }

  def getData(filename: String): Either[Throwable, Seq[AuthRoleData]] = {
    val jsonTry = Try(IOSource.fromResource(filename).mkString)
    jsonTry match {
      case Success(json) =>
        val resTry = Try(Json.parse(json).as[Seq[AuthRoleData]])
        resTry match {
          case Success(seq) => Right(seq)
          case Failure(th)  =>
            val message = s"Parsing auth role json failed: $filename"
            log.error(message, th)
            Left(new IllegalArgumentException(message, th))
        }
      case Failure(th)   =>
        val message = s"Auth role file load failed: $filename"
        log.error(message, th)
        Left(new IllegalArgumentException(message, th))

    }
  }

  def loadItem(
    item: AuthRoleData,
    principal: AnnettePrincipal
  ): Future[Either[Throwable, Done.type]] = {
    val payload = item
      .into[CreateRolePayload]
      .withFieldConst(_.permissions, item.permissions.map(_.toPermission))
      .withFieldConst(_.createdBy, principal)
      .transform

    authorizationService
      .createOrUpdateRole(payload)
      .map { _ =>
        log.debug(
          "Auth role loaded: {} - {}",
          item.id,
          item.name
        )
        Right(Done)
      }
      .recoverWith {
        case th: IllegalStateException => Future.failed(th)
        case th                        =>
          log.error("Load person category {} failed", item.id, th)
          Future.successful(
            Left(th)
          )
      }

  }

}
