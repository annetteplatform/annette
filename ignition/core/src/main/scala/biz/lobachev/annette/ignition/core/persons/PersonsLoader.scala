package biz.lobachev.annette.ignition.core.persons

import akka.Done
import akka.stream.Materializer
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.ignition.core.FileBatchLoader
import biz.lobachev.annette.persons.api.PersonService
import biz.lobachev.annette.persons.api.person.CreatePersonPayload
import io.scalaland.chimney.dsl._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class PersonsLoader(
  personService: PersonService,
  override implicit val materializer: Materializer,
  override implicit val executionContext: ExecutionContext
) extends FileBatchLoader[PersonData] {

  protected val log: Logger = LoggerFactory.getLogger(this.getClass)

  val name = "Person"

  protected override def loadItem(
    item: PersonData,
    principal: AnnettePrincipal
  ): Future[Either[Throwable, Done.type]] = {
    val payload = item
      .into[CreatePersonPayload]
      .withFieldConst(_.createdBy, principal)
      .transform

    personService
      .createOrUpdatePerson(payload)
      .map { _ =>
        log.debug(
          s"$name loaded: {} - {}, {} {}",
          item.id,
          item.lastname,
          item.firstname,
          item.middlename.getOrElse("")
        )
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
