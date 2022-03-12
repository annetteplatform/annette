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

package biz.lobachev.annette.camunda.impl

import akka.Done
import biz.lobachev.annette.camunda.api._
import biz.lobachev.annette.camunda.api.common.VariableValue
import biz.lobachev.annette.camunda.api.task.{
  CompleteTaskPayload,
  CreateTaskPayload,
  ModifyTaskVariablePayload,
  ResolveTaskPayload,
  Task,
  TaskFindQuery,
  TaskFindResult,
  UpdateTaskPayload
}
import play.api.libs.json.{JsValue, Json}

import scala.concurrent.{ExecutionContext, Future}

class TaskServiceImpl(client: CamundaClient)(implicit val ec: ExecutionContext) extends TaskService {

  /** Creates a new task. */
  override def createTask(payload: CreateTaskPayload): Future[Done] =
    for {
      response <- client
                    .request("/task/create")
                    .addHttpHeaders("Accept" -> "application/json")
                    .post(Json.toJson(payload))

    } yield response.status match {
      case 204 =>
        Done
      case _   =>
        val json = response.body[JsValue]
        throw BPMEngineError(
          (json \ "type").as[String],
          (json \ "message").as[String],
          json.toString
        )
    }

  /** Updates a task. */
  override def updateTask(id: String, payload: UpdateTaskPayload): Future[Done] =
    for {
      response <- client
                    .request(s"/task/$id")
                    .addHttpHeaders("Accept" -> "application/json")
                    .put(Json.toJson(payload))

    } yield response.status match {
      case 204 =>
        Done
      case 404 =>
        val json = response.body[JsValue]
        throw TaskNotFound(
          id,
          (json \ "message").as[String],
          json.toString
        )
      case _   =>
        val json = response.body[JsValue]
        throw BPMEngineError(
          (json \ "type").as[String],
          (json \ "message").as[String],
          json.toString
        )
    }

  /**
   * Removes a task by id. Only tasks that are not part of a running process or case can be deleted;
   * only standalone tasks can be deleted.
   *
   * @param id The id of the task to be removed.
   */
  override def deleteTask(id: String): Future[Done] =
    for {
      response <- client
                    .request(s"/task/$id")
                    .addHttpHeaders("Accept" -> "application/json")
                    .delete()

    } yield response.status match {
      case 204 =>
        Done
      case 404 =>
        val json = response.body[JsValue]
        throw TaskNotFound(
          id,
          (json \ "message").as[String],
          json.toString
        )
      case _   =>
        val json = response.body[JsValue]
        throw BPMEngineError(
          (json \ "type").as[String],
          (json \ "message").as[String],
          json.toString
        )
    }

  /** Retrieves a task by id. */
  override def getTask(id: String): Future[Task] =
    for {
      response <- client
                    .request(s"/task/$id")
                    .addHttpHeaders("Accept" -> "application/json")
                    .get()

    } yield response.status match {
      case 200 =>
        response.body[JsValue].as[Task]
      case 404 =>
        val json = response.body[JsValue]
        throw TaskNotFound(
          id,
          (json \ "message").as[String],
          json.toString
        )
      case _   =>
        val json = response.body[JsValue]
        throw BPMEngineError(
          (json \ "type").as[String],
          (json \ "message").as[String],
          json.toString
        )
    }

  override def findTasks(
    query: TaskFindQuery,
    firstResult: Option[Int],
    maxResults: Option[Int]
  ): Future[TaskFindResult] = {
    val params: Seq[(String, String)] = Seq(
      firstResult.map(r => "firstResult" -> r.toString),
      maxResults.map(r => "maxResults" -> r.toString)
    ).flatten
    for {
      response <- client
                    .request(s"/task")
                    .addHttpHeaders("Accept" -> "application/json")
                    .withQueryStringParameters(params: _*)
                    .post(Json.toJson(query))
      count <- client
                 .request(s"/task/count")
                 .addHttpHeaders("Accept" -> "application/json")
                 .post(Json.toJson(query))

    } yield response.status match {
      case 200 =>
        TaskFindResult(
          total = (count.json \ "count").as[Long],
          response.body[JsValue].as[Seq[Task]]
        )
      case _   =>
        val json = response.body[JsValue]
        throw BPMEngineError(
          (json \ "type").as[String],
          (json \ "message").as[String],
          json.toString
        )
    }
  }

  /**
   * Claims a task for a specific user.
   * Note: The difference with the Set Assignee method is that here a check is performed to see
   * if the task already has a user assigned to it.
   *
   * @param id     The id of the task to claim.
   * @param userId The id of the user that claims the task.
   * @return
   */
  override def claimTask(id: String, userId: String): Future[Done] =
    for {
      response <- client
                    .request(s"/task/$id/claim")
                    .addHttpHeaders("Accept" -> "application/json")
                    .post(Json.obj("userId" -> userId))

    } yield response.status match {
      case 204 =>
        Done
      case _   =>
        val json = response.body[JsValue]
        throw BPMEngineError(
          (json \ "type").as[String],
          (json \ "message").as[String],
          json.toString
        )
    }

  /**
   * Resets a task’s assignee. If successful, the task is not assigned to a user.
   *
   * @param id
   * @return
   */
  override def unclaimTask(id: String): Future[Done] =
    for {
      response <- client
                    .request(s"/task/$id/unclaim")
                    .addHttpHeaders("Accept" -> "application/json")
                    .post(Json.obj())

    } yield response.status match {
      case 204 =>
        Done
      case _   =>
        val json = response.body[JsValue]
        throw BPMEngineError(
          (json \ "type").as[String],
          (json \ "message").as[String],
          json.toString
        )
    }

  /**
   * Changes the assignee of a task to a specific user.
   * Note: The difference with the Claim Task method is that this method does not check
   * if the task already has a user assigned to it.
   *
   * @param id     The id of the task to set the assignee for.
   * @param userId The id of the user that will be the assignee of the task.
   * @return
   */
  override def setTaskAssignee(id: String, userId: Option[String]): Future[Done] = {
    val body = userId.map(u => Json.obj("userId" -> u)).getOrElse(Json.obj())
    for {
      response <- client
                    .request(s"/task/$id/assignee")
                    .addHttpHeaders("Accept" -> "application/json")
                    .post(body)

    } yield response.status match {
      case 204 =>
        Done
      case _   =>
        val json = response.body[JsValue]
        throw BPMEngineError(
          (json \ "type").as[String],
          (json \ "message").as[String],
          json.toString
        )
    }
  }

  /**
   * Delegates a task to another user.
   *
   * @param id     The id of the task to delegate.
   * @param userId The id of the user that the task should be delegated to.
   * @return
   */
  override def delegateTask(id: String, userId: String): Future[Done] =
    for {
      response <- client
                    .request(s"/task/$id/delegate")
                    .addHttpHeaders("Accept" -> "application/json")
                    .post(Json.obj("userId" -> userId))

    } yield response.status match {
      case 204 =>
        Done
      case _   =>
        val json = response.body[JsValue]
        throw BPMEngineError(
          (json \ "type").as[String],
          (json \ "message").as[String],
          json.toString
        )
    }

  /**
   * Resolves a task and updates execution variables.
   * Resolving a task marks that the assignee is done with the task delegated to them, and that it can be sent
   * back to the owner. Can only be executed when the task has been delegated. The assignee will be set to the owner,
   * who performed the delegation.
   *
   * @param id The id of the task to resolve.
   * @param payload
   * @return
   */
  override def resolveTask(id: String, payload: ResolveTaskPayload = ResolveTaskPayload()): Future[Done] =
    for {
      response <- client
                    .request(s"/task/$id/resolve")
                    .addHttpHeaders("Accept" -> "application/json")
                    .post(Json.toJson(payload))

    } yield response.status match {
      case 204 =>
        Done
      case _   =>
        val json = response.body[JsValue]
        throw BPMEngineError(
          (json \ "type").as[String],
          (json \ "message").as[String],
          json.toString
        )
    }

  /**
   * Completes a task and updates process variables.
   *
   * @param id The id of the task to complete.
   * @param payload
   * @return
   */
  override def completeTask(
    id: String,
    payload: CompleteTaskPayload = CompleteTaskPayload()
  ): Future[Either[Done, VariableValues]] =
    for {
      response <- client
                    .request(s"/task/$id/complete")
                    .addHttpHeaders("Accept" -> "application/json")
                    .post(Json.toJson(payload))

    } yield response.status match {
      case 200 =>
        Right(response.body[JsValue].as[VariableValues])
      case 204 =>
        Left(Done)
      case _   =>
        val json = response.body[JsValue]
        throw BPMEngineError(
          (json \ "type").as[String],
          (json \ "message").as[String],
          json.toString
        )
    }

  /**
   * Updates or deletes the variables visible from the task. Updates precede deletions. So, if a variable is updated
   * AND deleted, the deletion overrides the update. A variable is visible from the task if it is a local task variable
   * or declared in a parent scope of the task.
   *
   * @param id
   * @param payload
   * @return
   */
  override def modifyTaskVariables(id: String, payload: ModifyTaskVariablePayload): Future[Done] =
    for {
      response <- client
                    .request(s"/task/$id/variables")
                    .addHttpHeaders("Accept" -> "application/json")
                    .post(Json.toJson(payload))

    } yield response.status match {
      case 204 =>
        Done
      case 400 =>
        throw InvalidVariableValue(
          (response.body[JsValue] \ "type").as[String],
          (response.body[JsValue] \ "message").as[String],
          response.body[JsValue].toString
        )
      case _   =>
        val json = response.body[JsValue]
        throw BPMEngineError(
          (json \ "type").as[String],
          (json \ "message").as[String],
          json.toString
        )
    }

  /**
   * Updates a process variable that is visible from the Task scope. A variable is visible from the task
   * if it is a local task variable, or declared in a parent scope of the task. See the documentation
   * on variable scopes and visibility.
   * Note: If a variable doesn’t exist, the variable is created in the top-most scope visible from the task.
   *
   * @param id
   * @param varName
   * @param value
   * @return
   */
  override def updateTaskVariable(id: String, varName: String, value: VariableValue): Future[Done] =
    for {
      response <- client
                    .request(s"/task/$id/variables/$varName")
                    .addHttpHeaders("Accept" -> "application/json")
                    .put(Json.toJson(value))

    } yield response.status match {
      case 204 =>
        Done
      case 400 =>
        throw InvalidVariableValue(
          (response.body[JsValue] \ "type").as[String],
          (response.body[JsValue] \ "message").as[String],
          response.body[JsValue].toString
        )
      case _   =>
        val json = response.body[JsValue]
        throw BPMEngineError(
          (json \ "type").as[String],
          (json \ "message").as[String],
          json.toString
        )
    }

  /**
   * Removes a variable that is visible to a task. A variable is visible to a task if it is a local task variable
   * or declared in a parent scope of the task.
   *
   * @param id
   * @param varName
   * @return
   */
  override def deleteTaskVariable(id: String, varName: String): Future[Done] =
    for {
      response <- client
                    .request(s"/task/$id/variables/$varName")
                    .addHttpHeaders("Accept" -> "application/json")
                    .delete()

    } yield response.status match {
      case 204 =>
        Done
      case _   =>
        val json = response.body[JsValue]
        throw BPMEngineError(
          (json \ "type").as[String],
          (json \ "message").as[String],
          json.toString
        )
    }

  /**
   * Retrieves a variable from the context of a given task. The variable must be visible from the task.
   * It is visible from the task if it is a local task variable or declared in a parent scope of the task.
   *
   * @param id
   * @param varName
   * @param deserializeValue
   * @return
   */
  override def getTaskVariable(id: String, varName: String, deserializeValue: Boolean): Future[VariableValue] =
    for {
      response <- client
                    .request(s"/task/$id/variables/$varName")
                    .addHttpHeaders("Accept" -> "application/json")
                    .withQueryStringParameters("deserializeValue" -> deserializeValue.toString)
                    .get()

    } yield response.status match {
      case 200 =>
        response.body[JsValue].as[VariableValue]
      case 404 =>
        val json = response.body[JsValue]
        throw TaskVariableNotFound(
          id,
          varName,
          (json \ "message").as[String],
          json.toString
        )
      case _   =>
        val json = response.body[JsValue]
        throw BPMEngineError(
          (json \ "type").as[String],
          (json \ "message").as[String],
          json.toString
        )
    }

  /**
   * Retrieves all variables visible from the task. A variable is visible from the task if it is a local task variable
   * or declared in a parent scope of the task.
   *
   * @param id
   * @param deserializeValues
   * @return
   */
  override def getTaskVariables(id: String, deserializeValues: Boolean): Future[VariableValues] =
    for {
      response <- client
                    .request(s"/task/$id/variables")
                    .addHttpHeaders("Accept" -> "application/json")
                    .withQueryStringParameters("deserializeValues" -> deserializeValues.toString)
                    .get()

    } yield response.status match {
      case 200 =>
        response.body[JsValue].as[Map[String, VariableValue]]
      case 500 =>
        val json = response.body[JsValue]
        throw TaskNotFound(
          id,
          (json \ "message").as[String],
          json.toString
        )
      case _   =>
        val json = response.body[JsValue]
        throw BPMEngineError(
          (json \ "type").as[String],
          (json \ "message").as[String],
          json.toString
        )
    }
}
