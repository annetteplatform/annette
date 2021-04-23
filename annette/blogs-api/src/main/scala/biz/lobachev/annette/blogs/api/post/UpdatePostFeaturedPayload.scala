package biz.lobachev.annette.blogs.api.post

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class UpdatePostFeaturedPayload(
  id: PostId,
  featured: Boolean,
  updatedBy: AnnettePrincipal
)

object UpdatePostFeaturedPayload {
  implicit val format: Format[UpdatePostFeaturedPayload] = Json.format
}
