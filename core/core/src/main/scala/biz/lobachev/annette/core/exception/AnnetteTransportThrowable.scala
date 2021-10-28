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

object AnnetteTransportThrowable {
  val ANNETTE_THROWABLE = "annette.throwable"

  def apply(errorCode: TransportErrorCode, throwable: Throwable): AnnetteTransportException = {

    def getParams(th: Throwable, level: Int = 0): Map[String, String] = {
      val params = Map(
        "code" + level.toString    -> th.getClass.getCanonicalName,
        "message" + level.toString -> th.getMessage
      )
      params ++ Option(th.getCause).map(getParams(_, level + 1)).getOrElse(Map.empty)
    }

    val params: Map[String, String] = getParams(throwable)
    new AnnetteTransportException(errorCode, ANNETTE_THROWABLE, params)
  }

}
