package biz.lobachev.annette.cms.api.content

import play.api.libs.json.Json

object ContentTypes extends Enumeration {
  type ContentType = Value

  val Intro = Value("intro")
  val Post  = Value("post")

  implicit val format = Json.formatEnum(this)
}