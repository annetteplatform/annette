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
