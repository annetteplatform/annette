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

package biz.lobachev.annette.api_gateway_core.authentication

import biz.lobachev.annette.core.model.auth.AuthenticatedPrincipal

import javax.inject.{Inject, Singleton}
import play.api.mvc.Request

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DefaultAuthenticator @Inject() (
  bearerAuthenticator: BearerAuthenticator,
  basicAuthenticator: BasicAuthenticator,
  val ec: ExecutionContext
) {

  def authenticate[A](request: Request[A])(implicit ec: ExecutionContext): Future[Subject] =
    request.headers
      .get("Authorization")
      .map { authorizationHeader =>
        val splitted = authorizationHeader.split(" ")
        if (splitted.length == 2) {
          val authType = splitted(0).toLowerCase
          val token    = splitted(1)
          for {
            subject <- authType match {
                         case "bearer" => bearerAuthenticator.authenticate(request.headers, token)
                         case "basic"  => basicAuthenticator.authenticate(request.headers, token)
                         case _        => Future.failed(InvalidAuthorizationHeaderException())
                       }
          } yield subject.copy(principals = subject.principals :+ AuthenticatedPrincipal())
        } else
          Future.failed(InvalidAuthorizationHeaderException())
      }
      .getOrElse(Future.failed(InvalidAuthorizationHeaderException()))

}
