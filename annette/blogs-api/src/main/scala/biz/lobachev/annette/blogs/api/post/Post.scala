package biz.lobachev.annette.blogs.api.post

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

import java.time.OffsetDateTime

case class Post(
  id: PostId,
  featured: Boolean,
  authorId: AnnettePrincipal,
  title: String,
  introContentType: ContentType.ContentType,
  introContent: String,
  contentType: ContentType.ContentType,
  content: String,
  publicationStatus: PublicationStatus.PublicationStatus,
  publicationTimestamp: Option[OffsetDateTime],
  targets: Set[AnnettePrincipal] = Set.empty,
  media: Map[MediaId, Media],
  docs: Map[DocId, Doc],
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime = OffsetDateTime.now
)

object Post {
  implicit val format: Format[Post] = Json.format
}
