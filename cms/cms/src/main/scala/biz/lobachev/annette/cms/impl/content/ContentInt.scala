/*
 * Copyright 2013 Valery Lobachev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
