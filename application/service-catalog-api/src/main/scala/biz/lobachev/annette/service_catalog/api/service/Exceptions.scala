package biz.lobachev.annette.service_catalog.api.service

import biz.lobachev.annette.core.exception.AnnetteTransportExceptionCompanion1
import com.lightbend.lagom.scaladsl.api.transport.TransportErrorCode

object ServiceAlreadyExist extends AnnetteTransportExceptionCompanion1 {
  val ErrorCode = TransportErrorCode.BadRequest
  val MessageCode = "annette.serviceCatalog.service.serviceAlreadyExist"
  val Arg1Key: String = "id"
}

object ServiceNotFound extends AnnetteTransportExceptionCompanion1 {
  val ErrorCode = TransportErrorCode.NotFound
  val MessageCode = "annette.serviceCatalog.service.serviceNotFound"
  val Arg1Key: String = "id"
}

