package biz.lobachev.annette.camunda4s.models

import play.api.libs.json.Json

case class ProcessDefinition(
  id: String,
  key: String,
  category: String,
  description: Option[String],
  name: Option[String],
  version: Int,
  resource: String,
  deploymentId: String,
  diagram: Option[String],
  suspended: Boolean,
  tenantId: Option[String],
  versionTag: Option[String],
  historyTimeToLive: Option[Int],
  startableInTasklist: Boolean
)

object ProcessDefinition {
  implicit val format = Json.format[ProcessDefinition]
}
