package biz.lobachev.annette.ignition.core

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.ignition.core.model.{EntityLoadResult, LoadFailed, LoadOk, ServiceLoadResult}
import org.slf4j.Logger
import pureconfig.{ConfigReader, ConfigSource, Derivation}

import scala.concurrent.{ExecutionContext, Future}

protected trait ServiceLoader[A] {

  protected implicit val executionContext: ExecutionContext
  protected val log: Logger

  val name: String
  val configName: String

  def run(principal: AnnettePrincipal)(implicit reader: Derivation[ConfigReader[A]]): Future[ServiceLoadResult] =
    ConfigSource.default
      .at(s"annette.ignition.$configName")
      .load[A]
      .fold(
        failure => {
          val message = s"$name ignition config load error"
          log.error(message, failure.prettyPrint())
          Future.successful(ServiceLoadResult(name, LoadFailed(message), Seq.empty))
        },
        config =>
          for {
            results <- run(config, principal)
          } yield
            if (results.exists(_.status != LoadOk))
              ServiceLoadResult(name, LoadFailed(""), results)
            else
              ServiceLoadResult(name, LoadOk, results)
      )

  protected def run(config: A, principal: AnnettePrincipal): Future[Seq[EntityLoadResult]]
}
