package biz.lobachev.annette.cms.api.post

import play.api.libs.json.{Format, Json}

case class Doc(
  id: MediaId,
  name: String,
  filename: String
)

object Doc {
  implicit val format: Format[Doc] = Json.format
}
