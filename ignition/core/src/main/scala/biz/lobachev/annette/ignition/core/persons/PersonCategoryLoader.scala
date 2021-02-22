package biz.lobachev.annette.ignition.core.persons

import akka.actor.ActorSystem
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.ignition.core.SequentialProcess
import biz.lobachev.annette.ignition.core.model.{EntityLoadResult, LoadFailed, LoadOk}
import biz.lobachev.annette.persons.api.PersonService
import biz.lobachev.annette.persons.api.category.CreateCategoryPayload
import org.slf4j.{Logger, LoggerFactory}
import io.scalaland.chimney.dsl._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}

class PersonCategoryLoader(
  personService: PersonService,
  actorSystem: ActorSystem,
  implicit val executionContext: ExecutionContext
) extends SequentialProcess {

  protected val log: Logger = LoggerFactory.getLogger(this.getClass)

  val name = "Category"

  def load(categories: Seq[PersonCategoryConfig], principal: AnnettePrincipal): Future[EntityLoadResult] =
    load(categories, principal, Promise(), 100)

  private def load(
    categories: Seq[PersonCategoryConfig],
    principal: AnnettePrincipal,
    promise: Promise[EntityLoadResult],
    iteration: Int
  ): Future[EntityLoadResult] = {

    val future = for {
      res <- seqProcess(categories) { category =>
               loadCategory(category, principal)
             }
    } yield res.length

    future.foreach { quantity =>
      promise.success(EntityLoadResult(name, LoadOk, quantity, Seq.empty))
    }

    future.failed.foreach {
      case th: IllegalStateException =>
        log.warn(
          "Failed to load person categories. Retrying after delay. Failure reason: {}",
          th.getMessage
        )
        if (iteration > 0)
          actorSystem.scheduler.scheduleOnce(10.seconds)({
            load(categories, principal, promise, iteration - 1)
            ()
          })
        else
          closeFailed(promise, th)
      case th                        =>
        closeFailed(promise, th)
    }

    promise.future
  }

  private def loadCategory(category: PersonCategoryConfig, principal: AnnettePrincipal): Future[Unit] = {
    val payload = category
      .into[CreateCategoryPayload]
      .withFieldConst(_.createdBy, principal)
      .transform
    personService
      .createOrUpdateCategory(payload)
      .map { _ =>
        log.debug("Person category loaded: {}", category.id)
        ()
      }
      .recoverWith {
        case th: IllegalStateException => Future.failed(th)
        case th                        =>
          log.error("Load person category {} failed", category.id, th)
          Future.failed(
            new RuntimeException(s"Load id=${category.id} failed with message: ${th.getMessage}")
          )
      }
  }

  private def closeFailed(promise: Promise[EntityLoadResult], th: Throwable) = {
    log.error("Failed to load person categories", th)
    promise.success(EntityLoadResult(name, LoadFailed(th.getMessage), 0, Seq.empty))
  }
}
