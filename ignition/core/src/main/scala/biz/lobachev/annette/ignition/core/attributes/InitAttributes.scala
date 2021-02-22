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

package biz.lobachev.annette.ignition.core.attributes

import akka.Done
import akka.actor.ActorSystem
import biz.lobachev.annette.attributes.api.attribute.PreparedAttribute
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.ignition.core.SequentialProcess
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.Json
import pureconfig.ConfigSource
import pureconfig.generic.auto._

import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import scala.util.Try

class InitAttributes(
  schemaLoader: SchemaLoader,
  attributeDataLoader: AttributeDataLoader,
  val actorSystem: ActorSystem,
  implicit val executionContext: ExecutionContext
) extends SequentialProcess {
  final protected val log: Logger = LoggerFactory.getLogger(this.getClass)

  def run(): Future[Done] =
    ConfigSource.default
      .at("annette.init.attributes")
      .load[InitAttributeData]
      .fold(
        failure => {
          val message = "Init Attributes config load error"
          log.error(message, failure.prettyPrint())
          Future.failed(new RuntimeException(message))
        },
        config =>
          if (config.enable)
            sequentialProcess(config.schemas) { schema =>
              log.debug("Processing schema {}/{} - {}", schema.id, schema.sub.getOrElse(" "), schema.name)
              (for {
                _ <- loadSchema(schema, config.createdBy)
                _ <- loadAttributeValues(schema, config.createdBy)
              } yield {
                log.debug("Complete processing schema {}/{} - {}", schema.id, schema.sub.getOrElse(" "), schema.name)
                ()
              }).recover(th =>
                log
                  .error("Failed to process schema {}/{} - {}", schema.id, schema.sub.getOrElse(" "), schema.name, th)
              )
            }.map(_ => Done)
          else Future.successful(Done)
      )

  def loadSchema(schema: SchemaData, principal: AnnettePrincipal): Future[Unit] = {
    val preparedAttributes = schema.schemaFile.map { filename =>
      val jsonTry = Try(Source.fromResource(filename).mkString)
      jsonTry.failed.map { th =>
        log.error("Schema load failed", th)
      }
      val json    = jsonTry.get
      val resTry  = Try(Json.parse(json).as[Set[PreparedAttribute]])
      resTry.failed.map { th =>
        log.error("Parsing schema json failed", th)
      }
      resTry.get
    }.getOrElse(Set.empty)
    if (preparedAttributes.nonEmpty)
      schemaLoader.load(schema.id, schema.sub, schema.name, preparedAttributes, principal)
    else
      Future.successful(())

  }

  def loadAttributeValues(schema: SchemaData, principal: AnnettePrincipal): Future[Unit] = {
    val values = schema.attrFile.map { filename =>
      val jsonTry = Try(Source.fromResource(filename).mkString)
      jsonTry.failed.map { th =>
        log.error("Attribute values load failed", th)
      }
      val json    = jsonTry.get
      val resTry  = Try(Json.parse(json).as[Seq[AttributeData]])
      resTry.failed.map { th =>
        log.error("Parsing attribute values json failed", th)
      }
      resTry.get
    }.getOrElse(Seq.empty)
    if (values.nonEmpty)
      attributeDataLoader.load(schema.id, schema.sub, schema.name, values, principal)
    else
      Future.successful(())

  }

}
