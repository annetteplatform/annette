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

package biz.lobachev.annette.authorization.api.assignment

import play.api.libs.json.{Format, Json}

case class AssignmentFindResult(
  total: Long, // total items in query
  hits: Seq[AssignmentHitResult] // results of search
)

object AssignmentFindResult {
  implicit val format: Format[AssignmentFindResult] = Json.format
}

case class AssignmentHitResult(
  id: String, //  id
  score: Float, // store of this hit
  assignment: PermissionAssignment // date/time of last update
)

object AssignmentHitResult {
  implicit val format: Format[AssignmentHitResult] = Json.format
}
