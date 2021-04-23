package biz.lobachev.annette.blogs.api.post

import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

import java.time.OffsetDateTime

case class UpdatePostPublicationTimestampPayload(
  id: PostId,
  publicationTimestamp: Option[OffsetDateTime],
  updatedBy: AnnettePrincipal
)

object UpdatePostPublicationTimestampPayload {
  implicit val format: Format[UpdatePostPublicationTimestampPayload] = Json.format
}
