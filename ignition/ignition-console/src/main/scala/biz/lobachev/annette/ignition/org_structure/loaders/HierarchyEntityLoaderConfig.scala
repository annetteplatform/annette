package biz.lobachev.annette.ignition.org_structure.loaders

import biz.lobachev.annette.ignition.core.config.{EntityLoaderConfig, ErrorMode, LoadMode}
import com.typesafe.config.Config

import scala.jdk.CollectionConverters._
import scala.util.Try

case class HierarchyEntityLoaderConfig(
  data: Seq[String],
  onError: ErrorMode,
  mode: LoadMode,
  parallelism: Int,
  disposedCategory: String,
  removeDisposed: Boolean
) extends EntityLoaderConfig

object HierarchyEntityLoaderConfig {
  def apply(config: Config): HierarchyEntityLoaderConfig =
    HierarchyEntityLoaderConfig(
      data = Try(config.getStringList("data").asScala.toSeq).getOrElse(Seq.empty),
      onError = ErrorMode.fromConfig(config),
      mode = LoadMode.fromConfig(config),
      parallelism = Try(config.getInt("parallelism")).getOrElse(1),
      disposedCategory = Try(config.getString("disposed-category")).getOrElse("DISPOSED-UNIT"),
      removeDisposed = Try(config.getBoolean("remove-disposed")).getOrElse(false)
    )
}
