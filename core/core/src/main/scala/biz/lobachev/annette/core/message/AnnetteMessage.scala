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

package biz.lobachev.annette.core.message

import play.api.libs.json.{Format, Json}

sealed trait AnnetteMessage {
  val code: String
  val messageType: MessageType
  val params: Map[String, String]
}

class InfoMessage(
  override val code: String,
  override val params: Map[String, String] = Map.empty
) extends AnnetteMessage {
  override val messageType = "I"
}

object InfoMessage {
  implicit val format: Format[InfoMessage] = Json.format

  def apply(code: String, params: Map[String, String] = Map.empty) = new InfoMessage(code, params)

  def unapply(message: InfoMessage): Option[(String, Map[String, String])] = Some((message.code, message.params))
}

class WarningMessage(
  override val code: String,
  override val params: Map[String, String] = Map.empty
) extends AnnetteMessage {
  override val messageType = "W"
}

object WarningMessage {
  implicit val format: Format[WarningMessage] = Json.format

  def apply(code: String, params: Map[String, String] = Map.empty) = new WarningMessage(code, params)

  def unapply(message: WarningMessage): Option[(String, Map[String, String])] = Some((message.code, message.params))
}

class ErrorMessage(
  override val code: String,
  override val params: Map[String, String] = Map.empty,
  val errorCode: ErrorCode = ErrorCode.BadRequest
) extends AnnetteMessage {
  override val messageType = "E"
  override def toString() = {
    val paramsList = params.map { case (k, v) => s"$k: $v" }.mkString("[", ", ", "]")
    s"ErrorMessage: $errorCode - $code$paramsList"
  }
}

object ErrorMessage {
  implicit val format: Format[ErrorMessage] = Json.format

  def apply(
    code: String,
    params: Map[String, String] = Map.empty,
    errorCode: ErrorCode = ErrorCode.BadRequest
  ) = new ErrorMessage(code, params, errorCode)

  def unapply(message: ErrorMessage): Option[(String, Map[String, String], Int)] =
    Some(
      (message.code, message.params, message.errorCode)
    )
}

object AnnetteMessage {
  implicit val format: Format[AnnetteMessage] = Json.format
}
