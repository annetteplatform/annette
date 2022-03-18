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
 * @param id The id of the deployment.
 * @param name The name of the deployment.
 * @param source The source of the deployment.
 * @param deploymentTime The date and time of the deployment.
 */
case class Deployment(
  id: String,
  name: Option[String] = None,
  source: Option[String] = None,
  deploymentTime: Option[String] = None
)

object Deployment {
  implicit val format = Json.format[Deployment]
}
