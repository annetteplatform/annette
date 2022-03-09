package biz.lobachev.annette.camunda.impl

import akka.Done
import biz.lobachev.annette.camunda.api.runtime.{
  DeleteProcessInstancePayload,
  ProcessInstance,
  ProcessInstanceFindQuery,
  ProcessInstanceFindResult,
  StartProcessInstancePayload,
  SubmitStartFormPayload
}
import biz.lobachev.annette.camunda.api.{
  BPMEngineError,
  CamundaClient,
  InvalidVariableValue,
  ProcessDefinitionNotFoundById,
  ProcessDefinitionNotFoundByKey,
  ProcessInstanceNotFound,
  RuntimeService
}
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSResponse

import scala.concurrent.{ExecutionContext, Future}

class RuntimeServiceImpl(client: CamundaClient)(implicit val ec: ExecutionContext) extends RuntimeService {

  /**
   * Instantiates a given process definition. Process variables and business key may be supplied in the payload.
   *
   * @param id      The id of the process definition to be retrieved.
   * @param payload process instance parameters
   * @return
   */
  override def startProcessInstanceById(id: String, payload: StartProcessInstancePayload): Future[ProcessInstance] =
    for {
      response <- client
                    .request(s"/process-definition/${id}/start")
                    .addHttpHeaders("Accept" -> "application/json")
                    .post(
                      Json.toJson(payload)
                    )
    } yield processResponseWithId(id, response)

  /**
   * Instantiates a given process definition. Process variables and business key may be supplied in the payload.
   *
   * @param key     The key of the process definition (the latest version thereof) to be retrieved.
   * @param payload process instance parameters
   * @return
   */
  override def startProcessInstanceByKey(key: String, payload: StartProcessInstancePayload): Future[ProcessInstance] =
    for {
      response <- client
                    .request(s"/process-definition/key/${key}/start")
                    .addHttpHeaders("Accept" -> "application/json")
                    .post(
                      Json.toJson(payload)
                    )
    } yield processResponseWithKey(key, response)

  /**
   * Starts a process instance using a set of process variables and the business key. If the start event has
   * Form Field Metadata defined, the process engine will perform backend validation for any form fields
   * which have validators defined.
   *
   * @param id
   * @param payload
   * @return
   */
  override def submitStartFormById(id: String, payload: SubmitStartFormPayload): Future[ProcessInstance] =
    for {
      response <- client
                    .request(s"/process-definition/${id}/submit-form")
                    .addHttpHeaders("Accept" -> "application/json")
                    .post(
                      Json.toJson(payload)
                    )
    } yield processResponseWithId(id, response)

  /**
   * Starts a process instance using a set of process variables and the business key. If the start event has
   * Form Field Metadata defined, the process engine will perform backend validation for any form fields
   * which have validators defined.
   *
   * @param key
   * @param payload
   * @return
   */
  override def submitStartFormByKey(key: String, payload: SubmitStartFormPayload): Future[ProcessInstance] =
    for {
      response <- client
                    .request(s"/process-definition/key/${key}/submit-form")
                    .addHttpHeaders("Accept" -> "application/json")
                    .post(
                      Json.toJson(payload)
                    )
    } yield processResponseWithKey(key, response)

  private def processResponseWithId(id: String, response: WSResponse) =
    response.status match {
      case 200 =>
        println(Json.prettyPrint(response.body[JsValue]))
        response.body[JsValue].as[ProcessInstance]
      // The instance could not be created due to an invalid variable value, for example if the value
      // could not be parsed to an Integer value or the passed variable type is not supported.
      case 400 =>
        throw InvalidVariableValue(
          (response.body[JsValue] \ "type").as[String],
          (response.body[JsValue] \ "message").as[String],
          response.body[JsValue].toString
        )
      // The instance could not be created due to a non existing process definition key.
      case 404 =>
        throw ProcessDefinitionNotFoundById(
          id,
          (response.body[JsValue] \ "message").as[String],
          response.body[JsValue].toString
        )
      // The instance could not be created successfully.
      case _   =>
        throw BPMEngineError(
          (response.body[JsValue] \ "type").as[String],
          (response.body[JsValue] \ "message").as[String],
          response.body[JsValue].toString
        )
    }

  private def processResponseWithKey(key: String, response: WSResponse) =
    response.status match {
      case 200 =>
        println(Json.prettyPrint(response.body[JsValue]))
        response.body[JsValue].as[ProcessInstance]
      // The instance could not be created due to an invalid variable value, for example if the value
      // could not be parsed to an Integer value or the passed variable type is not supported.
      case 400 =>
        throw InvalidVariableValue(
          (response.body[JsValue] \ "type").as[String],
          (response.body[JsValue] \ "message").as[String],
          response.body[JsValue].toString
        )
      // The instance could not be created due to a non existing process definition key.
      case 404 =>
        throw ProcessDefinitionNotFoundByKey(
          key,
          (response.body[JsValue] \ "message").as[String],
          response.body[JsValue].toString
        )
      // The instance could not be created successfully.
      case _   =>
        throw BPMEngineError(
          (response.body[JsValue] \ "type").as[String],
          (response.body[JsValue] \ "message").as[String],
          response.body[JsValue].toString
        )
    }

  /** Deletes a running process instance by id. */
  override def deleteProcessInstance(payload: DeleteProcessInstancePayload): Future[Done] = {
    val params: Seq[(String, String)] = Seq(
      payload.skipCustomListeners.map(skipCustomListeners => "skipCustomListeners" -> skipCustomListeners.toString),
      payload.skipIoMappings.map(skipIoMappings => "skipIoMappings" -> skipIoMappings.toString),
      payload.skipSubprocesses.map(skipSubprocesses => "skipSubprocesses" -> skipSubprocesses.toString),
      payload.failIfNotExists.map(failIfNotExists => "failIfNotExists" -> failIfNotExists.toString)
    ).flatten
    for {
      response <- client
                    .request(s"/process-instance/${payload.id}")
                    .addHttpHeaders("Accept" -> "application/json")
                    .withQueryStringParameters(params: _*)
                    .delete()

    } yield response.status match {
      case 204 =>
        Done
      case 404 =>
        val json = response.body[JsValue]
        throw ProcessInstanceNotFound(
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

  /** Retrieves a process instance by id, according to the ProcessInstance interface in the engine. */
  override def getProcessInstanceById(id: String): Future[ProcessInstance] =
    for {
      response <- client
                    .request(s"/process-instance/$id")
                    .addHttpHeaders("Accept" -> "application/json")
                    .get()

    } yield response.status match {
      case 200 =>
        response.body[JsValue].as[ProcessInstance]
      case 404 =>
        val json = response.body[JsValue]
        throw ProcessInstanceNotFound(
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

  /**
   * Queries for process instances that fulfill given parameters. Parameters may be static as well
   * as dynamic runtime properties of process instances.
   *
   * @param query
   * @return
   */
  override def findProcessInstances(query: ProcessInstanceFindQuery): Future[ProcessInstanceFindResult] = {
    val params: Seq[(String, String)] = Seq(
      query.processInstanceIds.map(r => "processInstanceIds" -> r.mkString(",")),
      query.businessKey.map(r => "businessKey" -> r),
      query.businessKeyLike.map(r => "businessKeyLike" -> r),
      query.caseInstanceId.map(r => "caseInstanceId" -> r),
      query.processDefinitionId.map(r => "processDefinitionId" -> r),
      query.processDefinitionKey.map(r => "processDefinitionKey" -> r),
      query.processDefinitionKeyIn.map(r => "processDefinitionKeyIn" -> r.mkString(",")),
      query.processDefinitionKeyNotIn.map(r => "processDefinitionKeyNotIn" -> r.mkString(",")),
      query.deploymentId.map(r => "deploymentId" -> r),
      query.active.map(r => "active" -> r.toString),
      query.suspended.map(r => "suspended" -> r.toString),
      query.activityIdIn.map(r => "activityIdIn" -> r.mkString(",")),
      query.rootProcessInstances.map(r => "rootProcessInstances" -> r.toString),
      query.leafProcessInstances.map(r => "leafProcessInstances" -> r.toString),
      query.variables.map(r => "variables" -> r.mkString(",")),
      query.variableNamesIgnoreCase.map(r => "variableNamesIgnoreCase" -> r.toString),
      query.variableValuesIgnoreCase.map(r => "variableValuesIgnoreCase" -> r.toString),
      query.sortBy.map(r => "sortBy" -> r),
      query.sortOrder.map(r => "sortOrder" -> r),
      query.firstResult.map(r => "firstResult" -> r.toString),
      query.maxResults.map(r => "maxResults" -> r.toString)
    ).flatten
    for {
      response <- client
                    .request("/process-instance")
                    .addHttpHeaders("Accept" -> "application/json")
                    .withQueryStringParameters(params: _*)
                    .get()
      count <- client
                 .request("/process-instance/count")
                 .addHttpHeaders("Accept" -> "application/json")
                 .withQueryStringParameters(params: _*)
                 .get()
    } yield response.status match {
      case 200 =>
        ProcessInstanceFindResult(
          total = (count.json \ "count").as[Long],
          response.body[JsValue].as[Seq[ProcessInstance]]
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
