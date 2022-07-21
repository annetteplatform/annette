package biz.lobachev.annette.ignition.core.config

case class EntityLoaderConfig(
  name: Option[String],
  `type`: String,
  onError: String = ON_ERROR_STOP,
  mode: String = MODE_UPSERT,
  parallelism: Int = 1
)
