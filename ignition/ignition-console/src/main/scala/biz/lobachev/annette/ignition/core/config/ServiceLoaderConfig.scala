package biz.lobachev.annette.ignition.core.config

case class ServiceLoaderConfig(
  name: Option[String],
  stage: String,
  `type`: String,
  onError: String = ON_ERROR_STOP,
  sequence: Seq[String],
  entities: Map[String, EntityLoaderConfig]
)
