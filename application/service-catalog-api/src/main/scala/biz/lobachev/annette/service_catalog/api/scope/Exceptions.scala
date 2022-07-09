package biz.lobachev.annette.service_catalog.api.scope

import biz.lobachev.annette.core.exception.AnnetteTransportExceptionCompanion1
import com.lightbend.lagom.scaladsl.api.transport.TransportErrorCode

object ScopeAlreadyExist extends AnnetteTransportExceptionCompanion1 {
  val ErrorCode = TransportErrorCode.BadRequest
  val MessageCode = "annette.serviceCatalog.scope.scopeAlreadyExist"
  val Arg1Key: String = "id"
}

object ScopeNotFound extends AnnetteTransportExceptionCompanion1 {
  val ErrorCode = TransportErrorCode.NotFound
  val MessageCode = "annette.serviceCatalog.scope.scopeNotFound"
  val Arg1Key: String = "id"
}

