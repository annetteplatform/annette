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
import biz.lobachev.annette.camunda.api.external_task.{
  CompleteExternalTaskPayload,
  ExternalTask,
  ExternalTaskFindQuery,
  ExternalTaskFindResult,
  FetchAndLockQuery
}

import scala.concurrent.Future

trait ExternalTaskService {

  /**
   * Fetches and locks a specific number of external tasks for execution by a worker. Query can be restricted
   * to specific task topics and for each task topic an individual lock time can be provided.
   * @param query
   * @return
   */
  def fetchAndLockExternalTask(query: FetchAndLockQuery): Future[Seq[ExternalTask]]

  /**
   * Completes an external task by id and updates process variables.
   * @param id	The id of the task to complete.
   * @param payload
   * @return
   */
  def completeExternalTask(id: String, payload: CompleteExternalTaskPayload): Future[Done]

  /**
   * Retrieves an external task by id, corresponding to the ExternalTask interface in the engine.
   * @param id
   * @return
   */
  def getExternalTask(id: String): Future[ExternalTask]

  /**
   * Queries for external tasks that fulfill given parameters in the form of a JSON object. This method is slightly
   * more powerful than the Get External Tasks method because it allows to specify a hierarchical result sorting.
   * @param query
   * @return
   */
  def finsExternalTasks(
    query: ExternalTaskFindQuery,
    firstResult: Option[Int],
    maxResults: Option[Int]
  ): Future[ExternalTaskFindResult]
}
