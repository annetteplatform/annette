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

import akka.util.ByteString
import com.lightbend.lagom.scaladsl.api.deser.{DefaultExceptionSerializer, RawExceptionMessage}
import com.lightbend.lagom.scaladsl.api.transport._
import org.slf4j.LoggerFactory
import play.api.libs.json._
import play.api.{Environment, Mode}

import scala.collection.immutable.Seq
import scala.util.control.NonFatal

class AnnetteTransportExceptionSerializer(environment: Environment = Environment.simple(mode = Mode.Prod))
    extends DefaultExceptionSerializer(environment) {

  val log = LoggerFactory.getLogger(this.getClass)

  override def serialize(exception: Throwable, accept: Seq[MessageProtocol]): RawExceptionMessage =
    exception match {
      case ate: AnnetteTransportException =>
        val json         = Json.toJson(ate.params + ("code" -> ate.code))
        val messageBytes = ByteString.fromString(Json.stringify(json))
        RawExceptionMessage(
          errorCode = ate.errorCode,
          protocol = MessageProtocol(
            contentType = Some("application/json"),
            charset = None,
            version = None
          ),
          message = messageBytes
        )

      case _                              => super.serialize(exception, accept)
    }

  override def deserialize(message: RawExceptionMessage): Throwable = {
//    log.debug("deserialize: message {}", message)
    val messageJson =
      try Json.parse(message.message.iterator.asInputStream)
      catch {
        case NonFatal(_) =>
          Json.obj()
      }

    val jsonParseResult = Json.fromJson[Map[String, String]](messageJson)

    jsonParseResult match {
      case JsSuccess(params, _) if params.contains("code") =>
        AnnetteTransportException(message.errorCode, params("code"), params - "code")
      case _                                               =>
//        log.debug("deserialize:parse result {}", other)
        super.deserialize(message)
    }

  }
}
