package biz.lobachev.annette.ignition.console

import akka.Done
import biz.lobachev.annette.ignition.core.{Ignition, IgnitionLagomClient, ServiceLoaderFactory}
import biz.lobachev.annette.ignition.service_catalog.ServiceCatalogLoaderFactory

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object ConsoleIgnitionApp extends App {
  val client      = new IgnitionLagomClient()
  implicit val ec = client.executionContext

  val factories: Map[String, ServiceLoaderFactory] = Map(
    "service-catalog" -> ServiceCatalogLoaderFactory
  )
  val ignition                                     = new Ignition(client, factories)

  val ignitionFuture = for {
    _ <- ignition.run()
    _ <- client.actorSystem.terminate()
  } yield {
    println("Actor System terminated")
    Done
  }

  Await.ready(ignitionFuture, Duration.Inf)
  println("Exiting")
  System.exit(0)
}
