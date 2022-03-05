package biz.lobachev.annette.camunda

import biz.lobachev.annette.camunda.api.CamundaClient
import biz.lobachev.annette.camunda.api.repository.ProcessDefinition
import biz.lobachev.annette.camunda.impl.repository.GetProcessDefinitionsRequest
import play.api.libs.json.JsValue

import scala.concurrent.{ExecutionContext, Future}

class ProcessDefinitionApi(client: CamundaClient)(implicit val ec: ExecutionContext) {

  def getProcessDefinitions(): Future[Seq[ProcessDefinition]] =
    getProcessDefinitions(GetProcessDefinitionsRequest())

  /**
   * Queries for process definitions that fulfill given parameters.
   * @param request request parameters may be the properties of process definitions, such as the name, key or version
   * @return
   */
  def getProcessDefinitions(request: GetProcessDefinitionsRequest): Future[Seq[ProcessDefinition]] = {
    val params: Seq[(String, String)] = Seq(
      request.processDefinitionId.map(r => "processDefinitionId" -> r),
      request.processDefinitionIdIn.map(r => "processDefinitionIdIn" -> r.mkString(",")),
      request.name.map(r => "name" -> r),
      request.nameLike.map(r => "nameLike" -> r),
      request.deploymentId.map(r => "deploymentId" -> r),
      request.deployedAfter.map(r => "deployedAfter" -> r),
      request.deployedAt.map(r => "deployedAt" -> r),
      request.key.map(r => "key" -> r),
      request.keysIn.map(r => "keysIn" -> r.mkString(",")),
      request.keyLike.map(r => "keyLike" -> r),
      request.category.map(r => "category" -> r),
      request.categoryLike.map(r => "categoryLike" -> r),
      request.version.map(r => "version" -> r),
      request.latestVersion.map(r => "latestVersion" -> r),
      request.resourceName.map(r => "resourceName" -> r),
      request.resourceNameLike.map(r => "resourceNameLike" -> r),
      request.startableBy.map(r => "startableBy" -> r),
      request.active.map(r => "active" -> r.toString),
      request.suspended.map(r => "suspended" -> r.toString),
      request.incidentId.map(r => "incidentId" -> r),
      request.incidentType.map(r => "incidentType" -> r),
      request.incidentMessage.map(r => "incidentMessage" -> r),
      request.incidentMessageLike.map(r => "incidentMessageLike" -> r),
      request.tenantIdIn.map(r => "tenantIdIn" -> r),
      request.withoutTenantId.map(r => "withoutTenantId" -> r),
      request.includeProcessDefinitionsWithoutTenantId.map(r => "includeProcessDefinitionsWithoutTenantId" -> r),
      request.versionTag.map(r => "versionTag" -> r),
      request.versionTagLike.map(r => "versionTagLike" -> r),
      request.withoutVersionTag.map(r => "withoutVersionTag" -> r),
      request.startableInTasklist.map(r => "startableInTasklist" -> r),
      request.notStartableInTasklist.map(r => "notStartableInTasklist" -> r),
      request.startablePermissionCheck.map(r => "startablePermissionCheck" -> r),
      request.sortBy.map(r => "sortBy" -> r),
      request.sortOrder.map(r => "sortOrder" -> r),
      request.firstResult.map(r => "firstResult" -> r.toString),
      request.maxResults.map(r => "maxResults" -> r.toString)
    ).flatten

    for {
      response <- client
                    .request("/process-definition")
                    .addHttpHeaders("Accept" -> "application/json")
                    .withQueryStringParameters(params: _*)
                    .get()
    } yield response.body[JsValue].as[Seq[ProcessDefinition]]
  }

  /** Retrieves a process definition by id according to the ProcessDefinition interface in the engine. */
  def getProcessDefinitionById(id: String): Future[ProcessDefinition] =
    for {
      response <- client
                    .request(s"/process-definition/$id")
                    .addHttpHeaders("Accept" -> "application/json")
                    .get()
    } yield response.body[JsValue].as[ProcessDefinition]

  /** Retrieves a process definition by key according to the ProcessDefinition interface in the engine. */
  def getProcessDefinitionByKey(key: String): Future[ProcessDefinition] =
    for {
      response <- client
                    .request(s"/process-definition/key/$key")
                    .addHttpHeaders("Accept" -> "application/json")
                    .get()
    } yield response.body[JsValue].as[ProcessDefinition]
}
