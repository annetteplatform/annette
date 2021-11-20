package biz.lobachev.annette.cms.impl.content

import biz.lobachev.annette.cms.api.common.{SerialContent, WidgetContent}
import play.api.libs.json.{Format, Json}

case class Content(
  contentOrder: Seq[String],
  content: Map[String, WidgetContent]
) {
  def toSerialContent: SerialContent = contentOrder.map(content.get(_).map(_.copy(indexData = None))).flatten
}

object Content {
  implicit val format: Format[Content] = Json.format

  def fromSerialContent(serialContent: SerialContent): Content =
    Content(
      serialContent.map(_.id),
      serialContent.map(c => c.id -> c).toMap
    )
}
