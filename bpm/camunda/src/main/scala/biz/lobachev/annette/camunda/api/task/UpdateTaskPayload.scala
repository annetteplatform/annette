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
 * @param name The task name.
 * @param description The task description.
 * @param assignee The user to assign to this task.
 * @param owner The owner of the task.
 * @param delegationState The delegation state of the task. Corresponds to the DelegationState enum in the engine.
 *                        Possible values are RESOLVED and PENDING.
 * @param due The due date for the task. Default format* yyyy-MM-dd'T'HH:mm:ss.SSSZ.
 * @param followUp The follow-up date for the task. Default format* yyyy-MM-dd'T'HH:mm:ss.SSSZ.
 * @param priority The priority of the task.
 * @param parentTaskId The id of the parent task, if this task is a subtask.
 * @param caseInstanceId The id of the case instance the task belongs to.
 * @param tenantId The tenant id of the task. Note: the tenant id cannot be changed; only the existing tenant id can be passed.
 */
case class UpdateTaskPayload(
  name: Option[String] = None,
  description: Option[String] = None,
  assignee: Option[String] = None,
  owner: Option[String] = None,
  delegationState: Option[String] = None,
  due: Option[String] = None,
  followUp: Option[String] = None,
  priority: Option[Int] = None,
  parentTaskId: Option[String] = None,
  caseInstanceId: Option[String] = None,
  tenantId: Option[String] = None
)

object UpdateTaskPayload {
  implicit val format = Json.format[UpdateTaskPayload]
}
