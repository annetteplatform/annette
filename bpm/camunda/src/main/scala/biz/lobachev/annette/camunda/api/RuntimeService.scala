package biz.lobachev.annette.camunda.api

import akka.Done
import biz.lobachev.annette.camunda.api.common.VariableValue
import biz.lobachev.annette.camunda.api.runtime.{
  DeleteProcessInstancePayload,
  ModifyProcessVariablePayload,
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

  /**
   * Updates or deletes the variables of a process instance by id. Updates precede deletions.
   * So, if a variable is updated AND deleted, the deletion overrides the update.
   * @param id The id of the process instance to set variables for.
   * @param payload
   * @return
   */
  def modifyProcessVariables(id: String, payload: ModifyProcessVariablePayload): Future[Done]

  /**
   * Sets a variable of a given process instance by id.
   * @param id The id of the process instance to set the variable for.
   * @param varName The name of the variable to set.
   * @param value Value of the variable to set
   * @return
   */
  def updateProcessVariable(id: String, varName: String, value: VariableValue): Future[Done]

  /**
   * Deletes a variable of a process instance by id.
   * @param id The id of the process instance to delete the variable from.
   * @param varName The name of the variable to delete.
   * @return
   */
  def deleteProcessVariable(id: String, varName: String): Future[Done]

  /**
   * Retrieves a variable of a given process instance by id.
   * @param id The id of the process instance to retrieve the variable from.
   * @param varName The name of the variable to get.
   * @param deserializeValue Determines whether serializable variable values (typically variables that store
   *                         custom Java objects) should be deserialized on server side (default true).
   * @return
   */
  def getProcessVariable(id: String, varName: String, deserializeValue: Boolean = false): Future[VariableValue]

  /**
   * @param id The id of the process instance to retrieve the variables from.
   * @param deserializeValues Determines whether serializable variable values (typically variables that store
   *                         custom Java objects) should be deserialized on server side (default false).
   * @return
   */
  def getProcessVariables(id: String, deserializeValues: Boolean = false): Future[Map[String, VariableValue]]
}
