package biz.lobachev.annette.microservice_core.attribute.dao

import biz.lobachev.annette.core.attribute.{
  AttributeMetadata,
  AttributeValues,
  BooleanAttributeMetadata,
  DecimalAttributeMetadata,
  DoubleAttributeMetadata,
  IntAttributeMetadata,
  JsonAttributeMetadata,
  LocalDateAttributeMetadata,
  LocalTimeAttributeMetadata,
  OffsetDateTimeAttributeMetadata,
  StringAttributeMetadata
}

import java.time.OffsetDateTime

trait AttributeIndexing {

  private def convertType(value: String, attrMeta: AttributeMetadata): Any =
    attrMeta match {
      case _: StringAttributeMetadata         => value
      case _: BooleanAttributeMetadata        => value == "true"
      case _: IntAttributeMetadata            => value.toInt
      case _: DoubleAttributeMetadata         => value.toDouble
      case _: DecimalAttributeMetadata        => BigDecimal(value)
      case _: LocalDateAttributeMetadata      => value
      case _: LocalTimeAttributeMetadata      => value
      case _: OffsetDateTimeAttributeMetadata => OffsetDateTime.parse(value)
      case _: JsonAttributeMetadata           => value
    }

  def convertAttributes(attributes: AttributeValues, metadata: Map[String, AttributeMetadata]): List[(String, Any)] =
    (for {
      attribute -> value <- attributes
    } yield metadata.get(attribute).flatMap { attrMeta =>
      attrMeta.index.map { indexAlias =>
        if (value.length == 0) indexAlias -> null
        else indexAlias                   -> convertType(value, attrMeta)
      }

    }).flatten.toList

}
