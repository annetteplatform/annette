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

/**
 * Replacement for Lagom's `com.lightbend.lagom.scaladsl.api.transport.TransportErrorCode`
 * (removed in slice 013 of the Pekko migration). Carries the HTTP status the gateway
 * maps an [[AnnetteTransportException]] to, plus a human description for log messages.
 */
case class TransportErrorCode(http: Int, description: String)

object TransportErrorCode {
  val BadRequest        = TransportErrorCode(400, "Bad Request")
  val Forbidden         = TransportErrorCode(403, "Forbidden")
  val NotFound          = TransportErrorCode(404, "Not Found")
  // Lagom parity: the description string matched Lagom's TransportErrorCode verbatim
  // (asserted by test message expectations).
  val InternalServerError = TransportErrorCode(500, "Unexpected Condition/Internal Server Error")
}
