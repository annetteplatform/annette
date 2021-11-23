package biz.lobachev.annette.cms.api.content

import play.api.libs.json.{Format, JsValue, Json}

case class Widget(
  id: String,
  widgetType: String,
  data: JsValue,
  indexData: Option[String]
)

object Widget {
  implicit val format: Format[Widget] = Json.format
}
