package biz.lobachev.annette.blogs.impl.post.model

import biz.lobachev.annette.blogs.api.post.ContentType.ContentType
import biz.lobachev.annette.blogs.api.post.PublicationStatus.PublicationStatus
import biz.lobachev.annette.blogs.api.post._
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

import java.time.OffsetDateTime

case class PostState(
  id: PostId,
  featured: Boolean,
  authorId: AnnettePrincipal,
  title: String,
  introContentType: ContentType,
  introContent: String,
  contentType: ContentType,
  content: String,
  publicationStatus: PublicationStatus,
  publicationTimestamp: Option[OffsetDateTime],
  targets: Set[AnnettePrincipal] = Set.empty,
  media: Map[MediaId, Media],
  docs: Map[DocId, Doc],
  updatedBy: AnnettePrincipal,
  updatedAt: OffsetDateTime = OffsetDateTime.now
)

object PostState {
  implicit val format: Format[PostState] = Json.format
}
