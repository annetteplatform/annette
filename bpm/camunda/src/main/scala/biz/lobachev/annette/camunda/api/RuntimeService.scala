package biz.lobachev.annette.camunda.api

import akka.Done
import biz.lobachev.annette.camunda.api.runtime.{
  DeleteProcessInstancePayload,
  ProcessInstance,
  ProcessInstanceFindQuery,
  ProcessInstanceFindResult,
  StartProcessInstancePayload,
  SubmitStartFormPayload
}

import scala.concurrent.Future

trait RuntimeService {

  /**
   * Instantiates a given process definition. Process variables and business key may be supplied in the payload.
   * @param id  The id of the process definition to be retrieved.
   * @param payload process instance parameters
   * @return
   */
  def startProcessInstanceById(id: String, payload: StartProcessInstancePayload): Future[ProcessInstance]

  /**
   * Instantiates a given process definition. Process variables and business key may be supplied in the payload.
   * @param key The key of the process definition (the latest version thereof) to be retrieved.
   * @param payload process instance parameters
   * @return
   */
  def startProcessInstanceByKey(key: String, payload: StartProcessInstancePayload): Future[ProcessInstance]

  /**
   * Starts a process instance using a set of process variables and the business key. If the start event has
   * Form Field Metadata defined, the process engine will perform backend validation for any form fields
   * which have validators defined.
   * @param id
   * @param payload
   * @return
   */
  def submitStartFormById(id: String, payload: SubmitStartFormPayload): Future[ProcessInstance]

  /**
   * Starts a process instance using a set of process variables and the business key. If the start event has
   * Form Field Metadata defined, the process engine will perform backend validation for any form fields
   * which have validators defined.
   * @param key
   * @param payload
   * @return
   */
  def submitStartFormByKey(key: String, payload: SubmitStartFormPayload): Future[ProcessInstance]

  /** Deletes a running process instance by id. */
  def deleteProcessInstance(payload: DeleteProcessInstancePayload): Future[Done]

  /** Retrieves a process instance by id, according to the ProcessInstance interface in the engine. */
  def getProcessInstanceById(id: String): Future[ProcessInstance]

  /**
   * Queries for process instances that fulfill given parameters. Parameters may be static as well
   * as dynamic runtime properties of process instances.
   * @param query
   * @return
   */
  def findProcessInstances(query: ProcessInstanceFindQuery): Future[ProcessInstanceFindResult]
}
