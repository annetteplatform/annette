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

import biz.lobachev.annette.camunda.api.VariableValues
import play.api.libs.json.Json

case class ExternalTask(
  activityId: String,
  activityInstanceId: String,
  errorMessage: Option[String] = None,
  errorDetails: Option[String] = None,
  executionId: String,
  id: String,
  lockExpirationTime: String,
  processDefinitionId: String,
  processDefinitionKey: String,
  processDefinitionVersionTag: Option[String] = None,
  processInstanceId: String,
  tenantId: Option[String] = None,
  retries: Option[Int] = None,
  suspended: Option[Boolean] = None,
  workerId: String,
  priority: Option[Int] = None,
  topicName: String,
  businessKey: Option[String] = None,
  variables: Option[VariableValues] = None
)

object ExternalTask {
  implicit val format = Json.format[ExternalTask]
}
