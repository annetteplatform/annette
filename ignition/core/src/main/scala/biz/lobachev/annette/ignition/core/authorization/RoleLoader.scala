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

package biz.lobachev.annette.ignition.core.authorization

import akka.Done
import akka.stream.Materializer
import biz.lobachev.annette.authorization.api.AuthorizationService
import biz.lobachev.annette.authorization.api.role.CreateRolePayload
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.ignition.core.FileBatchLoader
import io.scalaland.chimney.dsl.TransformerOps
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class RoleLoader(
  authorizationService: AuthorizationService,
  implicit val materializer: Materializer,
  implicit val executionContext: ExecutionContext
) extends FileBatchLoader[AuthRoleData] {

  protected val log: Logger = LoggerFactory.getLogger(this.getClass)

  val name = "AuthRole"

  protected override def loadItem(
    item: AuthRoleData,
    principal: AnnettePrincipal
  ): Future[Either[Throwable, Done.type]] = {
    val payload = item
      .into[CreateRolePayload]
      .withFieldConst(_.permissions, item.permissions.map(_.toPermission))
      .withFieldConst(_.createdBy, principal)
      .transform

    authorizationService
      .createOrUpdateRole(payload)
      .map { _ =>
        log.debug(
          s"$name loaded: {} - {}",
          item.id,
          item.name
        )
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
