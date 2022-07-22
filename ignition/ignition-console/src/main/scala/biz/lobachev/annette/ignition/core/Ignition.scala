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

import akka.Done
import akka.stream.scaladsl.{Sink, Source}
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.ignition.core.config.{ON_ERROR_IGNORE, ON_ERROR_STOP}
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

  def run(): Future[Done] = {
    val config    = client.config.getConfig("annette.ignition")
    val stages    = Try(config.getStringList("stages").asScala.toSeq).getOrElse(Seq.empty)
    val onError   = Try(config.getString("on-error")).getOrElse(ON_ERROR_IGNORE)
    val principal = AnnettePrincipal.fromCode(config.getString("principal"))
    Source(stages)
      .mapAsync(1) { stage =>
        val future = runStage(stage, config, principal)
        if (onError == ON_ERROR_STOP) future
        else
          future.recover {
            case th =>
              log.error(s"Stage $stage failed ", th)
              Seq(Done)
          }
      }
      .runWith(Sink.ignore)
  }

  def runStage(stage: String, config: Config, principal: AnnettePrincipal): Future[Done] =
    try {
      log.info(s"Running stage $stage")
      val stageConfig = config.getConfig(stage)
      val loader      = factories(stage).create(client, stageConfig, principal)
      for {
        _ <- loader.run()
      } yield {
        log.info(s"Stage $stage completed")
        Done
      }
    } catch {
      case th: Throwable => Future.failed(th)
    }
}
