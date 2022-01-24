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

package biz.lobachev.annette.bpm_repository.api.rdb.serializers

import biz.lobachev.annette.core.exception.{
  AnnetteTransportException,
  AnnetteTransportExceptionCompanion2,
  AnnetteTransportExceptionCompanion4
}
import com.lightbend.lagom.scaladsl.api.transport.TransportErrorCode

object DeserializationStringMaximumLengthExceed extends AnnetteTransportExceptionCompanion4 {
  val ErrorCode       = TransportErrorCode.BadRequest
  val MessageCode     = "annette.rdb.deserialization.stringMaximumLengthExceed"
  val Arg1Key: String = "className"
  val Arg2Key: String = "maxLength"
  val Arg3Key: String = "actualLength"
  val Arg4Key: String = "value"

  def apply(
    className: String,
    maxLength: Int,
    actualLength: Int,
    value: String
  ): AnnetteTransportException =
    DeserializationStringMaximumLengthExceed(
      className,
      maxLength.toString,
      actualLength.toString,
      value
    )

}

object DeserializationStringValueExpected extends AnnetteTransportExceptionCompanion2 {
  val ErrorCode       = TransportErrorCode.BadRequest
  val MessageCode     = "annette.rdb.deserialization.stringValueExpected"
  val Arg1Key: String = "className"
  val Arg2Key: String = "value"
}

object DeserializationUnknownEnumerationValue extends AnnetteTransportExceptionCompanion2 {
  val ErrorCode       = TransportErrorCode.BadRequest
  val MessageCode     = "annette.rdb.deserialization.unknownEnumerationValue"
  val Arg1Key: String = "className"
  val Arg2Key: String = "value"
}
