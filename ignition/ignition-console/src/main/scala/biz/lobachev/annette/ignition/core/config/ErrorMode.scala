package biz.lobachev.annette.ignition.core.config

import com.typesafe.config.Config

import scala.util.Try

sealed trait ErrorMode
object IgnoreError extends ErrorMode
object StopOnError extends ErrorMode

object ErrorMode {
  def fromConfig(config: Config): ErrorMode =
    Try(
      if (config.getString("on-error") == "ignore") IgnoreError
      else StopOnError
    ).getOrElse(StopOnError)
}
