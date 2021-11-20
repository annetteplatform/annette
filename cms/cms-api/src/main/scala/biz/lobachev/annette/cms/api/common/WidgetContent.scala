package biz.lobachev.annette.cms.api.common

import play.api.libs.json.{Format, JsValue, Json}

case class WidgetContent(
  id: String,
  widgetType: String,
  data: JsValue,
  indexData: Option[String]
)

object WidgetContent {
  implicit val format: Format[WidgetContent] = Json.format
}
