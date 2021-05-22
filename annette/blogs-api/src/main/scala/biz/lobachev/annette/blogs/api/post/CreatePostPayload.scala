package biz.lobachev.annette.blogs.api.post

import biz.lobachev.annette.blogs.api.blog.BlogId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class CreatePostPayload(
  id: PostId,
  blogId: BlogId,
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
