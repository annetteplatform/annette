package biz.lobachev.annette.cms.api.blogs.post

import biz.lobachev.annette.cms.api.blogs.post.ContentTypes.ContentType
import biz.lobachev.annette.cms.api.content.WidgetContent
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import play.api.libs.json.{Format, Json}

case class UpdatePostWidgetContentPayload(
  id: String,
  contentType: ContentType,
  widgetContent: WidgetContent,
  order: Option[Int] = None,
  updatedBy: AnnettePrincipal
)

object UpdatePostWidgetContentPayload {
  implicit val format: Format[UpdatePostWidgetContentPayload] = Json.format
}
