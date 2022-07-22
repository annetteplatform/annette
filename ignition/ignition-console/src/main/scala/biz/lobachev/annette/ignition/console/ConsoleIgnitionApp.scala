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

package biz.lobachev.annette.ignition.console

import akka.Done
import biz.lobachev.annette.ignition.application.ApplicationLoaderFactory
import biz.lobachev.annette.ignition.core.{Ignition, IgnitionLagomClient, ServiceLoaderFactory}
import biz.lobachev.annette.ignition.service_catalog.ServiceCatalogLoaderFactory

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object ConsoleIgnitionApp extends App {
  val client      = new IgnitionLagomClient()
  implicit val ec = client.executionContext

  val factories: Map[String, ServiceLoaderFactory] = Map(
    "service-catalog" -> ServiceCatalogLoaderFactory,
    "application"     -> ApplicationLoaderFactory
  )

  val ignition = new Ignition(client, factories)

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
