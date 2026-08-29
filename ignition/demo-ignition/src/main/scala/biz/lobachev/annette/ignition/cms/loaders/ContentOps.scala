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

package biz.lobachev.annette.ignition.cms.loaders

import org.apache.pekko.Done
import biz.lobachev.annette.cms.api.content.{Content, Widget}
import play.api.libs.json.{JsObject, JsValue, Json}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Prepares widget content for the CMS and keeps existing content in sync with the
 * demo data. The CMS frontend renders markdown widgets with an editor-shaped payload
 * (`anchor`, `markdown`, `layout`); demo data may omit the editor defaults, and the
 * searchable plain text (`indexData`) is derived from the markdown body.
 */
object ContentOps {

  val empty: Content = Content(Json.obj(), Seq.empty, Map.empty)

  private val defaultLayout: JsValue = Json.parse(
    """
      |{
      |  "padding": {"top": "0px", "right": "0px", "bottom": "0px", "left": "0px"},
      |  "margin": {"top": "0px", "right": "0px", "bottom": "0px", "left": "0px"},
      |  "backgroundColor": "#ffffff"
      |}
    """.stripMargin
  )

  def prepare(content: Content): Content =
    withDerivedIndexData(
      content.copy(
        widgets = content.widgets.map { case (key, widget) =>
          val data =
            if (widget.widgetType == "markdown") fillEditorDefaults(widget.data)
            else widget.data
          key -> widget.copy(data = data)
        }
      )
    )

  private def fillEditorDefaults(data: JsValue): JsValue = {
    val filled = Json.obj("anchor" -> "") ++ data.asOpt[JsObject].getOrElse(Json.obj())
    (filled \ "layout").asOpt[JsObject].map(_ => filled).getOrElse(filled ++ Json.obj("layout" -> defaultLayout))
  }

  private def withDerivedIndexData(content: Content): Content =
    content.copy(
      widgets = content.widgets.map { case (key, widget) =>
        val indexData = widget.indexData.orElse(
          (widget.data \ "markdown")
            .asOpt[String]
            .orElse((widget.data \ "text").asOpt[String])
            .orElse((widget.data \ "content").asOpt[String])
        )
        key -> widget.copy(indexData = indexData)
      }
    )

  /**
   * Converges one content block to the desired state: widgets are upserted at their
   * data-defined position, widgets that are no longer in the data are deleted.
   */
  def replaceWidgets(
    update: (Widget, Int) => Future[Done],
    delete: String => Future[Done],
    desired: Content,
    current: Option[Content]
  )(implicit ec: ExecutionContext): Future[Done] = {
    val stale = current.map(_.widgets.keySet -- desired.widgets.keySet).getOrElse(Set.empty)
    val upserts = desired.widgetOrder.zipWithIndex.collect {
      case (widgetId, order) if desired.widgets.contains(widgetId) => (desired.widgets(widgetId), order)
    }.foldLeft(Future.successful(Done): Future[Done]) { case (acc, (widget, order)) =>
      acc.flatMap(_ => update(widget, order))
    }
    val deletions = stale.foldLeft(Future.successful(Done): Future[Done]) { (acc, widgetId) =>
      acc.flatMap(_ => delete(widgetId))
    }
    for {
      _ <- upserts
      _ <- deletions
    } yield Done
  }

}
