package biz.lobachev.annette.cms.api.post

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class UpdatePostIntroPayload(
  id: PostId,
  introContent: PostContent,
  updatedBy: AnnettePrincipal
)

object UpdatePostIntroPayload {
  implicit val format: Format[UpdatePostIntroPayload] = Json.format
}
