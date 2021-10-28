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
import biz.lobachev.annette.ignition.core.model.{EntityLoadResult, LoadFailed, LoadOk, ServiceLoadResult}
import org.slf4j.Logger
import pureconfig._

import scala.concurrent.{ExecutionContext, Future}

protected trait ServiceLoader[A] {

  protected implicit val executionContext: ExecutionContext
  protected val log: Logger

  val name: String
  val configName: String

  def run(principal: AnnettePrincipal)(implicit reader: ConfigReader[A]): Future[ServiceLoadResult] =
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
