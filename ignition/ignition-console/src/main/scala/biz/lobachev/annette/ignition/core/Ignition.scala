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

import akka.stream.scaladsl.{Sink, Source}
import biz.lobachev.annette.ignition.core.config.{IgnoreError, StopOnError}
import biz.lobachev.annette.ignition.core.result.{LoadFailed, ServiceLoadResult}
import com.typesafe.config.Config
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._
import scala.util.Try

class Ignition(
  client: IgnitionLagomClient,
  factories: Map[String, ServiceLoaderFactory]
) {
  implicit val ec: ExecutionContext = client.executionContext
  implicit val materializer         = client.materializer

  protected val log: Logger = LoggerFactory.getLogger(this.getClass)

  def run() = {
    val config  = client.config.getConfig("annette.ignition")
    val stages  = Try(config.getStringList("stages").asScala.toSeq).getOrElse(Seq.empty)
    val onError = Try(if (config.getString("on-error") == "ignore") IgnoreError else StopOnError)
      .getOrElse(IgnoreError)
    Source(stages)
      .mapAsync(1) { stage =>
        val future = runStage(stage, config)
        future.failed.foreach(th => log.error(s"Stage $stage failed ", th))
        future
      }
      .takeWhile(
        {
          case ServiceLoadResult(_, LoadFailed(_), _) if onError == StopOnError => false
          case _                                                                => true
        },
        inclusive = true
      )
      .runWith(Sink.seq)
      .map { seq =>
        println()
        println()
        println()
        seq.flatMap(_.toStrings()).foreach(println)
        println()
        println()
        println()
        seq.flatMap(_.toStrings()).foreach(log.info)
      }
  }

  def runStage(stage: String, config: Config) =
    try {
      log.info(s"Running stage $stage")
      val loader = factories(stage).create(client, config.getConfig(stage))
      for {
        res <- loader.run()
      } yield {
        log.info(s"Stage $stage completed")
        res
      }
    } catch {
      case th: Throwable => Future.failed(th)
    }
}
