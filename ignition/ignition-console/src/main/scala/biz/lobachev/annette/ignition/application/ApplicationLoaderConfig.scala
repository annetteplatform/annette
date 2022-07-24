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

import biz.lobachev.annette.ignition.core.config.{DefaultEntityLoaderConfig, ErrorMode, ServiceLoaderConfig}
import com.typesafe.config.Config

import scala.util.Try

case class ApplicationLoaderConfig(
  entities: Seq[String],
  onError: ErrorMode,
  application: Option[DefaultEntityLoaderConfig],
  language: Option[DefaultEntityLoaderConfig],
  translation: Option[DefaultEntityLoaderConfig],
  translationJson: Option[DefaultEntityLoaderConfig]
) extends ServiceLoaderConfig {}

object ApplicationLoaderConfig {
  def apply(config: Config): ApplicationLoaderConfig =
    ApplicationLoaderConfig(
      entities = ServiceLoaderConfig.entities(config),
      onError = ErrorMode.fromConfig(config),
      application = Try(DefaultEntityLoaderConfig(config.getConfig(ApplicationLoader.Application))).toOption,
      language = Try(DefaultEntityLoaderConfig(config.getConfig(ApplicationLoader.Language))).toOption,
      translation = Try(DefaultEntityLoaderConfig(config.getConfig(ApplicationLoader.Translation))).toOption,
      translationJson = Try(DefaultEntityLoaderConfig(config.getConfig(ApplicationLoader.TranslationJson))).toOption
    )
}
