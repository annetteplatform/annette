package biz.lobachev.annette.blogs.api.post

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

import java.time.OffsetDateTime

case class PostAnnotation(
  id: PostId,
  featured: Boolean,
  authorId: AnnettePrincipal,
  title: String,
  introContentType: ContentType.ContentType,
  introContent: String,
  publicationStatus: PublicationStatus.PublicationStatus,
  publicationTimestamp: Option[OffsetDateTime],
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime = OffsetDateTime.now
)

object PostAnnotation {
  implicit val format: Format[PostAnnotation] = Json.format
}
