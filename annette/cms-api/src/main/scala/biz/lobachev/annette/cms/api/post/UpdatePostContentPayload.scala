package biz.lobachev.annette.cms.api.post

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class UpdatePostContentPayload(
  id: PostId,
  content: PostContent,
  updatedBy: AnnettePrincipal
)

object UpdatePostContentPayload {
  implicit val format: Format[UpdatePostContentPayload] = Json.format
}
