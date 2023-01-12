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

package biz.lobachev.annette.bpm.gateway

import akka.stream.Materializer
import biz.lobachev.annette.api_gateway_core.authentication.AuthenticatedAction
import biz.lobachev.annette.api_gateway_core.authorization.Authorizer
import biz.lobachev.annette.bpm.gateway.repository.{BusinessProcessPayload, CamundaDeployPayload, DeployResponse}
import biz.lobachev.annette.bpm_repository.api.BpmRepositoryService
import biz.lobachev.annette.bpm_repository.api.bp.{
  BusinessProcess,
  CreateBusinessProcessPayload,
  UpdateBusinessProcessPayload
}
import biz.lobachev.annette.bpm_repository.api.domain.{ProcessDefinition, ProcessDefinitionType}
import biz.lobachev.annette.bpm_repository.api.model.BpmModel
import biz.lobachev.annette.camunda.api.RepositoryService
import biz.lobachev.annette.camunda.api.repository.CreateDeploymentPayload
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import io.scalaland.chimney.dsl._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CamundaRepositoryController @Inject() (
  authenticated: AuthenticatedAction,
  authorizer: Authorizer,
  cc: ControllerComponents,
  bpmRepositoryService: BpmRepositoryService,
  camundaRepositoryService: RepositoryService,
  implicit val ec: ExecutionContext,
  implicit val materializer: Materializer
) extends AbstractController(cc) {

  def deployBpmModel =
    authenticated.async(parse.json[CamundaDeployPayload]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_BPM) {
        for {
          bpmModel   <- bpmRepositoryService.getBpmModel(request.body.bpmModelId.value, Some(true))
          deployment <- camundaRepositoryService.createDeployment(
                          CreateDeploymentPayload(
                            xml = bpmModel.xml.get,
                            deploymentName = Some(bpmModel.id.value),
                            deploymentSource = Some("annette"),
                            deployChangedOnly = Some(true)
                          )
                        )
          bp         <- request.body.businessProcess
                          .map(bp =>
                            createBusinessProcess(bpmModel, bp, request.subject.principals.head)
                              .map(Some(_))
                          )
                          .getOrElse(Future.successful(None))
        } yield Ok(Json.toJson(DeployResponse(deployment, bp)))
      }
    }

  private def createBusinessProcess(
    bpmModel: BpmModel,
    bp: BusinessProcessPayload,
    principal: AnnettePrincipal
  ): Future[BusinessProcess] =
    for {
      res <- if (bp.action == "create") {
               val payload = bp
                 .into[CreateBusinessProcessPayload]
                 .withFieldConst(_.updatedBy, principal)
                 .withFieldConst(_.bpmModelId, Some(bpmModel.id))
                 .withFieldConst(_.processDefinition, ProcessDefinition(bpmModel.code))
                 .withFieldConst(_.processDefinitionType, ProcessDefinitionType.KEY)
                 .transform
               bpmRepositoryService.createBusinessProcess(payload)
             } else {
               val payload = bp
                 .into[UpdateBusinessProcessPayload]
                 .withFieldConst(_.updatedBy, principal)
                 .withFieldConst(_.bpmModelId, Some(bpmModel.id))
                 .withFieldConst(_.processDefinition, ProcessDefinition(bpmModel.code))
                 .withFieldConst(_.processDefinitionType, ProcessDefinitionType.KEY)
                 .transform
               bpmRepositoryService.updateBusinessProcess(payload)
             }
    } yield res

}
