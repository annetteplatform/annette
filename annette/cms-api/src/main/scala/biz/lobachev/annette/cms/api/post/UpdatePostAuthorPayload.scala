package biz.lobachev.annette.cms.api.post

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class UpdatePostAuthorPayload(
  id: PostId,
  authorId: AnnettePrincipal,
  updatedBy: AnnettePrincipal
)

object UpdatePostAuthorPayload {
  implicit val format: Format[UpdatePostAuthorPayload] = Json.format
}
