package biz.lobachev.annette.cms.api.space

import play.api.libs.json.Json

object SpaceType extends Enumeration {
  type SpaceType = Value

  val Wiki = Value("wiki")
  val Blog = Value("blog")

  def from(spaceType: String): SpaceType.SpaceType =
    spaceType match {
      case "wiki" => SpaceType.Wiki
      case _      => SpaceType.Blog
    }

  implicit val format = Json.formatEnum(this)
}
