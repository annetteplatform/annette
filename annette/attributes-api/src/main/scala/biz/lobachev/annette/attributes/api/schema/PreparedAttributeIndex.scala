package biz.lobachev.annette.attributes.api.schema

import play.api.libs.json.{Json, JsonConfiguration, JsonNaming}

sealed trait PreparedAttributeIndex {}

case class PreparedTextIndex(
  fielddata: Boolean = false,
  keyword: Boolean = false,
  analyzer: Option[AnalyzerId] = None,
  searchAnalyzer: Option[AnalyzerId] = None,
  searchQuoteAnalyzer: Option[AnalyzerId] = None
) extends PreparedAttributeIndex

object PreparedTextIndex {
  implicit val format = Json.format[PreparedTextIndex]
}

object PreparedKeywordIndex extends PreparedAttributeIndex {
  implicit val format = Json.format[PreparedKeywordIndex.type]
}
object PreparedBooleanIndex extends PreparedAttributeIndex {
  implicit val format = Json.format[PreparedBooleanIndex.type]
}

object PreparedLongIndex extends PreparedAttributeIndex {
  implicit val format = Json.format[PreparedLongIndex.type]
}

object PreparedDoubleIndex extends PreparedAttributeIndex {
  implicit val format = Json.format[PreparedDoubleIndex.type]
}

object PreparedOffsetDateTimeIndex extends PreparedAttributeIndex {
  implicit val format = Json.format[PreparedOffsetDateTimeIndex.type]
}

object PreparedLocalTimeIndex extends PreparedAttributeIndex {
  implicit val format = Json.format[PreparedLocalTimeIndex.type]
}

object PreparedLocalDateIndex extends PreparedAttributeIndex {
  implicit val format = Json.format[PreparedLocalDateIndex.type]
}

object PreparedJSONIndex extends PreparedAttributeIndex {
  implicit val format = Json.format[PreparedJSONIndex.type]
}

object PreparedAttributeIndex {
  implicit val config = JsonConfiguration(
    discriminator = "type",
    typeNaming = JsonNaming { fullName =>
      fullName.split("\\.").toSeq.last match {
        case "PreparedTextIndex"           => "text"
        case "PreparedKeywordIndex"        => "keyword"
        case "PreparedBooleanIndex"        => "boolean"
        case "PreparedLongIndex"           => "long"
        case "PreparedDoubleIndex"         => "double"
        case "PreparedOffsetDateTimeIndex" => "OffsetDateTime"
        case "PreparedLocalTimeIndex"      => "LocalTime"
        case "PreparedLocalDateIndex"      => "LocalDate"
        case "PreparedJSONIndex"           => "json"
      }
    }
  )
  implicit val format = Json.format[PreparedAttributeIndex]
}
