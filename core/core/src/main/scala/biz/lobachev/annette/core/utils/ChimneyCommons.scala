package biz.lobachev.annette.core.utils

object ChimneyCommons {

  implicit val projectConfig =
    io.scalaland.chimney.dsl.TransformerConfiguration.default.enableMethodAccessors.enableDefaultValues
}
