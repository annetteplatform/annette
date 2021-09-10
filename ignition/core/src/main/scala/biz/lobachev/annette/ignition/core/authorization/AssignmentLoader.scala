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
import biz.lobachev.annette.authorization.api.role.AssignPrincipalPayload
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.ignition.core.FileBatchLoader
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class AssignmentLoader(
  authorizationService: AuthorizationService,
  implicit val materializer: Materializer,
  implicit val executionContext: ExecutionContext
) extends FileBatchLoader[AssignmentData] {

  protected val log: Logger = LoggerFactory.getLogger(this.getClass)

  val name = "Assignment"

  protected override def loadItem(
    item: AssignmentData,
    principal: AnnettePrincipal
  ): Future[Either[Throwable, Done.type]] = {
    val payload = AssignPrincipalPayload(
      roleId = item.roleId,
      principal = AnnettePrincipal(item.principalType, item.principalId),
      updatedBy = principal
    )

    authorizationService
      .assignPrincipal(payload)
      .map { _ =>
        log.debug(
          s"$name loaded: {} - {} {}",
          item.roleId,
          item.principalType,
          item.principalId
        )
        Right(Done)
      }
      .recoverWith {
        case th: IllegalStateException => Future.failed(th)
        case th                        =>
          log.error(s"Load $name failed: {} - {} {}", item.roleId, item.principalType, item.principalId, th)
          Future.successful(
            Left(th)
          )
      }

  }

}
