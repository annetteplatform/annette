package biz.lobachev.annette.blogs.api.blog

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class DeactivateBlogPayload(
  id: BlogId,
  updatedBy: AnnettePrincipal
)

object DeactivateBlogPayload {
  implicit val format: Format[DeactivateBlogPayload] = Json.format
}
