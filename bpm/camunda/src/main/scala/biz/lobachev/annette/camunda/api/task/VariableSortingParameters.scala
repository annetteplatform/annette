package biz.lobachev.annette.camunda.api.task

import play.api.libs.json.Json

case class VariableSortingParameters(
  variable: String,
  `type`: String
)
object VariableSortingParameters {
  implicit val format = Json.format[VariableSortingParameters]
}
