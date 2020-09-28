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
import com.datastax.driver.core.BoundStatement

import scala.collection.immutable._
import scala.concurrent.Future

trait AssignmentDbDao {

  def createTables(): Future[Done]

  def prepareStatements(): Future[Done]

  def assignPermission(event: AssignmentEntity.PermissionAssigned): BoundStatement

  def unassignPermission(event: AssignmentEntity.PermissionUnassigned): BoundStatement

  def checkAnyPermission(payload: CheckPermissions): Future[Boolean]

  def checkAllPermission(payload: CheckPermissions): Future[Boolean]

  def findPermissions(payload: FindPermissions): Future[Set[PermissionAssignment]]

}
