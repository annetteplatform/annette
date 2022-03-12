/*
 * Copyright 2013 Valery Lobachev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package biz.lobachev.annette.camunda.api

import akka.Done
import biz.lobachev.annette.camunda.api.common.VariableValue
import biz.lobachev.annette.camunda.api.task._

import scala.concurrent.Future

trait TaskService {

  /** Creates a new task. */
  def createTask(payload: CreateTaskPayload): Future[Done]

  /** Updates a task. */
  def updateTask(id: String, payload: UpdateTaskPayload): Future[Done]

  /**
   * Removes a task by id. Only tasks that are not part of a running process or case can be deleted;
   * only standalone tasks can be deleted.
   * @param id The id of the task to be removed.
   */
  def deleteTask(id: String): Future[Done]

  /** Retrieves a task by id. */
  def getTask(id: String): Future[Task]
  def findTasks(
    query: TaskFindQuery,
    firstResult: Option[Int] = None,
    maxResults: Option[Int] = None
  ): Future[TaskFindResult]

  /**
   * Claims a task for a specific user.
   * Note: The difference with the Set Assignee method is that here a check is performed to see
   * if the task already has a user assigned to it.
   * @param id The id of the task to claim.
   * @param userId The id of the user that claims the task.
   * @return
   */
  def claimTask(id: String, userId: String): Future[Done]

  /**
   * Resets a task’s assignee. If successful, the task is not assigned to a user.
   * @param id
   * @return
   */
  def unclaimTask(id: String): Future[Done]

  /**
   * Changes the assignee of a task to a specific user.
   * Note: The difference with the Claim Task method is that this method does not check
   * if the task already has a user assigned to it.
   * @param id The id of the task to set the assignee for.
   * @param userId The id of the user that will be the assignee of the task.
   * @return
   */
  def setTaskAssignee(id: String, userId: Option[String]): Future[Done]

  /**
   * Delegates a task to another user.
   * @param id The id of the task to delegate.
   * @param userId The id of the user that the task should be delegated to.
   * @return
   */
  def delegateTask(id: String, userId: String): Future[Done]

  /**
   * Resolves a task and updates execution variables.
   * Resolving a task marks that the assignee is done with the task delegated to them, and that it can be sent
   * back to the owner. Can only be executed when the task has been delegated. The assignee will be set to the owner,
   * who performed the delegation.
   * @param id The id of the task to resolve.
   * @param payload
   * @return
   */
  def resolveTask(id: String, payload: ResolveTaskPayload = ResolveTaskPayload()): Future[Done]

  /**
   * Completes a task and updates process variables.
   * @param id The id of the task to complete.
   * @param payload
   * @return
   */
  def completeTask(
    id: String,
    payload: CompleteTaskPayload = CompleteTaskPayload()
  ): Future[Either[Done, VariableValues]]

  /**
   * Updates or deletes the variables visible from the task. Updates precede deletions. So, if a variable is updated
   * AND deleted, the deletion overrides the update. A variable is visible from the task if it is a local task variable
   * or declared in a parent scope of the task.
   * @param id
   * @param payload
   * @return
   */
  def modifyTaskVariables(id: String, payload: ModifyTaskVariablePayload): Future[Done]

  /**
   * Updates a process variable that is visible from the Task scope. A variable is visible from the task
   * if it is a local task variable, or declared in a parent scope of the task. See the documentation
   * on variable scopes and visibility.
   * Note: If a variable doesn’t exist, the variable is created in the top-most scope visible from the task.
   * @param id
   * @param varName
   * @param value
   * @return
   */
  def updateTaskVariable(id: String, varName: String, value: VariableValue): Future[Done]

  /**
   * Removes a variable that is visible to a task. A variable is visible to a task if it is a local task variable
   * or declared in a parent scope of the task.
   * @param id
   * @param varName
   * @return
   */
  def deleteTaskVariable(id: String, varName: String): Future[Done]

  /**
   * Retrieves a variable from the context of a given task. The variable must be visible from the task.
   * It is visible from the task if it is a local task variable or declared in a parent scope of the task.
   * @param id
   * @param varName
   * @param deserializeValue
   * @return
   */
  def getTaskVariable(id: String, varName: String, deserializeValue: Boolean = false): Future[VariableValue]

  /**
   * Retrieves all variables visible from the task. A variable is visible from the task if it is a local task variable
   * or declared in a parent scope of the task.
   * @param id
   * @param deserializeValues
   * @return
   */
  def getTaskVariables(id: String, deserializeValues: Boolean = false): Future[VariableValues]

}
