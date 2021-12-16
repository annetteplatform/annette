package biz.lobachev.annette.cms.impl.content

import biz.lobachev.annette.cms.api.content.Widget
import play.api.libs.json.{Format, Json}

case class WidgetInt(
  id: String,
  widgetType: String,
  data: String,
  indexData: Option[String]
) {
  def toWidget: Widget = Widget(id, widgetType, Json.parse(data), indexData)

}

object WidgetInt {
  def fromWidget(v: Widget): WidgetInt = WidgetInt(v.id, v.widgetType, v.data.toString(), v.indexData)

  implicit val format: Format[WidgetInt] = Json.format
}
