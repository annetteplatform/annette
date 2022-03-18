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
 * A process definition object. Each process definition object has the following properties:
 * @param id The id of the process definition.
 * @param key The key of the process definition, i.e., the id of the BPMN 2.0 XML process definition.
 * @param category The category of the process definition.
 * @param description The description of the process definition.
 * @param name The name of the process definition.
 * @param version The version of the process definition that the engine assigned to it.
 * @param resource The file name of the process definition.
 * @param deploymentId The deployment id of the process definition.
 * @param diagram The file name of the process definition diagram, if it exists.
 * @param suspended A flag indicating whether the definition is suspended or not.
 * @param tenantId The tenant id of the process definition.
 * @param versionTag The version tag of the process or null when no version tag is set
 * @param historyTimeToLive History time to live value of the process definition. Is used within History cleanup.
 * @param startableInTasklist A flag indicating whether the process definition is startable in Tasklist or not.
 */
case class ProcessDefinition(
  id: String,
  key: String,
  category: String,
  description: Option[String],
  name: Option[String],
  version: Int,
  resource: String,
  deploymentId: String,
  diagram: Option[String],
  suspended: Boolean,
  tenantId: Option[String],
  versionTag: Option[String],
  historyTimeToLive: Option[Int],
  startableInTasklist: Boolean
)

object ProcessDefinition {
  implicit val format = Json.format[ProcessDefinition]
}
