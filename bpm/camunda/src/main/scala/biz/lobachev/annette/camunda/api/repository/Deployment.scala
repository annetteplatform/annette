package biz.lobachev.annette.camunda.api.repository

import play.api.libs.json.Json

/**
 * @param id The id of the deployment.
 * @param name The name of the deployment.
 * @param source The source of the deployment.
 * @param deploymentTime The date and time of the deployment.
 */
case class Deployment(
  id: String,
  name: Option[String] = None,
  source: Option[String] = None,
  deploymentTime: Option[String] = None
)

object Deployment {
  implicit val format = Json.format[Deployment]
}
