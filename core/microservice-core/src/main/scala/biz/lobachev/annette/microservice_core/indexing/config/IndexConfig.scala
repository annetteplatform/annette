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

import com.sksamuel.elastic4s.analysis.{Analyzer, Tokenizer}
import com.sksamuel.elastic4s.requests.mappings.FieldDefinition

case class IndexConfig(
  index: String,
  mappings: Map[String, IndexFieldConf] = Map.empty,
  analyzers: Map[String, AnalyzerConf] = Map.empty,
  tokenizers: Map[String, TokenizerConf] = Map.empty
  //  tokenFilters: Option[Map[String, TokenFilterConf]] = None,
  //  charFilters: Option[Map[String, CharFilterConf]] = None,
  //  normalizers: Option[Map[String, Normalizer]Conf] = None,
) {
  def getProperties(aliases: Seq[String]): Seq[FieldDefinition] =
    aliases.map(alias => mappings(alias).fieldDefinition(alias))

  def getAnalyzers: List[Analyzer]   =
    analyzers.map { case name -> analyzerConf => analyzerConf.analyzer(name) }.toList

  def getTokenizers: List[Tokenizer] =
    tokenizers.map { case name -> tokenizerConf => tokenizerConf.tokenizer(name) }.toList

}
