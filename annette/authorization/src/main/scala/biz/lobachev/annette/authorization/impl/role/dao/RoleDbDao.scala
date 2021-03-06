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

package biz.lobachev.annette.authorization.impl.role.dao

import akka.Done
import biz.lobachev.annette.authorization.api.role._
import biz.lobachev.annette.authorization.impl.role.RoleEntity
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import com.datastax.driver.core.BoundStatement

import scala.concurrent.Future

trait RoleDbDao {

  def createTables(): Future[Done]

  def prepareStatements(): Future[Done]

  def createRole(event: RoleEntity.RoleCreated): List[BoundStatement]

  def updateRole(event: RoleEntity.RoleUpdated): List[BoundStatement]

  def deleteRole(event: RoleEntity.RoleDeleted): List[BoundStatement]

  def assignPrincipal(event: RoleEntity.PrincipalAssigned): List[BoundStatement]

  def unassignPrincipal(event: RoleEntity.PrincipalUnassigned): List[BoundStatement]

  def getRoleById(id: AuthRoleId): Future[Option[AuthRole]]

  def getRolePrincipals(id: AuthRoleId): Future[Option[Set[AnnettePrincipal]]]

  def getRoleById(ids: Set[AuthRoleId]): Future[Map[AuthRoleId, AuthRole]]

}
