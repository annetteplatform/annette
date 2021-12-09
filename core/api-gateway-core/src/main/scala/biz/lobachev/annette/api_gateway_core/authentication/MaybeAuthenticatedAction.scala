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
import biz.lobachev.annette.core.model.auth.AnonymousPrincipal
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.Json
import play.api.mvc._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class MaybeAuthenticatedAction @Inject() (
  authenticator: DefaultAuthenticator,
  subjectTransformer: SubjectTransformer,
  val parser: BodyParsers.Default,
  implicit val executionContext: ExecutionContext
) extends ActionBuilder[AuthenticatedRequest, AnyContent] {
  final private val log: Logger = LoggerFactory.getLogger(this.getClass)

  def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    log.info("Request method={}, uri={}", request.method, request.uri)
    val started       = System.currentTimeMillis()
    val subjectFuture = for {
      subject            <- authenticator.authenticate(request).recover {
                              case _ => Subject(Seq(AnonymousPrincipal()), Map.empty, Some(300L))
                            }
      transformedSubject <- subjectTransformer.transform(subject)
    } yield transformedSubject

    subjectFuture.transformWith {
      case Success(subject)   =>
        val result = block(AuthenticatedRequest[A](subject, request))
          .map(result =>
            subject.expirationTime
              .map(expirationTime =>
                result.withSession(
                  "principal" -> subject.principals.head.code,
                  "exp"       -> expirationTime.toString
                )
              )
              .getOrElse(result)
          )
        result.foreach { res =>
          val completedPeriod = System.currentTimeMillis() - started
          log.info(
            "Request completed method={}, uri={}, principal={}, completed={} ms, contentLength={}",
            request.method,
            request.uri,
            subject.principals.head.code,
            completedPeriod,
            res.body.contentLength.getOrElse("None")
          )
        }
        result
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
