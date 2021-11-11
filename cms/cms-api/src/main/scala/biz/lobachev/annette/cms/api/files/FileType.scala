package biz.lobachev.annette.cms.api.files

import play.api.libs.json.Json

object FileTypes extends Enumeration {
  type FileType = Value

  val Media = Value("media")
  val Doc   = Value("post")

  implicit val format = Json.formatEnum(this)
}
