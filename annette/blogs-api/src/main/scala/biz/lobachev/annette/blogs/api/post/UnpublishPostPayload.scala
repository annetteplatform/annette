package biz.lobachev.annette.blogs.api.post

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class UnpublishPostPayload(
  id: PostId,
  updatedBy: AnnettePrincipal
)

object UnpublishPostPayload {
  implicit val format: Format[UnpublishPostPayload] = Json.format
}
