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

package biz.lobachev.annette.camunda.api.repository

import play.api.libs.json.Json

/**
 * @param id The id of the deployment to be deleted.
 * @param cascade true, if all process instances, historic process instances and jobs for this deployment should be deleted.
 * @param skipCustomListeners true, if only the built-in ExecutionListeners should be notified with the end event.
 * @param skipIoMappings true, if all input/output mappings should not be invoked.
 */
case class DeleteDeploymentPayload(
  id: String,
  cascade: Option[Boolean] = None,
  skipCustomListeners: Option[Boolean] = None,
  skipIoMappings: Option[Boolean] = None
)

object DeleteDeploymentPayload {
  implicit val format = Json.format[DeleteDeploymentPayload]
}
