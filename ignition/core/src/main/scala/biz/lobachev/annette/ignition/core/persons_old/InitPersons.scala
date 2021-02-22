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

package biz.lobachev.annette.ignition.core.persons_old

import akka.Done
import akka.actor.ActorSystem
import biz.lobachev.annette.persons.api.PersonService
import org.slf4j.{Logger, LoggerFactory}
import pureconfig.ConfigSource
import pureconfig.generic.auto._

import scala.concurrent.{ExecutionContext, Future}

class InitPersons(
  val personService: PersonService,
  val actorSystem: ActorSystem,
  personLoader: PersonLoader,
  personCategoryLoader: PersonCategoryLoader,
  implicit val executionContext: ExecutionContext
) {
  final protected val log: Logger = LoggerFactory.getLogger(this.getClass)

  def run(): Future[Done] =
    ConfigSource.default
      .at("annette.init.persons")
      .load[InitPersonsConfig]
      .fold(
        failure => {
          val message = "Init Persons config load error"
          println(failure.prettyPrint())
          log.error(message, failure.prettyPrint())
          Future.failed(new RuntimeException(message))
        },
        config =>
          for {
            _ <- if (config.enableCategories) personCategoryLoader.load(config.categories, config.createdBy)
                 else Future.successful(Done)
            _ <- if (config.enablePersons) personLoader.load(config.persons, config.createdBy)
                 else Future.successful(Done)
          } yield Done
      )

}
