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

package biz.lobachev.annette.bpm_repository.impl.model

import biz.lobachev.annette.bpm_repository.api.domain.Notation
import biz.lobachev.annette.bpm_repository.api.model._

import scala.util.Try
import scala.xml.XML

trait CodeExtractor {

  def extractCode(notation: Notation.Notation, xmlStr: String): String =
    Try {
      val xml     = XML.loadString(xmlStr)
      val nodeSeq = (notation: @unchecked) match {
        case Notation.BPMN => xml \\ "definitions" \ "process" \ "@id"
        case Notation.DMN  => xml \\ "definitions" \ "decision" \ "@id"
        case Notation.CMMN => xml \\ "definitions" \ "case" \ "@id"
      }
      nodeSeq.text
    }.toOption
      .filter(_.trim.nonEmpty)
      .getOrElse(throw InvalidModel(notation, xmlStr))
}
