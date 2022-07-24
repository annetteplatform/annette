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

package biz.lobachev.annette.ignition.authorization

import biz.lobachev.annette.authorization.api.{AuthorizationServiceApi, AuthorizationServiceImpl}
import biz.lobachev.annette.ignition.authorization.loaders.{RoleAssignmentEntityLoader, RoleEntityLoader}
import biz.lobachev.annette.ignition.core.{EntityLoader, IgnitionLagomClient, ServiceLoader}
import com.softwaremill.macwire.wire

class AuthorizationLoader(val client: IgnitionLagomClient, val config: AuthorizationLoaderConfig)
    extends ServiceLoader[AuthorizationLoaderConfig] {

  lazy val serviceApi = client.serviceClient.implement[AuthorizationServiceApi]
  lazy val service    = wire[AuthorizationServiceImpl]

  override val name: String = "authorization"

  override def createEntityLoader(entity: String): EntityLoader[_, _] =
    entity match {
      case AuthorizationLoader.Role           => new RoleEntityLoader(service, config.role.get)
      case AuthorizationLoader.RoleAssignment => new RoleAssignmentEntityLoader(service, config.roleAssignment.get)
    }
}

object AuthorizationLoader {
  val Role: String           = "role"
  val RoleAssignment: String = "role-assignment"

}
