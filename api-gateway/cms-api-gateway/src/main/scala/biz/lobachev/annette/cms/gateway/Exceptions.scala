package biz.lobachev.annette.cms.gateway

import biz.lobachev.annette.core.exception.{AnnetteTransportExceptionCompanion2, AnnetteTransportExceptionCompanion3}
import com.lightbend.lagom.scaladsl.api.transport.TransportErrorCode

object FileNotFoundInRequest extends AnnetteTransportExceptionCompanion2 {
  val ErrorCode       = TransportErrorCode.NotFound
  val MessageCode     = "annette.cms.post.fileNotFoundInRequest"
  val Arg1Key: String = "postId"
  val Arg2Key: String = "fileId"
}

object FileNotFound extends AnnetteTransportExceptionCompanion3 {
  val ErrorCode       = TransportErrorCode.NotFound
  val MessageCode     = "annette.cms.post.fileNotFound"
  val Arg1Key: String = "postId"
  val Arg2Key: String = "fileType"
  val Arg3Key: String = "fileId"
}
