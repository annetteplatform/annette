/*
 * Copyright 2013 Valery Lobachev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package biz.lobachev.annette.ignition.core

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.ignition.core.attributes.AttributeServiceLoader
import biz.lobachev.annette.ignition.core.authorization.AuthorizationServiceLoader
import biz.lobachev.annette.ignition.core.model.ServiceLoadResult
import biz.lobachev.annette.ignition.core.org_structure.OrgStructureServiceLoader
import biz.lobachev.annette.ignition.core.persons.PersonServiceLoader
import org.slf4j.{Logger, LoggerFactory}
import pureconfig.ConfigSource
import pureconfig.generic.auto._

import scala.concurrent.{ExecutionContext, Future}

class AnnetteIgnition(
  personServiceLoader: PersonServiceLoader,
  orgStructureServiceLoader: OrgStructureServiceLoader,
  authorizationServiceLoader: AuthorizationServiceLoader,
  attributeServiceLoader: AttributeServiceLoader,
  implicit val ec: ExecutionContext
) {

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
      personLoadResult       <- personServiceLoader.run(principal)
      orgStructureLoadResult <- orgStructureServiceLoader.run(principal)
      authLoadResult         <- authorizationServiceLoader.run(principal)
      attributeLoadResult    <- attributeServiceLoader.run(principal)
    } yield {
      log.debug("Annette ignition completed...")
      logResults(personLoadResult :: orgStructureLoadResult :: authLoadResult :: attributeLoadResult :: Nil)
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
