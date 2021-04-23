package biz.lobachev.annette.blogs.api.post

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class DeletePostPayload(
  id: PostId,
  deletedBy: AnnettePrincipal
)

object DeletePostPayload {
  implicit val format: Format[DeletePostPayload] = Json.format
}
