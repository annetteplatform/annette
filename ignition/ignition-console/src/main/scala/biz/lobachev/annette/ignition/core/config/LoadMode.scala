package biz.lobachev.annette.ignition.core.config

import com.typesafe.config.Config

import scala.util.Try

sealed trait LoadMode
object InsertMode extends LoadMode
object UpsertMode extends LoadMode

object LoadMode {
  def fromConfig(config: Config): LoadMode =
    Try(
      if (config.getString("mode") == "upsert") UpsertMode
      else InsertMode
    ).getOrElse(UpsertMode)
}
