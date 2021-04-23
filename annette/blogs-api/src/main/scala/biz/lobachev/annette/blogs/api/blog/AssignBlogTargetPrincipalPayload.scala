package biz.lobachev.annette.blogs.api.blog

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class AssignBlogTargetPrincipalPayload(
  id: BlogId,
  principal: AnnettePrincipal,
  updatedBy: AnnettePrincipal
)

object AssignBlogTargetPrincipalPayload {
  implicit val format: Format[AssignBlogTargetPrincipalPayload] = Json.format
}
