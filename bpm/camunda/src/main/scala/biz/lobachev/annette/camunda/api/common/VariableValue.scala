package biz.lobachev.annette.camunda.api.common

import play.api.libs.json.{Format, Json, JsonConfiguration, JsonNaming}

/**
 * @param objectTypeName A string representation of the object's type name.
 * @param serializationDataFormat The serialization format used to store the variable.
 * @param transient Indicates whether the variable should be transient or not.
 */
case class ValueInfo(
  objectTypeName: Option[String] = None,
  serializationDataFormat: Option[String] = None,
  transient: Option[Boolean] = None
)

object ValueInfo {
  implicit val format = Json.format[ValueInfo]
}

sealed trait VariableValue

case class StringValue(value: String)                                    extends VariableValue
case class BooleanValue(value: Boolean)                                  extends VariableValue
case class LongValue(value: Long)                                        extends VariableValue
case class IntegerValue(value: Int)                                      extends VariableValue
case class DoubleValue(value: Double)                                    extends VariableValue
case class DateValue(value: String)                                      extends VariableValue
case class JsonValue(value: String, valueInfo: Option[ValueInfo] = None) extends VariableValue
case class XmlValue(value: String, valueInfo: Option[ValueInfo] = None)  extends VariableValue
case class ObjectValue(value: String, valueInfo: ValueInfo)              extends VariableValue

object StringValue  { implicit val format = Json.format[StringValue]  }
object BooleanValue { implicit val format = Json.format[BooleanValue] }
object LongValue    { implicit val format = Json.format[LongValue]    }
object IntegerValue { implicit val format = Json.format[IntegerValue] }
object DoubleValue  { implicit val format = Json.format[DoubleValue]  }
object DateValue    { implicit val format = Json.format[DateValue]    }
object JsonValue    { implicit val format = Json.format[JsonValue]    }
object XmlValue     { implicit val format = Json.format[XmlValue]     }
object ObjectValue  { implicit val format = Json.format[ObjectValue]  }

object VariableValue {
  implicit val config                        = JsonConfiguration(
    discriminator = "type",
    typeNaming = JsonNaming { fullName =>
      fullName.split("\\.").toSeq.last.dropRight("Value".length)
    }
  )
  implicit val format: Format[VariableValue] = Json.format
}
