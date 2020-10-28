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

package biz.lobachev.annette.attributes.api.attribute

import play.api.libs.json.Json

object AttributeValueType extends Enumeration {
  type AttributeValueType = Value

  val String         = Value("String")
  val Boolean        = Value("Boolean")
  val Long           = Value("Long")
  val Double         = Value("Double")
  val OffsetDateTime = Value("OffsetDateTime")
  val LocalTime      = Value("LocalTime")
  val LocalDate      = Value("LocalDate")
  val JSON           = Value("JSON")

  implicit val format = Json.formatEnum(this)
}
