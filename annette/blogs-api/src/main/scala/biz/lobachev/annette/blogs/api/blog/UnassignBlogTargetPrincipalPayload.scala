package biz.lobachev.annette.blogs.api.blog

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class UnassignBlogTargetPrincipalPayload(
  id: BlogId,
  principal: AnnettePrincipal,
  updatedBy: AnnettePrincipal
)

object UnassignBlogTargetPrincipalPayload {
  implicit val format: Format[UnassignBlogTargetPrincipalPayload] = Json.format
}
