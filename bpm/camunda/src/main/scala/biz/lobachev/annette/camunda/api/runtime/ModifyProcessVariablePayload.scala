package biz.lobachev.annette.camunda.api.runtime

import biz.lobachev.annette.camunda.api.common.VariableValue
import play.api.libs.json.Json

case class ModifyProcessVariablePayload(
  modifications: Option[Map[String, VariableValue]] = None,
  deletions: Option[Seq[String]] = None
)

object ModifyProcessVariablePayload {
  implicit val format = Json.format[ModifyProcessVariablePayload]
}
