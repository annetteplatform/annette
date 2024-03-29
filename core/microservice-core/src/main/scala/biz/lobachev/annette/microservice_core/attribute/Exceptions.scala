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

package biz.lobachev.annette.microservice_core.attribute

import biz.lobachev.annette.core.exception.{AnnetteTransportExceptionCompanion2, AnnetteTransportExceptionCompanion3}
import com.lightbend.lagom.scaladsl.api.transport.TransportErrorCode

object AttributeConfigError extends AnnetteTransportExceptionCompanion2 {
  val ErrorCode       = TransportErrorCode.InternalServerError
  val MessageCode     = "annette.core.attribute.configError"
  val Arg1Key: String = "path"
  val Arg2Key: String = "description"
}

object AttributeNotFound extends AnnetteTransportExceptionCompanion2 {
  val ErrorCode       = TransportErrorCode.InternalServerError
  val MessageCode     = "annette.core.attribute.notFound"
  val Arg1Key: String = "entity"
  val Arg2Key: String = "attribute"
}

object InvalidAttribute extends AnnetteTransportExceptionCompanion3 {
  val ErrorCode       = TransportErrorCode.InternalServerError
  val MessageCode     = "annette.core.attribute.invalidAttribute"
  val Arg1Key: String = "entity"
  val Arg2Key: String = "attribute"
  val Arg3Key: String = "value"
}
