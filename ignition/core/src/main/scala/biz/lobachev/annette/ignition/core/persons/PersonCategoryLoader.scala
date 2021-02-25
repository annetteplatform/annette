package biz.lobachev.annette.ignition.core.persons

import akka.Done
import biz.lobachev.annette.ignition.core.EntityLoader
import akka.stream.Materializer
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.persons.api.PersonService
import biz.lobachev.annette.persons.api.category.CreateCategoryPayload
import io.scalaland.chimney.dsl._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class PersonCategoryLoader(
  personService: PersonService,
  implicit val materializer: Materializer,
  implicit val executionContext: ExecutionContext
) extends EntityLoader[PersonCategoryData] {

  protected val log: Logger = LoggerFactory.getLogger(this.getClass)

  val name = "Category"

  override def loadItem(
    item: PersonCategoryData,
    principal: AnnettePrincipal
  ): Future[Either[Throwable, Done.type]] = {
    val payload = item
      .into[CreateCategoryPayload]
      .withFieldConst(_.createdBy, principal)
      .transform

    personService
      .createOrUpdateCategory(payload)
      .map { _ =>
        log.debug(s"$name loaded: {}", item.id)
        Right(Done)
      }
      .recoverWith {
        case th: IllegalStateException => Future.failed(th)
        case th                        =>
          log.error(s"Load $name {} failed", item.id, th)
          Future.successful(
            Left(th)
          )
      }
  }

}
