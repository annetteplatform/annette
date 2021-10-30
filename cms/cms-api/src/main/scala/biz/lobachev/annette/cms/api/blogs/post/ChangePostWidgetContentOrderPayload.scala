package biz.lobachev.annette.cms.api.blogs.post

import biz.lobachev.annette.cms.api.blogs.post.ContentTypes.ContentType
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class ChangePostWidgetContentOrderPayload(
  id: String,
  contentType: ContentType,
  widgetContentId: String,
  order: Int,
  updatedBy: AnnettePrincipal
)

object ChangePostWidgetContentOrderPayload {
  implicit val format: Format[ChangePostWidgetContentOrderPayload] = Json.format
}
