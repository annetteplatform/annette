package biz.lobachev.annette.attributes.api.attribute

import play.api.libs.json.{Json, JsonConfiguration, JsonNaming}

sealed trait AttributeType {}

case class StringAttribute(
  subtype: Option[String] = None,
  referenceId: Option[String] = None
) extends AttributeType

object StringAttribute {
  implicit val format = Json.format[StringAttribute]
}

object BooleanAttribute extends AttributeType {
  implicit val format = Json.format[BooleanAttribute.type]
}

object LongAttribute extends AttributeType {
  implicit val format = Json.format[LongAttribute.type]
}

object DoubleAttribute extends AttributeType {
  implicit val format = Json.format[DoubleAttribute.type]
}

object OffsetDateTimeAttribute extends AttributeType {
  implicit val format = Json.format[OffsetDateTimeAttribute.type]
}

object LocalTimeAttribute extends AttributeType {
  implicit val format = Json.format[LocalTimeAttribute.type]
}

object LocalDateAttribute extends AttributeType {
  implicit val format = Json.format[LocalDateAttribute.type]
}

object JSONAttribute extends AttributeType {
  implicit val format = Json.format[JSONAttribute.type]
}

object AttributeType {
  implicit val config = JsonConfiguration(
    discriminator = "type",
    typeNaming = JsonNaming { fullName =>
      fullName.split("\\.").toSeq.last match {
        case "StringAttribute"         => AttributeValueType.String.toString
        case "BooleanAttribute"        => AttributeValueType.Boolean.toString
        case "LongAttribute"           => AttributeValueType.Long.toString
        case "DoubleAttribute"         => AttributeValueType.Double.toString
        case "OffsetDateTimeAttribute" => AttributeValueType.OffsetDateTime.toString
        case "LocalTimeAttribute"      => AttributeValueType.LocalTime.toString
        case "LocalDateAttribute"      => AttributeValueType.LocalDate.toString
        case "JSONAttribute"           => AttributeValueType.JSON.toString
      }
    }
  )
  implicit val format = Json.format[AttributeType]
}
