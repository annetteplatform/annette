package biz.lobachev.annette.blogs.api.post

import play.api.libs.json.Json

object ContentType extends Enumeration {
  type ContentType = Value

  val HTML     = Value("html")
  val Markdown = Value("markdown")

  implicit val format = Json.formatEnum(this)
}
