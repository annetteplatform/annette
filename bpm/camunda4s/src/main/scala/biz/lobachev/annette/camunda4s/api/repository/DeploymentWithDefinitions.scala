package biz.lobachev.annette.camunda4s.api.repository

import play.api.libs.json.{JsObject, Json}

/**
 * @param id The id of the deployment.
 * @param name The name of the deployment.
 * @param source The source of the deployment.
 * @param tenantId The tenant id of the deployment.
 * @param deploymentTime The time when the deployment was created.
 * @param deployedProcessDefinitions A JSON Object containing a property for each of the process definitions, which are successfully deployed with that deployment. The key is the process definition id, the value is a JSON Object corresponding to the process definition, which is defined in the Process Definition resource.
 * @param deployedCaseDefinitions A JSON Object containing a property for each of the case definitions, which are successfully deployed with that deployment. The key is the case definition id, the value is a JSON Object corresponding to the case definition, which is defined in the Case Definition resource.
 * @param deployedDecisionDefinitions A JSON Object containing a property for each of the decision definitions, which are successfully deployed with that deployment. The key is the decision definition id, the value is a JSON Object corresponding to the decision definition, which is defined in the Decision Definition resource.
 * @param deployedDecisionRequirementsDefinitions A JSON Object containing a property for each of the decision requirements definitions, which are successfully deployed with that deployment. The key is the decision requirements definition id, the value is a JSON Object corresponding to the decision requirements definition, which is defined in the Decision Requirements Definition resource.
 */
case class DeploymentWithDefinitions(
  id: String,
  name: Option[String],
  source: Option[String],
  tenantId: Option[String],
  deploymentTime: Option[String],
  deployedProcessDefinitions: Option[Map[String, ProcessDefinition]],
  deployedCaseDefinitions: Option[Map[String, JsObject]],
  deployedDecisionDefinitions: Option[Map[String, JsObject]],
  deployedDecisionRequirementsDefinitions: Option[Map[String, JsObject]]
)

object DeploymentWithDefinitions {
  implicit val format = Json.format[DeploymentWithDefinitions]
}
