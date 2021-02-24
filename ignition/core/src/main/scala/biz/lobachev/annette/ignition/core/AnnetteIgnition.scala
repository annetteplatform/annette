package biz.lobachev.annette.ignition.core

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.ignition.core.model.ServiceLoadResult
import biz.lobachev.annette.ignition.core.persons.PersonServiceLoader
import org.slf4j.{Logger, LoggerFactory}
import pureconfig.ConfigSource
import pureconfig.generic.auto._

import scala.concurrent.{ExecutionContext, Future}

class AnnetteIgnition(personServiceLoader: PersonServiceLoader, implicit val ec: ExecutionContext) {

  protected val log: Logger = LoggerFactory.getLogger(this.getClass)

  def run(): Future[Unit] =
    ConfigSource.default
      .at("annette.ignition.principal")
      .load[AnnettePrincipal]
      .fold(
        failure => {
          val message = "Ignition config load error"
          log.error(message, failure.prettyPrint())
          Future.failed(new RuntimeException(message))
        },
        principal => run(principal)
      )

  private def run(principal: AnnettePrincipal): Future[Unit] = {
    log.debug("Annette ignition started...")
    (for {
      personLoadResult <- personServiceLoader.run(principal)
    } yield {
      log.debug("Annette ignition completed...")
      logResults(personLoadResult :: Nil)
    }).recover(th => log.error("Annette ignition failed with error: {}", th.getMessage, th))
  }

  def logResults(results: List[ServiceLoadResult]) = {
    println("*************************************************")
    println()
    println()
    println()
    for {
      result <- results
      line   <- result.toStrings()
    } yield println(line)
    println()
    println()
    println()
    println("*************************************************")

    for {
      result <- results
      line   <- result.toStrings()
    } yield log.info(line)
    ()
  }

}
