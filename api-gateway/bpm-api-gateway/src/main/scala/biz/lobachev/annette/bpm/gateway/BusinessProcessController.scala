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
import biz.lobachev.annette.bpm.gateway.bp._
import biz.lobachev.annette.bpm_repository.api.BpmRepositoryService
import biz.lobachev.annette.bpm_repository.api.bp._
import biz.lobachev.annette.bpm_repository.api.domain.BusinessProcessId
import io.scalaland.chimney.dsl._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class BusinessProcessController @Inject() (
  authenticated: AuthenticatedAction,
  authorizer: Authorizer,
  cc: ControllerComponents,
  bpmRepositoryService: BpmRepositoryService,
  implicit val ec: ExecutionContext,
  implicit val materializer: Materializer
) extends AbstractController(cc) {

  def createBusinessProcess =
    authenticated.async(parse.json[CreateBusinessProcessPayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_BPM) {
        val payload = request.body
          .into[CreateBusinessProcessPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          businessProcess <- bpmRepositoryService.createBusinessProcess(payload)
        } yield Ok(Json.toJson(businessProcess))
      }
    }

  def updateBusinessProcess =
    authenticated.async(parse.json[UpdateBusinessProcessPayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_BPM) {
        val payload = request.body
          .into[UpdateBusinessProcessPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          businessProcess <- bpmRepositoryService.updateBusinessProcess(payload)
        } yield Ok(Json.toJson(businessProcess))
      }
    }

  def updateBusinessProcessName =
    authenticated.async(parse.json[UpdateBusinessProcessNamePayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_BPM) {
        val payload = request.body
          .into[UpdateBusinessProcessNamePayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          businessProcess <- bpmRepositoryService.updateBusinessProcessName(payload)
        } yield Ok(Json.toJson(businessProcess))
      }
    }

  def updateBusinessProcessDescription =
    authenticated.async(parse.json[UpdateBusinessProcessDescriptionPayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_BPM) {
        val payload = request.body
          .into[UpdateBusinessProcessDescriptionPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          businessProcess <- bpmRepositoryService.updateBusinessProcessDescription(payload)
        } yield Ok(Json.toJson(businessProcess))
      }
    }

  def updateBusinessProcessBpmModel =
    authenticated.async(parse.json[UpdateBusinessProcessBpmModelPayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_BPM) {
        val payload = request.body
          .into[UpdateBusinessProcessBpmModelPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          businessProcess <- bpmRepositoryService.updateBusinessProcessBpmModel(payload)
        } yield Ok(Json.toJson(businessProcess))
      }
    }

  def updateBusinessProcessDataSchema =
    authenticated.async(parse.json[UpdateBusinessProcessDataSchemaPayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_BPM) {
        val payload = request.body
          .into[UpdateBusinessProcessDataSchemaPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          businessProcess <- bpmRepositoryService.updateBusinessProcessDataSchema(payload)
        } yield Ok(Json.toJson(businessProcess))
      }
    }

  def updateBusinessProcessProcessDefinition =
    authenticated.async(parse.json[UpdateBusinessProcessProcessDefinitionPayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_BPM) {
        val payload = request.body
          .into[UpdateBusinessProcessProcessDefinitionPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          businessProcess <- bpmRepositoryService.updateBusinessProcessProcessDefinition(payload)
        } yield Ok(Json.toJson(businessProcess))
      }
    }

  def storeBusinessProcessVariable =
    authenticated.async(parse.json[StoreBusinessProcessVariablePayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_BPM) {
        val payload = request.body
          .into[StoreBusinessProcessVariablePayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          businessProcess <- bpmRepositoryService.storeBusinessProcessVariable(payload)
        } yield Ok(Json.toJson(businessProcess))
      }
    }

  def deleteBusinessProcessVariable =
    authenticated.async(parse.json[DeleteBusinessProcessVariablePayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_BPM) {
        val payload = request.body
          .into[DeleteBusinessProcessVariablePayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          businessProcess <- bpmRepositoryService.deleteBusinessProcessVariable(payload)
        } yield Ok(Json.toJson(businessProcess))
      }
    }

  def deleteBusinessProcess =
    authenticated.async(parse.json[DeleteBusinessProcessPayloadDto]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_BPM) {
        val payload = request.body
          .into[DeleteBusinessProcessPayload]
          .withFieldConst(_.updatedBy, request.subject.principals.head)
          .transform
        for {
          _ <- bpmRepositoryService.deleteBusinessProcess(payload)
        } yield Ok("")
      }
    }

  def getBusinessProcess(id: String, withVariables: Option[Boolean] = None) =
    authenticated.async { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_BPM) {
        for {
          businessProcess <- bpmRepositoryService.getBusinessProcess(id, withVariables)
        } yield Ok(Json.toJson(businessProcess))
      }
    }

  def getBusinessProcesses(withVariables: Option[Boolean] = None) =
    authenticated.async(parse.json[Set[BusinessProcessId]]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_BPM) {
        for {
          businessProcesses <- bpmRepositoryService.getBusinessProcesses(request.body.toSeq, withVariables)
        } yield Ok(Json.toJson(businessProcesses))
      }
    }

  def findBusinessProcesses =
    authenticated.async(parse.json[BusinessProcessFindQuery]) { implicit request =>
      authorizer.performCheckAny(Permissions.MAINTAIN_BPM) {
        for {
          result <- bpmRepositoryService.findBusinessProcesses(request.body)
        } yield Ok(Json.toJson(result))
      }
    }

}
