package biz.lobachev.annette.ignition.core.persons

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.ignition.core.ServiceLoader
import biz.lobachev.annette.ignition.core.model.EntityLoadResult
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class PersonServiceLoader(
  personCategoryLoader: PersonCategoryLoader,
  personsLoader: PersonsLoader,
  implicit val executionContext: ExecutionContext
) extends ServiceLoader[PersonIgnitionData] {

  protected val log: Logger = LoggerFactory.getLogger(this.getClass)

  override val name       = "Persons"
  override val configName = "persons"

  override protected def run(config: PersonIgnitionData, principal: AnnettePrincipal): Future[Seq[EntityLoadResult]] =
    for {
      categoryLoadResult <- personCategoryLoader.loadEntity(config.categories, principal)
      personLoadResult   <- personsLoader.loadFromFiles(config.persons, principal)
    } yield Seq(categoryLoadResult, personLoadResult)

}
