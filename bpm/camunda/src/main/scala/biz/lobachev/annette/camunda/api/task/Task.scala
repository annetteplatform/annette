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

package biz.lobachev.annette.camunda.api.task

import play.api.libs.json.Json

/**
 * @param id The id of the task.
 * @param name The tasks name.
 * @param assignee The user assigned to this task.
 * @param created The time the task was created. Default format* yyyy-MM-dd'T'HH:mm:ss.SSSZ.
 * @param due The due date for the task. Default format* yyyy-MM-dd'T'HH:mm:ss.SSSZ.
 * @param followUp The follow-up date for the task. Default format* yyyy-MM-dd'T'HH:mm:ss.SSSZ.
 * @param delegationState The delegation state of the task. Corresponds to the DelegationState enum in the engine.
 *                        Possible values are RESOLVED and PENDING.
 * @param description The task description.
 * @param executionId The id of the execution the task belongs to.
 * @param owner The owner of the task.
 * @param parentTaskId The id of the parent task, if this task is a subtask.
 * @param priority The priority of the task.
 * @param processDefinitionId The id of the process definition this task belongs to.
 * @param processInstanceId The id of the process instance this task belongs to.
 * @param caseExecutionId The id of the case execution the task belongs to.
 * @param caseDefinitionId The id of the case definition the task belongs to.
 * @param caseInstanceId The id of the case instance the task belongs to.
 * @param taskDefinitionKey The task definition key.
 * @param suspended Whether the task belongs to a process instance that is suspended.
 * @param formKey If not None, the form key for the task.
 * @param tenantId If not None, the tenantId for the task.
 */
case class Task(
  id: String,
  name: Option[String] = None,
  assignee: Option[String] = None,
  created: String,
  due: Option[String] = None,
  followUp: Option[String] = None,
  delegationState: Option[String] = None,
  description: Option[String] = None,
  executionId: Option[String] = None,
  owner: Option[String] = None,
  parentTaskId: Option[String] = None,
  priority: Int,
  processDefinitionId: Option[String] = None,
  processInstanceId: Option[String] = None,
  caseExecutionId: Option[String] = None,
  caseDefinitionId: Option[String] = None,
  caseInstanceId: Option[String] = None,
  taskDefinitionKey: Option[String] = None,
  suspended: Boolean,
  formKey: Option[String] = None,
  tenantId: Option[String] = None
)

object Task {
  implicit val format = Json.format[Task]
}
