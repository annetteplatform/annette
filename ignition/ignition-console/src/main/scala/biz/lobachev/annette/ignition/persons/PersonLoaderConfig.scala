package biz.lobachev.annette.ignition.persons

import biz.lobachev.annette.ignition.core.config.{DefaultEntityLoaderConfig, ErrorMode, ServiceLoaderConfig}
import com.typesafe.config.Config

import scala.util.Try

case class PersonLoaderConfig(
  entities: Seq[String],
  onError: ErrorMode,
  category: Option[DefaultEntityLoaderConfig],
  person: Option[DefaultEntityLoaderConfig]
) extends ServiceLoaderConfig

object PersonLoaderConfig {
  def apply(config: Config): PersonLoaderConfig =
    PersonLoaderConfig(
      entities = ServiceLoaderConfig.entities(config),
      onError = ErrorMode.fromConfig(config),
      category = Try(DefaultEntityLoaderConfig(config.getConfig(PersonLoader.Category))).toOption,
      person = Try(DefaultEntityLoaderConfig(config.getConfig(PersonLoader.Person))).toOption
    )
}
