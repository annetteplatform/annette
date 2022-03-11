package biz.lobachev.annette.camunda.api.task

import play.api.libs.json.Json

case class TaskSorting(
  sortBy: String,
  sortOrder: String,
  parameters: Option[VariableSortingParameters]
)
object TaskSorting {
  implicit val format = Json.format[TaskSorting]
}
