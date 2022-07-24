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

import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import biz.lobachev.annette.ignition.core.config.{ServiceLoaderConfig, StopOnError}
import biz.lobachev.annette.ignition.core.result.{EntityLoadResult, LoadFailed, LoadOk, ServiceLoadResult}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

trait ServiceLoader[C <: ServiceLoaderConfig] {
  val name: String
  val client: IgnitionLagomClient
  val config: C
  implicit val ec: ExecutionContext       = client.executionContext
  implicit val materializer: Materializer = client.materializer

  protected val log: Logger = LoggerFactory.getLogger(this.getClass)

  def createEntityLoader(entity: String): EntityLoader[_, _]

  def run(): Future[ServiceLoadResult] =
    Source(config.entities)
      .mapAsync(1) { entity =>
        val entityLoader = createEntityLoader(entity)
        entityLoader.run()
      }
      .takeWhile(
        {
          case EntityLoadResult(_, LoadFailed(_), _) if config.onError == StopOnError => false
          case _                                                                      => true
        },
        inclusive = true
      )
      .runWith(Sink.seq)
      .map(seq =>
        if (config.onError == StopOnError)
          ServiceLoadResult(name, seq.last.status, seq)
        else
          ServiceLoadResult(name, LoadOk, seq)
      )

}
