package biz.lobachev.annette.console_ignition

import akka.Done
import biz.lobachev.annette.ignition.core.IgnitionModule
import biz.lobachev.annette.persons.api.{PersonServiceApi, PersonServiceImpl}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object ConsoleIgnitionApp extends App {
  val client = new ConsoleIgnitionServiceClient()

  val personServiceApi = client.serviceClient.implement[PersonServiceApi]
  val personService    = new PersonServiceImpl(personServiceApi, client.executionContext)
  implicit val ec      = client.executionContext

  val future = for {
    person <- personService.getPersonById("P0001", false, Some("all"))
  } yield println(s"Hello ${person.firstname} ${person.lastname}")

  Await.result(future, Duration.Inf)

  lazy val ignitionModule: IgnitionModule =
    new IgnitionModule(client.serviceClient, client.actorSystem, client.executionContext, client.materializer)
  import ignitionModule._

  val ignitionFuture = for {
    _ <- ignition.run()
  } yield Done

  Await.result(ignitionFuture, Duration.Inf)
}
