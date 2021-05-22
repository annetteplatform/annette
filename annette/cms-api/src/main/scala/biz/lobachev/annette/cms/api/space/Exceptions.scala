package biz.lobachev.annette.cms.api.space

import biz.lobachev.annette.core.exception.AnnetteTransportExceptionCompanion1
import com.lightbend.lagom.scaladsl.api.transport.TransportErrorCode

object SpaceAlreadyExist extends AnnetteTransportExceptionCompanion1 {
  val ErrorCode       = TransportErrorCode.BadRequest
  val MessageCode     = "annette.cms.space.spaceAlreadyExist"
  val Arg1Key: String = "id"
}

object SpaceNotFound extends AnnetteTransportExceptionCompanion1 {
  val ErrorCode       = TransportErrorCode.NotFound
  val MessageCode     = "annette.cms.space.spaceNotFound"
  val Arg1Key: String = "id"
}
