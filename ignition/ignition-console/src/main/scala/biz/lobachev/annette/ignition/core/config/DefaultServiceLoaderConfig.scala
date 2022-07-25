package biz.lobachev.annette.ignition.core.config

import com.typesafe.config.Config

case class DefaultServiceLoaderConfig(
  entities: Seq[String],
  onError: ErrorMode,
  config: Config
) extends ServiceLoaderConfig

object DefaultServiceLoaderConfig {
  def apply(config: Config): DefaultServiceLoaderConfig =
    DefaultServiceLoaderConfig(
      entities = ServiceLoaderConfig.entities(config),
      onError = ErrorMode.fromConfig(config),
      config = config
    )
}
