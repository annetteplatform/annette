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

package biz.lobachev.annette.microservice_core.indexing.config

import com.sksamuel.elastic4s.analysis._
import pureconfig.generic.FieldCoproductHint

sealed trait AnalyzerConf {
  def analyzer(name: String): Analyzer
}

case class CustomAnalyzerConf(
  tokenizer: String,
  charFilters: List[String] = Nil,
  tokenFilters: List[String] = Nil,
  positionIncrementGap: Int = 100
) extends AnalyzerConf {
  override def analyzer(name: String): Analyzer =
    CustomAnalyzer(
      name,
      tokenizer,
      charFilters,
      tokenFilters,
      positionIncrementGap
    )
}

case class StopAnalyzerConf(
  stopwords: List[String]
) extends AnalyzerConf {
  override def analyzer(name: String): Analyzer =
    StopAnalyzer(name, stopwords)
}

case class FingerprintAnalyzerConf(
  separator: Option[String] = None,
  stopwords: List[String] = Nil,
  maxOutputSize: Int = 255
) extends AnalyzerConf {
  override def analyzer(name: String): Analyzer =
    FingerprintAnalyzer(
      name,
      separator,
      stopwords,
      maxOutputSize
    )
}

case class PatternAnalyzerConf(
  regex: String,
  lowercase: Boolean = true
) extends AnalyzerConf {
  override def analyzer(name: String): Analyzer =
    PatternAnalyzer(
      name,
      regex,
      lowercase
    )
}

object AnalyzerConf {
  implicit val confHint = new FieldCoproductHint[AnalyzerConf]("type") {
    override def fieldValue(name: String) = name.dropRight("AnalyzerConf".length).toLowerCase
  }
}
