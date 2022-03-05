package biz.lobachev.annette.camunda4s.api

import akka.Done
import biz.lobachev.annette.camunda4s.api.repository.{
  CreateDeploymentPayload,
  DeleteDeploymentPayload,
  DeleteProcessDefinitionPayload,
  Deployment,
  DeploymentFindQuery,
  DeploymentFindResult,
  DeploymentWithDefinitions,
  ProcessDefinition,
  ProcessDefinitionFindQuery,
  ProcessDefinitionFindResult
}

import scala.concurrent.Future

trait RepositoryService {

  /** Creates a deployment */
  def createDeployment(payload: CreateDeploymentPayload): Future[DeploymentWithDefinitions]

  /** Deletes a deployment by id. */
  def deleteDeployment(payload: DeleteDeploymentPayload): Future[Done]

  /** Retrieves a deployment by id, according to the Deployment interface of the engine. */
  def getDeploymentById(id: String): Future[Deployment]

  /**
   * Queries for deployments that fulfill given parameters. Parameters may be the properties of deployments, such as the id or name or a range of the deployment time.
   */
  def findDeployments(query: DeploymentFindQuery): Future[DeploymentFindResult]

  /** Deletes a process definition from a deployment by id. */
  def deleteProcessDefinition(payload: DeleteProcessDefinitionPayload): Future[Done]

  /** Retrieves a process definition by id according to the ProcessDefinition interface in the engine. */
  def getProcessDefinitionById(id: String): Future[ProcessDefinition]

  /** Retrieves a process definition by key according to the ProcessDefinition interface in the engine. */
  def getProcessDefinitionByKey(key: String): Future[ProcessDefinition]

  /**
   * Queries for process definitions that fulfill given parameters.
   * @param query query parameters may be the properties of process definitions, such as the name, key or version
   * @return
   */
  def findProcessDefinitions(query: ProcessDefinitionFindQuery): Future[ProcessDefinitionFindResult]

}
