package biz.lobachev.annette.cms.impl.blogs.post.dao

import biz.lobachev.annette.cms.api.blogs.post.PostId
import biz.lobachev.annette.cms.api.blogs.post.ContentTypes.ContentType
import play.api.libs.json.JsValue

case class PostWidgetRecord(
  postId: PostId,
  contentType: ContentType,
  widgetContentId: String,
  widgetType: String,
  data: JsValue,
  indexData: Option[String]
)
