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

package biz.lobachev.annette.api_gateway_core.exception

import biz.lobachev.annette.core.exception.{AnnetteException, AnnetteTransportException}
import biz.lobachev.annette.core.message.ErrorMessage
import com.lightbend.lagom.scaladsl.api.transport.TransportException
import net.logstash.logback.argument.StructuredArguments._
import org.slf4j.LoggerFactory
import play.api.http.HttpErrorHandler
import play.api.http.Status.{BAD_REQUEST, FORBIDDEN, NOT_FOUND}
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc.{RequestHeader, Result}

import javax.inject.Singleton
import scala.concurrent.Future
import scala.util.Random

@Singleton
class ApiGatewayErrorHandler extends HttpErrorHandler {
  private val log = LoggerFactory.getLogger(classOf[ApiGatewayErrorHandler])

  def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    log.error("Client error: {}/{} - {}", statusCode, message, request)

    val resultMessage = statusCode match {
      case BAD_REQUEST => new ErrorMessage("client.error.badRequest", Map("message" -> message), statusCode)
      case FORBIDDEN   => new ErrorMessage("client.error.forbidden", Map("message" -> message), statusCode)
      case NOT_FOUND   => new ErrorMessage("client.error.notFound", Map("message" -> message), statusCode)
      case _           => new ErrorMessage(s"client.error.$statusCode", Map("message" -> message), statusCode)
    }
    Future.successful(Status(statusCode)(Json.toJson(resultMessage)))
  }

  def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    log.error("Server error", exception)
    exception match {
      case ex: AnnetteTransportException => Future.successful(Status(ex.errorCode.http)(Json.toJson(ex.errorMessage)))
      case ex: AnnetteException          => Future.successful(Status(ex.errorMessage.errorCode)(Json.toJson(ex.errorMessage)))
      case ex: TransportException        => Future.successful(Status(ex.errorCode.http)(Json.toJson(ex.exceptionMessage.name)))
      case th: Throwable                 =>
        val id      = Random.nextInt(1000000).toString
        val message = new ErrorMessage("annette.throwable", Map("id" -> id))
        log.error(
          s"Exception {} {} {}",
          keyValue("id", id),
          keyValue("method", request.method),
          keyValue("path", request.path),
          th
        )
        Future.successful(InternalServerError(Json.toJson(message)))
    }
  }
}
