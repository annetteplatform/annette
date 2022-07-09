package biz.lobachev.annette.service_catalog.api.service_principal

import biz.lobachev.annette.core.exception.AnnetteTransportExceptionCompanion1
import com.lightbend.lagom.scaladsl.api.transport.TransportErrorCode

object ServicePrincipalNotFound extends AnnetteTransportExceptionCompanion1 {
  val ErrorCode = TransportErrorCode.NotFound
  val MessageCode = "annette.serviceCatalog.servicePrincipal.servicePrincipalNotFound"
  val Arg1Key: String = "id"
}

