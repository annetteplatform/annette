package biz.lobachev.annette.cms.api.post

import biz.lobachev.annette.cms.api.space.SpaceId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

import java.time.OffsetDateTime

case class PostAnnotation(
  id: PostId,
  spaceId: SpaceId,
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
