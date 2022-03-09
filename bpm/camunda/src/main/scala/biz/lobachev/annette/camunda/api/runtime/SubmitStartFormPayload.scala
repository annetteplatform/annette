package biz.lobachev.annette.camunda.api.runtime

import biz.lobachev.annette.camunda.api.common.VariableValue
import play.api.libs.json.Json

case class SubmitStartFormPayload(
  variables: Option[Map[String, VariableValue]] = None,
  businessKey: Option[String] = None
)

object SubmitStartFormPayload {
  implicit val format = Json.format[SubmitStartFormPayload]
}
