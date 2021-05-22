package biz.lobachev.annette.cms.api.post

import play.api.libs.json.Json

object PublicationStatus extends Enumeration {
  type PublicationStatus = Value

  val Draft     = Value("draft")
  val Published = Value("published")

  implicit val format = Json.formatEnum(this)
}
