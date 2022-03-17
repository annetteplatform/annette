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
 * @param workerId Mandatory.The id of the worker on which behalf tasks are fetched. The returned tasks are locked
 *                 for that worker and can only be completed when providing the same worker id.
 * @param maxTasks Mandatory.The maximum number of tasks to return.
 * @param usePriority A boolean value, which indicates whether the task should be fetched
 *                    based on its priority or arbitrarily.
 * @param asyncResponseTimeout The Long Polling timeout in milliseconds.
 *                             Note: The value cannot be set larger than 1.800.000 milliseconds
 *                             (corresponds to 30 minutes).
 * @param topics A JSON array of topic objects for which external tasks should be fetched. The returned tasks
 *               may be arbitrarily distributed among these topics.
 */
case class FetchAndLockQuery(
  workerId: String,
  maxTasks: Int,
  usePriority: Option[Boolean] = None,
  asyncResponseTimeout: Option[Int] = None,
  topics: Seq[TopicQuery]
)

object FetchAndLockQuery {
  implicit val format = Json.format[FetchAndLockQuery]
}
