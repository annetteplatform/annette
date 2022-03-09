package biz.lobachev.annette.camunda.api.runtime

import biz.lobachev.annette.camunda.api.common.VariableValue
import play.api.libs.json.Json

case class StartProcessInstancePayload(
  variables: Option[Map[String, VariableValue]] = None,
  businessKey: Option[String] = None,
  caseInstanceId: Option[String] = None,
  skipCustomListeners: Option[Boolean] = None,
  skipIoMappings: Option[Boolean] = None,
  withVariablesInReturn: Option[Boolean] = None
)

object StartProcessInstancePayload {
  implicit val format = Json.format[StartProcessInstancePayload]
}
