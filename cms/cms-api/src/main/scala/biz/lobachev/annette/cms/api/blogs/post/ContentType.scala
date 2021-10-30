package biz.lobachev.annette.cms.api.blogs.post

import play.api.libs.json.Json

object ContentTypes extends Enumeration {
  type ContentType = Value

  val Intro = Value("intro")
  val Post  = Value("post")

  def from(spaceType: String): ContentTypes.ContentType =
    spaceType match {
      case "wiki" => ContentTypes.Intro
      case _      => ContentTypes.Post
    }

  implicit val format = Json.formatEnum(this)
}
