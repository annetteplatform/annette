package biz.lobachev.annette.cms.api.content

import play.api.libs.json.{Format, JsValue, Json}

case class Content(
  settings: JsValue,
  widgetOrder: Seq[String],
  widgets: Map[String, Widget]
)

object Content {
  implicit val format: Format[Content] = Json.format

}
