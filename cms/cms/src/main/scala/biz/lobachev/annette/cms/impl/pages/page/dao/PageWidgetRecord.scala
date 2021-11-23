package biz.lobachev.annette.cms.impl.pages.page.dao

import biz.lobachev.annette.cms.api.content.Widget
import biz.lobachev.annette.cms.api.pages.page.PageId
import play.api.libs.json.JsValue

case class PageWidgetRecord(
  pageId: PageId,
  widgetId: String,
  widgetType: String,
  data: JsValue,
  indexData: Option[String]
) {
  def toWidget: Widget =
    Widget(
      id = widgetId,
      widgetType = widgetType,
      data = data,
      indexData = indexData
    )
}
