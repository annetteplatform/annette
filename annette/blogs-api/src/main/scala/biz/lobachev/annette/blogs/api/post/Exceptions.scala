package biz.lobachev.annette.blogs.api.post

import biz.lobachev.annette.core.exception.{AnnetteTransportExceptionCompanion1, AnnetteTransportExceptionCompanion2}
import com.lightbend.lagom.scaladsl.api.transport.TransportErrorCode

object PostAlreadyExist extends AnnetteTransportExceptionCompanion1 {
  val ErrorCode       = TransportErrorCode.BadRequest
  val MessageCode     = "annette.blogs.post.postAlreadyExist"
  val Arg1Key: String = "id"
}

object PostNotFound extends AnnetteTransportExceptionCompanion1 {
  val ErrorCode       = TransportErrorCode.NotFound
  val MessageCode     = "annette.blogs.post.postNotFound"
  val Arg1Key: String = "id"
}

object PostMediaAlreadyExist extends AnnetteTransportExceptionCompanion2 {
  val ErrorCode       = TransportErrorCode.BadRequest
  val MessageCode     = "annette.blogs.post.postMediaAlreadyExist"
  val Arg1Key: String = "postId"
  val Arg2Key: String = "mediaId"
}

object PostMediaNotFound extends AnnetteTransportExceptionCompanion2 {
  val ErrorCode       = TransportErrorCode.NotFound
  val MessageCode     = "annette.blogs.post.postMediaNotFound"
  val Arg1Key: String = "postId"
  val Arg2Key: String = "mediaId"
}

object PostDocAlreadyExist extends AnnetteTransportExceptionCompanion2 {
  val ErrorCode       = TransportErrorCode.BadRequest
  val MessageCode     = "annette.blogs.post.postDocAlreadyExist"
  val Arg1Key: String = "postId"
  val Arg2Key: String = "mediaId"
}

object PostDocNotFound extends AnnetteTransportExceptionCompanion2 {
  val ErrorCode       = TransportErrorCode.NotFound
  val MessageCode     = "annette.blogs.post.postDocNotFound"
  val Arg1Key: String = "postId"
  val Arg2Key: String = "mediaId"
}
