package biz.lobachev.annette.cms.impl.content

import biz.lobachev.annette.cms.api.content.Content
import play.api.libs.json.{Format, Json}

case class ContentInt(
  settings: String,
  widgetOrder: Seq[String],
  widgets: Map[String, WidgetInt]
) {
  def toContent: Content =
    Content(
      settings = Json.parse(settings),
      widgetOrder = widgetOrder,
      widgets = widgets.map { case k -> v => k -> v.toWidget }
    )
}

object ContentInt {
  def fromContent(content: Content): ContentInt =
    ContentInt(
      settings = content.settings.toString(),
      widgetOrder = content.widgetOrder,
      widgets = content.widgets.map { case k -> v => k -> WidgetInt.fromWidget(v) }
    )

  implicit val format: Format[ContentInt] = Json.format

}
