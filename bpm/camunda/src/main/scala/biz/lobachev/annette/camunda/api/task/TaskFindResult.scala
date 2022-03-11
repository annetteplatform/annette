package biz.lobachev.annette.camunda.api.task

import play.api.libs.json.Json

case class TaskFindResult(
  total: Long,
  hits: Seq[Task]
)

object TaskFindResult {
  implicit val format = Json.format[TaskFindResult]
}
