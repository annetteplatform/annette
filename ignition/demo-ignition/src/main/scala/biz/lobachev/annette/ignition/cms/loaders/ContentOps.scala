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

import biz.lobachev.annette.cms.api.content.Content
import play.api.libs.json.Json

/**
 * Helpers shared by the post and page entity loaders: widgets carry their searchable
 * plain text in `indexData`; when the demo data omits it, it is derived from the
 * widget's `text`/`content` data field so full-text search works out of the box.
 */
object ContentOps {

  val empty: Content = Content(Json.obj(), Seq.empty, Map.empty)

  def withDerivedIndexData(content: Content): Content =
    content.copy(
      widgets = content.widgets.map { case (key, widget) =>
        val indexData = widget.indexData.orElse(
          (widget.data \ "text").asOpt[String].orElse((widget.data \ "content").asOpt[String])
        )
        key -> widget.copy(indexData = indexData)
      }
    )

}
