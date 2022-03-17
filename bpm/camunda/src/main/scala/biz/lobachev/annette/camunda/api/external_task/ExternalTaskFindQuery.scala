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

package biz.lobachev.annette.camunda.api.external_task

import play.api.libs.json.Json

/**
 * @param externalTaskId Filter by an external task's id.
 * @param externalTaskIdIn Filter by the comma-separated list of external task ids.
 * @param topicName Filter by an external task topic.
 * @param workerId Filter by the id of the worker that the task was most recently locked by.
 * @param locked Only include external tasks that are currently locked (i.e., they have a lock time and
 *               it has not expired). Value may only be true, as false matches any external task.
 * @param notLocked Only include external tasks that are currently not locked (i.e., they have no lock or
 *                  it has expired). Value may only be true, as false matches any external task.
 * @param withRetriesLeft Only include external tasks that have a positive (> 0) number of retries (or null).
 *                        Value may only be true, as false matches any external task.
 * @param noRetriesLeft Only include external tasks that have 0 retries. Value may only be true, as false
 *                      matches any external task.
 * @param lockExpirationAfter Restrict to external tasks that have a lock that expires after a given date.
 *                            By default*, the date must have the format yyyy-MM-dd'T'HH:mm:ss.SSSZ,
 *                            e.g., 2013-01-23T14:42:45.000+0200.
 * @param lockExpirationBefore Restrict to external tasks that have a lock that expires before a given date.
 *                             By default*, the date must have the format yyyy-MM-dd'T'HH:mm:ss.SSSZ,
 *                             e.g., 2013-01-23T14:42:45.000+0200.
 * @param activityId Filter by the id of the activity that an external task is created for.
 * @param activityIdIn Filter by the comma-separated list of ids of the activities that an external task is created for.
 * @param executionId Filter by the id of the execution that an external task belongs to.
 * @param processInstanceId Filter by the id of the process instance that an external task belongs to.
 * @param processInstanceIdIn Filter by a comma-separated list of process instance ids that an external task may belong to.
 * @param processDefinitionId Filter by the id of the process definition that an external task belongs to.
 * @param tenantIdIn Filter by a comma-separated list of tenant ids. An external task must have one of the given tenant ids.
 * @param active Only include active tasks. Value may only be true, as false matches any external task.
 * @param suspended Only include suspended tasks. Value may only be true, as false matches any external task.
 * @param priorityHigherThanOrEquals Only include jobs with a priority higher than or equal to the given value.
 *                                   Value must be a valid long value.
 * @param priorityLowerThanOrEquals Only include jobs with a priority lower than or equal to the given value.
 *                                  Value must be a valid long value.
 * @param sorting A JSON array of criteria to sort the result by. Each element of the array is a JSON object
 *                that specifies one ordering. The position in the array identifies the rank of an ordering,
 *                i.e., whether it is primary, secondary, etc.
 */
case class ExternalTaskFindQuery(
  externalTaskId: Option[String] = None,
  externalTaskIdIn: Option[Seq[String]] = None,
  topicName: Option[String] = None,
  workerId: Option[String] = None,
  locked: Option[Boolean] = None,
  notLocked: Option[Boolean] = None,
  withRetriesLeft: Option[Boolean] = None,
  noRetriesLeft: Option[Boolean] = None,
  lockExpirationAfter: Option[String] = None,
  lockExpirationBefore: Option[String] = None,
  activityId: Option[String] = None,
  activityIdIn: Option[Seq[String]] = None,
  executionId: Option[String] = None,
  processInstanceId: Option[String] = None,
  processInstanceIdIn: Option[Seq[String]] = None,
  processDefinitionId: Option[String] = None,
  tenantIdIn: Option[String] = None,
  active: Option[Seq[String]] = None,
  suspended: Option[Boolean] = None,
  priorityHigherThanOrEquals: Option[Long] = None,
  priorityLowerThanOrEquals: Option[Long] = None,
  sorting: Option[Seq[ExternalTaskSorting]] = None
)

object ExternalTaskFindQuery {
  implicit val format = Json.format[ExternalTaskFindQuery]
}
