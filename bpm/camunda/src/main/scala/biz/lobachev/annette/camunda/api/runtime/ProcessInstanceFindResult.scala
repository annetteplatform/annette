package biz.lobachev.annette.camunda.api.runtime

import play.api.libs.json.Json

case class ProcessInstanceFindResult(
  total: Long,
  hits: Seq[ProcessInstance]
)

object ProcessInstanceFindResult {
  implicit val format = Json.format[ProcessInstanceFindResult]
}
