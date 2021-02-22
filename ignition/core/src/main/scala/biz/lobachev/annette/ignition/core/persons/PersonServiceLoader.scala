package biz.lobachev.annette.ignition.core.persons

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.ignition.core.ServiceLoader
import biz.lobachev.annette.ignition.core.model.{LoadFailed, LoadOk, ServiceLoadResult}
import org.slf4j.{Logger, LoggerFactory}
import pureconfig.ConfigSource
import pureconfig.generic.auto._

import scala.concurrent.{ExecutionContext, Future}

class PersonServiceLoader(
  personCategoryLoader: PersonCategoryLoader,
  implicit val executionContext: ExecutionContext
) extends ServiceLoader {

  protected val log: Logger = LoggerFactory.getLogger(this.getClass)

  val name = "Persons"

  override def run(principal: AnnettePrincipal): Future[ServiceLoadResult] =
    ConfigSource.default
      .at("annette.ignition.persons")
      .load[PersonIgnitionConfig]
      .fold(
        failure => {
          val message = "Person ignition config load error"
          log.error(message, failure.prettyPrint())
          Future.successful(ServiceLoadResult(name, LoadFailed(message), Seq.empty))
        },
        personConfig =>
          for {
            categoryLoadResult <- personCategoryLoader.load(personConfig.categories, principal)
          } yield
            if (categoryLoadResult.status != LoadOk)
              ServiceLoadResult(name, LoadFailed(""), Seq(categoryLoadResult))
            else
              ServiceLoadResult(name, LoadOk, Seq(categoryLoadResult))
      )

}
