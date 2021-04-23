package biz.lobachev.annette.blogs.api.post

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class PublishPostPayload(
  id: PostId,
  updatedBy: AnnettePrincipal
)

object PublishPostPayload {
  implicit val format: Format[PublishPostPayload] = Json.format
}
