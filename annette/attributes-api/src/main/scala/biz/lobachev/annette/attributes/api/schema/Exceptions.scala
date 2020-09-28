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

package biz.lobachev.annette.attributes.api.schema

import biz.lobachev.annette.core.exception.{AnnetteTransportException, AnnetteTransportExceptionCompanion}
import com.lightbend.lagom.scaladsl.api.transport.TransportErrorCode

object SchemaAlreadyExist extends AnnetteTransportExceptionCompanion {
  val ErrorCode   = TransportErrorCode.BadRequest
  val MessageCode = "annette.attributeService.schema.schemaAlreadyExist"
}

object SchemaNotFound extends AnnetteTransportExceptionCompanion {
  val ErrorCode   = TransportErrorCode.NotFound
  val MessageCode = "annette.attributeService.schema.schemaNotFound"
}

object EmptySchema extends AnnetteTransportExceptionCompanion {
  val ErrorCode   = TransportErrorCode.BadRequest
  val MessageCode = "annette.attributeService.schema.emptySchema"
}

object TypeChangeNotAllowed extends AnnetteTransportExceptionCompanion {
  val ErrorCode   = TransportErrorCode.BadRequest
  val MessageCode = "annette.attributeService.schema.typeChangeNotAllowed"
}

object AttributeNotFound extends AnnetteTransportExceptionCompanion {
  val ErrorCode   = TransportErrorCode.NotFound
  val MessageCode = "annette.attributeService.schema.attributeNotFound"
}

object InvalidComposedId extends AnnetteTransportExceptionCompanion {
  val ErrorCode   = TransportErrorCode.BadRequest
  val MessageCode = "annette.attributeService.schema.invalidComposedId"
}

object AttributesHasAssignments {
  val ErrorCode                              = TransportErrorCode.BadRequest
  val MessageCode                            = "annette.attributeService.schema.attributesHasAssignments"
  def apply(attributes: String)              = new AnnetteTransportException(ErrorCode, MessageCode, Map("attributes" -> attributes))
  def unapply(ex: Exception): Option[String] =
    ex match {
      case ate: AnnetteTransportException
          if ate.errorCode == ErrorCode &&
            ate.code == MessageCode &&
            ate.params.isDefinedAt("attributes") =>
        Some(ate.params("attributes"))
      case _ => None
    }
}
