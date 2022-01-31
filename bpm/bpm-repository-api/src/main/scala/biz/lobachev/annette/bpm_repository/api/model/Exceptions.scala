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

package biz.lobachev.annette.bpm_repository.api.model

import biz.lobachev.annette.bpm_repository.api.domain.{Notation}
import biz.lobachev.annette.core.exception.{
  AnnetteTransportException,
  AnnetteTransportExceptionCompanion1,
  AnnetteTransportExceptionCompanion2
}
import com.lightbend.lagom.scaladsl.api.transport.TransportErrorCode

object InvalidModel extends AnnetteTransportExceptionCompanion2 {
  val ErrorCode       = TransportErrorCode.BadRequest
  val MessageCode     = "annette.bpm.invalidModel"
  val Arg1Key: String = "notation"
  val Arg2Key: String = "xml"

  def apply(notation: Notation.Notation, xml: String): AnnetteTransportException = InvalidModel(notation.toString, xml)
}

object BpmModelAlreadyExist extends AnnetteTransportExceptionCompanion1 {
  val ErrorCode       = TransportErrorCode.InternalServerError
  val MessageCode     = "annette.bpm.bpmModel.alreadyExist"
  val Arg1Key: String = "id"
}

object BpmModelNotFound extends AnnetteTransportExceptionCompanion1 {
  val ErrorCode       = TransportErrorCode.NotFound
  val MessageCode     = "annette.bpm.bpmModel.notFound"
  val Arg1Key: String = "id"
}
