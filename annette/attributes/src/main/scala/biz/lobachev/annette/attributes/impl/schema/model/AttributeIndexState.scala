package biz.lobachev.annette.attributes.impl.schema.model

import biz.lobachev.annette.attributes.api.attribute_def.AttributeId
import biz.lobachev.annette.attributes.api.schema._
import biz.lobachev.annette.attributes.impl.schema.SchemaEntity
import io.scalaland.chimney.dsl._
import play.api.libs.json.{Json, JsonConfiguration, JsonNaming}

sealed trait AttributeIndexState {
  val aliasNo: Int

  def toAttributeIndex(attributeId: AttributeId): AttributeIndex
}

case class TextIndexState(
  aliasNo: Int,
  fielddata: Boolean = false,
  keyword: Boolean = false,
  analyzer: Option[AnalyzerId] = None,
  searchAnalyzer: Option[AnalyzerId] = None,
  searchQuoteAnalyzer: Option[AnalyzerId] = None
) extends AttributeIndexState {
  override def toAttributeIndex(attributeId: AttributeId): AttributeIndex =
    this
      .into[TextIndex]
      .withFieldConst(_.alias, SchemaEntity.alias(attributeId, this.aliasNo))
      .transform
}

case class KeywordIndexState(aliasNo: Int) extends AttributeIndexState {
  override def toAttributeIndex(attributeId: AttributeId): AttributeIndex =
    this
      .into[KeywordIndex]
      .withFieldConst(_.alias, SchemaEntity.alias(attributeId, this.aliasNo))
      .transform
}

case class BooleanIndexState(aliasNo: Int) extends AttributeIndexState {
  override def toAttributeIndex(attributeId: AttributeId): AttributeIndex =
    this
      .into[BooleanIndex]
      .withFieldConst(_.alias, SchemaEntity.alias(attributeId, this.aliasNo))
      .transform
}

case class LongIndexState(aliasNo: Int) extends AttributeIndexState {
  override def toAttributeIndex(attributeId: AttributeId): AttributeIndex =
    this
      .into[LongIndex]
      .withFieldConst(_.alias, SchemaEntity.alias(attributeId, this.aliasNo))
      .transform
}

case class DoubleIndexState(aliasNo: Int) extends AttributeIndexState {
  override def toAttributeIndex(attributeId: AttributeId): AttributeIndex =
    this
      .into[DoubleIndex]
      .withFieldConst(_.alias, SchemaEntity.alias(attributeId, this.aliasNo))
      .transform
}

case class OffsetDateTimeIndexState(aliasNo: Int) extends AttributeIndexState {
  override def toAttributeIndex(attributeId: AttributeId): AttributeIndex =
    this
      .into[OffsetDateTimeIndex]
      .withFieldConst(_.alias, SchemaEntity.alias(attributeId, this.aliasNo))
      .transform
}

case class LocalTimeIndexState(aliasNo: Int) extends AttributeIndexState {
  override def toAttributeIndex(attributeId: AttributeId): AttributeIndex =
    this
      .into[LocalTimeIndex]
      .withFieldConst(_.alias, SchemaEntity.alias(attributeId, this.aliasNo))
      .transform
}

case class LocalDateIndexState(aliasNo: Int) extends AttributeIndexState {
  override def toAttributeIndex(attributeId: AttributeId): AttributeIndex =
    this
      .into[LocalDateIndex]
      .withFieldConst(_.alias, SchemaEntity.alias(attributeId, this.aliasNo))
      .transform
}

case class JSONIndexState(aliasNo: Int) extends AttributeIndexState {
  override def toAttributeIndex(attributeId: AttributeId): AttributeIndex =
    this
      .into[JSONIndex]
      .withFieldConst(_.alias, SchemaEntity.alias(attributeId, this.aliasNo))
      .transform
}

object TextIndexState {
  implicit val format = Json.format[TextIndexState]
}

object KeywordIndexState {
  implicit val format = Json.format[KeywordIndexState]
}

object BooleanIndexState {
  implicit val format = Json.format[BooleanIndexState]
}

object LongIndexState {
  implicit val format = Json.format[LongIndexState]
}

object DoubleIndexState {
  implicit val format = Json.format[DoubleIndexState]
}

object OffsetDateTimeIndexState {
  implicit val format = Json.format[OffsetDateTimeIndexState]
}

object LocalTimeIndexState {
  implicit val format = Json.format[LocalTimeIndexState]
}

object LocalDateIndexState {
  implicit val format = Json.format[LocalDateIndexState]
}

object JSONIndexState {
  implicit val format = Json.format[JSONIndexState]
}

object AttributeIndexState {
  implicit val config = JsonConfiguration(
    discriminator = "type",
    typeNaming = JsonNaming { fullName =>
      fullName.split("\\.").toSeq.last match {
        case "TextIndexState"           => "text"
        case "KeywordIndexState"        => "keyword"
        case "BooleanIndexState"        => "boolean"
        case "LongIndexState"           => "long"
        case "DoubleIndexState"         => "double"
        case "OffsetDateTimeIndexState" => "offsetDateTime"
        case "LocalTimeIndexState"      => "localTime"
        case "LocalDateIndexState"      => "localDate"
        case "JSONIndexState"           => "json"
      }
    }
  )
  implicit val format = Json.format[AttributeIndexState]

  def from(prepared: PreparedAttributeIndex, aliasNo: Int): AttributeIndexState =
    prepared match {
      case index: PreparedTextIndex    =>
        index
          .into[TextIndexState]
          .withFieldConst(_.aliasNo, aliasNo)
          .transform
      case PreparedKeywordIndex        => KeywordIndexState(aliasNo)
      case PreparedBooleanIndex        => BooleanIndexState(aliasNo)
      case PreparedLongIndex           => LongIndexState(aliasNo)
      case PreparedDoubleIndex         => DoubleIndexState(aliasNo)
      case PreparedOffsetDateTimeIndex => OffsetDateTimeIndexState(aliasNo)
      case PreparedLocalTimeIndex      => LocalTimeIndexState(aliasNo)
      case PreparedLocalDateIndex      => LocalDateIndexState(aliasNo)
      case PreparedJSONIndex           => JSONIndexState(aliasNo)
    }
}
