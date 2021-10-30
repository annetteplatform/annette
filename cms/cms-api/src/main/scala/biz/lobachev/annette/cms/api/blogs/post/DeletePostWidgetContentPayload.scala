package biz.lobachev.annette.cms.api.blogs.post

import biz.lobachev.annette.cms.api.blogs.post.ContentTypes.ContentType
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class DeletePostWidgetContentPayload(
  id: String,
  contentType: ContentType,
  widgetContentId: String,
  updatedBy: AnnettePrincipal
)

object DeletePostWidgetContentPayload {
  implicit val format: Format[DeletePostWidgetContentPayload] = Json.format
}
