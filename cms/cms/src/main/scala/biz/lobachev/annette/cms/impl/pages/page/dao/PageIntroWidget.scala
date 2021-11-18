package biz.lobachev.annette.cms.impl.pages.page.dao

import play.api.libs.json.JsValue
import biz.lobachev.annette.cms.api.pages.page.PageId

case class PageIntroWidget(
  pageId: PageId,
  widgetContentId: String,
  widgetType: String,
  data: JsValue,
  indexData: Option[String]
)
