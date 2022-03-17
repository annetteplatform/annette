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
import biz.lobachev.annette.camunda.api.external_task._
import play.api.libs.json.{JsValue, Json}

import scala.concurrent.{ExecutionContext, Future}

class ExternalTaskServiceImpl(client: CamundaClient)(implicit val ec: ExecutionContext) extends ExternalTaskService {

  /**
   * Fetches and locks a specific number of external tasks for execution by a worker. Query can be restricted
   * to specific task topics and for each task topic an individual lock time can be provided.
   * @param query
   * @return
   */
  override def fetchAndLockExternalTask(query: FetchAndLockQuery): Future[Seq[ExternalTask]] =
    for {
      response <- client
                    .request("/external-task/fetchAndLock")
                    .addHttpHeaders("Accept" -> "application/json")
                    .post(Json.toJson(query))

    } yield response.status match {
      case 200 =>
        response.body[JsValue].as[Seq[ExternalTask]]
      case _   =>
        val json = response.body[JsValue]
        throw BPMEngineError(
          (json \ "type").as[String],
          (json \ "message").as[String],
          json.toString
        )
    }

  /**
   * Completes an external task by id and updates process variables.
   *
   * @param payload
   * @return
   */
  override def completeExternalTask(id: String, payload: CompleteExternalTaskPayload): Future[Done] =
    for {
      response <- client
                    .request(s"/external-task/$id/complete")
                    .addHttpHeaders("Accept" -> "application/json")
                    .post(Json.toJson(payload))

    } yield response.status match {
      case 204 =>
        Done

      case 400 =>
        val json = response.body[JsValue]
        throw LockNotAquired(
          id,
          (json \ "message").as[String],
          json.toString
        )
      case 404 =>
        val json = response.body[JsValue]
        throw ExternalTaskNotFound(
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
   * Retrieves an external task by id, corresponding to the ExternalTask interface in the engine.
   *
   * @param id
   * @return
   */
  override def getExternalTask(id: String): Future[ExternalTask] =
    for {
      response <- client
                    .request(s"/external-task/$id")
                    .addHttpHeaders("Accept" -> "application/json")
                    .get()

    } yield response.status match {
      case 204 =>
        response.body[JsValue].as[ExternalTask]
      case 404 =>
        val json = response.body[JsValue]
        throw ExternalTaskNotFound(
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
   * Queries for external tasks that fulfill given parameters in the form of a JSON object. This method is slightly
   * more powerful than the Get External Tasks method because it allows to specify a hierarchical result sorting.
   *
   * @param query
   * @return
   */
  override def finsExternalTasks(
    query: ExternalTaskFindQuery,
    firstResult: Option[Int],
    maxResults: Option[Int]
  ): Future[ExternalTaskFindResult] = {
    val params: Seq[(String, String)] = Seq(
      firstResult.map(r => "firstResult" -> r.toString),
      maxResults.map(r => "maxResults" -> r.toString)
    ).flatten
    for {
      response <- client
                    .request("/external-task")
                    .addHttpHeaders("Accept" -> "application/json")
                    .withQueryStringParameters(params: _*)
                    .post(Json.toJson(query))
      count <- client
                 .request("/external-task/count")
                 .addHttpHeaders("Accept" -> "application/json")
                 .post(Json.toJson(query))

    } yield response.status match {
      case 200 =>
        ExternalTaskFindResult(
          total = (count.json \ "count").as[Long],
          response.body[JsValue].as[Seq[ExternalTask]]
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
