package biz.lobachev.annette.blogs.impl.post.model

import biz.lobachev.annette.blogs.api.blog.BlogId
import biz.lobachev.annette.blogs.api.post.PublicationStatus.PublicationStatus
import biz.lobachev.annette.blogs.api.post._
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

import java.time.OffsetDateTime

case class PostState(
  id: PostId,
  blogId: BlogId,
  featured: Boolean,
  authorId: AnnettePrincipal,
  title: String,
  introContent: PostContent,
  content: PostContent,
  publicationStatus: PublicationStatus = PublicationStatus.Draft,
  publicationTimestamp: Option[OffsetDateTime] = None,
  targets: Set[AnnettePrincipal] = Set.empty,
  media: Map[MediaId, Media] = Map.empty,
  docs: Map[DocId, Doc] = Map.empty,
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime = OffsetDateTime.now
)

object PostState {
  implicit val format: Format[PostState] = Json.format
}
