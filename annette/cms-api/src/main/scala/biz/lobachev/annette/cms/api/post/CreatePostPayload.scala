package biz.lobachev.annette.cms.api.post

import biz.lobachev.annette.cms.api.space.SpaceId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class CreatePostPayload(
  id: PostId,
  spaceId: SpaceId,
  featured: Boolean,
  authorId: AnnettePrincipal,
  title: String,
  introContent: PostContent,
  content: PostContent,
  targets: Set[AnnettePrincipal] = Set.empty,
  createdBy: AnnettePrincipal
)

object CreatePostPayload {
  implicit val format: Format[CreatePostPayload] = Json.format
}
