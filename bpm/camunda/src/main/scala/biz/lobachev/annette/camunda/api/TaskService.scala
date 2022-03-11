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
//import biz.lobachev.annette.camunda.api.common.VariableValue
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
}
