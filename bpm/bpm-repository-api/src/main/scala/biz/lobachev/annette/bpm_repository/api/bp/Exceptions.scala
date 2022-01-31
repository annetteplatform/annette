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

package biz.lobachev.annette.bpm_repository.api.bp

import biz.lobachev.annette.core.exception.{AnnetteTransportExceptionCompanion1, AnnetteTransportExceptionCompanion2}
import com.lightbend.lagom.scaladsl.api.transport.TransportErrorCode

object BusinessProcessUpdateFailed extends AnnetteTransportExceptionCompanion2 {
  val ErrorCode       = TransportErrorCode.InternalServerError
  val MessageCode     = "annette.bpm.businessProcess.updateFailed"
  val Arg1Key: String = "action"
  val Arg2Key: String = "value"
}

object BusinessProcessAlreadyExist extends AnnetteTransportExceptionCompanion1 {
  val ErrorCode       = TransportErrorCode.InternalServerError
  val MessageCode     = "annette.bpm.businessProcess.alreadyExist"
  val Arg1Key: String = "id"
}

object BusinessProcessNotFound extends AnnetteTransportExceptionCompanion1 {
  val ErrorCode       = TransportErrorCode.NotFound
  val MessageCode     = "annette.bpm.businessProcess.notFound"
  val Arg1Key: String = "id"
}
