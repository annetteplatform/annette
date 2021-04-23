package biz.lobachev.annette.blogs.api.blog

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class ActivateBlogPayload(
  id: BlogId,
  updatedBy: AnnettePrincipal
)

object ActivateBlogPayload {
  implicit val format: Format[ActivateBlogPayload] = Json.format
}
