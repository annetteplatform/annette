package biz.lobachev.annette.camunda.api.task

import play.api.libs.json.Json

case class VariableExpression(
  name: String,
  value: String,
  operator: String
)

object VariableExpression {
  implicit val format = Json.format[VariableExpression]
}
