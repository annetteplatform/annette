package biz.lobachev.annette.core.model

object DataSource {
  val ORIGIN    = "origin"
  val READ_SIDE = "read"
  val CACHE     = "cache"

  val FROM_ORIGIN    = Some(ORIGIN)
  val FROM_READ_SIDE = Some(READ_SIDE)

  def fromOrigin(source: Option[String]): Boolean = source.getOrElse(READ_SIDE) == ORIGIN
}
