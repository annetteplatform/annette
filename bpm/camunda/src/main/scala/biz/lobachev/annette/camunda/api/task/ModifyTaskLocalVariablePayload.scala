package biz.lobachev.annette.camunda.api.task

import biz.lobachev.annette.camunda.api.VariableValues
import play.api.libs.json.Json

case class ModifyTaskLocalVariablePayload(
  modifications: Option[VariableValues] = None,
  deletions: Option[Seq[String]] = None
)

object ModifyTaskLocalVariablePayload {
  implicit val format = Json.format[ModifyTaskLocalVariablePayload]
}
