package biz.lobachev.annette.service_catalog.api.group

import biz.lobachev.annette.core.exception.AnnetteTransportExceptionCompanion1
import com.lightbend.lagom.scaladsl.api.transport.TransportErrorCode

object GroupAlreadyExist extends AnnetteTransportExceptionCompanion1 {
  val ErrorCode = TransportErrorCode.BadRequest
  val MessageCode = "annette.serviceCatalog.group.groupAlreadyExist"
  val Arg1Key: String = "id"
}

object GroupNotFound extends AnnetteTransportExceptionCompanion1 {
  val ErrorCode = TransportErrorCode.NotFound
  val MessageCode = "annette.serviceCatalog.group.groupNotFound"
  val Arg1Key: String = "id"
}

