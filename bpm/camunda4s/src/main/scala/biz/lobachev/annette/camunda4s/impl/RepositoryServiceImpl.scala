package biz.lobachev.annette.camunda4s.impl

import akka.stream.scaladsl.Source
import akka.util.ByteString
import akka.{Done, NotUsed}
import biz.lobachev.annette.camunda4s.api.repository._
import biz.lobachev.annette.camunda4s.api.{
  BPMEngineError,
  CamundaClient,
  CreateDeploymentError,
  DeploymentNotFound,
  ProcessDefinitionNotFoundById,
  ProcessDefinitionNotFoundByKey,
  RepositoryService
}
import play.api.libs.json.JsValue
import play.api.mvc.MultipartFormData
import play.api.mvc.MultipartFormData.{DataPart, FilePart}

import scala.concurrent.{ExecutionContext, Future}

class RepositoryServiceImpl(client: CamundaClient)(implicit val ec: ExecutionContext) extends RepositoryService {

  override def createDeployment(payload: CreateDeploymentPayload): Future[DeploymentWithDefinitions] = {
    val parts: Seq[MultipartFormData.Part[Source[ByteString, NotUsed]] with Serializable] = (
      payload.deploymentName.map(deploymentName => DataPart("deployment-name", deploymentName)) ::
        payload.enableDuplicateFiltering.map(enableDuplicateFiltering =>
          DataPart("enable-duplicate-filtering", enableDuplicateFiltering.toString)
        ) ::
        payload.deployChangedOnly.map(deployChangedOnly =>
          DataPart("deploy-changed-only", deployChangedOnly.toString)
        ) ::
        payload.deploymentSource.map(deploymentSource => DataPart("deployment-source", deploymentSource)) ::
        payload.deploymentActivationTime.map(deploymentActivationTime =>
          DataPart("deployment-activation-time", deploymentActivationTime.toString)
        ) ::
        payload.tenantId.map(tenantId => DataPart("tenant-id", tenantId)) ::
        Some(
          FilePart("data", "data.bpmn", Option("application/octet-stream"), Source.single(ByteString(payload.xml)))
        ) :: List()
    ).flatten
    for {
      response <- client
                    .request("/deployment/create")
                    .addHttpHeaders("Accept" -> "application/json")
                    .post(
                      Source(parts)
                    )
    } yield response.status match {
      case 200 =>
        response.body[JsValue].as[DeploymentWithDefinitions]
      case _   =>
        throw CreateDeploymentError(
          (response.body[JsValue] \ "type").as[String],
          (response.body[JsValue] \ "message").as[String],
          response.body[JsValue].toString
        )
    }
  }

  override def deleteDeployment(payload: DeleteDeploymentPayload): Future[Done] = {
    val params: Seq[(String, String)] = Seq(
      payload.cascade.map(cascade => "cascade" -> cascade.toString),
      payload.skipCustomListeners.map(skipCustomListeners => "skipCustomListeners" -> skipCustomListeners.toString),
      payload.skipIoMappings.map(skipIoMappings => "skipIoMappings" -> skipIoMappings.toString)
    ).flatten

    for {
      response <- client
                    .request(s"/deployment/${payload.id}")
                    .addHttpHeaders("Accept" -> "application/json")
                    .withQueryStringParameters(params: _*)
                    .delete()

    } yield response.status match {
      case 204 =>
        Done
      case 404 =>
        val json = response.body[JsValue]
        throw DeploymentNotFound(
          payload.id,
          (json \ "message").as[String],
          json.toString
        )
      case _   =>
        val json = response.body[JsValue]
        throw BPMEngineError(
          (json \ "type").as[String],
          (json \ "message").as[String],
          json.toString
        )
    }
  }

  override def getDeploymentById(id: String): Future[Deployment] =
    for {
      response <- client
                    .request(s"/deployment/$id")
                    .addHttpHeaders("Accept" -> "application/json")
                    .get()

    } yield response.status match {
      case 200 =>
        response.body[JsValue].as[Deployment]
      case 404 =>
        val json = response.body[JsValue]
        throw DeploymentNotFound(
          id,
          (json \ "message").as[String],
          json.toString
        )
      case _   =>
        val json = response.body[JsValue]
        throw BPMEngineError(
          (json \ "type").as[String],
          (json \ "message").as[String],
          json.toString
        )
    }

  override def findDeployments(query: DeploymentFindQuery): Future[DeploymentFindResult] = {
    val params: Seq[(String, String)] = Seq(
      query.id.map(r => "id" -> r),
      query.name.map(r => "name" -> r),
      query.nameLike.map(r => "nameLike" -> r),
      query.source.map(r => "source" -> r),
      query.withoutSource.map(r => "withoutSource" -> r),
      query.tenantIdIn.map(r => "tenantIdIn" -> r.mkString(",")),
      query.withoutTenantId.map(r => "withoutTenantId" -> r.toString),
      query.includeDeploymentsWithoutTenantId.map(r => "includeDeploymentsWithoutTenantId" -> r.toString),
      query.after.map(r => "after" -> r),
      query.before.map(r => "before" -> r),
      query.sortBy.map(r => "sortBy" -> r),
      query.sortOrder.map(r => "sortOrder" -> r),
      query.firstResult.map(r => "firstResult" -> r.toString),
      query.maxResults.map(r => "maxResults" -> r.toString)
    ).flatten

    for {
      response <- client
                    .request("/deployment")
                    .addHttpHeaders("Accept" -> "application/json")
                    .withQueryStringParameters(params: _*)
                    .get()
      count    <- client
                    .request("/deployment/count")
                    .addHttpHeaders("Accept" -> "application/json")
                    .withQueryStringParameters(params: _*)
                    .get()
    } yield response.status match {
      case 200 =>
        DeploymentFindResult(
          total = (count.json \ "count").as[Long],
          response.body[JsValue].as[Seq[Deployment]]
        )
      case _   =>
        throw BPMEngineError(
          (response.body[JsValue] \ "type").as[String],
          (response.body[JsValue] \ "message").as[String],
          response.body[JsValue].toString
        )
    }
  }

  override def deleteProcessDefinition(payload: DeleteProcessDefinitionPayload): Future[Done] = {
    val params: Seq[(String, String)] = Seq(
      payload.cascade.map(cascade => "cascade" -> cascade.toString),
      payload.skipCustomListeners.map(skipCustomListeners => "skipCustomListeners" -> skipCustomListeners.toString),
      payload.skipIoMappings.map(skipIoMappings => "skipIoMappings" -> skipIoMappings.toString)
    ).flatten
    for {
      response <- client
                    .request(s"/process-definition/${payload.id}")
                    .addHttpHeaders("Accept" -> "application/json")
                    .withQueryStringParameters(params: _*)
                    .delete()

    } yield response.status match {
      case 200 =>
        Done
      case 404 =>
        val json = response.body[JsValue]
        throw ProcessDefinitionNotFoundById(
          payload.id,
          (json \ "message").as[String],
          json.toString
        )
      case _   =>
        val json = response.body[JsValue]
        throw BPMEngineError(
          (json \ "type").as[String],
          (json \ "message").as[String],
          json.toString
        )
    }
  }

  /** Retrieves a process definition by id according to the ProcessDefinition interface in the engine. */
  override def getProcessDefinitionById(id: String): Future[ProcessDefinition] =
    for {
      response <- client
                    .request(s"/process-definition/$id")
                    .addHttpHeaders("Accept" -> "application/json")
                    .get()
    } yield response.status match {
      case 200 =>
        response.body[JsValue].as[ProcessDefinition]
      case 404 =>
        val json = response.body[JsValue]
        throw ProcessDefinitionNotFoundById(
          id,
          (json \ "message").as[String],
          json.toString
        )
      case _   =>
        throw BPMEngineError(
          (response.body[JsValue] \ "type").as[String],
          (response.body[JsValue] \ "message").as[String],
          response.body[JsValue].toString
        )
    }

  /** Retrieves a process definition by key according to the ProcessDefinition interface in the engine. */
  override def getProcessDefinitionByKey(key: String): Future[ProcessDefinition] =
    for {
      response <- client
                    .request(s"/process-definition/key/$key")
                    .addHttpHeaders("Accept" -> "application/json")
                    .get()
    } yield response.status match {
      case 200 =>
        response.body[JsValue].as[ProcessDefinition]
      case 404 =>
        val json = response.body[JsValue]
        throw ProcessDefinitionNotFoundByKey(
          key,
          (json \ "message").as[String],
          json.toString
        )
      case _   =>
        throw BPMEngineError(
          (response.body[JsValue] \ "type").as[String],
          (response.body[JsValue] \ "message").as[String],
          response.body[JsValue].toString
        )
    }

  override def findProcessDefinitions(query: ProcessDefinitionFindQuery): Future[ProcessDefinitionFindResult] = {
    val params: Seq[(String, String)] = Seq(
      query.processDefinitionId.map(r => "processDefinitionId" -> r),
      query.processDefinitionIdIn.map(r => "processDefinitionIdIn" -> r.mkString(",")),
      query.name.map(r => "name" -> r),
      query.nameLike.map(r => "nameLike" -> r),
      query.deploymentId.map(r => "deploymentId" -> r),
      query.key.map(r => "key" -> r),
      query.keysIn.map(r => "keysIn" -> r.mkString(",")),
      query.keyLike.map(r => "keyLike" -> r),
      query.version.map(r => "version" -> r),
      query.latestVersion.map(r => "latestVersion" -> r.toString),
      query.resourceName.map(r => "resourceName" -> r),
      query.resourceNameLike.map(r => "resourceNameLike" -> r),
      query.active.map(r => "active" -> r.toString),
      query.suspended.map(r => "suspended" -> r.toString),
      query.versionTag.map(r => "versionTag" -> r),
      query.versionTagLike.map(r => "versionTagLike" -> r),
      query.withoutVersionTag.map(r => "withoutVersionTag" -> r),
      query.sortBy.map(r => "sortBy" -> r),
      query.sortOrder.map(r => "sortOrder" -> r),
      query.firstResult.map(r => "firstResult" -> r.toString),
      query.maxResults.map(r => "maxResults" -> r.toString)
    ).flatten

    for {
      response <- client
                    .request("/process-definition")
                    .addHttpHeaders("Accept" -> "application/json")
                    .withQueryStringParameters(params: _*)
                    .get()
      count    <- client
                    .request("/process-definition/count")
                    .addHttpHeaders("Accept" -> "application/json")
                    .withQueryStringParameters(params: _*)
                    .get()
    } yield response.status match {
      case 200 =>
        ProcessDefinitionFindResult(
          total = (count.json \ "count").as[Long],
          response.body[JsValue].as[Seq[ProcessDefinition]]
        )
      case _   =>
        throw BPMEngineError(
          (response.body[JsValue] \ "type").as[String],
          (response.body[JsValue] \ "message").as[String],
          response.body[JsValue].toString
        )
    }
  }
}
