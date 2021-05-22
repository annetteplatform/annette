package biz.lobachev.annette.blogs.api.post

import biz.lobachev.annette.blogs.api.blog.BlogId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

import java.time.OffsetDateTime

case class PostAnnotation(
  id: PostId,
  blogId: BlogId,
  featured: Boolean,
  authorId: AnnettePrincipal,
  title: String,
  introContent: PostContent,
  publicationStatus: PublicationStatus.PublicationStatus,
  publicationTimestamp: Option[OffsetDateTime],
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime = OffsetDateTime.now
)

object PostAnnotation {
  implicit val format: Format[PostAnnotation] = Json.format
}
