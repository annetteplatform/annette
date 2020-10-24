package biz.lobachev.annette.attributes.api.schema

import play.api.libs.json.{Json, JsonConfiguration, JsonNaming}

sealed trait AttributeIndex {
  val alias: String
}

case class TextIndex(
  alias: String,
  fielddata: Boolean = false,
  keyword: Boolean = false,
  analyzer: Option[AnalyzerId] = None,
  searchAnalyzer: Option[AnalyzerId] = None,
  searchQuoteAnalyzer: Option[AnalyzerId] = None
) extends AttributeIndex

case class KeywordIndex(alias: String)        extends AttributeIndex
case class BooleanIndex(alias: String)        extends AttributeIndex
case class LongIndex(alias: String)           extends AttributeIndex
case class DoubleIndex(alias: String)         extends AttributeIndex
case class OffsetDateTimeIndex(alias: String) extends AttributeIndex
case class LocalTimeIndex(alias: String)      extends AttributeIndex
case class LocalDateIndex(alias: String)      extends AttributeIndex
case class JSONIndex(alias: String)           extends AttributeIndex

object TextIndex {
  implicit val format = Json.format[TextIndex]
}

object KeywordIndex {
  implicit val format = Json.format[KeywordIndex]
}

object BooleanIndex {
  implicit val format = Json.format[BooleanIndex]
}

object LongIndex {
  implicit val format = Json.format[LongIndex]
}

object DoubleIndex {
  implicit val format = Json.format[DoubleIndex]
}

object OffsetDateTimeIndex {
  implicit val format = Json.format[OffsetDateTimeIndex]
}

object LocalTimeIndex {
  implicit val format = Json.format[LocalTimeIndex]
}

object LocalDateIndex {
  implicit val format = Json.format[LocalDateIndex]
}

object JSONIndex {
  implicit val format = Json.format[JSONIndex]
}

object AttributeIndex {
  implicit val config = JsonConfiguration(
    discriminator = "type",
    typeNaming = JsonNaming { fullName =>
      fullName.split("\\.").toSeq.last match {
        case "TextIndex"           => "text"
        case "KeywordIndex"        => "keyword"
        case "BooleanIndex"        => "boolean"
        case "LongIndex"           => "long"
        case "DoubleIndex"         => "double"
        case "OffsetDateTimeIndex" => "offsetDateTime"
        case "LocalTimeIndex"      => "localTime"
        case "LocalDateIndex"      => "localDate"
        case "JSONIndex"           => "json"
      }
    }
  )
  implicit val format = Json.format[AttributeIndex]
}
