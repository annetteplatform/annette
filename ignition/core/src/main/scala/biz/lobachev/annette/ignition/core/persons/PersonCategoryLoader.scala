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

package biz.lobachev.annette.ignition.core.persons

import akka.Done
import biz.lobachev.annette.ignition.core.EntityLoader
import akka.stream.Materializer
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.core.model.category.CreateCategoryPayload
import biz.lobachev.annette.persons.api.PersonService
import io.scalaland.chimney.dsl._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class PersonCategoryLoader(
  personService: PersonService,
  implicit val materializer: Materializer,
  implicit val executionContext: ExecutionContext
) extends EntityLoader[PersonCategoryData] {

  protected val log: Logger = LoggerFactory.getLogger(this.getClass)

  val name = "Category"

  override def loadItem(
    item: PersonCategoryData,
    principal: AnnettePrincipal
  ): Future[Either[Throwable, Done.type]] = {
    val payload = item
      .into[CreateCategoryPayload]
      .withFieldConst(_.createdBy, principal)
      .transform

    personService
      .createOrUpdateCategory(payload)
      .map { _ =>
        log.debug(s"$name loaded: {}", item.id)
        Right(Done)
      }
      .recoverWith {
        case th: IllegalStateException => Future.failed(th)
        case th                        =>
          log.error(s"Load $name {} failed", item.id, th)
          Future.successful(
            Left(th)
          )
      }
  }

}
