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

/**
 * @param workerId The id of the worker that completes the task. Must match the id of the worker who has
 *                 most recently locked the task.
 * @param variables A JSON object containing variable key-value pairs. Each key is a variable name and
 *                  each value a JSON variable value object
 * @param localVariables A JSON object containing variable key-value pairs. Each key is a variable name and
 *                       each value a JSON variable value object
 */
case class CompleteExternalTaskPayload(
  workerId: String,
  variables: Option[VariableValues] = None,
  localVariables: Option[VariableValues] = None
)

object CompleteExternalTaskPayload {
  implicit val format = Json.format[CompleteExternalTaskPayload]
}
