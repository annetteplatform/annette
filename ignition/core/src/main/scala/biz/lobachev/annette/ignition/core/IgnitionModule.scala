package biz.lobachev.annette.ignition.core

import akka.actor.ActorSystem
import akka.stream.Materializer
import biz.lobachev.annette.attributes.api.{AttributeServiceApi, AttributeServiceImpl}
import biz.lobachev.annette.authorization.api.{AuthorizationServiceApi, AuthorizationServiceImpl}
import biz.lobachev.annette.ignition.core.attributes.{AttributeDataLoader, AttributeServiceLoader, SchemaLoader}
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

  lazy val attributeServiceApi    = serviceClient.implement[AttributeServiceApi]
  lazy val attributeService       = wire[AttributeServiceImpl]
  lazy val schemaLoader           = wire[SchemaLoader]
  lazy val attributeDataLoader    = wire[AttributeDataLoader]
  lazy val attributeServiceLoader = wire[AttributeServiceLoader]
}
