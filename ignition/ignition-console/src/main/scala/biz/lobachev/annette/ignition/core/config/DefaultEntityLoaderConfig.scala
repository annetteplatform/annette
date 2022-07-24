package biz.lobachev.annette.ignition.core.config

import com.typesafe.config.Config

import scala.jdk.CollectionConverters._
import scala.util.Try

case class DefaultEntityLoaderConfig(
  override val data: Seq[String],
  override val onError: ErrorMode,
  override val mode: LoadMode,
  override val parallelism: Int
) extends EntityLoaderConfig

object DefaultEntityLoaderConfig {
  def apply(config: Config): DefaultEntityLoaderConfig =
    DefaultEntityLoaderConfig(
      data = Try(config.getStringList("data").asScala.toSeq).getOrElse(Seq.empty),
      onError = ErrorMode.fromConfig(config),
      mode = LoadMode.fromConfig(config),
      parallelism = Try(config.getInt("parallelism")).getOrElse(1)
    )
}
