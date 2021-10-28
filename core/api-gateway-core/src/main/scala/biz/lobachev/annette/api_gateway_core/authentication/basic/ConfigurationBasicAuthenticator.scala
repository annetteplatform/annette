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

import biz.lobachev.annette.api_gateway_core.authentication.{AuthenticationFailedException, BasicAuthenticator, Subject}

import java.util.Base64
import biz.lobachev.annette.core.exception.AnnetteException
import org.slf4j.LoggerFactory
import play.api.mvc.Headers

import scala.concurrent.{ExecutionContext, Future}

class ConfigurationBasicAuthenticator(basicAuthConfigOpt: Option[BasicAuthConfig])
    extends BasicAuthenticator
    with BasicAuthSubjectBuilder {

  val log = LoggerFactory.getLogger(this.getClass)

  override def authenticate(headers: Headers, token: String)(implicit
    ec: ExecutionContext
  ): Future[Subject] =
    basicAuthConfigOpt.map { basicAuthConfig =>
      Future {
        val splited = new String(Base64.getDecoder.decode(token)).split(":")
        val key     = splited(0)
        val secret  = splited(1)
        basicAuthConfig.accounts
          .get(key)
          .map { basicAuthAccount =>
            if (basicAuthAccount.secret == secret)
              buildSubject(headers, basicAuthAccount)
            else
              throw AuthenticationFailedException()
          }
          .getOrElse(throw AuthenticationFailedException())

      }.recover {
        case aex: AnnetteException => throw aex
        case th                    =>
          log.error("Authentication exception: ", th)
          throw AuthenticationFailedException()
      }
    }.getOrElse {
      log.error("Authentication failed. Basic Auth config failure")
      Future.failed(AuthenticationFailedException())
    }

}
