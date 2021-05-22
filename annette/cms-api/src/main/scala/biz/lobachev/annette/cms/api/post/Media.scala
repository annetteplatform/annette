package biz.lobachev.annette.cms.api.post

import play.api.libs.json.{Format, Json}

case class Media(
  id: MediaId,
  filename: String
)

object Media {
  implicit val format: Format[Media] = Json.format
}
