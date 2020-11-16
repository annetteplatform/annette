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

package biz.lobachev.annette.org_structure.api.hierarchy

import biz.lobachev.annette.core.exception.AnnetteTransportExceptionCompanion
import com.lightbend.lagom.scaladsl.api.transport.TransportErrorCode

object OrganizationAlreadyExist   extends AnnetteTransportExceptionCompanion {
  val ErrorCode   = TransportErrorCode.BadRequest
  val MessageCode = "annette.orgStructureService.hierarchy.organizationAlreadyExist"
}
object OrganizationNotFound       extends AnnetteTransportExceptionCompanion {
  val ErrorCode   = TransportErrorCode.NotFound
  val MessageCode = "annette.orgStructureService.hierarchy.organizationNotFound"
}
object OrganizationNotEmpty       extends AnnetteTransportExceptionCompanion {
  val ErrorCode   = TransportErrorCode.BadRequest
  val MessageCode = "annette.orgStructureService.hierarchy.organizationNotEmpty"
}
object UnitNotEmpty               extends AnnetteTransportExceptionCompanion {
  val ErrorCode   = TransportErrorCode.BadRequest
  val MessageCode = "annette.orgStructureService.hierarchy.unitNotEmpty"
}
object ItemNotFound               extends AnnetteTransportExceptionCompanion {
  val ErrorCode   = TransportErrorCode.NotFound
  val MessageCode = "annette.orgStructureService.hierarchy.itemNotFound"
}
object PositionNotEmpty           extends AnnetteTransportExceptionCompanion {
  val ErrorCode   = TransportErrorCode.BadRequest
  val MessageCode = "annette.orgStructureService.hierarchy.positionNotEmpty"
}
object AlreadyExist               extends AnnetteTransportExceptionCompanion {
  val ErrorCode   = TransportErrorCode.BadRequest
  val MessageCode = "annette.orgStructureService.hierarchy.alreadyExist"
}
object ParentNotFound             extends AnnetteTransportExceptionCompanion {
  val ErrorCode   = TransportErrorCode.NotFound
  val MessageCode = "annette.orgStructureService.hierarchy.parentNotFound"
}
object ChiefNotFound              extends AnnetteTransportExceptionCompanion {
  val ErrorCode   = TransportErrorCode.NotFound
  val MessageCode = "annette.orgStructureService.hierarchy.chiefNotFound"
}
object ChiefAlreadyAssigned       extends AnnetteTransportExceptionCompanion {
  val ErrorCode   = TransportErrorCode.BadRequest
  val MessageCode = "annette.orgStructureService.hierarchy.chiefAlreadyAssigned"
}
object ChiefNotAssigned           extends AnnetteTransportExceptionCompanion {
  val ErrorCode   = TransportErrorCode.BadRequest
  val MessageCode = "annette.orgStructureService.hierarchy.chiefNotAssigned"
}
object PositionLimitExceeded      extends AnnetteTransportExceptionCompanion {
  val ErrorCode   = TransportErrorCode.BadRequest
  val MessageCode = "annette.orgStructureService.hierarchy.positionLimitExceeded"
}
object PersonAlreadyAssigned      extends AnnetteTransportExceptionCompanion {
  val ErrorCode   = TransportErrorCode.BadRequest
  val MessageCode = "annette.orgStructureService.hierarchy.personAlreadyAssigned"
}
object PersonNotAssigned          extends AnnetteTransportExceptionCompanion {
  val ErrorCode   = TransportErrorCode.BadRequest
  val MessageCode = "annette.orgStructureService.hierarchy.personNotAssigned"
}
object IncorrectOrder             extends AnnetteTransportExceptionCompanion {
  val ErrorCode   = TransportErrorCode.BadRequest
  val MessageCode = "annette.orgStructureService.hierarchy.incorrectOrder"
}
object IncorrectMoveItemArguments extends AnnetteTransportExceptionCompanion {
  val ErrorCode   = TransportErrorCode.BadRequest
  val MessageCode = "annette.orgStructureService.hierarchy.incorrectMoveItemArguments"
}
object IncorrectCategory          extends AnnetteTransportExceptionCompanion {
  val ErrorCode   = TransportErrorCode.BadRequest
  val MessageCode = "annette.orgStructureService.hierarchy.invalidCategory"
}
