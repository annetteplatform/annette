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

import com.sksamuel.elastic4s.requests.mappings.FieldDefinition
import pureconfig.generic.FieldCoproductHint
import com.sksamuel.elastic4s.requests.mappings

sealed trait IndexFieldConf {
  val field: Option[String]
  def fieldType: String
  def fieldDefinition(alias: String): FieldDefinition =
    mappings.BasicField(
      name = field.getOrElse(alias),
      `type` = fieldType
    )
}

case class TextFieldConf(
  field: Option[String] = None,
  fielddata: Boolean = false,
  analyzer: Option[String] = None,
  searchAnalyzer: Option[String] = None,
  fields: Map[String, IndexFieldConf] = Map.empty
) extends IndexFieldConf {
  override def fieldType: String = "text"

  override def fieldDefinition(alias: String): FieldDefinition =
    mappings.TextField(
      name = field.getOrElse(alias),
      fielddata = Some(fielddata),
      analysis = mappings.Analysis(
        analyzer = analyzer,
        searchAnalyzer = searchAnalyzer
      ),
      fields = fields.map { case k -> v => v.fieldDefinition(k) }.toSeq
    )

}

case class KeywordFieldConf(
  field: Option[String] = None
) extends IndexFieldConf {
  override def fieldType: String = "keyword"

  override def fieldDefinition(alias: String): FieldDefinition =
    mappings.KeywordField(
      name = field.getOrElse(alias)
    )
}

case class DateFieldConf(
  field: Option[String] = None
) extends IndexFieldConf {
  override def fieldType: String = "date"
}

case class BooleanFieldConf(
  field: Option[String] = None
) extends IndexFieldConf {
  override def fieldType: String = "boolean"
}

case class LongFieldConf(
  field: Option[String] = None
) extends IndexFieldConf {
  override def fieldType: String = "long"
}

case class IntegerFieldConf(
  field: Option[String] = None
) extends IndexFieldConf {
  override def fieldType: String = "integer"
}

case class ShortFieldConf(
  field: Option[String] = None
) extends IndexFieldConf {
  override def fieldType: String = "short"
}

case class ByteFieldConf(
  field: Option[String] = None
) extends IndexFieldConf {
  override def fieldType: String = "byte"
}

case class DoubleFieldConf(
  field: Option[String] = None
) extends IndexFieldConf {
  override def fieldType: String = "double"
}

case class FloatFieldConf(
  field: Option[String] = None
) extends IndexFieldConf {
  override def fieldType: String = "float"
}

object IndexFieldConf {
  implicit val confHint = new FieldCoproductHint[IndexFieldConf]("type") {
    override def fieldValue(name: String) = name.dropRight("FieldConf".length).toLowerCase
  }
}
