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

package biz.lobachev.annette.ignition.application

import biz.lobachev.annette.application.client.http.{ApplicationServiceLagomApi, ApplicationServiceLagomImpl}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.ignition.application.loaders.{
  ApplicationEntityLoader,
  LanguageEntityLoader,
  TranslationEntityLoader,
  TranslationJsonEntityLoader
}
import biz.lobachev.annette.ignition.core.{EntityLoader, IgnitionLagomClient, ServiceLoader}
import com.softwaremill.macwire.wire
import com.typesafe.config.Config

class ApplicationLoader(val client: IgnitionLagomClient, val config: Config, val principal: AnnettePrincipal)
    extends ServiceLoader {

  lazy val serviceApi = client.serviceClient.implement[ApplicationServiceLagomApi]
  lazy val service    = wire[ApplicationServiceLagomImpl]

  override def createEntityLoader(entity: String, entityConfig: Config, principal: AnnettePrincipal): EntityLoader[_] =
    entity match {
      case "language"         => new LanguageEntityLoader(service, entityConfig, principal)
      case "translation"      => new TranslationEntityLoader(service, entityConfig, principal)
      case "translation-json" => new TranslationJsonEntityLoader(service, entityConfig, principal)
      case "application"      => new ApplicationEntityLoader(service, entityConfig, principal)
    }
}
