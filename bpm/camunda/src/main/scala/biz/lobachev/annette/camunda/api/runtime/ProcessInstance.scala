package biz.lobachev.annette.camunda.api.runtime

import biz.lobachev.annette.camunda.api.common.VariableValue
import play.api.libs.json.Json

case class ProcessInstance(
  id: String,
  definitionId: String,
  businessKey: Option[String] = None,
  caseInstanceId: Option[String] = None,
  tenantId: Option[String] = None,
  ended: Boolean,
  suspended: Boolean,
  variables: Option[Map[String, VariableValue]] = None
)

object ProcessInstance {
  implicit val format = Json.format[ProcessInstance]
}
