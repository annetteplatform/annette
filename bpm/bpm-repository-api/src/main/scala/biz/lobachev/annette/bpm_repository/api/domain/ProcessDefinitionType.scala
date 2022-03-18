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

case class ProcessDefinitionType(value: String) extends AnyVal

object ProcessDefinitionType extends Enumeration with EnumerationJsonSerializer with EnumerationDBSerializer {
  type ProcessDefinitionType = Value
  val KEY, ID = Value
  protected type T = ProcessDefinitionType

  protected val fromString: String => ProcessDefinitionType = string => this.withName(string.toUpperCase)
  val toJsonRepresentation: ProcessDefinitionType => String = _.toString.toLowerCase

  protected val codes =
    Seq(KEY -> "key", ID -> "id")

  val maxLength       = 3
}
