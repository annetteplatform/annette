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
import biz.lobachev.annette.ignition.application.loaders.{
  ApplicationEntityLoader,
  LanguageEntityLoader,
  TranslationEntityLoader,
  TranslationJsonEntityLoader
}
import biz.lobachev.annette.ignition.core.{EntityLoader, IgnitionLagomClient, ServiceLoader}
import com.softwaremill.macwire.wire

class ApplicationLoader(
  val client: IgnitionLagomClient,
  val config: ApplicationLoaderConfig
) extends ServiceLoader[ApplicationLoaderConfig] {

  lazy val serviceApi = client.serviceClient.implement[ApplicationServiceLagomApi]
  lazy val service    = wire[ApplicationServiceLagomImpl]

  override val name: String = "application"

  override def createEntityLoader(entity: String): EntityLoader[_, _] =
    entity match {
      case ApplicationLoader.Language        => new LanguageEntityLoader(service, config.language.get)
      case ApplicationLoader.Translation     => new TranslationEntityLoader(service, config.translation.get)
      case ApplicationLoader.TranslationJson => new TranslationJsonEntityLoader(service, config.translationJson.get)
      case ApplicationLoader.Application     => new ApplicationEntityLoader(service, config.application.get)
    }

}

object ApplicationLoader {
  val Language        = "language"
  val Translation     = "translation"
  val TranslationJson = "translation-json"
  val Application     = "application"

}
