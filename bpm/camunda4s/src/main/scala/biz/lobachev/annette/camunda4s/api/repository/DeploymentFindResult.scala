package biz.lobachev.annette.camunda4s.api.repository

import play.api.libs.json.Json

case class DeploymentFindResult(
  total: Long,
  hits: Seq[Deployment]
)

object DeploymentFindResult {
  implicit val format = Json.format[DeploymentFindResult]
}
