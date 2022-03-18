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

package biz.lobachev.annette.bpm_repository.api.rdb.serializers

import play.api.libs.json._

import scala.util.{Failure, Success, Try}

trait WrappedStringSerializer[T] extends ClassName {
  val maxLength: Int
  def getInstance(value: String): T
  def getValue(instance: T): String

  def deserializeFromString(stringValue: String): Try[T] =
    if (stringValue.length <= maxLength)
      Success(getInstance(stringValue))
    else
      Failure(DeserializationStringMaximumLengthExceed(className, maxLength, stringValue.length, stringValue))

  implicit val format: Format[T] = Format[T](
    Reads {
      case JsString(stringValue) => deserializeFromString(stringValue).map(JsSuccess(_)).get
      case jsValue               => throw DeserializationStringValueExpected(className, jsValue.toString)
    },
    Writes { instance =>
      JsString(getValue(instance))
    }
  )
}
