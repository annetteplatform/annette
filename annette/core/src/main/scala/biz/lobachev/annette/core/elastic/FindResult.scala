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

package biz.lobachev.annette.core.elastic

import java.time.OffsetDateTime

import play.api.libs.json.{Format, Json}

case class FindResult(
  total: Long, // total items in query
  hits: Seq[HitResult] // results of search
)

object FindResult {
  implicit val format: Format[FindResult] = Json.format
}

case class HitResult(
  id: String, //  id
  score: Float, // store of this hit
  updatedAt: OffsetDateTime // date/time of last update
)

object HitResult {
  implicit val format: Format[HitResult] = Json.format
}
