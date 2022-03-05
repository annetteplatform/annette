package biz.lobachev.annette.camunda4s.api.repository

import play.api.libs.json.Json

case class ProcessDefinitionFindResult(
  total: Long,                 // total items in query
  hits: Seq[ProcessDefinition] // results of search
)

object ProcessDefinitionFindResult {
  implicit val format = Json.format[ProcessDefinitionFindResult]
}
