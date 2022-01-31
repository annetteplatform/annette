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

package biz.lobachev.annette.bpm_repository.api.domain

import biz.lobachev.annette.bpm_repository.api.rdb.serializers.{EnumerationDBSerializer, EnumerationJsonSerializer}

case class Datatype(value: String) extends AnyVal

object Datatype extends Enumeration with EnumerationJsonSerializer with EnumerationDBSerializer {
  type Datatype = Value
  val Boolean, Bytes, Short, Integer, Long, Double, Date, String, Object, Json, Xml = Value
  protected type T = Datatype

  protected val fromString: String => Datatype = string => this.withName(string)
  val toJsonRepresentation: Datatype => String = _.toString

  protected val codes =
    Seq(
      Boolean -> "Boolean",
      Bytes   -> "Bytes",
      Short   -> "Short",
      Integer -> "Integer",
      Long    -> "Long",
      Double  -> "Double",
      Date    -> "Date",
      String  -> "String",
      Object  -> "Object",
      Json    -> "Json",
      Xml     -> "Xml"
    )

  val maxLength = 20
}
