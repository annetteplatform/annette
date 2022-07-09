package biz.lobachev.annette.service_catalog.api.scope_principal

import biz.lobachev.annette.core.exception.AnnetteTransportExceptionCompanion1
import com.lightbend.lagom.scaladsl.api.transport.TransportErrorCode

object ScopePrincipalNotFound extends AnnetteTransportExceptionCompanion1 {
  val ErrorCode = TransportErrorCode.NotFound
  val MessageCode = "annette.serviceCatalog.scopePrincipal.scopePrincipalNotFound"
  val Arg1Key: String = "id"
}

