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

package biz.lobachev.annette.authorization.impl.assignment.dao

import akka.Done
import biz.lobachev.annette.authorization.api.assignment._
import biz.lobachev.annette.authorization.impl.assignment.AssignmentEntity

import scala.concurrent.Future

trait AssignmentIndexDao {

  def createEntityIndex(): Future[Done]

  def assignPermission(event: AssignmentEntity.PermissionAssigned): Future[Unit]

  def unassignPermission(event: AssignmentEntity.PermissionUnassigned): Future[Unit]

  def findAssignments(query: FindAssignmentsQuery): Future[AssignmentFindResult]

}
