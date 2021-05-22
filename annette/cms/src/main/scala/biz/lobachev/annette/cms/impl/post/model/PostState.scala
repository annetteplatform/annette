package biz.lobachev.annette.cms.impl.post.model

import biz.lobachev.annette.cms.api.post.PublicationStatus.PublicationStatus
import biz.lobachev.annette.cms.api.post._
import biz.lobachev.annette.cms.api.space.SpaceId
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

import java.time.OffsetDateTime

case class PostState(
  id: PostId,
  spaceId: SpaceId,
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
