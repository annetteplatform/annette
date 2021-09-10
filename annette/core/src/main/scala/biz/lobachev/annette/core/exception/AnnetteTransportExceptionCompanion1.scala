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

package biz.lobachev.annette.core.exception

import com.lightbend.lagom.scaladsl.api.transport.TransportErrorCode

trait AnnetteTransportExceptionCompanion1 {
  val ErrorCode: TransportErrorCode
  val MessageCode: String
  val Arg1Key: String

  def apply(arg1: String) = new AnnetteTransportException(ErrorCode, MessageCode, Map(Arg1Key -> arg1))

  def unapply(ex: Exception): Option[(String)] =
    ex match {
      case ate: AnnetteTransportException
          if ate.errorCode == ErrorCode && ate.code == MessageCode && ate.params.isDefinedAt(Arg1Key) =>
        Some((ate.params(Arg1Key)))
      case _ => None
    }
}
