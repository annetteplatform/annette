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

import biz.lobachev.annette.core.exception.AnnetteException
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.Json
import play.api.mvc._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class CookieAuthenticatedAction @Inject() (
  authenticator: DefaultAuthenticator,
  subjectTransformer: SubjectTransformer,
  val parser: BodyParsers.Default,
  implicit val executionContext: ExecutionContext
) extends ActionBuilder[AuthenticatedRequest, AnyContent] {
  final private val log: Logger = LoggerFactory.getLogger(this.getClass)

  def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    log.info("Request method={}, uri={}", request.method, request.uri, request.connection.remoteAddressString)
    val expirationTime = request.session.data.get("exp").flatMap(_.toLongOption)
    val principal      = request.session.data.get("principal").map(AnnettePrincipal.fromCode(_))
    val maybeSubject   =
      if (principal.isDefined && expirationTime.isDefined && System.currentTimeMillis() / 1000L < expirationTime.get)
        Some(Subject(principals = principal.toSeq, Map.empty, None))
      else None
    val subjectFuture  = for {
      subject            <- maybeSubject.map(Future.successful(_)).getOrElse(authenticator.authenticate(request))
      transformedSubject <- subjectTransformer.transform(subject)
    } yield transformedSubject

    subjectFuture.transformWith {
      case Success(subject)   => block(AuthenticatedRequest[A](subject, request))
      case Failure(throwable) => notAuthenticated(request, throwable)
    }

  }

  protected def notAuthenticated[A](request: Request[A], throwable: Throwable): Future[Result] =
    Future.successful {
      log.warn("Not authenticated method={}, uri={}", request.method, request.uri)
      throwable match {
        case exception: AnnetteException =>
          Results.Unauthorized(Json.toJson(exception.errorMessage))
        case _                           =>
          log.error("notAuthenticated exception", throwable)
          Results.Unauthorized(Json.toJson(AuthenticationFailedException().errorMessage))
      }
    }

}
