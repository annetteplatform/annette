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

package biz.lobachev.annette.api_gateway_core.authentication

import biz.lobachev.annette.core.exception.AnnetteException
import biz.lobachev.annette.core.message.{ErrorCode, ErrorMessage}

case class AuthenticationFailedException()
    extends AnnetteException(
      ErrorMessage("core.authentication.failed", errorCode = ErrorCode.Unauthorized)
    )

case class TokenExpiredException()
    extends AnnetteException(
      ErrorMessage("core.authentication.tokenExpired", errorCode = ErrorCode.Unauthorized)
    )

case class InvalidAuthorizationHeaderException()
    extends AnnetteException(
      ErrorMessage("core.authentication.invalidAuthorizationHeader", errorCode = ErrorCode.Unauthorized)
    )
