package biz.lobachev.annette.ignition.core

import akka.actor.ActorSystem
import akka.stream.Materializer
import biz.lobachev.annette.ignition.core.persons.{PersonCategoryLoader, PersonServiceLoader, PersonsLoader}
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
  lazy val personServiceApi          = serviceClient.implement[PersonServiceApi]
  lazy val personService             = wire[PersonServiceImpl]
  lazy val personCategoryLoader      = wire[PersonCategoryLoader]
  lazy val personsLoader             = wire[PersonsLoader]
  lazy val personServiceLoader       = wire[PersonServiceLoader]
  lazy val ignition: AnnetteIgnition = wire[AnnetteIgnition]
}
