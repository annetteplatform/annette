package biz.lobachev.annette.blogs.api.category

import biz.lobachev.annette.core.exception.AnnetteTransportExceptionCompanion1
import com.lightbend.lagom.scaladsl.api.transport.TransportErrorCode

object CategoryAlreadyExist extends AnnetteTransportExceptionCompanion1 {
  val ErrorCode = TransportErrorCode.BadRequest
  val MessageCode = "annette.blogs.category.categoryAlreadyExist"
  val Arg1Key: String = "id"
}

object CategoryNotFound extends AnnetteTransportExceptionCompanion1 {
  val ErrorCode = TransportErrorCode.NotFound
  val MessageCode = "annette.blogs.category.categoryNotFound"
  val Arg1Key: String = "id"
}

