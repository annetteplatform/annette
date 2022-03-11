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
import biz.lobachev.annette.camunda.api.task.{CreateTaskPayload, Task, TaskFindQuery, TaskFindResult, UpdateTaskPayload}
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
}
