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

package biz.lobachev.annette.microservice_core.indexing

import biz.lobachev.annette.core.exception.{AnnetteTransportExceptionCompanion1, AnnetteTransportExceptionCompanion2}
import com.lightbend.lagom.scaladsl.api.transport.TransportErrorCode

object IndexingRequestFailure extends AnnetteTransportExceptionCompanion1 {
  val ErrorCode       = TransportErrorCode.InternalServerError
  val MessageCode     = "annette.core.indexing.requestFailure"
  val Arg1Key: String = "reason"
}

object AliasNotFound extends AnnetteTransportExceptionCompanion1 {
  val ErrorCode       = TransportErrorCode.BadRequest
  val MessageCode     = "annette.core.indexing.aliasNotFound"
  val Arg1Key: String = "alias"
}

object InvalidIndexError extends AnnetteTransportExceptionCompanion1 {
  val ErrorCode       = TransportErrorCode.InternalServerError
  val MessageCode     = "annette.core.indexing.invalidIndexError"
  val Arg1Key: String = "aliases"
}

object IndexConfigError extends AnnetteTransportExceptionCompanion2 {
  val ErrorCode       = TransportErrorCode.InternalServerError
  val MessageCode     = "annette.core.indexing.indexConfigError"
  val Arg1Key: String = "path"
  val Arg2Key: String = "description"
}

object ConnectionConfigError extends AnnetteTransportExceptionCompanion2 {
  val ErrorCode       = TransportErrorCode.InternalServerError
  val MessageCode     = "annette.core.indexing.connectionConfigError"
  val Arg1Key: String = "path"
  val Arg2Key: String = "description"
}

object DuplicateIndexFields extends AnnetteTransportExceptionCompanion1 {
  val ErrorCode       = TransportErrorCode.InternalServerError
  val MessageCode     = "annette.core.indexing.duplicateIndexFields"
  val Arg1Key: String = "fields"
}
