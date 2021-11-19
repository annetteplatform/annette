package biz.lobachev.annette.cms.impl.pages.page.dao

import biz.lobachev.annette.cms.api.pages.page.PageId
import biz.lobachev.annette.cms.api.content.WidgetContent
import play.api.libs.json.JsValue

case class PageWidgetRecord(
  pageId: PageId,
  widgetContentId: String,
  widgetType: String,
  data: JsValue,
  indexData: Option[String]
) {
  def toWidgetContent: WidgetContent =
    WidgetContent(
      id = widgetContentId,
      widgetType = widgetType,
      data = data,
      indexData = indexData
    )
}
