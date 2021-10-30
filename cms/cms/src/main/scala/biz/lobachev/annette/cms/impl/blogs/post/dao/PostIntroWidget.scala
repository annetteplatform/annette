package biz.lobachev.annette.cms.impl.blogs.post.dao

import play.api.libs.json.JsValue
import biz.lobachev.annette.cms.api.blogs.post.PostId

case class PostIntroWidget(
  postId: PostId,
  widgetContentId: String,
  widgetType: String,
  data: JsValue,
  indexData: Option[String]
)
