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

package biz.lobachev.annette.ignition.core.authorization_old

import akka.Done
import org.slf4j.{Logger, LoggerFactory}
import pureconfig._
import pureconfig.generic.auto._

import scala.concurrent.{ExecutionContext, Future}

class InitAuthorization(
  authRoleLoader: AuthRoleLoader,
  implicit val executionContext: ExecutionContext
) {
  final private val log: Logger = LoggerFactory.getLogger(this.getClass)

  def run(): Future[Done] =
    ConfigSource.default
      .at("annette.init.authorization")
      .load[InitAuthorizationConfig]
      .fold(
        failure => {
          val message = "Init authorization config load error"
          log.error(message, failure.prettyPrint())
          Future.failed(new RuntimeException(message))
        },
        config =>
          if (config.enable) authRoleLoader.load(config.roles, config.assignments, config.createdBy)
          else Future.successful(Done)
      )
}
