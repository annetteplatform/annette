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

sealed trait TokenizerConf {
  def tokenizer(name: String): Tokenizer
}

case class UaxUrlEmailTokenizerConf(maxTokenLength: Int = 255) extends TokenizerConf {
  override def tokenizer(name: String): Tokenizer =
    UaxUrlEmailTokenizer(name, maxTokenLength)
}

case class CharGroupTokenizerConf(tokenizeOnChars: List[String]) extends TokenizerConf {
  override def tokenizer(name: String): Tokenizer = CharGroupTokenizer(name, tokenizeOnChars)
}

case class StandardTokenizerConf(maxTokenLength: Int = 255) extends TokenizerConf {
  override def tokenizer(name: String): Tokenizer = StandardTokenizer(name, maxTokenLength)
}

case class PatternTokenizerConf(pattern: String = "\\W+", flags: String = "", group: Int = -1) extends TokenizerConf {
  override def tokenizer(name: String): Tokenizer = PatternTokenizer(name, pattern, flags, group)
}

case class KeywordTokenizerConf(bufferSize: Int = 256) extends TokenizerConf {
  override def tokenizer(name: String): Tokenizer = KeywordTokenizer(name, bufferSize)
}

case class NGramTokenizerConf(minGram: Int = 1, maxGram: Int = 2, tokenChars: List[String] = Nil)
    extends TokenizerConf {
  override def tokenizer(name: String): Tokenizer = NGramTokenizer(name, minGram, maxGram, tokenChars)
}

case class EdgeNGramTokenizerConf(minGram: Int = 1, maxGram: Int = 2, tokenChars: List[String] = Nil)
    extends TokenizerConf {
  override def tokenizer(name: String): Tokenizer =
    EdgeNGramTokenizer(name, minGram, maxGram, tokenChars)
}

case class PathHierarchyTokenizerConf(
  delimiter: Char = '/',
  replacement: Char = '/',
  bufferSize: Int = 1024,
  reverse: Boolean = false,
  skip: Int = 0
) extends TokenizerConf {
  override def tokenizer(name: String): Tokenizer =
    PathHierarchyTokenizer(name, delimiter, replacement, bufferSize, reverse, skip)
}

case class WhitespaceTokenizerConf(maxTokenLength: Int) extends TokenizerConf {
  override def tokenizer(name: String): Tokenizer =
    WhitespaceTokenizer(name, maxTokenLength)
}

case class ClassicTokenizerConf(maxTokenLength: Int) extends TokenizerConf {
  override def tokenizer(name: String): Tokenizer = ClassicTokenizer(name, maxTokenLength)
}

case class ThaiTokenizerConf() extends TokenizerConf {
  override def tokenizer(name: String): Tokenizer = ThaiTokenizer(name)
}

case class ICUTokenizerConf(ruleFiles: String) extends TokenizerConf {
  override def tokenizer(name: String): Tokenizer = ICUTokenizer(name, ruleFiles)
}

object TokenizerConf {
  implicit val confHint = new FieldCoproductHint[TokenizerConf]("type") {
    override def fieldValue(name: String) =
      FieldCoproductHint.defaultMapping(name.dropRight("TokenizerConf".length))
  }
}
