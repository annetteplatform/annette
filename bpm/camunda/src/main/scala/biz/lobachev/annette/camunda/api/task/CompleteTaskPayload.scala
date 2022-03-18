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

import biz.lobachev.annette.camunda.api.common.VariableValues
import play.api.libs.json.Json

/**
 * @param variables A JSON object containing variable key-value pairs. Each key is a variable name and each value
 *                  a JSON variable value object.
 * @param withVariablesInReturn Indicates whether the response should contain the process variables or not.
 *                              The default is false with a response code of 204. If set to true the response
 *                              contains the process variables and has a response code of 200. If the task is not
 *                              associated with a process instance (e.g. if it's part of a case instance)
 *                              no variables will be returned.
 */
case class CompleteTaskPayload(
  variables: Option[VariableValues] = None,
  withVariablesInReturn: Option[Boolean] = None
)

object CompleteTaskPayload {
  implicit val format = Json.format[CompleteTaskPayload]
}
