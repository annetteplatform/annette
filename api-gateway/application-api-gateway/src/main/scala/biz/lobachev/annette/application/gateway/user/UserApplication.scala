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

package biz.lobachev.annette.application.gateway.user

import biz.lobachev.annette.application.api.application.{Application, ApplicationId}
import biz.lobachev.annette.core.model.text.{Icon, MultiLanguageText}
import io.scalaland.chimney.dsl._
import play.api.libs.json.Json

case class UserApplication(
  id: ApplicationId,
  icon: Option[Icon],
  label: MultiLanguageText,
  labelDescription: MultiLanguageText,
  frontendUrl: Option[String],
  backendUrl: Option[String]
)

object UserApplication {

  def apply(app: Application): UserApplication = app.transformInto[UserApplication]

  implicit val format = Json.format[UserApplication]
}
