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

package biz.lobachev.annette.api_gateway_core.authentication.basic

import org.slf4j.LoggerFactory
import pureconfig._
import pureconfig.generic.auto._

object BasicAuthConfigProvider {
  val log                            = LoggerFactory.getLogger(this.getClass)
  def get(): Option[BasicAuthConfig] =
    ConfigSource.default
      .at("annette.authentication")
      .load[AuthenticationConfig]
      .fold(
        failure => {
          log.error("Basic Auth config load error {}", failure.prettyPrint())
          None
        },
        config => {
          config.basic.map { basicAuthConfig =>
            val conf =
              basicAuthConfig.copy(accounts = basicAuthConfig.accounts.map(a => a._1 -> a._2.copy(secret = "***")))
            log.info("Basic Auth config load success")
            log.debug("Basic Auth config: {}", conf)
          }.getOrElse(log.warn("Basic Auth config not found"))
          config.basic
        }
      )
}

case class AuthenticationConfig(basic: Option[BasicAuthConfig])
