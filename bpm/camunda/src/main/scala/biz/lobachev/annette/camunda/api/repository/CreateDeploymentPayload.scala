package biz.lobachev.annette.camunda.api.repository

import play.api.libs.json.Json

import java.time.OffsetDateTime

/**
 * @param xml The string data to create the deployment resource.
 * @param deploymentName The name for the deployment to be created.
 * @param enableDuplicateFiltering A flag indicating whether the process engine should perform duplicate checking for the deployment or not. This allows you to check if a deployment with the same name and the same resouces already exists and if true, not create a new deployment but instead return the existing deployment. The default value is false.
 * @param deployChangedOnly A flag indicating whether the process engine should perform duplicate checking on a per-resource basis. If set to true, only those resources that have actually changed are deployed. Checks are made against resources included previous deployments of the same name and only against the latest versions of those resources. If set to true, the option enable-duplicate-filtering is overridden and set to true.
 * @param deploymentSource The source for the deployment to be created.
 * @param deploymentActivationTime Sets the date on which the process definitions contained in this deployment will be activated. This means that all process definitions will be deployed as usual, but they will be suspended from the start until the given activation date. By default*, the date must have the format yyyy-MM-dd'T'HH:mm:ss.SSSZ, e.g., 2013-01-23T14:42:45.000+0200.
 * @param tenantId The tenant id for the deployment to be created.
 */
case class CreateDeploymentPayload(
  xml: String,
  deploymentName: Option[String] = None,
  enableDuplicateFiltering: Option[Boolean] = None,
  deployChangedOnly: Option[Boolean] = None,
  deploymentSource: Option[String] = None,
  deploymentActivationTime: Option[OffsetDateTime] = None,
  tenantId: Option[String] = None
)

object CreateDeploymentPayload {
  implicit val format = Json.format[CreateDeploymentPayload]
}
