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

package biz.lobachev.annette.ignition.core

import akka.actor.ActorSystem
import akka.stream.Materializer
import biz.lobachev.annette.authorization.api.{AuthorizationServiceApi, AuthorizationServiceImpl}
import biz.lobachev.annette.ignition.core.authorization.{AssignmentLoader, AuthorizationServiceLoader, RoleLoader}
import biz.lobachev.annette.ignition.core.org_structure.{
  OrgCategoryLoader,
  OrgRoleLoader,
  OrgStructureLoader,
  OrgStructureServiceLoader
}
import biz.lobachev.annette.ignition.core.persons.{PersonCategoryLoader, PersonServiceLoader, PersonsLoader}
import biz.lobachev.annette.org_structure.api.{OrgStructureServiceApi, OrgStructureServiceImpl}
import biz.lobachev.annette.persons.api.{PersonServiceApi, PersonServiceImpl}
import com.lightbend.lagom.scaladsl.client.ServiceClient
import com.softwaremill.macwire.wire

import scala.concurrent.ExecutionContext

class IgnitionModule(
  val serviceClient: ServiceClient,
  val actorSystem: ActorSystem,
  val executionContext: ExecutionContext,
  val materializer: Materializer
) {
  lazy val personServiceApi     = serviceClient.implement[PersonServiceApi]
  lazy val personService        = wire[PersonServiceImpl]
  lazy val personCategoryLoader = wire[PersonCategoryLoader]
  lazy val personsLoader        = wire[PersonsLoader]
  lazy val personServiceLoader  = wire[PersonServiceLoader]

  lazy val authorizationServiceApi    = serviceClient.implement[AuthorizationServiceApi]
  lazy val authorizationService       = wire[AuthorizationServiceImpl]
  lazy val roleLoader                 = wire[RoleLoader]
  lazy val assignmentLoader           = wire[AssignmentLoader]
  lazy val authorizationServiceLoader = wire[AuthorizationServiceLoader]

  lazy val orgStructureServiceApi    = serviceClient.implement[OrgStructureServiceApi]
  lazy val orgStructureService       = wire[OrgStructureServiceImpl]
  lazy val orgCategoryLoader         = wire[OrgCategoryLoader]
  lazy val orgRoleLoader             = wire[OrgRoleLoader]
  lazy val orgStructureLoader        = wire[OrgStructureLoader]
  lazy val orgStructureServiceLoader = wire[OrgStructureServiceLoader]
  lazy val ignition: AnnetteIgnition = wire[AnnetteIgnition]

}