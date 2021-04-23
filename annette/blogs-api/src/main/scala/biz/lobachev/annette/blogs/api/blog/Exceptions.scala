package biz.lobachev.annette.blogs.api.blog

import biz.lobachev.annette.core.exception.AnnetteTransportExceptionCompanion1
import com.lightbend.lagom.scaladsl.api.transport.TransportErrorCode

object BlogAlreadyExist extends AnnetteTransportExceptionCompanion1 {
  val ErrorCode = TransportErrorCode.BadRequest
  val MessageCode = "annette.blogs.blog.blogAlreadyExist"
  val Arg1Key: String = "id"
}

object BlogNotFound extends AnnetteTransportExceptionCompanion1 {
  val ErrorCode = TransportErrorCode.NotFound
  val MessageCode = "annette.blogs.blog.blogNotFound"
  val Arg1Key: String = "id"
}

