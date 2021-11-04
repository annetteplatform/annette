/*
 * Copyright 2013 Valery Lobachev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package biz.lobachev.annette.cms.api.blogs.post

import biz.lobachev.annette.core.exception.{AnnetteTransportExceptionCompanion1, AnnetteTransportExceptionCompanion2}
import com.lightbend.lagom.scaladsl.api.transport.TransportErrorCode

object PostAlreadyExist extends AnnetteTransportExceptionCompanion1 {
  val ErrorCode       = TransportErrorCode.BadRequest
  val MessageCode     = "annette.cms.post.postAlreadyExist"
  val Arg1Key: String = "id"
}

object PostPublicationDateClearNotAllowed extends AnnetteTransportExceptionCompanion1 {
  val ErrorCode       = TransportErrorCode.NotFound
  val MessageCode     = "annette.cms.post.postPublicationDateClearNotAllowed"
  val Arg1Key: String = "id"
}

object PostNotFound extends AnnetteTransportExceptionCompanion1 {
  val ErrorCode       = TransportErrorCode.NotFound
  val MessageCode     = "annette.cms.post.postNotFound"
  val Arg1Key: String = "id"
}

object PostMediaAlreadyExist extends AnnetteTransportExceptionCompanion2 {
  val ErrorCode       = TransportErrorCode.BadRequest
  val MessageCode     = "annette.cms.post.postMediaAlreadyExist"
  val Arg1Key: String = "postId"
  val Arg2Key: String = "mediaId"
}

object PostMediaNotFound extends AnnetteTransportExceptionCompanion2 {
  val ErrorCode       = TransportErrorCode.NotFound
  val MessageCode     = "annette.cms.post.postMediaNotFound"
  val Arg1Key: String = "postId"
  val Arg2Key: String = "mediaId"
}

object PostDocAlreadyExist extends AnnetteTransportExceptionCompanion2 {
  val ErrorCode       = TransportErrorCode.BadRequest
  val MessageCode     = "annette.cms.post.postDocAlreadyExist"
  val Arg1Key: String = "postId"
  val Arg2Key: String = "docId"
}

object PostDocNotFound extends AnnetteTransportExceptionCompanion2 {
  val ErrorCode       = TransportErrorCode.NotFound
  val MessageCode     = "annette.cms.post.postDocNotFound"
  val Arg1Key: String = "postId"
  val Arg2Key: String = "docId"
}

object WidgetContentNotFound extends AnnetteTransportExceptionCompanion2 {
  val ErrorCode       = TransportErrorCode.NotFound
  val MessageCode     = "annette.cms.post.widgetContentNotFound"
  val Arg1Key: String = "postId"
  val Arg2Key: String = "widgetContentId"
}
