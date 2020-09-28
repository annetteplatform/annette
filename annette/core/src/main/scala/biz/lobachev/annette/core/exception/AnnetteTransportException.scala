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

package biz.lobachev.annette.core.exception

import biz.lobachev.annette.core.message.{AnnetteMessage, ErrorMessage}
import com.lightbend.lagom.scaladsl.api.transport.TransportErrorCode

case class AnnetteTransportException(
  errorCode: TransportErrorCode,
  code: String,
  params: Map[String, String] = Map.empty
) extends RuntimeException(AnnetteTransportException.toMessageString(errorCode, code, params)) {
  def toMessage: AnnetteMessage = ErrorMessage(code, params, errorCode.http)
}

object AnnetteTransportException {
  def toMessageString(
    errorCode: TransportErrorCode,
    code: String,
    params: Map[String, String] = Map.empty
  ) = {
    val errorCodeString = errorCode.description
    val errorCodeVal    = errorCode.http
    val paramsList      = params.map { case (k, v) => s"$k: $v" }.mkString("[", ", ", "]")
    s"$errorCodeVal ($errorCodeString) - $code$paramsList"
  }
}
