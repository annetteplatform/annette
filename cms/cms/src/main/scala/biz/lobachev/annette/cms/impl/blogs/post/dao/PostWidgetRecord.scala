package biz.lobachev.annette.cms.impl.blogs.post.dao

import biz.lobachev.annette.cms.api.blogs.post.PostId
import biz.lobachev.annette.cms.api.content.ContentTypes.ContentType
import play.api.libs.json.JsValue

case class PostWidgetRecord(
  postId: PostId,
  contentType: ContentType,
  widgetId: String,
  widgetType: String,
  data: JsValue,
  indexData: Option[String]
)
