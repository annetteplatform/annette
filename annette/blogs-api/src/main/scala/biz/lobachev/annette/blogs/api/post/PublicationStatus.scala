package biz.lobachev.annette.blogs.api.post

import play.api.libs.json.Json

object PublicationStatus extends Enumeration {
  type PublicationStatus = Value

  val HTML     = Value("html")
  val Markdown = Value("markdown")

  implicit val format = Json.formatEnum(this)
}
