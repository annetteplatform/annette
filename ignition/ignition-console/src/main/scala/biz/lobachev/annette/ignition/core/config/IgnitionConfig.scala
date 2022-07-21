package biz.lobachev.annette.ignition.core.config

import biz.lobachev.annette.core.model.auth.AnnettePrincipal

case class IgnitionConfig(
  stages: Seq[String],
  onError: String = ON_ERROR_STOP,
  principal: AnnettePrincipal,
  loaders: Map[String, ServiceLoaderConfig]
)
