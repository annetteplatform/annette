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

package biz.lobachev.annette.ignition.persons.loaders

import akka.Done
import akka.stream.Materializer
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.ignition.core.EntityLoader
import biz.lobachev.annette.ignition.core.config.MODE_UPSERT
import biz.lobachev.annette.ignition.persons.loaders.data.PersonData
import biz.lobachev.annette.persons.api.PersonService
import biz.lobachev.annette.persons.api.person.{CreatePersonPayload, PersonAlreadyExist, UpdatePersonPayload}
import com.typesafe.config.Config
import io.scalaland.chimney.dsl._
import play.api.libs.json.Reads

import scala.concurrent.{ExecutionContext, Future}

class PersonEntityLoader(
  service: PersonService,
  val config: Config,
  val principal: AnnettePrincipal
)(implicit val ec: ExecutionContext, val materializer: Materializer)
    extends EntityLoader[PersonData] {

  override implicit val reads: Reads[PersonData] = PersonData.format

  def loadItem(item: PersonData, mode: String): Future[Either[Throwable, Done.type]] = {
    val createPayload = item
      .into[CreatePersonPayload]
      .withFieldConst(_.createdBy, principal)
      .transform
    service
      .createPerson(createPayload)
      .map(_ => Right(Done))
      .recoverWith {
        case PersonAlreadyExist(_) if mode == MODE_UPSERT =>
          val updatePayload = createPayload
            .into[UpdatePersonPayload]
            .withFieldComputed(_.updatedBy, _.createdBy)
            .transform
          service
            .updatePerson(updatePayload)
            .map(_ => Right(Done))
            .recover(th => Left(th))
        case th                                           => Future.failed(th)
      }

  }

}
