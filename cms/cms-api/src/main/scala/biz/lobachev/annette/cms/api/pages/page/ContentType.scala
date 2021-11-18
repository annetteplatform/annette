package biz.lobachev.annette.cms.api.pages.page

import play.api.libs.json.Json

object ContentTypes extends Enumeration {
  type ContentType = Value

  val Intro = Value("intro")
  val Page  = Value("page")

  implicit val format = Json.formatEnum(this)
}
